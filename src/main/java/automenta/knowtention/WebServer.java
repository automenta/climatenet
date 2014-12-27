///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package automenta.knowtention;
//
//import automenta.knowtention.channel.LineFileChannel;
//import static io.undertow.Handlers.path;
//import static io.undertow.Handlers.resource;
//import static io.undertow.Handlers.websocket;
//import io.undertow.Undertow;
//import io.undertow.server.handlers.resource.FileResourceManager;
//import io.undertow.websockets.extensions.PerMessageDeflateHandshake;
//import java.io.File;
//
///**
// *
// * @author me
// */
//public class WebServer {
//
//
//    public WebServer(WebSocketCore c, int port) {
//        this(c, "localhost", port);
//    }
//
//    public WebServer(WebSocketCore c, String host, int port) {
//        this(c, host, port, "./src/web/know");
//    }
//
//    public WebServer(WebSocketCore c, String host, int port, String clientPath) {
//
//
//        Undertow server = Undertow.builder()
//                .addHttpListener(port, host)
//                .setHandler(path()
//                        .addPrefixPath("/ws", c.handler())
//                        .addPrefixPath("/", resource(
//                                        new FileResourceManager(new File(clientPath), 100)).
//                                setDirectoryListingEnabled(true)))
//                .build();
//        server.start();
//
//    }
//
//    /**
//     * usage: [port] [host]
//     */
//    public static void main(final String[] args) {
//        String host = "localhost";
//        int port = 9090;
//
//        if (args.length >= 1) {
//            port = Integer.parseInt(args[0]);
//        }
//        if (args.length >= 2) {
//            host = args[1];
//        }
//
//        System.out.println("Running: " + host + ":" + port);
//
//        WebSocketCore c = new WebSocketCore();
//        
//        WebServer w = new WebServer(c, host, port);
//
//        new LineFileChannel("chat_netention", c.getChannel("chat"), "/home/me/.xchat2/scrollback/FreeNode/#netention.txt").start();
//    }
//
//}
