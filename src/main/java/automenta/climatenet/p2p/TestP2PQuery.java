/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.p2p;

import automenta.climatenet.ElasticSpacetime;
import static automenta.climatenet.p2p.TestP2PDHT.bootstrap;
import static automenta.climatenet.p2p.TestP2PDHT.createMasters;
import automenta.climatenet.p2p.TomPeer.Answering;
import java.util.Map;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.PeerAddress;

/**
 *
 * @author me
 */
public class TestP2PQuery {
    public static void main(String[] args) throws Exception {
        
            PeerDHT[] peers = createMasters(3, 4001);

            bootstrap(peers);

            TomPeer a = new TomPeer(peers[0]);
            a.add(new ElasticSpacetime("cv"));
            
            
            TomPeer b = new TomPeer(peers[1]);
            
            TomPeer c = new TomPeer(peers[2]);


            c.ask("layer", 1500, new Answering() {

                @Override
                public void onAnswer(Map<PeerAddress, Object> x) {
                    System.out.println(x);
                }
                
            });
            

            Thread.sleep(2000);
            
            System.exit(0);
    }
    
}
