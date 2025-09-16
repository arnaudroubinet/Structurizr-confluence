package com.structurizr.confluence.adf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a node in an ADF document structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdfNode {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("attrs")
    private Map<String, Object> attrs;
    
    @JsonProperty("content")
    private List<AdfNode> content;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("marks")
    private List<AdfMark> marks;
    
    public AdfNode(String type) {
        this.type = type;
    }
    
    public static AdfNode paragraph() {
        return new AdfNode("paragraph");
    }
    
    public static AdfNode heading(int level) {
        AdfNode heading = new AdfNode("heading");
        heading.addAttr("level", level);
        return heading;
    }
    
    public static AdfNode text(String text) {
        AdfNode textNode = new AdfNode("text");
        textNode.setText(text);
        return textNode;
    }
    
    public static AdfNode bulletList() {
        return new AdfNode("bulletList");
    }
    
    public static AdfNode listItem() {
        return new AdfNode("listItem");
    }
    
    public static AdfNode codeBlock(String language) {
        AdfNode codeBlock = new AdfNode("codeBlock");
        if (language != null) {
            codeBlock.addAttr("language", language);
        }
        return codeBlock;
    }
    
    public AdfNode addContent(AdfNode node) {
        if (this.content == null) {
            this.content = new ArrayList<>();
        }
        this.content.add(node);
        return this;
    }
    
    public AdfNode addAttr(String key, Object value) {
        if (this.attrs == null) {
            this.attrs = new HashMap<>();
        }
        this.attrs.put(key, value);
        return this;
    }
    
    public AdfNode addMark(AdfMark mark) {
        if (this.marks == null) {
            this.marks = new ArrayList<>();
        }
        this.marks.add(mark);
        return this;
    }
    
    // Getters and setters
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
    
    public List<AdfNode> getContent() {
        return content;
    }
    
    public void setContent(List<AdfNode> content) {
        this.content = content;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public List<AdfMark> getMarks() {
        return marks;
    }
    
    public void setMarks(List<AdfMark> marks) {
        this.marks = marks;
    }
}