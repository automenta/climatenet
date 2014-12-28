/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Tag {
    Logger logger = LoggerFactory.getLogger(Tag.class);
    
    public String id;
    public Map<String,Double> supers;   
    public String name;
    public String description;
    public Map<String,Object> meta = new HashMap();

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = null;
        this.supers = Collections.EMPTY_MAP;
    }
    
    public Tag url(String u) {
        meta.put("url", u);
        return this;
    }

    public XContentBuilder toJSON(boolean withID) {
        try {
            XContentBuilder b = jsonBuilder().startObject();
            if (withID)
                b.field("id", id);
            b.field("name", name);
            if (description!=null)
                b.field("description", description);
            b.field("supers", supers);
            b.field("meta", meta);
            return b.endObject();
        } catch (IOException ex) {
            logger.warn(ex.toString());
            return null;
        }
    }
    
    
}
