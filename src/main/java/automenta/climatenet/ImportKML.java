/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.elastic.ElasticSpacetime;
import automenta.climatenet.kml.giscore.KmlReader;
import automenta.climatenet.kml.giscore.UrlRef;
import automenta.climatenet.proxy.ProxyServer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.mapper.MapperBuilders.id;
import org.opensextant.geodesy.Geodetic2DPoint;
import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.data.DocumentTypeRegistration;
import org.opensextant.giscore.data.FactoryDocumentTypeRegistry;
import org.opensextant.giscore.events.ContainerEnd;
import org.opensextant.giscore.events.ContainerStart;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Element;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.events.Schema;
import org.opensextant.giscore.events.SimpleField;
import org.opensextant.giscore.events.SimpleField.Type;
import org.opensextant.giscore.geometry.Geometry;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.output.IGISOutputStream;
import org.opensextant.giscore.output.kml.KmlOutputStream;
import org.opensextant.giscore.output.shapefile.ShapefileOutputStream;



/**
 * 
 * TODO 
 *  - remove null descriptions
 *  - store HTML content separately so it does not get indexed
 *  - max token size
 * 
 * @see https://github.com/OpenSextant/giscore/wiki
 * @author me
 */
public class ImportKML implements Runnable {

        
    static final HtmlCompressor compressor = new HtmlCompressor();
    
    private final Proxy proxy;
    

    public static String getSerial(String layer, int serial ) {
        return (Base64.getEncoder().encodeToString(BigInteger.valueOf( serial ).add( BigInteger.valueOf(layer.hashCode()).shiftRight(32) ).toByteArray()));        
    }
    private final ElasticSpacetime st;
    private final String id;
    private final String name;
    private final String url;

    
    public String[] getPath(Deque<String> p) {
        return p.toArray(new String[p.size()]);
    }
    
    public int transformKML(String layer, String name, String urlString, ElasticSpacetime st, boolean esri, boolean kml) throws Exception {
        URL url = new URL(urlString);
        Deque<String> path = new ArrayDeque();
        
        /*Logger.getLogger(KmlInputStream.class).setLevel(Level.OFF);
Logger.getLogger(AltitudeModeEnumType.class).setLevel(Level.OFF);*/
        
        
        KmlReader reader = new KmlReader(url, proxy);
        reader.setRewriteStyleUrls(true);
        
        int numFeatures = 0;
        
        
        File temp = Files.createTempDirectory("kml" + layer).toFile();
        
        String p = null;
        //String p = basePath + "/" + layer;
        
        //Files.deleteIfExists(Paths.get(p));
        //if (!Files.exists(Paths.get(p)))
          //  Files.createDirectory(Paths.get(p));
        
        
        IGISOutputStream shpos = null;
        ZipOutputStream esriOut = null;

        SimpleField layerfield = new SimpleField("layer", Type.STRING);
        layerfield.setLength(32);
        SimpleField descriptionField = new SimpleField("description", Type.STRING);
        SimpleField addressField = new SimpleField("address", Type.STRING);
        
        int serial = 1;
        
        if (esri) {
            String outputFile = p + "/" + layer + ".shape.zip";
            if (Files.exists(Paths.get(outputFile)))
                Files.delete(Paths.get(outputFile));
            esriOut = new ZipOutputStream(new FileOutputStream(outputFile));
            
            DocumentTypeRegistration docreg = FactoryDocumentTypeRegistry.get(DocumentType.Shapefile);
            
            shpos = new ShapefileOutputStream(esriOut, new Object[] { temp }) {

                
            };
            

            //shpos = GISFactory.getOutputStream(DocumentType.Shapefile, esriOut, temp);
            
            
            System.out.println("  ESRI Shapefile: Output file: " + outputFile);
            System.out.println("  ESRI Shapefile: Temporary folder: " + temp);
        
            Schema schema = new Schema(new URI("urn:climateviewer"));
            schema.put(layerfield);
            schema.put(addressField);
            schema.put(descriptionField);
            
            DocumentStart ds = new DocumentStart(DocumentType.Shapefile);
            shpos.write(ds);
            
            ContainerStart cs = new ContainerStart("Folder");
            cs.setName(layer);
            shpos.write(cs);
            shpos.write(schema);
        }
        
        KmlOutputStream kout = null;
        String kmlFile = p + "/" + layer + ".kml";
        String geojsonFile = p + "/" + layer + ".geojson";
        
        if (kml) {
            
            
            
            FileOutputStream fout = new FileOutputStream(new File(kmlFile));
            
            //String cmd = "ogr2ogr -F GeoJSON " + geojsonoutput + " /vsistdin/";
             //ogr2ogr -update -append -f "PostGreSQL" PG:"host=localhost user=me dbname=cv" ccr01.kml
            //String cmd = "ogr2ogr -update -append -skipFailures -F \"PostGreSQL\" PG:\"host=localhost user=me dbname=cv\" /vsistdin/";
            //String[] cmdParm = { "/bin/sh", "-c", cmd };
            
            
            //jsonproc = Runtime.getRuntime().exec(cmdParm);            
            
                        
            kout = new KmlOutputStream(fout, "UTF-8");
    
            
            //TODO
            //tee out to a pipe that executes:
            //  {cat cvr01.kml } | ogr2ogr -F GeoJSON cvr01.json /vsistdin/
            //     | ogr2ogr -update -append -f "PostGreSQL" PG:"host=localhost user=me dbname=cv" /vsistdin/
        }
        
        
        
        if (st!=null) {
            st.bulkStart();
        }
	
        
        final AtomicInteger exceptions = new AtomicInteger();
        final Set<Class> exceptionClass = new HashSet();
        
        do {
        
        //for (IGISObject go; (go = reader.read()) != null;) {
  // do something with the gis object; e.g. check for placemark, NetworkLink, etc.
            try {
                IGISObject go = reader.read();
                if (go == null)
                    break;

                if (st!=null) {
                    if (go instanceof DocumentStart) {
                        DocumentStart ds = (DocumentStart)go;
                        XContentBuilder d;

                        //System.out.println("Document " + ds.getType().name());
                        d = jsonBuilder().startObject()
                                .field("url", urlString)
                                .field("name", name);
                        d.endObject();

                        st.add("tag", layer, d);
                        
                        path.add(layer);


                    }                
                    else if (go instanceof ContainerEnd) {
                        path.removeLast();  
                    }
                    else if (go instanceof ContainerStart) {
                        ContainerStart cs = (ContainerStart)go;
                        //TODO startTime?
                        //System.out.println(cs + " " + cs.getId());
                        String i = getSerial(layer, serial++);

                        XContentBuilder d;
                        d = jsonBuilder().startObject().field("name", cs.getName());
                        
                        String desc = cs.getDescription();
                        if ((desc!=null) && (desc.length() > 0)) {
                            //filter 
                            desc = filterHTML(desc);
                            if (desc.length() > 0)
                                d.field("description", desc);
                        }
                        
                        d.field("path", getPath(path)).endObject();

                        path.add(i);


                        st.add("tag", i, d);

                    }
                    else if (go instanceof Feature) {
                        Feature f = (Feature)go;                


                        XContentBuilder fb = jsonBuilder().startObject();
                        fb.field("name", f.getName());
                        
                        String desc = f.getDescription();
                        if ((desc!=null) && (desc.length() > 0)) {
                            //filter 
                            desc = filterHTML(desc);
                            if (desc.length() > 0)
                                fb.field("description", desc);
                        }
                        
                        if (f.getSnippet()!=null)
                            fb.field("snippet", f.getSnippet());
                        if (f.getStartTime()!=null)
                            fb.field("startTime", f.getStartTime().getTime());
                        if (f.getEndTime()!=null)
                            fb.field("endTime", f.getEndTime().getTime());

                        Geometry geo = f.getGeometry();
                        if (geo!=null) {
                            if (geo instanceof Point) {
                                Point pp = (Point)geo;
                                Geodetic2DPoint c = pp.getCenter();
                                float lat = (float)c.getLatitudeAsDegrees();
                                float lon = (float)c.getLongitudeAsDegrees();

                                //http://geojson.org/
                                fb.startObject("geom").field("type", "point").field("coordinates", new float[] { lon, lat } ).endObject();

                            }
                            /*else if (g instanceof Line) {

                            }*/
                            //TODO other types
                        }
                        if (f.getStyleUrl()!=null)
                            fb.field("styleUrl", f.getStyleUrl());

                        fb.field("path", getPath(path));
                        //f.getStyleUrl()

                        String fid = getSerial(layer, serial++);
                        st.add("feature", fid, fb.endObject());
                        numFeatures++;
                    }
                }

                if (esri) {
                    if (go instanceof Feature) {
                        Feature f = (Feature)go;                

                        if (!f.getFields().isEmpty()) {
                            System.out.println("Fields: " + f.getFields());
                        }
                        if (!f.getExtendedElements().isEmpty()) {
                            System.out.println("Extended Elements: " + f.getExtendedElements());
                        }
                        for (Element e : f.getElements()) {
                            String n = e.getName();
                            switch (n) {
                                case "address":
                                    f.putData(addressField, e.getText());
                                    break;
                                default:
                                    System.err.println("Unknown element: " + e);
                                    break;
                            }

                        }
                        String x = f.getDescription();
                        if (x!=null) {
                            f.putData(descriptionField, x);
                        }


                        //f.putData(null, kout);
                        //System.out.println(gisObj);
                    }                
                    else {
                        //System.err.println(gisObj.getClass() + " not handled");                     
                    }

                    shpos.write(go);  
                }
                if (kml) {
                    kout.write(go);
                }
            }
            catch (Throwable t) {
                exceptions.incrementAndGet();
                exceptionClass.add(t.getClass());            
            }
        } while (true);
        
        
        
// get list of network links that were retrieved from step above
        
        List<URI> networkLinks = reader.getNetworkLinks();
        
        reader.close();

        final Set<UrlRef> visited = new HashSet();
        
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
                            if (visited.contains(ref))
                                return false;
                            
                            System.out.println("Loading NetworkLink: " + ref);
                            visited.add(ref);
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
            System.err.println("  Exceptions: " + exceptions + " of " + exceptionClass );
        }

        if (st!=null) {
            st.bulkEnd();
        }
        
        if (esri) {
            ContainerEnd ce = new ContainerEnd();            
            shpos.write(ce);
            
            
            try {
                shpos.close();
            }
            catch (Exception e) {
                System.err.println(e);
            }

            esriOut.flush();
            esriOut.close();	
        }
        if (kml) {            
            kout.close();
            toGeoJSON(kmlFile, geojsonFile);
            toPostgres(layer, geojsonFile, "localhost", "me", "cv");
        }

        
        return numFeatures;

        
    }
    
    public static void exec(String cmd) {
        try {
            String[] cmdParm = { "/bin/sh", "-c", cmd };
            
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
    
    public static void toGeoJSON(String inputFile, String outputFile){
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
    public ImportKML(ElasticSpacetime st, Proxy proxy, String id, String name, String url) {
        this.st = st;
        this.proxy = proxy;
        this.id = id;
        this.name = name;
        this.url = url;
    }
    
    
    
    
    @Override
    public void run() {
        try {
            //logger.info(this + " run(): " + id);
            int features = transformKML(id, name, url, st, false, false);

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
compressor.setCompressCss(true);               //compress inline css 

    }

    private String filterHTML(String html) {
        
        
        try {
            String compressedHtml = compressor.compress(html);

            return compressedHtml;
        }
        catch (Exception e) {
            return html;
        }
        
    }
}
