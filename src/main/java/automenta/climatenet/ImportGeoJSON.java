/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;

/**
 *
 * @author me
 */
public class ImportGeoJSON {

    public static void main(String[] args) throws IOException {
 

        
        FeatureJSON io = new FeatureJSON();
        FeatureCollection c = io.readFeatureCollection(new FileInputStream("/home/me/share/climatenet/data/cvr01/cvr01.json"));
        
        System.out.println(c);
// io.writeFeature(feature, "feature.json"));
// 
// Iterator features = io.streamFeatureCollection("features.json");
// while(features.hasNext()) {
//   feature = features.next();
//   ...
// }        
    }
}
