/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.run;

import automenta.climatenet.ElasticChannel;
import automenta.climatenet.ImportKML;
import automenta.climatenet.ReadOnlyChannel;
import automenta.climatenet.data.elastic.ElasticSpacetime;
import automenta.climatenet.p2p.Wikipedia;
import automenta.climatenet.p2p.proxy.CachingProxyServer;
import automenta.knowtention.Channel;
import automenta.knowtention.Core;
import automenta.knowtention.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import io.undertow.websockets.core.WebSocketChannel;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static automenta.knowtention.Core.newJson;
import static io.undertow.Handlers.resource;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 *
 * @author me
 */
abstract public class SpacetimeWebServer extends PathHandler {

    public static final Logger logger = LoggerFactory.getLogger(SpacetimeWebServer.class);

    final ElasticSpacetime db;

    final String clientPath = "./src/web";
    private final ReadOnlyChannel<SearchResponse> index;


    private List<String> paths = new ArrayList();
    private final Undertow server;
    private final String host;
    private final int port;


    /**
     * wraps a channel as an HTTP handler which returns a snapshot (text) JSON
     * representation
     */
    public static class ChannelSnapshot implements HttpHandler {

        public final Channel channel;

        public ChannelSnapshot(Channel c) {
            super();
            this.channel = c;
        }

        @Override
        public void handleRequest(HttpServerExchange hse) throws Exception {
            send(channel.commit(), hse);
        }

    }

    public SpacetimeWebServer(ElasticSpacetime db, int port) throws Exception {
        this(db, "localhost", port);
    }

    public SpacetimeWebServer(final ElasticSpacetime db, String host, int port) throws Exception {
        this.db = db;
        this.host = host;
        this.port = port;




        server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(8)
                .setHandler(this)
                .build();

        this.index = new Index(db);


        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        addPrefixPath("/", resource(
                new FileResourceManager(new File(clientPath), 100, true, "/")).
                setDirectoryListingEnabled(false));

        addPrefixPath("/socket", new WebSocketCore(
                index
        ).handler());

        addPrefixPath("/tag/index", new ChannelSnapshot(index));



        addPrefixPath("/tag", (new WebSocketCore() {

            final String cachePath = "cache";
            final int cacheProxyPort = 16000;
            final CachingProxyServer cache = new CachingProxyServer(cacheProxyPort, cachePath);

            @Override
            public synchronized Channel getChannel(WebSocketCore.WebSocketConnection socket, String id) {
                Channel c = super.getChannel(socket, id);

                if (c == null) {
                    //Tag t = new Tag(id, id);
                    c = new ElasticChannel(db, id, "tag");
                    super.addChannel(c);
                }

                return c;
            }

            @Override
            protected void onOperation(String operation, Channel c, JsonNode param, WebSocketChannel socket) {

                //TODO prevent interrupting update operation if already in-progress
                switch (operation) {
                    case "update":
                        try {
                            ObjectNode meta = (ObjectNode) c.getSnapshot().get("meta");
                            if (meta!=null && meta.has("kmlLayer")) {
                                String kml = meta.get("kmlLayer").textValue();

                                {
                                    ObjectNode nc = c.getSnapshot();
                                    meta = (ObjectNode) nc.get("meta");

                                    meta.put("status", "Updating");
                                    c.commit(nc);
                                }

                                System.out.println("Updating " + c);

                                try {
                                    new ImportKML(db, cache.proxy, c.id, kml).run();
                                } catch (Exception e) {
                                    ObjectNode nc = c.getSnapshot();
                                    meta = (ObjectNode) nc.get("meta");
                                    meta.put("status", e.toString());
                                    c.commit(nc);
                                    throw e;
                                }

                                {
                                    ObjectNode nc = c.getSnapshot();
                                    meta = (ObjectNode) nc.get("meta");

                                    meta.put("status", "Ready");
                                    meta.put("modifiedAt", new Date().getTime());
                                    c.commit(nc);

                                }

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }

            }

        }).handler());


        addPrefixPath("/tag/meta", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                sendTags(
                        db.searchID(
                                getStringArrayParameter(ex, "id"), 0, 60, "tag"
                        ),
                        ex);

            }

        });
        addPrefixPath("/style/meta", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {

                send(json(
                                db.searchID(
                                        getStringArrayParameter(ex, "id"), 0, 60, "style"
                                )),
                        ex);

            }

        });

        addPrefixPath("/geoCircle", new HttpHandler() {

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> lats = reqParams.get("lat");
                Deque<String> lons = reqParams.get("lon");
                Deque<String> rads = reqParams.get("radiusM");

                if (lats != null && lons != null && rads != null) {
                    //System.out.println(lats.getFirst() + "  "+ lons.getFirst() + " "+ rads.getFirst());
                    double lat = Double.parseDouble(lats.getFirst());
                    double lon = Double.parseDouble(lons.getFirst());
                    double rad = Double.parseDouble(rads.getFirst());

                    SearchHits result = db.search(lat, lon, rad, 60);

                    XContentBuilder d = responseTagOrFeature(result);

                    send(d, ex);

                }
                ex.getResponseSender().send("");

            }

        });

        addPrefixPath("/wikipedia", new Wikipedia());


    }

    public void add(String route, Channel c) {
        db.update("tag", route, "{ws: \"" + route + '#' + c.id + "\"}");
        addPrefixPath("/" + route, new WebSocketCore(
                c
        ).handler());
    }


    public void start() {
        server.start();
        logger.info("Started web server @ " + host + ":" + port + "\n  " + paths);

    }

    @Override
    public synchronized PathHandler addPrefixPath(String path, HttpHandler handler) {
        paths.add(path);
        return super.addPrefixPath(path, handler);
    }


    public static void send(String s, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            ex.getOutputStream().write(Charset.forName("UTF-8").encode(s).array());
        } catch (IOException e) {
            logger.error(e.toString());
        }

        ex.getResponseSender().close();
    }

    public static void send(JsonNode d, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            Core.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            logger.warn(ex1.toString());
        }

        ex.getResponseSender().close();
    }

    public static void send(XContentBuilder d, HttpServerExchange ex) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.getResponseSender().send(d.bytes().toChannelBuffer().toByteBuffer());
        ex.getResponseSender().close();
    }

    public static XContentBuilder responseTagOrFeature(Iterable<SearchHit> result) throws IOException {
        XContentBuilder d = jsonBuilder().startObject();
        for (SearchHit h : result) {

            Map<String, Object> s = h.getSource();

            d.startObject(h.getId());

            Object name = s.get("name");
            if (name != null) {
                d.field("name", name);
            }

            Object desc = s.get("description");
            if (desc != null) {
                d.field("description", desc);
            }

            Object path = s.get("path");
            if (path != null) {
                d.field("path", path);
            }

            Object geom = s.get("geom");
            if (geom != null) {
                d.field("geom", geom);
            }

            Object style = s.get("style");
            if (style != null) {
                d.field("style", style);
            }

            Object styleUrl = s.get("styleUrl");
            if (styleUrl != null) {
                d.field("styleUrl", styleUrl);
            }

            Object inh = s.get("inh");
            if (inh != null) {
                d.field("inh", inh);
            }
            d.endObject();

        }

        d.endObject();
        return d;
    }

    public static boolean sendTags(SearchResponse response, HttpServerExchange ex) throws IOException {
        SearchHits result = response.getHits();
        if (result.totalHits() == 0) {
            ex.getResponseSender().send("");
            return true;
        }
        XContentBuilder d = responseTagOrFeature(result);

        send(d, ex);
        return false;
    }

    public static ObjectNode json(Object o) {
        if (o instanceof SearchResponse) {
            return json((SearchResponse) o);
        }
        return Core.json.convertValue(o, ObjectNode.class);
    }

    public static ObjectNode json(SearchResponse response) {
        SearchHits result = response.getHits();

        ObjectNode o = newJson.objectNode();
        if (result.totalHits() == 0) {
            return o;
        }
        for (SearchHit h : result) {

            ObjectNode p = newJson.objectNode();
            o.put(h.getId(), p);

            Map<String, Object> s = h.getSource();

            for (Map.Entry<String, Object> e : s.entrySet()) {
                try {
                    p.put(e.getKey(), Core.json.convertValue( e.getValue(), JsonNode.class ) );
                }
                catch (Exception ee) {
                    System.err.println(ee);
                }
            }


        }

        return o;

    }

    public static String[] getStringArrayParameter(HttpServerExchange ex, String param) throws IOException {
        Map<String, Deque<String>> reqParams = ex.getQueryParameters();

        Deque<String> idArray = reqParams.get(param);

        ArrayNode a = Core.json.readValue(idArray.getFirst(), ArrayNode.class);

        String[] ids = new String[a.size()];
        int j = 0;
        for (JsonNode x : a) {
            ids[j++] = x.textValue();
        }

        return ids;
    }

    public static class Index extends ReadOnlyChannel<SearchResponse> {
        private final ElasticSpacetime db;

        public Index(ElasticSpacetime db) {
            super("index");
            this.db = db;
        }

        @Override
        public SearchResponse nextValue() {
            return db.tagRoots();
        }

    }
}
