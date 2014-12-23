/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 *
 * @author me
 */
public class SpacetimeP2P {

    
    
    
    
    public static void main(String[] args) {
        new SpacetimeP2P();
    }


//    //http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/modules-discovery-zen.html#fault-detection
//    //http://www.elasticsearch.org/guide/en/elasticsearch/client/java-api/current/client.html
//    
//    public SpacetimeP2P() {
//        
//
//        Settings s = ImmutableSettings.settingsBuilder()
//                //put("cluster.name", "myClusterName")
//                .put("client.transport.sniff", true)
//                .put("discovery.zen.ping.unicast.enabled", "false")
//                .put("discovery.zen.ping.multicast.enabled", "false")
//                
//                //https://aphyr.com/posts/317-call-me-maybe-elasticsearch
//                /*
//                    # Set the time to wait for ping responses from other nodes when discovering.
//                    # Set this option to a higher value on a slow or congested network
//                    # to minimize discovery failures:
//                    #
//                    discovery.zen.ping.timeout: 3s
//                http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/modules-discovery-zen.html#fault-detection
//                discovery.zen.fd.ping_timeout and ping_retries
//                            */
//                .put("discovery.zen.ping.timeout", "3s")
//                .put("discovery.zen.fd.ping.timeout", "3s")
//                .put("discovery.zen.ping_retries", "16")
//        
//                .build();
//        
//        TransportClient client = new TransportClient(s);
//
//        client.addTransportAddress(new InetSocketTransportAddress(
//                "localhost",
//                9300)
//        );        
//        
//        while (true) {
//            ImmutableList<DiscoveryNode> c = client.connectedNodes();
//            System.out.println(c);
//            System.out.println();
//            
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(SpacetimeP2P.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            
//        }
//    }

}
