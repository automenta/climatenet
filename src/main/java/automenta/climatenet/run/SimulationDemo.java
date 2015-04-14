package automenta.climatenet.run;

import automenta.climatenet.data.elastic.ElasticSpacetime;
import automenta.climatenet.data.sim.SimpleSimulation;
import automenta.knowtention.WebSocketCore;

/**
 * Created by me on 4/14/15.
 */
public class SimulationDemo {

    public static void main(String[] args) throws Exception {
        int webPort = 9090;

        NetentionServer s = new NetentionServer(
                //ElasticSpacetime.temporary("cv", -1),
                ElasticSpacetime.local("cv", "cache", true),
                "localhost",
                webPort);



        //EXAMPLES
        {
            //new IRCBot(s.db, "RAWinput", "irc.freenode.net", "#netention");
            //new FileTailWindow(s.db, "netlog", "/home/me/.xchat2/scrollback/FreeNode/#netention.txt").start();

            s.addPrefixPath("/sim", new WebSocketCore(
                    new SimpleSimulation("x")
            ).handler());
        }


        s.start();
    }


}
