/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

/**
 *
 * @author me
 */
public class ImportSpecificKML {
 
    public static void main(String[] args) throws Exception {
        
        new ImportKML().transformKML("haarp", "http://climateviewer.com/kml/places/HAARP-HIPAS-Poker-Flat-CV3D.kmz", false, true);
        
    }
}
