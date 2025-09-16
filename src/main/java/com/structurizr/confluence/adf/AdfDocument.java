package com.structurizr.confluence.adf;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an Atlassian Document Format (ADF) document.
 * ADF is the internal representation of rich content in Atlassian products.
 */
public class AdfDocument {
    
    @JsonProperty("version")
    private int version = 1;
    
    @JsonProperty("type")
    private String type = "doc";
    
    @JsonProperty("content")
    private List<AdfNode> content;
    
    public AdfDocument() {
        this.content = new ArrayList<>();
    }
    
    public AdfDocument addContent(AdfNode node) {
        this.content.add(node);
        return this;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public List<AdfNode> getContent() {
        return content;
    }
    
    public void setContent(List<AdfNode> content) {
        this.content = content;
    }
}