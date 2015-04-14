package automenta.climatenet.data.sim;

import org.geojson.LngLatAlt;
import org.geojson.Polygon;

/**
 * Created by me on 4/14/15.
 */
public class RobotSimulant {

    public final String id;
    boolean air;
    boolean ground;

    /** 2d shadow (top-down) geometric representation, local coordinates */
    Polygon shadow;

    /** 2d shadow (top-down) geometric representation, world coordinates */
    Polygon shadowWorld;

    public final LngLatAlt position;
    public final LngLatAlt velocity;

    public RobotSimulant(String id, double lat, double lon, double size) {
        this.id = id;
        this.position = new LngLatAlt(lon,lat,0);
        this.velocity = new LngLatAlt(0,0,0);
        shadow = new Polygon(l(-size/2,-size/2), l(-size/2,-size/2), l(-size/2,-size/2), l(-size/2,-size/2));
        shadowWorld = new Polygon(shadow.getCoordinates().get(0));
    }

    public Polygon update(double dt) {
        this.position.setLatitude(velocity.getLatitude() * dt);
        this.position.setLongitude(velocity.getLongitude() * dt);
        this.position.setAltitude(velocity.getAltitude() * dt);

        int npoints = shadowWorld.getCoordinates().get(0).size();
        for (int i = 0; i < npoints; i++) {
            LngLatAlt w = shadowWorld.getCoordinates().get(0).get(i);
            LngLatAlt l = shadow.getCoordinates().get(0).get(i);
            w.setLongitude(position.getLongitude() + l.getLongitude());
            w.setLatitude(position.getLatitude() + l.getLatitude());
        }

        return shadowWorld;
    }

    public static LngLatAlt l(double lat, double lon) {
        return new LngLatAlt(lon, lat);
    }

}
