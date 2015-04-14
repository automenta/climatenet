/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.data.elastic;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;

import java.io.File;
import java.io.IOException;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 *
 * @author http://cupofjava.de/blog/2012/11/27/embedded-elasticsearch-server-for-tests/
 */
public class EmbeddedES {

    private final Node node;
    private final String dataDirectory;

    public EmbeddedES() {
        this(-1);
    }

    public EmbeddedES(int port) {
        this( Files.createTempDir().getAbsolutePath(), port );
    }


    /** if port=-1, disables http server */
    public EmbeddedES(String dataDirectory, int port) {
        this.dataDirectory = dataDirectory;

        //http://www.elastic.co/guide/en/elasticsearch/reference/1.5/modules-http.html
        ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
                .put("http.enabled", port != -1 ? "true" : "false")
                .put("http.cors.enabled", true)
                .put("http.cors.allow-origin ", "http://localhost:8080")
                .put("http.port", port)
                .put("http.compression_level", 0); //for now, leave at zero because it will assume local access


        if (dataDirectory!=null)
            elasticsearchSettings.put("path.data", dataDirectory);
        else {

            //TODO THIS STILL NEEDS A DISK?
            elasticsearchSettings.put("path.data", Files.createTempDir().getAbsolutePath());

            //elasticsearchSettings.put("index.store.fs.memory.enabled", "true");
            //elasticsearchSettings.put("index.store.fs.memory.extensions", "", "del", "gen");
            elasticsearchSettings.put("index.store.type", "memory");
            elasticsearchSettings.put("index.gateway.type", "none");
            elasticsearchSettings.put("config.ignore_system_properties", true); // make sure we get what we set :)
        }

        node = nodeBuilder()
                .local(true)
                .settings(elasticsearchSettings.build())
                .node();
    }

    public String getDataDirectory() {
        return dataDirectory;
    }        

    public Client getClient() {
        return node.client();
    }

    public void close(boolean delete) {
        node.close();
        if (delete)
            deleteData();
    }

    private void deleteData() {
        try {
            FileUtils.deleteDirectory(new File(dataDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete data directory of embedded elasticsearch server", e);
        }
    }
    
    public static void main(String[] args) {
        new EmbeddedES(9200);
    }
}