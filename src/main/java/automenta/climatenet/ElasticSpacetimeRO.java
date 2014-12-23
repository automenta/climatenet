/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.geoShapeQuery;
import org.elasticsearch.search.SearchHits;

/**
 *
 * @author me
 */
public class ElasticSpacetimeRO implements Spacetime {
 
    protected final Client client;
    protected final String index;
    protected BulkRequestBuilder bulkRequest;
    protected boolean debug = false;
    
    public ElasticSpacetimeRO(String indexName) {
        //this.node = nodeBuilder().client(true).local(true).node();;
        //this.client = node.client();;
        this.index = indexName;
        client = new TransportClient()                
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
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
    
}
