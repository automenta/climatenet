package automenta.climatenet.data;

import automenta.climatenet.p2p.HttpRequestCached;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by me on 4/23/15.
 */
public class RSS {

    public static void main(String[] args) throws Exception {
        //String url = "http://www.geonames.org/recent-changes.xml";
        String url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";

        new HttpRequestCached(new File("cache")).run(url, new Callback() {

            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
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
                    GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(entry);

                    System.out.println(entry.getTitle());
                    if (geoRSSModule != null) {
                        double lat = geoRSSModule.getPosition().getLatitude();
                        double lng = geoRSSModule.getPosition().getLongitude();

                        System.out.println(lat + "," + lng);

                        //System.out.println(geoRSSModule.getGeometry());
                    }

            /*System.out.println(entry.getTitle() + " : lat="

                    + geoRSSModule.getPosition().getLatitude() + ",lng="
                    + geoRSSModule.getPosition().getLongitude() + ", desc="
                    + entry.getDescription().getValue() + "; time="
                    + entry.getPublishedDate());*/

                }

            }


        });
    }
    }




////to generate a GeoRSS item
//
//        GeoRSSModule geoRSSModule = new W3CGeoModuleImpl();
////GeoRSSModule geoRSSModule = new SimpleModuleImpl();
//        geoRSSModule.setLatitude(47.0);
//        geoRSSModule.setLongitude(9.0);
//        entry.getModules().add(geoRSSModule);


