package automenta.climatenet;

import automenta.climatenet.data.elastic.ElasticSpacetime;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;

/**
 * synchronizes a document with elastic db
 */
public class ElasticChannel extends ReadOnlyChannel {

    private final String eType;
    private final String eID;
    private final ElasticSpacetime db;
    boolean readOnly = false;

//    /** manages a stream of documents with automatically generated UUID */
//    public ElasticChannel(ElasticSpacetime db, String type) {
//        this(db, null, type);
//    }

    /** manages a specific document */
    public ElasticChannel(ElasticSpacetime db, String id, String type) {
        super(id);
        this.db = db;
        this.eID = id;
        this.eType = type;
    }

    @Override
    public Object nextValue() {
        SearchResponse sr = db.searchID(new String[]{eID}, 0, 1, eType);
        SearchHits hits = sr.getHits();
        long num = hits.getTotalHits();
        if (num == 0) {
            return "Missing";
        } else if (num > 1) {
            return "Ambiguous";
        }

        return hits.getAt(0).sourceAsMap();
    }

    @Override
    public synchronized ObjectNode commit(ObjectNode next) {

        if (readOnly) {
            throw new RuntimeException(this + " is set read-only; unable to commit change to database");
        }

        db.update(eType, eID, next.toString());

        return super.commit(next);
    }



}
