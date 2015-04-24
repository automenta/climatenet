package automenta.climatenet;

import automenta.climatenet.data.graph.MapDBGraph;
import com.google.common.collect.Lists;
import org.mapdb.DBMaker;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 4/23/15.
 */
public class TestMapDBGraph {

    public static void main(String[] ags) {
        MapDBGraph g = new MapDBGraph(DBMaker.newMemoryDB());
        MapDBGraph.Vertex x = g.addVertex("x");
        MapDBGraph.Vertex y = g.addVertex("y");
        MapDBGraph.Edge xy = g.addEdge(x, y, "xy");

        assertEquals(2, Lists.newArrayList(g.getVertices()).size() );
        assertEquals(1, Lists.newArrayList(g.getEdges()).size() );

        assertNotNull(g.getVertex("x"));
    }
}
