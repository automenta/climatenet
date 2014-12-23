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
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
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
                
        addPrefixPath("/geoCircle", new HttpHandler() {

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception 
            {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();
                
                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> lats = reqParams.get("lat");
                Deque<String> lons = reqParams.get("lon");
                Deque<String> rads = reqParams.get("radiusM");
                
                if (lats!=null && lons!=null && rads!=null) {
                    //System.out.println(lats.getFirst() + "  "+ lons.getFirst() + " "+ rads.getFirst());
                    double lat = Double.parseDouble(lats.getFirst());
                    double lon = Double.parseDouble(lons.getFirst());
                    double rad = Double.parseDouble(rads.getFirst());
                    
                    SearchHits result = get(lat, lon, rad, 60);
                    
                    XContentBuilder d = jsonBuilder().startObject();
                    
                    for (SearchHit h : result) {
                        
                                
                        Map<String, Object> s = h.getSource();

                        Object pp = s.get("path");
                        Object[] path = new Object[] { s.get("layer"), pp };
                        
                        //System.out.println(h.getId() + " " + s);
                        
                        d.startObject(h.getId())
                                .field("path", path)
                                .field("name", s.get("name"))
                                .field("description", s.get("description"))
                                .field("geom", s.get("geom"));
                        d.endObject();
                        
                    }
                    d.endObject();
                 
                    ex.startBlocking();
                    
                    ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    
                    ex.getResponseSender().send(d.bytes().toChannelBuffer().toByteBuffer());
                    
                    
                }

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

    public SearchHits get(double lat, double lon, double radiusMeters, int max) {

        CircleBuilder shape = ShapeBuilder.newCircleBuilder().center(lon, lat).radius(radiusMeters, DistanceUnit.METERS);

        QueryBuilder qb = geoShapeQuery("geom", shape);

        //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html
        SearchResponse response = spacetime.client.prepareSearch(spacetime.index)
                .setTypes("feature")
                .setSearchType(SearchType.DEFAULT)
                .setExplain(false)
                .setQuery(qb).setFrom(0).setSize(max)
                .execute()
                .actionGet();
        return response.getHits();
    }

    public static void main(String[] args) throws Exception {
        SpacetimeServer s = new SpacetimeServer(9090, "cv");

 

    }
}
