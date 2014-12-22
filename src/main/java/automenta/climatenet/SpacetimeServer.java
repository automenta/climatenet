/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import static io.undertow.Handlers.resource;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import java.io.File;
import java.util.Deque;
import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.geoShapeQuery;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

/**
 *
 * @author me
 */
public class SpacetimeServer extends PathHandler {

    final ElasticSpacetimeReadOnly spacetime;
    final String clientPath = "./src/web";
    
    public SpacetimeServer(int port, String indexName) throws Exception {
        this("localhost", port, indexName);
    }

    public SpacetimeServer(String host, int port, String indexName) throws Exception {
        spacetime = new ElasticSpacetime(indexName);

        Undertow server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(4)
                
                .setHandler(this)
                .build();
        server.start();

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        
                //addPrefixPath("/ws", websocket(new WebSocketConnector(core)).addExtension(new PerMessageDeflateHandshake()))
                        
        addPrefixPath("/", resource(
                new FileResourceManager(new File(clientPath), 100)).
                    setDirectoryListingEnabled(true));
                
        addPrefixPath("geom", new HttpHandler() {

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception 
            {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();
                
                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                
                ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                ex.getResponseSender().send("Hello World");
            }

        });
    }

    public SearchHits get(double minLat, double minLon, double maxLat, double maxLon) {

        //NOT WORKING YET
        EnvelopeBuilder envelope = ShapeBuilder.newEnvelope().topLeft(
                Math.max(minLon, maxLon), Math.max(minLat, maxLat))
                .bottomRight(Math.min(minLon, maxLon), Math.min(minLat, maxLat));

        QueryBuilder qb = geoShapeQuery("geom", envelope);

        //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html
        SearchResponse response = spacetime.client.prepareSearch(spacetime.index)
                //.setTypes("type1", "type2")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb).setFrom(0).setSize(60).setExplain(true)
                .execute()
                .actionGet();
        return response.getHits();
    }

    public SearchHits get(double lat, double lon, double radiusMeters) {

        CircleBuilder shape = ShapeBuilder.newCircleBuilder().center(lon, lat).radius(radiusMeters, DistanceUnit.METERS);

        QueryBuilder qb = geoShapeQuery("geom", shape);

        //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html
        SearchResponse response = spacetime.client.prepareSearch(spacetime.index)
                .setTypes("feature")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb).setFrom(0).setSize(60).setExplain(true)
                .execute()
                .actionGet();
        return response.getHits();
    }

    public static void main(String[] args) throws Exception {
        SpacetimeServer s = new SpacetimeServer(8080, "cv");

        for (SearchHit h : s.get(35, -79, 100000).getHits()) {

            System.out.println(h.sourceAsString());
        }

    }
}
