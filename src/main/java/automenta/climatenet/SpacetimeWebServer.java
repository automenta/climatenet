/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import static automenta.climatenet.SpacetimeWebServer.json;
import automenta.climatenet.data.ClimateViewer;
import automenta.climatenet.data.SchemaOrg;
import automenta.climatenet.elastic.ElasticSpacetime;
import automenta.climatenet.p2p.TomPeer;
import automenta.climatenet.proxy.CachingProxyServer;
import automenta.knowtention.Channel;
import automenta.knowtention.Core;
import static automenta.knowtention.Core.newJson;
import automenta.knowtention.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static io.undertow.Handlers.resource;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import io.undertow.websockets.core.WebSocketChannel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.tomp2p.connection.PeerBean;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author me
 */
public class SpacetimeWebServer extends PathHandler {

    public static final Logger logger = LoggerFactory.getLogger(SpacetimeWebServer.class);

    final ElasticSpacetime db;
    
    final String clientPath = "./src/web";
    
    final String cachePath = "cache";
    
    private List<String> paths = new ArrayList();
    private final Undertow server;
    private final String host;
    private final int port;
    private final CachingProxyServer cache;
    private final int cacheProxyPort = 16000;

    abstract public static class ReadOnlyChannel<O> extends Channel {

        public ReadOnlyChannel(String id) {
            super(id);
        }

        abstract public O nextValue();

        @Override
        public ObjectNode commit() {
            O o = nextValue();
            if (o instanceof ObjectNode) {
                return super.commit((ObjectNode) o);
            } else {
                return super.commit(json(o));
            }
        }

    }

    /**
     * synchronizes a document with elastic db
     */
    public class ElasticChannel extends ReadOnlyChannel {
        private final String eType;
        private final String eID;
        boolean readOnly = false;

        public ElasticChannel(String id, String type) {
            super(id);
            this.eID = id;
            this.eType = type;
        }

        @Override
        public Object nextValue() {
            SearchResponse sr = db.searchID(new String[]{eID}, 0, 1, eType);
            SearchHits hits = sr.getHits();
            long num = hits.getTotalHits();
            if (num == 0) {
                return "Missing";
            } else if (num > 1) {
                return "Ambiguous";
            }

            return hits.getAt(0).sourceAsMap();
        }

        @Override
        public synchronized ObjectNode commit(ObjectNode next) {
                        
            if (readOnly)
                throw new RuntimeException(this + " is set read-only; unable to commit change to database");
            
            db.update(eType, eID, next.toString());
            
            return super.commit(next);
        }

    }

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
            send(hse, channel.commit());
        }

    }

    public SpacetimeWebServer(ElasticSpacetime db, int port) throws Exception {
        this(db, "localhost", port);
    }

    public SpacetimeWebServer(final ElasticSpacetime db, String host, int port) throws Exception {
        this.db = db;
        this.host = host;
        this.port = port;

        cache = new CachingProxyServer(cacheProxyPort, cachePath);
        
        server = Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(8)
                .setHandler(this)
                .build();

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        
        addPrefixPath("/", resource(
                new FileResourceManager(new File(clientPath), 100)).
                setDirectoryListingEnabled(false));

        Channel sourceIndex = new ReadOnlyChannel<SearchResponse>("/source/index") {
            @Override
            public SearchResponse nextValue() {
                return db.tagRoots();
            }
        };

        addPrefixPath("/socket", new WebSocketCore(
                sourceIndex
        ).handler());

        addPrefixPath("/tag", (new WebSocketCore() {

            @Override
            public synchronized Channel getChannel(WebSocketCore.WebSocketConnection socket, String id) {
                Channel c = super.getChannel(socket, id);

                if (c == null) {
                    //Tag t = new Tag(id, id);
                    c = new ElasticChannel(id, "tag");
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
                            String kml = c.getSnapshot().get("meta").get("url").textValue();

                            {
                                ObjectNode nc = c.getSnapshot();
                                ObjectNode meta = (ObjectNode) nc.get("meta");

                                meta.put("status", "Updating");
                                c.commit(nc);
                            }

                            System.out.println("Updating " + c);
                            
                            try {
                                new ImportKML(db, cache.proxy, c.id, kml).run();
                            }
                            catch (Exception e) {
                                ObjectNode nc = c.getSnapshot();
                                ObjectNode meta = (ObjectNode) nc.get("meta");
                                meta.put("status", e.toString());
                                c.commit(nc);
                                throw e;
                            }

                            {
                                ObjectNode nc = c.getSnapshot();
                                ObjectNode meta = (ObjectNode) nc.get("meta");

                                meta.put("status", "Ready");
                                meta.put("modifiedAt", new Date().getTime());
                                c.commit(nc);

                            }
                            //TODO save to DB
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }

            }

        }).handler());

        addPrefixPath("/tag/index", new ChannelSnapshot(sourceIndex));

        addPrefixPath("/tag/meta", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> idArray = reqParams.get("id");

                ArrayNode a = Core.json.readValue(idArray.getFirst(), ArrayNode.class);

                String[] ids = new String[a.size()];
                int j = 0;
                for (JsonNode x : a) {
                    ids[j++] = x.textValue();
                }

                SearchResponse response = db.searchID(ids, 0, 60, "tag");

                sendTags(response, ex);

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

                    XContentBuilder d = jsonBuilder().startObject();

                    for (SearchHit h : result) {

                        Map<String, Object> s = h.getSource();

                        //System.out.println(h.getId() + " " + s);
                        d.startObject(h.getId())
                                .field("path", s.get("path"))
                                .field("name", s.get("name"))
                                .field("description", s.get("description"))
                                .field("geom", s.get("geom"));
                        d.endObject();

                    }
                    d.endObject();

                    send(ex, d);

                }
                ex.getResponseSender().send("");

            }

        });

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

    public static void main(String[] args) throws Exception {
        int webPort = 9090;
        int p2pPort = 9091;
        String peerID = UUID.randomUUID().toString();

        SpacetimeWebServer s = new SpacetimeWebServer(
                ElasticSpacetime.server("cv", false),
                webPort);

        final TomPeer peer = new TomPeer(
                new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerID)).ports(p2pPort).start()).start());
        peer.add(s.db);

        SchemaOrg.load(s.db);
        new ClimateViewer(s.db);

        s.addPrefixPath("/peer/index", new ChannelSnapshot(new ReadOnlyChannel<PeerBean>("/peer/index") {
            @Override
            public PeerBean nextValue() {
                return peer.peer.peerBean();
            }
        }));
        s.addPrefixPath("/peer/connection", new ChannelSnapshot(new ReadOnlyChannel("/peer/connection") {
            @Override
            public Object nextValue() {
                return peer.peer.peer().connectionBean();
            }
        }));
        s.addPrefixPath("/peer/route", new ChannelSnapshot(new ReadOnlyChannel("/peer/route") {
            @Override
            public Object nextValue() {
                return peer.peer.peer().distributedRouting().peerMap();
            }
        }));

        s.start();
    }

    public static void send(HttpServerExchange ex, JsonNode d) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            Core.json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            logger.warn(ex1.toString());
        }

        ex.getResponseSender().close();
    }

    public static void send(HttpServerExchange ex, XContentBuilder d) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.getResponseSender().send(d.bytes().toChannelBuffer().toByteBuffer());
        ex.getResponseSender().close();
    }

    public static boolean sendTags(SearchResponse response, HttpServerExchange ex) throws IOException {
        SearchHits result = response.getHits();
        if (result.totalHits() == 0) {
            ex.getResponseSender().send("");
            return true;
        }
        XContentBuilder d = jsonBuilder().startObject();
        for (SearchHit h : result) {

            Map<String, Object> s = h.getSource();

            d.startObject(h.getId())
                    .field("name", s.get("name"));
            Object desc = s.get("description");
            if (desc != null) {
                d.field("description", s.get("description"));
            }
            Object inh = s.get("inh");
            if (inh != null) {
                d.field("inh", inh);
            }
            d.endObject();

        }
        d.endObject();
        send(ex, d);
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

            Map<String, Object> s = h.getSource();

            ObjectNode p = newJson.objectNode();
            o.put(h.getId(), p);

            p.put("name", s.get("name").toString());

            Object desc = s.get("description");
            if (desc != null) {
                p.put("description", desc.toString());
            }

            Object inh = s.get("inh");
            if (inh != null) {
                p.put("inh", Core.json.convertValue(inh, ObjectNode.class));
            }

        }

        return o;

    }

}
