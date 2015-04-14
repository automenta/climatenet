/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.data.elastic.EmbeddedES;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author me
 */
public class EmbeddedESTest {
    
    @Test public void testES() {
        EmbeddedES e = new EmbeddedES();
        
        assertTrue( Files.exists(Paths.get(e.getDataDirectory())) );
        
        System.out.println(e.getDataDirectory());
        
        e.close(true);
        
        assertTrue( !Files.exists(Paths.get(e.getDataDirectory())) );
    }
}
