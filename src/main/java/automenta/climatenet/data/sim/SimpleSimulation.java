package automenta.climatenet.data.sim;

import automenta.knowtention.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geojson.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by me on 4/14/15.
 */
public class SimpleSimulation extends Channel implements Runnable {

    List<RobotSimulant> obj = new ArrayList();
    long lastUpdate = System.currentTimeMillis();
    private final ObjectMapper om = new ObjectMapper();
    long updatePeriodMS = 500;

    public SimpleSimulation(String id) {
        super(id);
        RobotSimulant drone1 = new RobotSimulant("drone1", 0,0,0.1,
                new RobotSimulant.GeoSynchOrbitController());

        //om.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
        //om.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        //om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);

        obj.add(drone1);

        new Thread(this).start();
    }

    @Override
    public void run() {

        while (true) {
            update();

            try {
                Thread.sleep(updatePeriodMS);
            } catch (InterruptedException e) {}
        }
    }

    protected void update() {
        long now = System.currentTimeMillis();
        double dt = (now - lastUpdate) / 1000.0;
        lastUpdate = now;




        ObjectNode next = om.getNodeFactory().objectNode();
        for (RobotSimulant r : obj) {
            Polygon p = r.update(dt);


            try {
                ////TODO hack fuck jackson this is ridiculous
                //TODO avoid using intermediate string repr here
                String s = om.writeValueAsString(p);
                ObjectNode vv = om.readValue(s, ObjectNode.class);

                next.put(r.id, vv);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        commit(next);
    }

}
