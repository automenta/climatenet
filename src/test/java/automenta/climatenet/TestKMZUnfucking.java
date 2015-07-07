package automenta.climatenet;

import automenta.climatenet.data.elastic.ElasticSpacetime;

import java.io.File;

/**
 * Created by me on 7/7/15.
 */
public class TestKMZUnfucking {

    public static void main(String[] args) throws Exception {


        ImportKML kml = new ImportKML(ElasticSpacetime.memory("cv"),
                TestKMZUnfucking.class.getSimpleName().toString());

        kml.task(new File("/tmp/kmz/OneFolder.kmz")).run();



    }
}
