/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;

/**
 *
 * @author me
 */
public class ElasticSpacetime {
    //private final Node node;
    /*
me:  http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html elastic search and solr are competitors
 me:  http://www.elasticsearch.org/guide/en/kibana/current/using-kibana-for-the-first-time.html
 me:  http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/geoloc.html
http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5
http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-index_.html
 */
    private final Client client;
    
    public ElasticSpacetime() {
        //this.node = nodeBuilder().client(true).local(true).node();;
        //this.client = node.client();;
        
        client = new TransportClient()
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        
    }

    public void add(String index, String type, XContentBuilder n) {
        System.out.println(index + " " + type + " " + n.toString());
        IndexResponse response = client.prepareIndex(index, type /* id */)
        .setSource(n)
        .execute()
        .actionGet();
        System.out.println(index + " " + type + " " + n + " " + response.getHeaders());
    }
    
    public void close() {
        client.close();
    }
}
