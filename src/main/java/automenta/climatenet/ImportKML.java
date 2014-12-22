/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.proxy.ProxyServer;
import automenta.knowtention.Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
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
import org.opensextant.giscore.input.kml.KmlReader;
import org.opensextant.giscore.input.kml.UrlRef;
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
public class ImportKML {

    String basePath = "data";
    String layersFile = "src/main/java/automenta/climatenet/cvlayers.json";
    
    
    private final Proxy proxy;
    
    public static String getSerial(String layer, int serial ) {
        return (Base64.getEncoder().encodeToString(BigInteger.valueOf( serial ).add( BigInteger.valueOf(layer.hashCode()).shiftLeft(32) ).toByteArray()));        
    }
    
    public void transformKML(String layer, String urlString, ElasticSpacetime st, boolean esri, boolean kml) throws Exception {
        URL url = new URL(urlString);
        Deque<String> groups = new ArrayDeque();
        String[] g = null;
        
        Logger.getLogger(org.opensextant.giscore.input.kml.KmlInputStream.class).setLevel(Level.OFF);
Logger.getLogger(org.opensextant.giscore.events.AltitudeModeEnumType.class).setLevel(Level.OFF);
        
        
        KmlReader reader = new KmlReader(url, proxy);
        reader.setRewriteStyleUrls(true);
        
        
        
        
        File temp = Files.createTempDirectory("kml" + layer).toFile();
        
        String p = basePath + "/" + layer;
        
        //Files.deleteIfExists(Paths.get(p));
        if (!Files.exists(Paths.get(p)))
            Files.createDirectory(Paths.get(p));
        
        
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
                        d = jsonBuilder().startObject().field("url", urlString);
                        d.endObject();

                        st.add("layer", layer, d);

                        groups.clear();
                        g = groups.toArray(new String[groups.size()]);

                    }                
                    else if (go instanceof ContainerEnd) {
                        groups.pop();  
                        g = groups.toArray(new String[groups.size()]);
                    }
                    else if (go instanceof ContainerStart) {
                        ContainerStart cs = (ContainerStart)go;
                        //TODO startTime?
                        //System.out.println(cs + " " + cs.getId());
                        String i = getSerial(layer, serial++);

                        XContentBuilder d;
                        d = jsonBuilder().startObject().field("name", cs.getName()).field("description", cs.getDescription()).field("path", g).field("layer", layer).endObject();

                        groups.add(i);

                        g = groups.toArray(new String[groups.size()]);

                        st.add("layer", i, d);

                    }
                    else if (go instanceof Feature) {
                        Feature f = (Feature)go;                


                        XContentBuilder builder = jsonBuilder().startObject();
                        builder.field("name", f.getName());
                        builder.field("layer", layer);
                        if (f.getDescription()!=null)
                            builder.field("description", f.getDescription());
                        if (f.getSnippet()!=null)
                            builder.field("snippet", f.getSnippet());
                        if (f.getStartTime()!=null)
                            builder.field("startTime", f.getStartTime().getTime());
                        if (f.getEndTime()!=null)
                            builder.field("endTime", f.getEndTime().getTime());

                        Geometry geo = f.getGeometry();
                        if (geo!=null) {
                            if (geo instanceof Point) {
                                Point pp = (Point)geo;
                                Geodetic2DPoint c = pp.getCenter();
                                float lat = (float)c.getLatitudeAsDegrees();
                                float lon = (float)c.getLongitudeAsDegrees();

                                //http://geojson.org/
                                builder.startObject("geom").field("type", "point").field("coordinates", new float[] { lon, lat } ).endObject();

                            }
                            /*else if (g instanceof Line) {

                            }*/
                            //TODO other types
                        }
                        if (f.getStyleUrl()!=null)
                            builder.field("styleUrl", f.getStyleUrl());

                        builder.field("path", g);
                        //f.getStyleUrl()

                        String fid = getSerial(layer, serial++);
                        st.add("feature", fid, builder.endObject());
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
                System.err.println(t);
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
                            System.err.println(uri + ": "+ excptn);
                        }
                        
                    });

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

    public ImportKML(Proxy p) throws Exception {
        this.proxy = p;
        
        
    }
    
    
    
    public void loadLayers(int threads, final long postDelay) throws Exception {
        
        ExecutorService executor = 
                
                threads == 1 ?
                Executors.newSingleThreadExecutor() :
                Executors.newFixedThreadPool(threads);
        
        ElasticSpacetime stInit = new ElasticSpacetime("cv", true);
        stInit.close();
            
          
        
        if (!Files.exists(Paths.get(basePath)))
            Files.createDirectory(Paths.get(basePath));
        
        URI uri = new File(layersFile).toURI();

        byte[] encoded = Files.readAllBytes(Paths.get(uri));

        String layers = new String(encoded, "UTF8");

        ObjectNode j = Core.fromJSON(layers);
        ArrayNode n = (ArrayNode) j.get("cv");
        String currentSection = "Unknown";
        for (JsonNode x : n) {
            if (x.isTextual()) {
                currentSection = x.textValue();
            } else if (x.isObject() && x.has("section")) {
                currentSection = x.get("section").textValue();
            } else if (!x.isObject() && !x.has("layer")) {
                System.err.println("Unrecognized item: " + x);
            } else {
                final String layer = x.get("layer").textValue();
                final String url = x.get("kml").textValue();
                
                System.out.println(currentSection + " " + x);
                                      
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("START: " + layer);
                            final ElasticSpacetime st = new ElasticSpacetime("cv");
                            transformKML(layer, url, st, false, false);
                            
                            st.close();
                            System.out.println("FINISH: " + layer);
                            
                        }
                        catch (Throwable e) {
                            //e.printStackTrace();;
                            System.err.println(e);
                        }
                        
                        try {
                            Thread.sleep(postDelay);
                        } catch (InterruptedException ex) {

                        }
                        
                    }
                    
                });
            
            }
        }
        
        
        executor.shutdown();
        while (!executor.isTerminated()) {
        }        

        
        
                
        
    }
    

    
    public static void main(String[] args) throws Exception {
        
        ProxyServer cache = new ProxyServer(16000);
        
        
        new ImportKML(cache.proxy).loadLayers(1, 5000);
    }
}
