package automenta.climatenet.data;

import automenta.climatenet.p2p.HttpRequestCached;
import automenta.climatenet.p2p.NObject;
import automenta.knowtention.Channel;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.squareup.okhttp.Response;

import java.util.List;


public class RSS extends Channel.FeedChannel<RSS.RSSItem> {

    private final String url;

    public static class RSSItem extends NObject {

        RSSItem(String uri, String title) { super(uri, title); }

    }

    public RSS(String id, String url, int maxItems) {
        super(id, maxItems);
        this.url = url;

        try {
            update();
        } catch (Exception e) {
            //TODO add NObject error message
            e.printStackTrace();
        }
    }

    public void update() throws Exception {

        Response response = HttpRequestCached.the.get(url);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(response.body().byteStream()));
        } catch (FeedException e) {
            e.printStackTrace();
            return;
        }

        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            append(newItem(entry));

            /*System.out.println(entry.getTitle() + " : lat="

                    + geoRSSModule.getPosition().getLatitude() + ",lng="
                    + geoRSSModule.getPosition().getLongitude() + ", desc="
                    + entry.getDescription().getValue() + "; time="
                    + entry.getPublishedDate());*/

        }

    }

    protected RSSItem newItem(SyndEntry entry) {
        RSSItem r = new RSSItem(entry.getUri(), entry.getTitle());

        GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(entry);

        if (geoRSSModule != null) {
            double lat = geoRSSModule.getPosition().getLatitude();
            double lng = geoRSSModule.getPosition().getLongitude();
            r.where(lat, lng);

            //System.out.println(geoRSSModule.getGeometry());
        }

        return r;
    }

}


////to generate a GeoRSS item
//
//        GeoRSSModule geoRSSModule = new W3CGeoModuleImpl();
////GeoRSSModule geoRSSModule = new SimpleModuleImpl();
//        geoRSSModule.setLatitude(47.0);
//        geoRSSModule.setLongitude(9.0);
//        entry.getModules().add(geoRSSModule);


