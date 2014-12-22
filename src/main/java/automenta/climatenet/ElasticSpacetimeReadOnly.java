/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 *
 * @author me
 */
class ElasticSpacetimeReadOnly {
 
    protected final Client client;
    protected final String index;
    protected BulkRequestBuilder bulkRequest;
    protected boolean debug = false;
    
    public ElasticSpacetimeReadOnly(String indexName) {
        //this.node = nodeBuilder().client(true).local(true).node();;
        //this.client = node.client();;
        this.index = indexName;
        client = new TransportClient()                
                .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
    }
}
