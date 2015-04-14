/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.data.elastic.ElasticSpacetime;
import automenta.climatenet.data.gis.KmlReader;
import automenta.climatenet.data.gis.UrlRef;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.giscore.events.ContainerEnd;
import org.opensextant.giscore.events.ContainerStart;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.events.Pair;
import org.opensextant.giscore.events.Schema;
import org.opensextant.giscore.events.SimpleField;
import org.opensextant.giscore.events.SimpleField.Type;
import org.opensextant.giscore.events.Style;
import org.opensextant.giscore.events.StyleMap;
import org.opensextant.giscore.events.StyleSelector;
import org.opensextant.giscore.geometry.Geometry;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.utils.Color;

/**
 *
 * TODO - remove null descriptions - store HTML content separately so it does
 * not get indexed - max token size
 *
 * @see https://github.com/OpenSextant/giscore/wiki
 * @author me
 */
public class ImportKML implements Runnable {

    static final HtmlCompressor compressor = new HtmlCompressor();

    private final Proxy proxy;

    public static String getSerial(String layer, int serial) {
        return (Base64.getEncoder().encodeToString(BigInteger.valueOf(serial).add(BigInteger.valueOf(layer.hashCode()).shiftRight(32)).toByteArray()));
    }
    private final ElasticSpacetime st;
    private final String id;
    private final String url;
    int serial;

    int numFeatures = 0;
    private String layer;
    private int BULK_SIZE = 1000;

    public String[] getPath(Deque<String> p) {
        return p.toArray(new String[p.size()]);
    }

    public static interface GISVisitor {

        public boolean on(IGISObject go, String[] path) throws IOException;

        public void start(String layer);

        public void end();
    }

    Deque<String> path = new ArrayDeque();

    public void transformKML(String layer, String urlString, ElasticSpacetime st, final GISVisitor visitor) throws Exception {
        URL url = new URL(urlString);
        this.layer = layer;

        KmlReader reader = new KmlReader(url, proxy);
        reader.setRewriteStyleUrls(true);

        SimpleField layerfield = new SimpleField("layer", Type.STRING);
        layerfield.setLength(32);

        final AtomicInteger exceptions = new AtomicInteger();
        final Set<Class> exceptionClass = new HashSet();

        serial = 1;
        path.clear();

        visitor.start(layer);

        do {

            //for (IGISObject go; (go = reader.read()) != null;) {
            // do something with the gis object; e.g. check for placemark, NetworkLink, etc.
            try {

                IGISObject go = reader.read();
                if (go == null) {
                    break;
                }

                if (go instanceof DocumentStart) {
                    DocumentStart ds = (DocumentStart) go;

                    path.add(layer);

                }

                if (go instanceof ContainerEnd) {
                    path.removeLast();
                }

                if ((go instanceof ContainerStart) || (go instanceof Feature)) {
                    serial++;
                }

                if (!visitor.on(go, getPath(path))) {
                    break;
                }

                //add to the path after container start is processed
                if (go instanceof ContainerStart) {
                    ContainerStart cs = (ContainerStart) go;
                    //TODO startTime?
                    //System.out.println(cs + " " + cs.getId());
                    String i = getSerial(layer, serial);
                    path.add(i);
                }

            } catch (Throwable t) {
                System.err.println(t);
                exceptions.incrementAndGet();
                exceptionClass.add(t.getClass());
            }
        } while (true);

// get list of network links that were retrieved from step above
        Set<URI> networkLinks = new HashSet(reader.getNetworkLinks());

        reader.close();

        if (!networkLinks.isEmpty()) {

            // Now import features from all referenced network links.
            // if Networklinks have nested network links then they will be added to end
            // of the list and processed one after another. The handleEvent() callback method
            // below will be called with each feature (i.e. Placemark, GroundOverlay, etc.)
            // as it is processed in the target KML resources.
            reader.importFromNetworkLinks(
                    new KmlReader.ImportEventHandler() {
                        public boolean handleEvent(UrlRef ref, IGISObject gisObj) {

            // if gisObj instanceOf Feature, GroundOverlay, etc.
                            // do something with the gisObj
                            // return false to abort the recursive network link parsing
                            /*if (visited.contains(ref))
                             return false;*/
                            //System.out.println("Loading NetworkLink: " + ref + " " + gisObj);
                            String r = ref.toString();
                            boolean pathChanged = false;
                            if (!(!path.isEmpty()) && (path.getLast().equals(r))) {
                                path.add(ref.toString());
                                pathChanged = true;
                            }

                            serial++;

                            try {
                                visitor.on(gisObj, getPath(path));
                            } catch (Throwable t) {
                                System.err.println(t);
                            }

                            if (pathChanged) {
                                path.removeLast();
                            }

                            return true;
                        }

                        @Override
                        public void handleError(URI uri, Exception excptn) {
                            exceptions.incrementAndGet();
                            exceptionClass.add(excptn.getClass());
                        }

                    });

        }

        if (exceptions.get() > 0) {
            System.err.println("  Exceptions: " + exceptions + " of " + exceptionClass);
        }

        visitor.end();

    }

    public static void exec(String cmd) {
        try {
            String[] cmdParm = {"/bin/sh", "-c", cmd};

            Process proc = Runtime.getRuntime().exec(cmdParm);
            IOUtils.copy(proc.getInputStream(), System.out);
            IOUtils.copy(proc.getErrorStream(), System.err);
            proc.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void toPostgres(String layerName, String inputFile, String host, String user, String db) {
        exec("ogr2ogr -update -append -skipFailures -f \"PostgreSQL\" PG:\"host=" + host + " user=" + user + " dbname=" + db + "\" " + inputFile + " -nln " + layerName);
    }

    public static void toGeoJSON(String inputFile, String outputFile) {
//        ogr2ogr -f "GeoJSON" output.json input.kml
        exec("rm -f " + outputFile);
        exec("ogr2ogr -f GeoJSON -skipFailures " + outputFile + " " + inputFile);
    }

    final public static ObjectMapper json = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true)
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static ObjectNode fromJSON(String x) {
        try {

            return json.readValue(x, ObjectNode.class);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ImportKML(ElasticSpacetime st, Proxy proxy, String id, String url) {
        this.st = st;
        this.proxy = proxy;
        this.id = id;
        this.url = url;
    }

    @Override
    public void run() {
        try {
            final Map<String, Style> styles = new HashMap();

            //1. pre-process: collect style information
            transformKML(id, url, st, new GISVisitor() {

                Map<String, String> styleMap = new HashMap();

                @Override
                public void start(String layer) {
                }

                protected void onStyle(Style s) {
                    String id = s.getId();
                    styles.put(id, s);
                }

                protected void onStyleMap(StyleMap ss) {

                    String ssid = ss.getId();
                    if (ssid == null) {
                        System.err.println("null id: " + ss);
                        return;
                    }
                    Pair p = ss.getPair(StyleMap.NORMAL);
                    if (p.getStyleSelector() instanceof Style) {
                        styles.put(ssid, ((Style) p.getStyleSelector()));
                    } else if (p.getStyleSelector() instanceof StyleMap) {
                        //System.out.println("Unmanaged StyleMap: " + p);
                        styleMap.put(ssid, p.getStyleUrl());
                    }

                    //TODO highlight?
                }

                @Override
                public boolean on(IGISObject go, String[] path) throws IOException {

                    if (go instanceof Style) {
                        onStyle((Style) go);
                    } else if (go instanceof StyleMap) {
                        onStyleMap((StyleMap) go);
                    }
                    if (go instanceof ContainerStart) {
                        ContainerStart cs = (ContainerStart) go;

                        for (StyleSelector ss : cs.getStyles()) {
                            if (ss instanceof Style) {
                                onStyle((Style) ss);
                            } else if (ss instanceof StyleMap) {
                                onStyleMap((StyleMap) ss);
                            }
                        }
                    }

                    return true;
                }

                @Override
                public void end() {
                    for (Map.Entry<String, String> e : styleMap.entrySet()) {
                        String from = e.getKey();
                        String to = e.getValue();
                        Style toStyle = styles.get(to);
                        if (toStyle == null) {
                            System.err.println("Missing style: " + to);
                            continue;
                        }
                        styles.put(from, toStyle);
                    }
                }

            });

            System.out.println(layer + " STYLES:  \n" + styles.keySet());

            //2. process features
            transformKML(id, url, st, new GISVisitor() {

                private BulkRequestBuilder bulk = null;

                @Override
                public void start(String layer) {
                    bulk = st.newBulk();
                }

                @Override
                public boolean on(IGISObject go, String[] path) throws IOException {
                    if (go == null) {
                        return false;
                    }

                    if (st == null) {
                        return false;
                    }

                    if (go instanceof ContainerStart) {
                        ContainerStart cs = (ContainerStart) go;
                        //TODO startTime?
                        //System.out.println(cs + " " + cs.getId());

                        XContentBuilder d;
                        d = jsonBuilder().startObject().field("name", cs.getName());

                        /*String styleUrl = cs.getStyleUrl();
                         if (styleUrl != null) {
                         if (styleUrl.startsWith("#")) {
                         styleUrl = styleUrl.substring(1);
                         }
                         styleUrl = layer + "_" + styleUrl;
                         System.err.println("Container styleUrl: " + styleUrl);
                         d.field("styleUrl", styleUrl);
                         }
                         */
                        String desc = cs.getDescription();
                        if ((desc != null) && (desc.length() > 0)) {
                            //filter 
                            desc = filterHTML(desc);
                            if (desc.length() > 0) {
                                d.field("description", desc);
                            }
                        }

                        d.field("path", path).endObject();

                        String i = getSerial(layer, serial);
                        bulk = st.add(bulk, "tag", i, d);

                        bulk = updateBulk();
                    }

                    if (go instanceof Feature) {
                        Feature f = (Feature) go;

                        XContentBuilder fb = jsonBuilder().startObject();
                        fb.field("name", f.getName());

                        String desc = f.getDescription();
                        if ((desc != null) && (desc.length() > 0)) {
                            //filter 
                            desc = filterHTML(desc);
                            if (desc.length() > 0) {
                                fb.field("description", desc);
                            }
                        }

                        if (f.getSnippet() != null) {
                            if (f.getSnippet().length() > 0) {
                                fb.field("snippet", f.getSnippet());
                            }
                        }
                        if (f.getStartTime() != null) {
                            fb.field("startTime", f.getStartTime().getTime());
                        }
                        if (f.getEndTime() != null) {
                            fb.field("endTime", f.getEndTime().getTime());
                        }

                        Geometry geo = f.getGeometry();
                        if (geo != null) {
                            if (geo instanceof Point) {
                                Point pp = (Point) geo;
                                Geodetic2DPoint c = pp.getCenter();
                                double lat = c.getLatitudeAsDegrees();
                                double lon = c.getLongitudeAsDegrees();

                                //http://geojson.org/
                                fb.startObject("geom").field("type", "point").field("coordinates", new double[]{lon, lat}).endObject();

                            } else if (geo instanceof org.opensextant.giscore.geometry.Line) {
                                org.opensextant.giscore.geometry.Line l = (org.opensextant.giscore.geometry.Line) geo;

                                List<Point> lp = l.getPoints();
                                double[][] points = toArray(lp);

                                fb.startObject("geom").field("type", "linestring").field("coordinates", points).endObject();

                            } else if (geo instanceof org.opensextant.giscore.geometry.Polygon) {
                                org.opensextant.giscore.geometry.Polygon p = (org.opensextant.giscore.geometry.Polygon) geo;
                                
                                double[][] outerRing = toArray(p.getOuterRing().getPoints());
                                //TODO handle inner rings

                                fb.startObject("geom").field("type", "polygon").field("coordinates", new double[][][] {outerRing /* inner rings */}).endObject();
                            }
                            
                            //TODO other types
                        }

                        Style styleInline = null;
                        if (f.getStyle() != null) {

                            if (f.getStyle() instanceof Style) {

                                Style ss = (Style) f.getStyle();
                                styleInline = ss;

                            } else if (f.getStyle() instanceof StyleMap) {
                                StyleMap ss = (StyleMap) f.getStyle();
                                styleInline = styles.get(ss.getId());
                                if (styleInline == null) {
                                    System.err.println("Missing: " + ss.getId());
                                }
                            }

                        }

                        if ((f.getStyleUrl() != null) || (styleInline != null)) {

                            fb.startObject("style");

                            if (f.getStyleUrl() != null) {
                                String su = f.getStyleUrl();
                                if (su.startsWith("#")) {
                                    su = su.substring(1);
                                }

                                Style s = styles.get(su);
                                if (s == null) {
                                    //System.err.println("Missing: " + f.getStyleUrl());
                                } else {
                                    styleJson(fb, s);
                                }
                            }

                            if (styleInline != null) {
                                styleJson(fb, styleInline);
                            }

                            fb.endObject();

                        }

                        fb.field("path", path);

                        String fid = getSerial(layer, serial);
                        bulk = st.add(bulk, "feature", fid, fb.endObject());
                        numFeatures++;

                        bulk = updateBulk();
                    }

                    if (go instanceof Schema) {
                        //..
                    }

                    return true;
                }

                @Override
                public void end() {
                    st.commit(bulk);
                    bulk = null;
                }

                private BulkRequestBuilder updateBulk() {
                    if (bulk.numberOfActions() < BULK_SIZE) {
                        return bulk;
                    } else {
                        st.commit(bulk);
                        bulk = null;
                        return null;
                    }
                }

            });

        } catch (Throwable e) {
            //e.printStackTrace();;
            System.err.println(e);
        }

    }

    static {
        //https://code.google.com/p/htmlcompressor/wiki/Documentation#Using_HTML_Compressor_from_Java_API

        compressor.setRemoveComments(true);            //if false keeps HTML comments (default is true)
        compressor.setRemoveMultiSpaces(true);         //if false keeps multiple whitespace characters (default is true)
        compressor.setRemoveIntertagSpaces(true);      //removes iter-tag whitespace characters
        compressor.setRemoveQuotes(true);              //removes unnecessary tag attribute quotes
        compressor.setSimpleDoctype(true);             //simplify existing doctype
        compressor.setRemoveScriptAttributes(true);    //remove optional attributes from script tags

        compressor.setRemoveLinkAttributes(true);      //remove optional attributes from link tags
        compressor.setRemoveJavaScriptProtocol(true);      //remove optional attributes from link tags
        compressor.setRemoveHttpProtocol(true);        //replace "http://" with "//" inside tag attributes
        compressor.setRemoveHttpsProtocol(true);       //replace "https://" with "//" inside tag attributes
        compressor.setRemoveSurroundingSpaces("br,p"); //remove spaces around provided tags
        compressor.setCompressCss(false);               //compress inline css 

    }

    private String filterHTML(String html) {

        try {
            String compressedHtml = compressor.compress(html);

            return compressedHtml;
        } catch (Exception e) {
            return html;
        }

    }

    XContentBuilder stylemapJson(XContentBuilder fb, StyleMap s) throws IOException {
        Pair normal = s.getPair(StyleMap.NORMAL);
        //System.out.println(normal.getStyleUrl() + " " + normal.getStyleSelector());
        return fb;
    }

    XContentBuilder styleJson(XContentBuilder fb, Style s) throws IOException {

        //System.out.println("Applying style: " + s);
        String iconUrl = s.getIconUrl();
        if (iconUrl != null) {
            fb.field("iconUrl", iconUrl);
        }

        String baloonText = s.getBalloonText();
        if (baloonText != null) {
            fb.field("baloonText", baloonText);
        }

        /*Color iconColor = s.getIconColor();
         if (iconColor!=null)
         fb.field("iconColor", iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), iconColor.getAlpha() );
        
        
         Color lineColor = s.getLineColor();
         if (lineColor!=null)
         fb.field("lineColor", new Integer[] { lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha() } );
        
         Color polyColor = s.getPolyColor();
         if (polyColor!=null)
         fb.field("polyColor", "rgba(" + polyColor.getRed() + "," + polyColor.getGreen()+ "," + polyColor.getBlue()+ "," + polyColor.getAlpha() + ")"  );
         */
        Color polyColor = s.getPolyColor();
        if (polyColor != null) {
            fb.field("polyColor", polyColor.toString().substring("org.opensextant.giscore.utils.".length()));
        }

        Double lineWidth = s.getLineWidth();
        if (lineWidth != null) {
            fb.field("lineWidth", lineWidth);
        }

        return fb;

    }

//    /*
//    private synchronized boolean processStyle(Style s) throws IOException {
//        String id = s.getId();
//
//        String fullStyleId = layer + "_" + id;
//
//        System.err.println("processStyle: " + s.getId() + " -> " + fullStyleId);
//
//        XContentBuilder fb = jsonBuilder().startObject();
//        styleJson(fb, s);
//        fb.endObject();
//
//        //use '_' instead of '#' which is reserved for URL encoding
//        bulk = st.add(bulk, "style", fullStyleId, fb);
//        updateBulk();
//
//        /*else {
//         System.out.println(s);
//         }*/
//        return true;
//
//    }
    /*
     private String processStyleMap(StyleMap styleMap) throws IOException {
     //System.out.println(styleMap.getId() + " " + styleMap.getPair(StyleMap.NORMAL));
     if (styleMap.getPair(StyleMap.NORMAL).getStyleSelector() != null) {
     processStyle((Style) styleMap.getPair(StyleMap.NORMAL).getStyleSelector());
     }
     String normalURL = styleMap.getPair(StyleMap.NORMAL).getStyleUrl();
     if (normalURL.startsWith("#")) {
     normalURL = normalURL.substring(1);
     }
     normalURL = layer + "_" + normalURL;
     return normalURL;
     }
     */
    public static double[][] toArray(List<Point> lp) {
        double[][] points = new double[lp.size()][2];
        
        for (int i = 0; i < points.length; i++) {
            Geodetic2DPoint c = lp.get(i).getCenter();
            points[i][0] = c.getLongitudeAsDegrees();
            points[i][1] = c.getLatitudeAsDegrees();
        }
        return points;
    }

}
