/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.feature.type.Name;

public class OSMDemo {

    public static void main(String[] args) throws DataStoreException {
        //Demos.init();
        
        final Map<String,Serializable> parameters = new HashMap<String,Serializable>();
        parameters.put("url", OSMDemo.class.getResource("/data/sampleOSM.osm"));

        final FeatureStore store = FeatureStoreFinder.open(parameters);

        System.out.println("=================== Feature types ====================");
        final Set<Name> names = store.getNames();
        for(Name name : names){
            System.out.println(store.getFeatureType(name));
        }

    }

}