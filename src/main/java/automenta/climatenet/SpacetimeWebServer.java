/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import static automenta.climatenet.ImportKML.json;
import automenta.climatenet.p2p.TomPeer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import static io.undertow.Handlers.resource;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

/**
 *
 * @author me
 */
public class SpacetimeWebServer extends PathHandler {

    final ElasticSpacetimeRO index;
    final String clientPath = "./src/web";

    public SpacetimeWebServer(int port, String indexName) throws Exception {
        this("localhost", port, indexName);
    }

    public SpacetimeWebServer(String host, int port, String indexName) throws Exception {
        index = new ElasticSpacetime(indexName);

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(8)
                .setHandler(this)
                .build();
        server.start();

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        //addPrefixPath("/ws", websocket(new WebSocketConnector(core)).addExtension(new PerMessageDeflateHandshake()))
        addPrefixPath("/", resource(
                new FileResourceManager(new File(clientPath), 100)).
                setDirectoryListingEnabled(false));

        addPrefixPath("/layer/index", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange hse) throws Exception {
                sendLayers(index.rootLayers(), hse);
            }
            
        });
        addPrefixPath("/layer/meta", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> idArray = reqParams.get("id");

                ArrayNode a = json.readValue(idArray.getFirst(), ArrayNode.class);

                String[] ids = new String[a.size()];
                int j = 0;
                for (JsonNode x : a) {
                    ids[j++] = x.textValue();
                }
                QueryBuilder qb = QueryBuilders.termsQuery("_id", ids);

                SearchResponse response = index.search(qb, 0, 60);

                sendLayers(response, ex);

            }

        });

        addPrefixPath("/geoCircle", new HttpHandler() {

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> lats = reqParams.get("lat");
                Deque<String> lons = reqParams.get("lon");
                Deque<String> rads = reqParams.get("radiusM");

                if (lats != null && lons != null && rads != null) {
                    //System.out.println(lats.getFirst() + "  "+ lons.getFirst() + " "+ rads.getFirst());
                    double lat = Double.parseDouble(lats.getFirst());
                    double lon = Double.parseDouble(lons.getFirst());
                    double rad = Double.parseDouble(rads.getFirst());

                    SearchHits result = index.search(lat, lon, rad, 60);

                    XContentBuilder d = jsonBuilder().startObject();

                    for (SearchHit h : result) {

                        Map<String, Object> s = h.getSource();

                        //System.out.println(h.getId() + " " + s);
                        d.startObject(h.getId())
                                .field("path", s.get("path"))
                                .field("name", s.get("name"))
                                .field("description", s.get("description"))
                                .field("geom", s.get("geom"));
                        d.endObject();

                    }
                    d.endObject();

                    send(ex, d);

                }
                ex.getResponseSender().send("");

            }

        });
    }

    public static void main(String[] args) throws Exception {
        int webPort = 9090;
        int p2pPort = 9091;
        String peerID = UUID.randomUUID().toString();
        SpacetimeWebServer s = new SpacetimeWebServer(webPort, "cv");

        TomPeer peer = new TomPeer(
                new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerID)).ports(p2pPort).start()).start());
        peer.add(s.index);

    }

    public static void send(HttpServerExchange ex, XContentBuilder d) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.getResponseSender().send(d.bytes().toChannelBuffer().toByteBuffer());
        ex.getResponseSender().close();
    }

    public static boolean sendLayers(SearchResponse response, HttpServerExchange ex) throws IOException {
        SearchHits result = response.getHits();
        if (result.totalHits() == 0) {
            ex.getResponseSender().send("");
            return true;
        }
        XContentBuilder d = jsonBuilder().startObject();
        for (SearchHit h : result) {

            Map<String, Object> s = h.getSource();

            d.startObject(h.getId())
                    .field("name", s.get("name"));
            Object desc = s.get("description");
            if (desc != null) {
                d.field("description", s.get("description"));
            }
            d.endObject();

        }
        d.endObject();
        send(ex, d);
        return false;
    }

}
