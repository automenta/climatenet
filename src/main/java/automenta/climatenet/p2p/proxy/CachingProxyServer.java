/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.p2p.proxy;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy server which caches requests and their data to files on disk
 */
public class CachingProxyServer implements HttpHandler {

    public static final Logger logger = LoggerFactory.getLogger(CachingProxyServer.class);
    
    private AsyncHttpClient client = new AsyncHttpClient();
    public final Proxy proxy;
    final String cachePath;

    public CachingProxyServer(int port, String cachePath) {

        this.cachePath = cachePath + "/";
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setIoThreads(4)
                .setHandler(this)
                .build();
        server.start();

        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(port));

        logger.info("Cache proxy started: " + proxy + ", saving to: " + cachePath);
    }

    public synchronized void caching(String uri, CachedURL c) throws IOException {
        File ficheiro;

        ficheiro = getCacheFile(uri);
        //System.out.println("Caching from: " + pedido.URI + " para " + ficheiro.getAbsolutePath());

        ObjectOutputStream paraficheiro = new ObjectOutputStream(new FileOutputStream(ficheiro));
        paraficheiro.writeObject(c);

        paraficheiro.close();
        //cache.put(pedido.URI, ficheiro.getAbsolutePath());

    }

    public File getCacheFile(String uri) throws UnsupportedEncodingException {
        String filename = URLEncoder.encode(uri, "UTF-8");
        return new File(cachePath + filename);
    }

    public CachedURL uncaching(String uripedido) throws Exception {

        File ficheirocached = getCacheFile(uripedido);

            ObjectInputStream deficheiro = new ObjectInputStream(new FileInputStream(ficheirocached));

            CachedURL x = (CachedURL) deficheiro.readObject();
            /*bytescached = new byte[(int) ficheirocached.length()];
             deficheiro.read(bytescached);*/
            //System.out.println("Caching: Hit on " + uripedido + " returning cache to user");
            return x;


    }

    public static class CachedURL implements Serializable {

        public byte[] content;
        public int responseCode;
        public List<String[]> responseHeader;

        public CachedURL() {
        }

        public CachedURL(HeaderMap requestHeaders, int responseCode, ByteBuffer content) {
            responseHeader = new ArrayList();
            for (HeaderValues x : requestHeaders) {
                for (String y : x) {
                    responseHeader.add(new String[] { x.toString(), y } );
                }
            }
            this.responseCode = responseCode;
            this.content = content.array();
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        final String url = exchange.getRequestURL();

        try {
            CachedURL existing = uncaching(url);
            if (existing != null) {
                exchange.setResponseCode(existing.responseCode);
                for (String[] x : existing.responseHeader) {
                    exchange.getResponseHeaders().add(new HttpString(x[0]), x[1]);
                }
                
                exchange.getResponseSender().send(ByteBuffer.wrap(existing.content));
                return;
            }
        } catch (Exception e) {
        }

        ListenableFuture<Response> r = client.prepareGet(url).execute(new AsyncCompletionHandler<Response>() {

            @Override
            public Response onCompleted(Response response) throws Exception {
                exchange.setResponseCode(response.getStatusCode());

                setHeader(exchange, response.getHeaders());

                ByteBuffer content = response.getResponseBodyAsByteBuffer();

                exchange.getResponseSender().send(content);

                caching(url, new CachedURL(exchange.getResponseHeaders(), exchange.getResponseCode(), content));
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
                exchange.setResponseCode(500);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                StringWriter data = new StringWriter();
                t.printStackTrace(new PrintWriter(data));
                exchange.getResponseSender().send(data.getBuffer().toString());
            }

        });
        r.get();
    }

    public static void setHeader(HttpServerExchange exchange, FluentCaseInsensitiveStringsMap response) {
        for (Entry<String, List<String>> entry : response.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                exchange.getResponseHeaders().put(new HttpString(entry.getKey()),
                        entry.getValue().iterator().next());
            }
        }
    }

}
