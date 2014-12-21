/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.knowtention.Core;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.GISFactory;
import org.opensextant.giscore.events.ContainerStart;
import org.opensextant.giscore.events.DocumentStart;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.events.IGISObject;
import org.opensextant.giscore.events.Schema;
import org.opensextant.giscore.events.SimpleField;
import org.opensextant.giscore.input.kml.KmlReader;
import org.opensextant.giscore.input.kml.UrlRef;
import org.opensextant.giscore.output.IGISOutputStream;
import org.opensextant.giscore.output.kml.KmlOutputStream;

/**
 * @see https://github.com/OpenSextant/giscore/wiki
 * @author me
 */
public class ImportKML {

    String basePath = "data";
    String layersFile = "src/main/java/automenta/climatenet/cvlayers.json";
    
    public void transformKML(String layer, String urlString, boolean esri, boolean kml) throws Exception {
        URL url = new URL(urlString);
        
        KmlReader reader = new KmlReader(url);
        reader.setRewriteStyleUrls(true);
        
        
        
        
        File temp = Files.createTempDirectory("kml" + layer).toFile();
        
        String p = basePath + "/" + layer;
        
        //Files.deleteIfExists(Paths.get(p));
        if (!Files.exists(Paths.get(p)))
            Files.createDirectory(Paths.get(p));
        
        
        IGISOutputStream shpos = null;
        ZipOutputStream esriOut = null;
        
        if (esri) {
            String outputFile = p + "/" + layer + ".shape.zip";
            if (Files.exists(Paths.get(outputFile)))
                Files.delete(Paths.get(outputFile));
            esriOut = new ZipOutputStream(new FileOutputStream(outputFile));


            shpos = GISFactory.getOutputStream(DocumentType.Shapefile, esriOut, temp);
            System.out.println("  ESRI Shapefile: Output file: " + outputFile);
            System.out.println("  ESRI Shapefile: Temporary folder: " + temp);
        
            Schema schema = new Schema(new URI("urn:test"));
            SimpleField id = new SimpleField(layer);
            id.setLength(10);
            schema.put(id);
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
        
        
        
                
	
        
        
        
        
        for (IGISObject gisObj; (gisObj = reader.read()) != null;) {
  // do something with the gis object; e.g. check for placemark, NetworkLink, etc.
            if (gisObj instanceof Feature) {
                Feature f = (Feature)gisObj;                
                
                //System.out.println(f.getElements());
                //System.out.println(f.getFields() + " " + schema.getFields());
                
                //f.putData(null, kout);
                
                
                //System.out.println(gisObj);
            }
            if (esri)
                    shpos.write(gisObj);  
                if (kml) {
                    kout.write(gisObj);
                }
        }
        
        
        
// get list of network links that were retrieved from step above
        
        List<URI> networkLinks = reader.getNetworkLinks();
        
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
                            System.out.println("Loading NetworkLink: " + ref);
                            return true;
                        }

                        @Override
                        public void handleError(URI uri, Exception excptn) {
                            System.err.println(uri + ": "+ excptn);
                        }
                        
                    });

        }
        
        if (esri) {
            shpos.close();

            esriOut.flush();
            esriOut.close();	
        }
        if (kml) {            
            kout.close();
        }

        toGeoJSON(kmlFile, geojsonFile);
        toPostgres(layer, geojsonFile, "localhost", "me", "cv");
        


        
    }
    
    public static void exec(String cmd) {
        try {
            String[] cmdParm = { "/bin/sh", "-c", cmd };
            
            Process proc = Runtime.getRuntime().exec(cmdParm);
            IOUtils.copy(proc.getInputStream(), System.out);
            IOUtils.copy(proc.getErrorStream(), System.err);        
            proc.waitFor();
        } catch (Exception ex) {
            Logger.getLogger(ImportKML.class.getName()).log(Level.SEVERE, null, ex);
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

    public ImportKML() throws Exception {

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
                String layer = x.get("layer").textValue();
                String url = x.get("kml").textValue();
                System.out.println(currentSection + " " + x);
                
                try {
                    transformKML(layer, url, false, true);
                }
                catch (Exception e) {
                    e.printStackTrace();;
                }
            }
        }
    }
    

    
    public static void main(String[] args) throws Exception {
        new ImportKML();
    }
}
