/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.run;

import automenta.climatenet.p2p.TomPeer;
import automenta.climatenet.p2p.TomPeer.Answering;
import net.tomp2p.connection.Bindings;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author me
 */
public class SpacetimePeer {

    //public final Spacetime index;
    public final TomPeer peer;
    
    public SpacetimePeer(Number160 peerID, int port) throws IOException {
        this(peerID, "localhost", port);
    }
    
    public SpacetimePeer(Number160 peerID, String host, int port) throws IOException {

        peer = new TomPeer(
                new PeerBuilderDHT(new PeerBuilder(peerID).bindings(new Bindings().addAddress(InetAddress.getByName(host))).ports(port).start()).start());
        
        //peer.add(index);        
        
    }
    
    public SpacetimePeer(String host, int port) throws IOException {
        this((String)null, host, port);        
    }
    
    public SpacetimePeer(String peerID, String host, int port) throws IOException {
        this(peerID != null ? Number160.createHash(peerID) : new Number160(new Random()), port);        
    }

    public void addPeer(String hostPort) {
        String[] hp = hostPort.split(":");
        if (hp.length!=2)
            throw new RuntimeException("Invalid 'host:port' peer address: " + hostPort);
        
        String h = hp[0];
        int port = Integer.parseInt(hp[1]);
        
        try {
            FutureDiscover s = peer.connect(h, port);
            s.await(5000);
            System.out.println(hostPort + " " + s.isSuccess() + " " + s.peerAddress());
        } catch (Exception ex) {
            Logger.getLogger(SpacetimePeer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }    
    
    
    
    public static void main(String[] args) throws IOException {
        String hosts[] = new String[] { "localhost:9091" };
        
        SpacetimePeer p = new SpacetimePeer("abc", 9696);
        for (String h : hosts)
            p.addPeer(h);
        
        p.peer.ask("layer", 5000, new Answering() {

            @Override
            public void onAnswer(Map<PeerAddress, Object> x) {
                System.out.println(x);
            }
            
        });
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
