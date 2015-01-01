/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.IOException;
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
    
    public final String id;
    public Map<String,Double> inh;   /** intensional inheritance */
    public String name;
    public String description;
    public Map<String,Object> meta = new HashMap();
    private String icon;

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = null;
        this.inh = new HashMap();
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
            if ((description!=null) && (!description.isEmpty()))
                b.field("description", description);
            if ((inh!=null) && (!inh.isEmpty()))
                b.field("inh", inh);
            if ((meta!=null) && (!meta.isEmpty()))
                b.field("meta", meta);
            if (icon!=null)
                b.startObject("style").field("iconUrl", icon).endObject();
            
            return b.endObject();
        } catch (IOException ex) {
            logger.warn(ex.toString());
            return null;
        }
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public void icon(String icon) {
        this.icon = icon;
    }
    
    
}
