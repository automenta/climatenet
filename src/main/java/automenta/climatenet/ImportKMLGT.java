/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.sis.referencing.CommonCRS;
import static org.geotoolkit.data.geojson.utils.GeoJSONParser.LOGGER;
import static org.geotoolkit.data.kml.KmlFeatureUtilities.cutAtMeridian;
import org.geotoolkit.data.kml.model.AbstractGeometry;
import org.geotoolkit.data.kml.model.DefaultMultiGeometry;
import org.geotoolkit.data.kml.model.Kml;
import org.geotoolkit.data.kml.model.KmlModelConstants;
import org.geotoolkit.data.kml.xml.KmlReader;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.simple.SimpleFeature;
import org.geotoolkit.feature.simple.SimpleFeatureBuilder;
import org.geotoolkit.feature.simple.SimpleFeatureType;

/**
 *
 * https://github.com/Geomatys/geotoolkit/tree/master/modules/storage/geotk-feature-kml/src/test/java/org/geotoolkit/data/kml
 */
public class ImportKMLGT {

    public ImportKMLGT() {

    }
    
    
    /**
     * FROM: https://github.com/Geomatys/geotoolkit/blob/master/modules/storage/geotk-feature-kml/src/main/java/org/geotoolkit/data/kml/KmlFeatureUtilities.java
     * get {@link SimpleFeature} {@link List} from a {@link Kml} object
     * @param kmlObject : object which can have feature to extract
     * @return {@link SimpleFeature} {@link List} include in kml file
     */
    public static List<Feature> resolveFeaturesFromKml(final Kml kmlObject) {
        final List<Feature> results = new ArrayList<>();
        if (kmlObject != null) {
            final org.geotoolkit.feature.Feature document = kmlObject.getAbstractFeature();
            final Iterator propertiesFeat = document.getProperties(KmlModelConstants.ATT_DOCUMENT_FEATURES.getName()).iterator();

            //increment for each features
            int idgeom = 0;

            //loop on document properties
            while (propertiesFeat.hasNext()) {
                final Object object = propertiesFeat.next();
                if (object instanceof org.geotoolkit.feature.Feature) {
                    final org.geotoolkit.feature.Feature candidat = (org.geotoolkit.feature.Feature) object;

                    results.add(candidat);
//                    
//                    
//                    //find geometry on tree
//                    final List<Map.Entry<Object, Map<String, String>>> geometries = new ArrayList<Map.Entry<Object, Map<String, String>>>();
//                    fillGeometryListFromFeature(candidat, geometries);
//
//                    //if geometry was found
//                    if (!geometries.isEmpty()) {
//                        //loop to create simpleFeature from geometry
//                        for (final Map.Entry<Object, Map<String, String>> geometry : geometries) {
//                            SimpleFeature simpleFeature = extractFeature(idgeom, geometry);
//
//                            //test if geometry already exist
//                            if(simpleFeature!=null){
//                                if (!results.contains(simpleFeature)) {
//                                    results.add(simpleFeature);
//                                    idgeom++;
//                                }
//                            }
//                        }
                }
            }
        }
        return results;
    }
    
    /** input can be URL or File */
    public void loadURL(Object input) throws Exception {
        final KmlReader reader = new KmlReader();
        reader.setUseNamespace(false);
        reader.setInput(input);
        final Kml kml = reader.read();

        final List<Feature> simplefeatList = resolveFeaturesFromKml(kml);

        for (Feature x : simplefeatList) {
            System.out.println(x);
            
            Property sp = x.getProperty(KmlModelConstants.ATT_STYLE_URL.getName());
            if (sp!=null)
                 System.out.println(sp.getValue());
            System.out.println(x.getClass());
            
            System.out.println(x.getProperties());
            System.out.println("\n\n");
        }
        //System.out.println(simplefeatList);

        reader.dispose();
    }

    /*public void loadKMZ(String path) {
        List<SimpleFeature> e = KmlFeatureUtilities.getAllKMLGeometriesEntries(new File(path));
        System.out.println(e);
    }*/
    
    public void loadLayers() {

    }

    public static void main(String[] args) throws Exception {
        new ImportKMLGT().loadURL(new File("/tmp/haarp.kml"));
        
        //http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week_age.kml
        
        //http://climateviewer.com/kml/pollution/nuclear/Chernobyl-meltdown-Climate-Viewer-3D.kmz
        //http://climateviewer.com/kml/3rdparty/pollution/nuclear/radioactive_seawater/fukushima_radioactive_seawater_ASR.kmz
        //http://climateviewer.com/kml/places/HAARP-HIPAS-Poker-Flat-CV3D.kmz
        
    }
    
    

    /**
     * create a {@link SimpleFeature} from an {@link Map.Entry}
     * @param idgeom current id iterator
     * @param geometry : {@link Map.Entry} which contains a geometry
     * @return a {@link SimpleFeature}
     */
    private static SimpleFeature extractFeature(int idgeom, Map.Entry<Object, Map<String, String>> geometry) {
        final Object geom = geometry.getKey();
        Geometry finalGeom = null;
        //if it's a simple geometry
        if (geom instanceof Geometry) {
            finalGeom = (Geometry) geometry.getKey();
            try {
                //if it's lineString it can cut meridian. So test it
                if (finalGeom instanceof LineString) {
                    LineString lineString = (LineString) finalGeom;
                    MultiLineString multiLine = cutAtMeridian(lineString);
                    if(multiLine!=null){
                        finalGeom = multiLine;
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }

        //it's a geometry collection
        else if(geom instanceof DefaultMultiGeometry){
            final DefaultMultiGeometry kmlabstractGeometry = (DefaultMultiGeometry)geom;
            final List<Geometry> multiGeometry = new ArrayList<Geometry>(0);

            //loop on geometry to add id on a GeometryList
            for (AbstractGeometry abstractGeometry : kmlabstractGeometry.getGeometries()) {
                if(abstractGeometry instanceof Geometry){
                    final Geometry currentGeom = (Geometry)abstractGeometry;
                    multiGeometry.add(currentGeom);
                }
            }

            final GeometryFactory gf = new GeometryFactory();
            Geometry[] geometryArray = new Geometry[multiGeometry.size()];
            for (int i = 0; i < multiGeometry.size(); i++) {
                geometryArray[i] = multiGeometry.get(i);

            }
            finalGeom = new GeometryCollection(geometryArray, gf);
        }

        if(finalGeom!=null){
            return BuildSimpleFeature(idgeom, geometry.getValue(), finalGeom);
        }
        return null;
    }    
    

    /**
     * Build simple feature
     * @param idgeom geometry id
     * @param values no geographic data
     * @param finalGeom geometry need to be insert in feature
     * @return a {@link SimpleFeature}
     */
    private static SimpleFeature BuildSimpleFeature(int idgeom, Map<String, String> values, Geometry finalGeom) {
        //Building simplefeature
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        final String name = "Geometry";
        ftb.setName(name);
        ftb.add("geometry", Geometry.class, CommonCRS.WGS84.normalizedGeographic());

        
        //loop on values to find data names
        for (String valName : values.keySet()) {
            ftb.add(valName, String.class);
        }


        final SimpleFeatureType sft = ftb.buildSimpleFeatureType();
        final SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(sft);

        //Clear the feature builder before creating a new feature.
        sfb.reset();

        //add geometry
        sfb.set("geometry", finalGeom);

        
        
        //add other data
        for (String valName : values.keySet()) {            
            sfb.set(valName, values.get(valName));
        }

        //create simple feature
        final SimpleFeature simpleFeature = sfb.buildFeature("feature" + idgeom);
        simpleFeature.validate();
        return simpleFeature;
    }    
}
