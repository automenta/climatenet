/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 *
 * @author me
 */
public class ElasticSpacetime extends ElasticSpacetimeReadOnly {
    //private final Node node;
    /*
     me:  http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html elastic search and solr are competitors
     me:  http://www.elasticsearch.org/guide/en/kibana/current/using-kibana-for-the-first-time.html
     me:  http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/geoloc.html
     http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5
     http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-index_.html
    
    To install attachment mapper:
    
        bin/plugin -i elasticsearch/elasticsearch-mapper-attachments/2.4.1
    
     */


    public ElasticSpacetime(String indexName) throws Exception {
        this(indexName, false);
    }

    public ElasticSpacetime(String indexName, boolean initIndex) throws Exception {
        super(indexName);
        
        if (initIndex) {
            final IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
            if (res.isExists()) {
                final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
                delIdx.execute().actionGet();
            }            
            
            final CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

            // MAPPING GOES HERE
            String featureType = "feature";
            final XContentBuilder mappingBuilder = jsonBuilder().startObject().startObject(featureType)
                    //.startObject("_ttl").field("enabled", "true").field("default", "1s").endObject().
                    .startObject("properties")
                    
                        .startObject("description")
                    //max_token_length
                            .field("type","string")
                            //.field("type","attachment")
                            //.field("analyzer", "html_strip")
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

    public void bulkStart() {
        bulkRequest = client.prepareBulk();
    }

    public void bulkEnd() {
        System.out.println("Elasticsearch:" + bulkRequest.numberOfActions() + " actions");
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
                Logger.getLogger(ElasticSpacetime.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        bulkRequest.add(client.prepareIndex(index, type, id)
                .setSource(n));
        //System.out.println(index + " " + type + " " + n + " " + response.getHeaders());

    }

    public void close() {
        client.close();
    }
}
