/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.elastic;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 *
 * @author http://cupofjava.de/blog/2012/11/27/embedded-elasticsearch-server-for-tests/
 */
public class EmbeddedES {

    private final Node node;
    private final String dataDirectory;

    public EmbeddedES() {
        this( Files.createTempDir().getAbsolutePath() );
    }

    public EmbeddedES(String dataDirectory) {
        this.dataDirectory = dataDirectory;

        ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder()
                .put("http.enabled", "false")
                .put("path.data", dataDirectory);

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
        new EmbeddedES("/tmp/es1");
    }
}