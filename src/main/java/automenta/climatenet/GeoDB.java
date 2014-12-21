/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.Serializable;

/**
 * Geographic database 
 * https://github.com/Geomatys/geotoolkit/blob/master/demos/geotk-demo-samples/src/main/java/org/geotoolkit/pending/demo/tree/TreeDemo.java
 */
public class GeoDB {
    
    public interface Geobound {
        public boolean intersects(Geobound p);
    }
    
    public static class Geopoint implements Geobound, Serializable {
        public float lat;
        public float lon;
        public float alt;        

        @Override
        public boolean intersects(Geobound p) {
            if (p instanceof Geopoint) {
                
            }
            else if (p instanceof Geocube) {
                
            }
            return false;
        }
        
    }
    
    public static class Geocube implements Geobound, Serializable {
        public Geopoint upper;
        public Geopoint lower;

        @Override
        public boolean intersects(Geobound p) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
  
    }
    
    public static class Feature {

        public String[] style;
        public String name;
        public String description;
        
    }
    
    public GeoDB() {
        
    }
    
    public static void main(String[] args) {
        
        GeoDB g = new GeoDB();
        
    }
}
