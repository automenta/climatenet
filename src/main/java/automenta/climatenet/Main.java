/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.elastic.ElasticSpacetime;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;


/**
 *
 * @author me
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("spacetime")
                .description("Decentralized Environment Awareness System");
        //parser.addMutuallyExclusiveGroup("")
        
        
        ArgumentGroup peerGroup = parser.addArgumentGroup("peer");
        
        
        peerGroup.addArgument("-p2pport").type(Integer.class).required(false).
                help("Port to connect P2P network (ensure it is not firewalled)");
        peerGroup.addArgument("-p2pseeds").type(String.class).setDefault("").required(false).
                help("Comma-separated (no spaces) list of host:port pairs");
        
        ArgumentGroup webGroup = parser.addArgumentGroup("web");
        webGroup.addArgument("-webport").type(Integer.class).required(false).setDefault(8080).help("Port to connect Web Server");

        MutuallyExclusiveGroup dbGroup = parser.addMutuallyExclusiveGroup("Database");
        dbGroup.addArgument("-esserver").type(String.class).help("ElasticSearch server host:port");
        
        dbGroup.addArgument("-espath").type(String.class).help("Elasticsearch embedded DB path");

        MutuallyExclusiveGroup dbOptGroup = parser.addMutuallyExclusiveGroup("Database Options");
        dbOptGroup.addArgument("-esindex").type(String.class).help("ElasticSearch index name").required(false).setDefault("spacetime");
        

        try {
            Namespace res = parser.parseArgs(args);
            System.out.println(res);            
            
            //1. configure DB
            String esPath = res.getString("espath");
            String esServer = res.getString("esserver");
            String esIndex = res.getString("esindex");
            Integer webPort = res.getInt("webport");
            Integer p2pPort = res.getInt("p2pport");
            
            System.out.println(esPath + " " + esServer + " " + esIndex);
            
            ElasticSpacetime e = null;            
            SpacetimeWebServer w = null;
            SpacetimePeer p = null;
            
            if (esPath!=null) {
                e = ElasticSpacetime.local(esIndex, esPath, false);
            }
            else if (esServer!=null) {
                e = ElasticSpacetime.server(esIndex, esServer, false);
            }
            
            
            if (e !=null && webPort!=null) {
                w = new SpacetimeWebServer(e, webPort);
            }
            
            if (e!=null && p2pPort!=null) {
                p = new SpacetimePeer(p2pPort);
                p.peer.add(e);
                
                //TODO add seeds
            }
            
            /*System.out.println(((Accumulate) res.get("accumulate"))
                    .accumulate((List<Integer>) res.get("integers")));*/
        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }        
    }
}
