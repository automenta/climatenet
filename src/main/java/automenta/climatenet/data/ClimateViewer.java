/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.data;

import static automenta.climatenet.ImportKML.fromJSON;
import automenta.climatenet.Tag;
import automenta.climatenet.elastic.ElasticSpacetime;
import automenta.climatenet.proxy.CachingProxyServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.elasticsearch.action.bulk.BulkRequestBuilder;

/**
 *
 * @author me
 */
public class ClimateViewer {

    static final String basePath = "cache";
    static final String layersFile = "data/climateviewer.json";

    public ClimateViewer(final ElasticSpacetime st) throws Exception {

        /*ExecutorService executor = 
                
         threads == 1 ?
         Executors.newSingleThreadExecutor() :
         Executors.newFixedThreadPool(threads);*/
        if (!Files.exists(Paths.get(basePath))) {
            Files.createDirectory(Paths.get(basePath));
        }

        URI uri = new File(layersFile).toURI();

        byte[] encoded = Files.readAllBytes(Paths.get(uri));

        String layers = new String(encoded, "UTF8");

        ObjectNode j = fromJSON(layers);
        final ArrayNode n = (ArrayNode) j.get("cv");

        BulkRequestBuilder bulk = st.newBulk();

        String currentSection = "Unknown";

        for (JsonNode x : n) {
            if (x.isObject() && x.has("section")) {

                String name = x.get("section").textValue();
                String id = getSectionID(x);
                String icon = x.get("icon").textValue();

                if (id == null) {
                    throw new RuntimeException("Section " + x + " missing ID");
                }
                Tag t = new Tag(id, name);

                st.addTag(bulk, t);

            }
        }
        for (JsonNode x : n) {
            if (x.isTextual()) {
                currentSection = x.textValue();
            } else if (x.isObject() && x.has("section")) {
                currentSection = getSectionID(x);
            } else if (!x.isObject() && !x.has("layer")) {
                System.err.println("Unrecognized item: " + x);
            } else {
                final String id = x.get("layer").textValue();
                final String url = x.get("kml").textValue();
                final String name = x.get("name").textValue();

                Tag t = new Tag(id, name);
                t.url(url);

                if (currentSection != null) {
                    t.inh.put(currentSection, 1.0);
                }

                st.addTag(bulk, t);

                        //System.out.println(currentSection + " " + name + " " + x);
                //executor.submit(new ImportKML(st, proxy, id, name, url));
            }
        }

        st.commit(bulk);
        /*
         executor.shutdown();
         while (!executor.isTerminated()) {
         }*/
    }

    public static void _main(String[] args) throws Exception {

        CachingProxyServer cache = new CachingProxyServer(16000, basePath);

        ElasticSpacetime es = ElasticSpacetime.server("cv", false);
        //new ClimateViewer(cache.proxy, layersFile, es, 1, 3);
    }

    public static String getSectionID(JsonNode x) {
        if (x.has("id")) {
            return x.get("id").textValue();
        } else if (x.has("section")) {
            String s = x.get("section").textValue();
            if (s.contains(" ")) {
                return null;
            }
            return s;
        }
        return null;
    }

}
