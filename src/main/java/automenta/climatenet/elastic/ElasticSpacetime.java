/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.elastic;

import automenta.climatenet.Spacetime;
import com.google.common.io.Files;
import java.io.IOException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.elasticsearch.index.query.QueryBuilders.geoShapeQuery;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author me
 */
public class ElasticSpacetime  implements Spacetime {

    public final static Logger logger = LoggerFactory.getLogger(ElasticSpacetime.class);
    
    
    //private final Node node;
    /*
     me:  http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html elastic search and solr are competitors
     me:  http://www.elasticsearch.org/guide/en/kibana/current/using-kibana-for-the-first-time.html
     me:  http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/geoloc.html
     http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5
     http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-index_.html
    
    To install attachment mapper:
    
        bin/plugin -i elasticsearch/elasticsearch-mapper-attachments/2.4.1
    
    Marvel: http://www.elasticsearch.org/blog/building-marvel/
        ./bin/plugin -i elasticsearch/marvel/latest
    
     */
    
    protected final Client client;
    protected final String index;
    protected BulkRequestBuilder bulkRequest;
    protected boolean debug = false;

    
    public static ElasticSpacetime temporary(String index) throws Exception {
        String dbPath = Files.createTempDir().getAbsolutePath();
        final EmbeddedES e = new EmbeddedES(dbPath);
        return new ElasticSpacetime(index, e.getClient(), true) {

            @Override public void close() {
                super.close();
                e.close(true);
            }
            
        };
    }
    
    /** creates an embedded instance */
    public static ElasticSpacetime local(String index, String dbPath, boolean forceInit) throws Exception {
        EmbeddedES e = new EmbeddedES(dbPath);
        return new ElasticSpacetime(index, e.getClient(), forceInit);
    }
    
    
    /** connects to ES defaults: localhost:9300 */
    public static ElasticSpacetime server(String index, boolean forceInit) throws Exception {
        return server(index, "localhost", 9300, forceInit);
    }
    
    public static ElasticSpacetime server(String index, String hostport, boolean forceInit) throws Exception {    
        String[] hp = hostport.split(":");
        String host = hp[0];
        int port = Integer.parseInt(hp[1]);
        return server(index, host, port, forceInit);
    }
    
    public static ElasticSpacetime server(String index, String host, int port, boolean forceInit) throws Exception {
        Client c = new TransportClient()                
                .addTransportAddress(new InetSocketTransportAddress(host, port));
        return new ElasticSpacetime(index, c, forceInit);
    }

    protected ElasticSpacetime(String indexName, Client client, boolean initIndex) throws Exception {
        
        this.index = indexName;
        
        this.client = client;

        boolean existsIndex = true;
         final IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
            if (!res.isExists()) {
                initIndex = true;
                existsIndex = false;
            }
    
        if (initIndex) {
            logger.info("Initializing index '" + indexName + "' (exists=" + existsIndex +")");

            if (existsIndex) {
                //delete existing
                
                final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
                delIdx.execute().actionGet();
            }   

            final CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

           createIndexRequestBuilder.setSettings(ImmutableSettings.settingsBuilder().loadFromSource(jsonBuilder()
                .startObject()
                   .startObject("analysis")
                        .startObject("analyzer")
                            .startObject("html")
                                .field("type", "custom")
                                .field("tokenizer", "standard")
                                .field("char_filter", new String[] { "html_strip"} )
                                .field("max_token_length", 48)
                                .field("filter", new String[]{"snowball", "standard", "lowercase"})
                            .endObject()
                        .endObject()
                    .endObject()                   
                .endObject().string()));
            
            // MAPPING GOES HERE
            String featureType = "feature";
            final XContentBuilder mappingBuilder = jsonBuilder().startObject().startObject(featureType)
                    //.startObject("_ttl").field("enabled", "true").field("default", "1s").endObject().

                    //TODO no analyzer for path and other meta fields

                    .startObject("properties")
                        .startObject("path")
                            .field("type","string")
                            .field("index","no")
                        .endObject()
                        .startObject("description")
                            .field("type","string")
                            .field("analyzer", "html")
                            //.field("type","attachment")
                        .endObject()
                        .startObject("geom")
                            .field("type", "geo_shape")
                            .field("tree", "quadtree")
                            .field("precision", "5m")                    
                        .endObject()
                        //http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-core-types.html
                        .startObject("startTime")
                            .field("type", "date")
                        .endObject()
                        .startObject("endTime")
                            .field("type", "date")
                        .endObject()
                    .endObject()
                .endObject();
            System.out.println(mappingBuilder.string());
            createIndexRequestBuilder.addMapping(featureType, mappingBuilder);

            // MAPPING DONE
            createIndexRequestBuilder.execute().actionGet();
        }

    }

    /** returns a list of the root layers (having no parents) in the index */
    public SearchResponse rootLayers() {
        
        QueryStringQueryBuilder q = QueryBuilders.queryString("+_type:layer -path:*");        
                
        int max = 100;
        
        SearchResponse response = client.prepareSearch(index)
            .setSearchType(SearchType.DEFAULT)                
            .setQuery(q).setExplain(false)
                .setFrom(0).setSize(max)
            .execute()
            .actionGet();
        return response;
    }

    public SearchResponse search(QueryBuilder qb, int i, int i0) {
        SearchResponse response = client.prepareSearch(index)
        //.setTypes("type1", "type2")
            .setSearchType(SearchType.DEFAULT)
            .setQuery(qb).setFrom(0).setSize(60).setExplain(false)
            .execute()
            .actionGet();
        return response;
    }
    
    public SearchHits search(double minLat, double minLon, double maxLat, double maxLon) {

        //NOT WORKING YET
        EnvelopeBuilder envelope = ShapeBuilder.newEnvelope().topLeft(
                Math.max(minLon, maxLon), Math.max(minLat, maxLat))
                .bottomRight(Math.min(minLon, maxLon), Math.min(minLat, maxLat));

        QueryBuilder qb = geoShapeQuery("geom", envelope);

        //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html
        SearchResponse response = client.prepareSearch(index)
                //.setTypes("type1", "type2")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb).setFrom(0).setSize(60).setExplain(true)
                .execute()
                .actionGet();
        return response.getHits();
    }

    public SearchHits search(double lat, double lon, double radiusMeters, int max) {

        CircleBuilder shape = ShapeBuilder.newCircleBuilder().center(lon, lat).radius(radiusMeters, DistanceUnit.METERS);

        QueryBuilder qb = geoShapeQuery("geom", shape);

        //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/search.html
        SearchResponse response = client.prepareSearch(index)
                .setTypes("feature")
                .setSearchType(SearchType.DEFAULT)
                .setExplain(false)
                .setQuery(qb).setFrom(0).setSize(max)
                .execute()
                .actionGet();
        return response.getHits();
    }
    

    
    public void bulkStart() {
        bulkRequest = client.prepareBulk();
    }

    public void bulkEnd() {
        System.out.println("  FINISH:" + bulkRequest.numberOfActions() + " ElasticSearch actions");
        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        
 
        if (bulkResponse.hasFailures()) {
            System.err.println(bulkResponse.buildFailureMessage());
        }
        
        bulkRequest = null;
    }

    public void add(String type, String id, XContentBuilder n) {
        if (debug) {
            try {
                System.out.println(index + " " + type + " " + n.string());
            } catch (IOException ex) {
                logger.warn(ex.toString());
            }
        }

        bulkRequest.add(client.prepareIndex(index, type, id)
                .setSource(n));
        //System.out.println(index + " " + type + " " + n + " " + response.getHeaders());

    }

    @Override
    public void close() {
        client.close();        
    }
}
