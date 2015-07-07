package automenta.climatenet;

import automenta.climatenet.knowtention.Channel;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by me on 4/13/15.
 */
abstract public class ReadOnlyChannel<O> extends Channel {

    public ReadOnlyChannel(String id) {
        super(id);
    }

    abstract public O nextValue();

    @Override
    public ObjectNode commit() {
        O o = nextValue();
        if (o instanceof ObjectNode) {
            return super.commit((ObjectNode) o);
        } else {
            return super.commit(SpacetimeWebServer.json(o));
        }
    }

}
