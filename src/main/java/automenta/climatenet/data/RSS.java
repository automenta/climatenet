package automenta.climatenet.data;

import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;
import java.util.List;

/**
 * Created by me on 4/23/15.
 */
public class RSS {

    public static void main(String[] args) throws Exception {
        //String url = "http://www.geonames.org/recent-changes.xml";
        String url = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(url)));

        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(entry);

            System.out.println(entry.getTitle());
            if (geoRSSModule!=null) {
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


////to generate a GeoRSS item
//
//        GeoRSSModule geoRSSModule = new W3CGeoModuleImpl();
////GeoRSSModule geoRSSModule = new SimpleModuleImpl();
//        geoRSSModule.setLatitude(47.0);
//        geoRSSModule.setLongitude(9.0);
//        entry.getModules().add(geoRSSModule);
    }
}
