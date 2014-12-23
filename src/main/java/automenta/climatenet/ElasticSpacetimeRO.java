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
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;

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
}
