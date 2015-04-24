package automenta.climatenet;

import automenta.climatenet.data.SchemaOrg;
import automenta.climatenet.data.graph.MapDBGraph;
import org.junit.Test;
import org.mapdb.DBMaker;

import java.io.IOException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 4/23/15.
 */
public class TestMapDBGraph {

    @Test
    public void testVertexEdge() {
        MapDBGraph g = new MapDBGraph(DBMaker.newMemoryDB());
        MapDBGraph.Vertex x = g.addVertex("x");
        MapDBGraph.Vertex y = g.addVertex("y");
        MapDBGraph.Edge xy = g.addEdge(x, y, "xy");

        assertEquals(2, g.getVertexList().size());
        assertEquals(1, g.getEdgeList().size());

        assertNotNull(g.getVertex("x"));
    }

    @Test public void testSchemaOrg() throws IOException {

        MapDBGraph g = new MapDBGraph(DBMaker.newMemoryDB());
        SchemaOrg.load(g);

//        for (MapDBGraph.Vertex v : g.getVertices()) {
//            System.out.println(v);
//        }
//        for (MapDBGraph.Edge v : g.getEdges()) {
//            System.out.println(v);
//        }

        assertTrue( g.getVertexList().size() > 50 );
        assertTrue( g.getEdgeList().size() > 50 );
    }
}
