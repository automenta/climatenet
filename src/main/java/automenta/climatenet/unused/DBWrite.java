package automenta.climatenet.unused;

///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package automenta.climatenet;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.geotools.data.DataStore;
//import org.geotools.data.DataStoreFinder;
//import org.geotools.data.DefaultTransaction;
//import org.geotools.data.Transaction;
//import org.geotools.data.collection.ListFeatureCollection;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.data.simple.SimpleFeatureSource;
//import org.geotools.feature.FeatureCollection;
//import org.geotools.feature.simple.SimpleFeatureBuilder;
//import org.geotools.geometry.jts.GeometryBuilder;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.feature.simple.SimpleFeatureType;
//
///**
// *
// * @see http://docs.geotools.org/stable/userguide/library/jdbc/postgis.html
// * http://docs.geotools.org/stable/userguide/library/data/featuresource.html#adding-data
// */
//public class DBWrite {
// 
//    public static void main(String[] arg) throws Exception {
//            Map params = new HashMap();
//            params.put("dbtype", "postgis");        //must be postgis
//            params.put("host", "localhost");        //the name or ip address of the machine running PostGIS
//            params.put("port", 5432);  //the port that PostGIS is running on (generally 5432)
//            
//            params.put("database", "cv");      //the name of the database to connect to.
//            params.put("user", "me");         //the user to connect with
//            params.put("passwd", "");               //the password of the user.
//
//                    
//            DataStore pgDatastore = DataStoreFinder.getDataStore(params);
//            
//            if (pgDatastore == null) throw new RuntimeException("Can not connect to database: " + params);
//
//            String layer = "ogrgeojson";
//            
//            System.out.println(pgDatastore.getSchema(layer));                        
//
//            SimpleFeatureSource f = pgDatastore.getFeatureSource(layer);
//    
//    
//            //SimpleFeatureStore f = (SimpleFeatureStore) pgDatastore.getFeatureWriter(layer, Transaction.AUTO_COMMIT);
//    
//            SimpleFeatureType featureType = f.getSchema();
//    
//            SimpleFeatureBuilder build = new SimpleFeatureBuilder(featureType);
//            GeometryBuilder geom = new GeometryBuilder();
//    
//            List<SimpleFeature> list = new ArrayList<SimpleFeature>();
//            list.add( build.buildFeature("fid1", new Object[]{ geom.point(1,1), "hello", "desc" } ) );
//            list.add( build.buildFeature("fid2", new Object[]{ geom.point(2,3), "martin", "desc" } ) );
//    
//            SimpleFeatureCollection collection = new ListFeatureCollection(featureType, list);
//
//            Transaction transaction = new DefaultTransaction();
//            f.setTransaction( transaction );
//            try {
//                f.addFeatures( collection );
//                transaction.commit(); 
//                System.out.println("Commited Transaction");
//            }
//            catch( Exception eek){
//                System.err.println(eek);
//                transaction.rollback();
//            }
//    
//    
//            //System.out.println("count: " + fsBC.getCount(Query.ALL));        
//            
//            FeatureCollection c = f.getFeatures();
//            
//            System.out.println(c + " " + c.size());
//            
//            /*FeatureIterator ci = c.features();
//            while (ci.hasNext()) {
//                try {
//                    System.out.println(ci.next());
//                }
//                catch (Exception e) {
//                    System.err.println(e);
//                }
//            }*/
//
//    }
//}
