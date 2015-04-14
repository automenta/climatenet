package automenta.climatenet.data.sim;

import automenta.knowtention.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public SimpleSimulation() {
        super("sim");
        RobotSimulant drone1 = new RobotSimulant("drone1", 0,0,0.1);
        drone1.velocity.setLatitude(0.001);
        drone1.velocity.setLongitude(0.001);

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

        root.removeAll();

        for (RobotSimulant r : obj) {
            Polygon p = r.update(dt);
            root.set(r.id, om.getNodeFactory().pojoNode(p));
        }

        commit();
    }

}
