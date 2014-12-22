//package climatenet.proxy;
//
///**
// * ProxyCache.java - Simple caching proxy
// *
// * $Id: ProxyCache.java,v 1.3 2004/02/16 15:22:00 kangasha Exp $
// *
// */
//
//import java.net.*;
//import java.io.*;
//import java.lang.*;
//import org.apache.commons.io.IOUtils;
//import org.apache.http.Header;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//
//public class ProxyCache implements Runnable {
//        CloseableHttpClient httpclient = HttpClients.createDefault();
//
//    /**
//     * Port for the proxy
//     */
//    private int port;
//    /**
//     * Socket for client connections
//     */
//    private ServerSocket socket;
//    /**
//     * Create the ProxyCache object and the socket
//     */
//    //private  Map<String, String> cache = new Hashtable<String, String>();
//
//    final String cachePath = "cache/";
//    public final Proxy proxy;
//
//    public ProxyCache(int myPort) {
//        File cachedir = new File(cachePath);
//        if (!cachedir.exists()) {
//            cachedir.mkdir();
//        }
//
//        init(myPort);
//
//        proxy = new Proxy(Proxy.Type.HTTP, socket.getLocalSocketAddress());
//
//    }
//
//    public void run() {
//        /**
//         * Main loop. Listen for incoming connections and spawn a new thread for
//         * handling them
//         */
//        Socket client = null;
//
//        while (true) {
//            try {
//                client = socket.accept(); /* Aceita novos clientes */
//
//                (new Thread(new Threads(client))).start(); /* Criar threads para cada novo cliente */
//
//            } catch (IOException e) {
//                System.out.println("Error reading request from client: " + e);
//                /* Definitely cannot continue processing this request,
//                 * so skip to next iteration of while loop. */
//                continue;
//            }
//        }
//
//    }
//
//    public synchronized void caching(HttpRequest pedido, String headers, byte[] data) throws IOException {
//        File ficheiro;
//        DataOutputStream paraficheiro;
//
//        String uri = pedido.URI;
//        ficheiro = getCacheFile(uri);
//        //System.out.println("Caching from: " + pedido.URI + " para " + ficheiro.getAbsolutePath());
//
//        paraficheiro = new DataOutputStream(new FileOutputStream(ficheiro));
//        paraficheiro.writeBytes(headers); /* Escreve headers */
//
//        paraficheiro.write(data); /* Escreve body */
//
//        paraficheiro.close();
//    	//cache.put(pedido.URI, ficheiro.getAbsolutePath());
//
//    }
//
//    public File getCacheFile(String uri) throws UnsupportedEncodingException {
//        String filename = URLEncoder.encode(uri, "UTF-8");
//        return new File(cachePath + filename);
//    }
//
//    public byte[] uncaching(String uripedido) throws IOException {
//        File ficheirocached;
//        FileInputStream deficheiro;
//        byte[] bytescached;
//
//        ficheirocached = getCacheFile(uripedido);
//        try {
//            deficheiro = new FileInputStream(ficheirocached);
//            bytescached = new byte[(int) ficheirocached.length()];
//            deficheiro.read(bytescached);
//            //System.out.println("Caching: Hit on " + uripedido + " returning cache to user");
//        }
//        catch (IOException e) {
//            return new byte[0];
//        }
//        
//        return bytescached;
//
//    }
//
//    public void init(int p) {
//        port = p;
//        try {
//            socket = new ServerSocket(port);
//        } catch (IOException e) {
//            System.out.println("Error creating socket: " + e);
//            System.exit(-1);
//        }
//    }
//
//    public class Threads implements Runnable {
//
//        private final Socket client;
//
//        public Threads(Socket client) {
//            this.client = client;
//        }
//
//        public void run() {
//            Socket server = null;
//            HttpRequest request = null;
//            HttpResponse response = null;
//
//            /* Process request. If there are any exceptions, then simply
//             * return and end this request. This unfortunately means the
//             * client will hang for a while, until it timeouts. */
//
//            /* Read request */
//            try {
//                BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                request = new HttpRequest(fromClient);
//            } catch (IOException e) {
//                System.out.println("Error reading request from client: " + e);
//                try {
//                    client.close();
//                } catch (IOException ex) {                  
//                }
//                return;
//            }
//            /* Send request to server */
//            //try {
//                /* Open socket and write request to socket */
//                //server = new Socket(request.getHost(), request.getPort()); /* Criar socket */
//
//                //DataOutputStream toServer = new DataOutputStream(server.getOutputStream()); /* Criar outputstream para o servidor no socket */
//
//                //toServer.writeBytes(request.toString()); /* Escrever pedido para o outputstream */
//
//            /*} catch (UnknownHostException e) {
//                System.out.println("Unknown host: " + request.getHost());
//                System.out.println(e);
//                try {
//                    client.close();
//                } catch (IOException ex) {                  
//                }                return;
//            } catch (IOException e) {
//                try {
//                    client.close();
//                } catch (IOException ex) {                  
//                }                
//                return;
//            }*/
//                
//            /* Read response and forward it to client */
//            try {
//                byte[] cache = uncaching(request.URI);
//                if (cache.length == 0) {
//                    
//                    //DataInputStream fromServer = new DataInputStream(server.getInputStream()); /* Criar inputstream do servidor */
//                    DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
//
//
//                    //response = new HttpResponse(fromServer); /* Criar objecto com a response do servidor */
//                      HttpGet method = null;
//	            method = new HttpGet(request.URI);
//                                CloseableHttpResponse response1 = httpclient.execute(method);
//
//	        
//                    byte[] data = IOUtils.toByteArray(response1.getEntity().getContent());                                
//                    
//	            //method.setFollowRedirects(true);
//                    String headers = "";
//                    headers += response1.getStatusLine().toString();
//                    for(Header h : response1.getAllHeaders())
//                        headers += h.toString() + "\n";
//                    
//                    System.out.println(headers);
//                    toClient.writeBytes(headers);
//                    
//
//                    toClient.write(data); /* Escreve body */
//                    /* Write response to client. First headers, then body */
//
//                    caching(request, headers, data); /* Guardar em cache */
//
//                    client.close();
//                    /* Insert object into the cache */
//                    /* Fill in (optional exercise only) */
//                } else {
//                    DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
//                    
//                    toClient.write(cache);
//                    client.close();
//                    server.close();
//                }
//
//            } catch (IOException e) {
//                System.out.println("Error writing response to client: " + e);
//                e.printStackTrace();;
//                try {
//                    client.close();
//                } catch (IOException ex) {
//                    
//                }
//            }
//        }
//    }
//
//}
