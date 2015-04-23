package automenta.climatenet.p2p;

import automenta.knowtention.Core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by me on 4/22/15.
 */
public class NObject {

    String id, name;
    SpacetimeTagPlan.TimePoint when = null;
    SpacetimeTagPlan.SpacePoint where = null;
    Map<String, Double> tags = new HashMap(); //TODO use a ObjectDouble primitive map structure

    public NObject() {
        this(Core.uuid());
    }

    public NObject(String id) {
        this(id, null);

    }

    public NObject(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Double> getTags() {
        return tags;
    }

    /**
     * timepoint, or -1 if none
     */
    public SpacetimeTagPlan.TimePoint getWhen() {
        return when;
    }

    public SpacetimeTagPlan.SpacePoint getWhere() {
        return where;
    }

    public Collection<String> tagSet() {
        return tags.keySet();
    }

    public NObject when(long when) {
        this.when = new SpacetimeTagPlan.TimePoint(when);
        return this;
    }

    public NObject where(double lat, double lng) {
        return where(new SpacetimeTagPlan.SpacePoint(lat, lng));
    }

    public NObject where(SpacetimeTagPlan.SpacePoint s) {
        this.where = s;
        return this;
    }

    public NObject tag(String tag) {
        return tag(tag, 1.0);
    }

    public NObject tag(String tag, double strength) {
        tags.put(tag, strength);
        return this;
    }

}
