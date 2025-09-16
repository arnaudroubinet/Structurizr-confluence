package com.structurizr.confluence.adf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a mark (formatting) in an ADF document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdfMark {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("attrs")
    private Map<String, Object> attrs;
    
    public AdfMark(String type) {
        this.type = type;
    }
    
    public static AdfMark strong() {
        return new AdfMark("strong");
    }
    
    public static AdfMark em() {
        return new AdfMark("em");
    }
    
    public static AdfMark code() {
        return new AdfMark("code");
    }
    
    public static AdfMark link(String href) {
        AdfMark link = new AdfMark("link");
        link.addAttr("href", href);
        return link;
    }
    
    public AdfMark addAttr(String key, Object value) {
        if (this.attrs == null) {
            this.attrs = new HashMap<>();
        }
        this.attrs.put(key, value);
        return this;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, Object> getAttrs() {
        return attrs;
    }
    
    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
}