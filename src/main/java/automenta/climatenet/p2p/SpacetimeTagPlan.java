package automenta.climatenet.p2p;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.ml.clustering.*;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author me
 *
 * http://commons.apache.org/proper/commons-math/javadocs/api-3.3/org/apache/commons/math3/ml/clustering/FuzzyKMeansClusterer.html
 */
public class SpacetimeTagPlan {

    public final TagVectorMapping mapping;
    public final List<Goal> goals = new ArrayList();

    //Parameters, can be changed between computations
    protected double timeWeight = 1.0;  //normalized to seconds
    protected double spaceWeight = 1.0; //meters
    protected double altWeight = 1.0; //meters
    protected double tagWeight = 1.0;  //weight per each individual tag
    protected double minPossibilityTagStrength = 0.02; //minimum strength that a resulting tag must be to be added to a generated Possibility

    public static class NObject {

        TimeRange when = null;
        SpacePoint where = null;
        Map<String,Double> tags = new HashMap(); //TODO use a ObjectDouble primitive map structure

        public Map<String, Double> getTagStrengths() {
            return null;
        }

        /** timepoint, or -1 if none */
        public TimeRange when() {
            return when;
        }

        public SpacePoint where() {
            return where;
        }

        public Collection<String> tagSet() {
            return tags.keySet();
        }

        public void setWhen(long when) {
            this.when = new TimeRange(when);
        }

        public void setWhere(SpacePoint s) {
            this.where = s;
        }

        public void setTag(String tag, double strength) {
            tags.put(tag, strength);
        }

    }

    public static class SpacePoint implements Serializable {
        //public String planet = "Earth";
        public double lat;
        public double lon;
        public double alt; //in meters

        public SpacePoint(double lat, double lon) {
            this(lat, lon, Double.NaN);
        }

        public SpacePoint(double lat, double lon, double alt) {
            this.lat = lat;
            this.lon = lon;
            this.alt = alt;
        }

//        public GeoHash getGeoHash(int bits) {
//            /**
//             * This method uses the given number of characters as the desired precision
//             * value. The hash can only be 64bits long, thus a maximum precision of 12
//             * characters can be achieved.
//             */
//            return GeoHash.withBitPrecision(lat,lon, bits);
//        }



        public String toString() {
            String s = String.format("%.2f", lat) + "," + String.format("%.2f", lon);

            if (!Double.isNaN(alt)) {
                s += "," + alt;
            }
            return s;
        }
    }

    /**
     * TODO use a better discretization method (Ex: 1D SOM)
     * @author me
     */
    public static class TimeRange {

        public long from, to;

        public TimeRange(long at) {
            this(at, at);
        }

        public TimeRange(long from, long to) {
            this.from = from;
            this.to = to;
        }

        public boolean isInstant() { return from == to; }

        public long duration() { return to-from; }

        public List<Long> discretize(long timePeriod) {
            List<Long> l = new ArrayList();
            long d = duration();
            if (d < timePeriod) {
                //mid point
                l.add( ((from + to)/2) );
            }
            else {
                //distribute the points evenly
                long remainder = d % timePeriod;
                long t = from + remainder/2;
                while (t < to) {
                    l.add( (t) );
                    t+=timePeriod;
                }
            }
            return l;
        }

    }

    //internal
    private final boolean time;
    private final boolean space;
    private final boolean spaceAltitude;
    private final boolean tags;
    public final List<NObject> objects;
    private double timeWeightNext = timeWeight;
    private double spaceWeightNext = spaceWeight;
    private double tagWeightNext = tagWeight;
    private double altWeightNext = altWeight;

    public static class TagVectorMapping extends ArrayList<String> {
        public final long timePeriod;
        private static final long NullTimePoint = -1;
        double[] min, max;
        private int tagIndex = -1; //index where tags begin in the mapping; they continue until the end

        /**
         *
         * @param timePeriod  in ms (unixtime)
         */
        public TagVectorMapping(long timePeriod) {
            this.timePeriod = timePeriod;
        }

        /** reset before generating a new sequence of goals */
        public void reset() {
            min = max = null;
        }

        public List<Goal> newGoals(NObject o) {
            boolean firstGoal = false;
            if (min == null) {
                firstGoal = true;
                min = new double[size()];
                max = new double[size()];
            }

            List<Goal> goals = new LinkedList();
            Map<String,Double> ts = o.getTagStrengths();


            SpacePoint sp = null;

            List<Long> times = times = new ArrayList(1);

            if (get(0).equals("time")) {
                //convert time ranges to a set of time points
                TimeRange tr = o.when();
                if (tr != null) {
                    times.addAll(tr.discretize(timePeriod));
                }
                else {
//                    long tp = o.when();
//                    if (tp!=-1) {
//                        times.add(tp);
//                    }
//                    else {
                    //no time involvement, ignore this NObject
                    return goals;
                    //}
                }
            }
            else {
                //add a null timepoint so the following iteration occurs
                times.add(NullTimePoint);
            }

            tagIndex = -1;

            for (long currentTime : times) {

                double[] d = new double[this.size()];
                int i = 0;

                for (String s : this) {
                    if (s.equals("lat")) {
                        sp = o.where();
                        if (sp==null) {
                            //this nobject is invalid, return; goals will be empty
                            return goals;
                        }
                        d[i] = sp.lat;
                    }
                    else if (s.equals("lon")) {
                        d[i] = sp.lon;
                    }
                    else if (s.equals("time")) {
                        d[i] = currentTime;
                    }
                    else if (s.equals("alt")) {
                        d[i] = sp.alt;
                    }
                    else {
                        if (tagIndex == -1) {
                            tagIndex = i;
                        }
                        Double strength = ts.get(s);
                        if (strength!=null) {
                            d[i] = strength;
                        }
                    }
                    i++;
                }
                if (firstGoal) {
                    System.arraycopy(d, 0, min, 0, d.length);
                    System.arraycopy(d, 0, max, 0, d.length);
                }
                else {
                    for (int j = 0; j < d.length; j++) {
                        if (d[j] < min[j]) min[j] = d[j];
                        if (d[j] > max[j]) max[j] = d[j];
                    }
                }

                goals.add(new Goal(o, this, d));
            }

            return goals;
        }

        /** normalize (to 0..1.0) a collection of Goals with respect to the min/max calculated during the prior goal generation */
        public void normalize(Collection<Goal> goals) {

            for (Goal g : goals) {
                double d[] = g.getPoint();
                for (int i = 0; i < d.length; i++) {
                    double MIN = min[i];
                    double MAX = max[i];
                    if (MIN!=MAX) {
                        d[i] = (d[i] - MIN) / (MAX-MIN);
                    }
                    else {
                        d[i] = 0.5;
                    }
                }
            }
        }

        public void denormalize(Goal g) {
            denormalize(g.getPoint());
        }

        public void denormalize(double[] d) {
            for (int i = 0; i < d.length; i++) {
                double MIN = min[i];
                double MAX = max[i];
                if (MIN!=MAX) {
                    d[i] = d[i] * (MAX-MIN) + MIN;
                }
                else {
                    d[i] = MIN;
                }
            }

            //normalize tags against each other
            if (tagIndex >= d.length) return;

            double min, max;
            min = max = d[tagIndex];
            for (int i = tagIndex+1; i < d.length; i++) {
                if (d[i] > max) max = d[i];
                if (d[i] < min) min = d[i];
            }
            if (min!=max) {
                for (int i = tagIndex; i < d.length; i++) {
                    d[i] = (d[i] - min)/(max-min);
                }
            }

        }


    }


    /** a point in goal-space; the t parameter is included for referencing what the dimensions mean */
    public static class Goal extends DoublePoint {
        private final TagVectorMapping mapping;

        /** the involved object */
        private final NObject object;

        public Goal(NObject o, TagVectorMapping t, double[] v) {
            super(v);
            this.object = o;
            this.mapping = t;
        }


    }

    //TODO add a maxDimensions parameter that will exclude dimensions with low aggregate strength

    //TODO support negative strengths to indicate avoidance

    /**
     *
     * @param n list of objects
     * @param tags whether to involve tags
     * @param timePeriod  time period of discrete minimum interval; set to zero to not involve time as a dimension
     * @param space whether to involve space latitude & longitude
     * @param spaceAltitude whether to involve space altitude
     */
    public SpacetimeTagPlan(List<NObject> n, boolean tags, long timePeriod, boolean space, boolean spaceAltitude) {

        this.objects = n;

        //1. compute mapping
        this.mapping = new TagVectorMapping(timePeriod);

        this.time = timePeriod > 0;
        this.space = space;
        this.spaceAltitude = spaceAltitude;
        this.tags = tags;


        if (this.time)
            mapping.add("time");
        if (space) {
            mapping.add("lat");
            mapping.add("lon");
        }
        if (spaceAltitude) {
            mapping.add("alt");
        }

        //TODO filter list of objects according to needed features for the clustering parameters

        if (tags) {
            Set<String> uniqueTags = new HashSet();
            for (NObject o : n) {
                uniqueTags.addAll(o.tagSet());
            }
            mapping.addAll(uniqueTags);
        }


    }

    public interface PlanResult {
        public void onFinished(SpacetimeTagPlan plan, List<Possibility> possibilities);
        public void onError(SpacetimeTagPlan plan, Exception e);
    }


    public void update(PlanResult r) {
        try {
            List<Possibility> result = compute();
            r.onFinished(this, result);
            return;
        }
        catch (Exception e) {
            r.onError(this, e);
        }
    }

    protected synchronized List<Possibility> compute() {
        goals.clear();
        mapping.reset();

        this.spaceWeight = this.spaceWeightNext;
        this.altWeight = this.altWeightNext;
        this.timeWeight = this.timeWeightNext;
        this.tagWeight = this.tagWeightNext;

        //2. compute goal vectors
        for (NObject o : objects) {
            goals.addAll(mapping.newGoals(o));
        }

        //3. normalize
        mapping.normalize(goals);


        //4. distance function
        DistanceMeasure distanceMetric = new DistanceMeasure() {

            @Override
            public double compute(double[] a, double[] b) {
                double dist = 0;
                int i = 0;

                if (time) {
                    dist += Math.abs(a[i] - b[i]) * timeWeight;
                    i++;
                }
                if (space) {
                    //TODO use earth surface distance measurement on non-normalized space lat,lon coordinates

                    if (spaceWeight!=0) {
                        double dx = Math.abs(a[i] - b[i]);
                        i++;
                        double dy = Math.abs(a[i] - b[i]);
                        i++;

                        double ed = Math.sqrt( dx*dx + dy*dy );
                        dist += ed * spaceWeight;
                    }
                    else {
                        i+=2;
                    }
                }
                if (spaceAltitude) {
                    dist += Math.abs(a[i] - b[i]) * altWeight;
                    i++;
                }
                if (tags) {
                    if ((a.length > 0) && (tagWeight!=0)) {
                        double tagWeightFraction = tagWeight / (a.length);
                        for ( ;i < a.length; i++) {
                            dist += Math.abs(a[i] - b[i]) * tagWeightFraction;
                        }
                    }
                }

                return dist;
            }

        };

        //5. cluster

        List<Cluster<Goal>> centroids = cluster(distanceMetric);

        //6. denormalize and return annotated objects
        for (Goal g : goals) {
            mapping.denormalize(g);
        }

        return getPossibilities(centroids);
    }

    private List<Cluster<Goal>> cluster(DistanceMeasure distanceMetric) {
        return clusterDBScan(distanceMetric);
    }

    private List<Cluster<Goal>> clusterDBScan(DistanceMeasure distanceMetric) {
        double radius = 1.0; //if all points are normalized
        DBSCANClusterer<Goal> clusterer = new DBSCANClusterer<Goal>(radius, 1, distanceMetric);
        return clusterer.cluster(goals);
    }

    private List<CentroidCluster<Goal>> clusterFuzzyKMeans(DistanceMeasure distanceMetric) {

        //TODO use a clustering class to hold these for the fuzzyKmeans impl
        int numCentroids = 1;
        int maxIterations = 2;
        double fuzziness = 0.5;

        FuzzyKMeansClusterer<Goal> clusterer = new FuzzyKMeansClusterer<Goal>(numCentroids, fuzziness, maxIterations, distanceMetric);
        List<CentroidCluster<Goal>> centroids = clusterer.cluster(goals);
        return centroids;
    }


    public TagVectorMapping getMapping() {
        return mapping;
    }

    public class Possibility extends NObject {
        //private final double[] center;

        public Possibility() {
            //this.center = center;
        }

        //public double[] getCenter() {
            //return center;
        //}


    }

    protected List<Possibility> getPossibilities(List<Cluster<Goal>> centroids) {
        List<Possibility> l = new ArrayList(centroids.size());

        for (Cluster<Goal> c : centroids) {
            double[] point;
            if (c instanceof CentroidCluster) {
                point = ((CentroidCluster)c).getCenter().getPoint();
            }
            else {
                //find the centroid of the points in the cluster
                Goal g0 = c.getPoints().get(0);

                ArrayRealVector v = new ArrayRealVector(g0.getPoint().length);
                for (Goal g : c.getPoints()) {
                    //TODO avoid allocating new ArrayRealVector here
                    v.combineToSelf(1, 1, new ArrayRealVector(g.getPoint()));
                }
                point = v.getDataRef();
            }

            mapping.denormalize(point);

            Possibility p = new Possibility();
            int i = 0;
            if (time) {
                long when = (long)point[i++];

                //TODO use timerange based on discretizing period duration?
                //p.add("when", new TimePoint((long)when));
                p.setWhen(when);
            }
            SpacePoint s = null;
            if (space) {
                double lat = point[i++];
                double lon = point[i++];
                p.setWhere(s = new SpacePoint(lat, lon));
            }


            if (spaceAltitude) {
                double alt = point[i++];
                if (s == null) {
                    p.setWhere(s = new SpacePoint(0,0,alt));
                }
                else
                    s.alt = alt;
            }


            if (tags) {
                for ( ;i < point.length; i++) {
                    double strength = point[i];
                    if (strength > minPossibilityTagStrength) {
                        String tag = mapping.get(i);
                        p.setTag(tag, strength);
                    }
                }
            }

            l.add(p);
        }

        return l;
    }

    public void setTimeWeight(double timeWeight) {        this.timeWeightNext = timeWeight;    }
    public void setSpaceWeight(double spaceWeight) {        this.spaceWeightNext = spaceWeight;    }
    public void setTagWeight(double tagWeight) {       this.tagWeightNext = tagWeight;    }
    public void setAltWeight(double altWeight) {        this.altWeightNext = altWeight;    }
    public double getAltWeight() {  return altWeight;   }
    public double getSpaceWeight() { return spaceWeight;   }
    public double getTagWeight() {  return tagWeight;    }
    public double getTimeWeight() { return timeWeight;    }




}
