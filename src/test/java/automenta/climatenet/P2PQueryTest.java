/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.data.elastic.ElasticSpacetime;
import automenta.climatenet.p2p.TomPeer;
import automenta.climatenet.p2p.TomPeer.Answering;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.PeerAddress;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static automenta.climatenet.TestP2PDHT.bootstrap;
import static automenta.climatenet.TestP2PDHT.createMasters;
import static junit.framework.TestCase.assertTrue;

/**
 *
 * @author me
 */
public class P2PQueryTest {
    
    @Test
    public void testP2PQuery() throws Exception {
            
            ElasticSpacetime es;
        
            PeerDHT[] peers = createMasters(3, 14001);

            bootstrap(peers);

            TomPeer a = new TomPeer(peers[0]);
            
            a.add(es = ElasticSpacetime.temporary("index"));            
            
            TomPeer b = new TomPeer(peers[1]);
            
            TomPeer c = new TomPeer(peers[2]);


            final AtomicBoolean received = new AtomicBoolean(false);
            
            c.ask("layer", 1500, new Answering() {

                @Override
                public void onAnswer(Map<PeerAddress, Object> x) {
                    //System.out.println(x);
                    received.set(true);
                }
                
            });
            
            int count = 0;
            while (!received.get()) {
                Thread.sleep(50);
                if (count++ == 2000 / 50)
                    break;
            }
            
            a.close();
            b.close();
            c.close();
                        
            assertTrue(received.get());
            
    }
    
}
