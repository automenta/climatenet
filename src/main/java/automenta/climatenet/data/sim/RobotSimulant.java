package automenta.climatenet.data.sim;

import com.google.common.collect.Lists;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import java.util.List;

/**
 * Created by me on 4/14/15.
 */
public class RobotSimulant {

    public final String id;
    boolean air;
    boolean ground;

    /** 2d shadow (top-down) geometric representation, local coordinates */
    Polygon shadowLocal;

    /** 2d shadow (top-down) geometric representation, world coordinates */
    Polygon shadowWorld;

    public final LngLatAlt position;

    public interface SimulantController {
        void update(RobotSimulant r, double dt);
    }

    public final List<SimulantController> controllers;

    public RobotSimulant(String id, double lat, double lon, double size, SimulantController... ctl) {
        this.id = id;
        this.position = new LngLatAlt(lon,lat,0);
        shadowLocal = new Polygon(l(-size/2,-size/2), l(-size/2,-size/2), l(size/2,size/2), l(size/2,size/2));
        shadowWorld = new Polygon(shadowLocal.getCoordinates().get(0));
        controllers = Lists.newArrayList(ctl);
    }

    public static class GeoSynchOrbitController implements SimulantController {

        double lonPerSecond = 0.1;
        double altitutde = 0;

        @Override
        public void update(RobotSimulant r, double dt) {
            r.position.setLatitude(0);
            r.position.setLongitude(lonPerSecond * dt + r.position.getLongitude());
        }
    }

    public static class VelocityController implements SimulantController {
        public final LngLatAlt velocity = new LngLatAlt(0,0,0);

        @Override
        public void update(RobotSimulant r, double dt) {
            r.position.setLatitude(velocity.getLatitude() * dt);
            r.position.setLongitude(velocity.getLongitude() * dt);
            r.position.setAltitude(velocity.getAltitude() * dt);
        }
    }

    public Polygon update(double dt) {

        for (SimulantController c : controllers) {
            c.update(this, dt);
        }

        int npoints = shadowWorld.getCoordinates().get(0).size();
        for (int i = 0; i < npoints; i++) {
            LngLatAlt w = shadowWorld.getCoordinates().get(0).get(i);
            LngLatAlt l = shadowLocal.getCoordinates().get(0).get(i);
            w.setLongitude(position.getLongitude() + l.getLongitude());
            w.setLatitude(position.getLatitude() + l.getLatitude());
        }

        return shadowWorld;
    }

    public static LngLatAlt l(double lat, double lon) {
        return new LngLatAlt(lon, lat);
    }

}
