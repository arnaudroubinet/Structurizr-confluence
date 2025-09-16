package com.structurizr.confluence.adf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdfNodeTest {
    
    @Test
    void shouldCreateHeadingNode() {
        AdfNode heading = AdfNode.heading(2);
        
        assertEquals("heading", heading.getType());
        assertEquals(2, heading.getAttrs().get("level"));
    }
    
    @Test
    void shouldCreateTextNode() {
        AdfNode text = AdfNode.text("Hello World");
        
        assertEquals("text", text.getType());
        assertEquals("Hello World", text.getText());
    }
    
    @Test
    void shouldCreateParagraphWithText() {
        AdfNode paragraph = AdfNode.paragraph()
            .addContent(AdfNode.text("This is a paragraph."));
        
        assertEquals("paragraph", paragraph.getType());
        assertEquals(1, paragraph.getContent().size());
        assertEquals("text", paragraph.getContent().get(0).getType());
        assertEquals("This is a paragraph.", paragraph.getContent().get(0).getText());
    }
    
    @Test
    void shouldCreateBulletList() {
        AdfNode list = AdfNode.bulletList()
            .addContent(AdfNode.listItem()
                .addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text("First item"))))
            .addContent(AdfNode.listItem()
                .addContent(AdfNode.paragraph()
                    .addContent(AdfNode.text("Second item"))));
        
        assertEquals("bulletList", list.getType());
        assertEquals(2, list.getContent().size());
        assertEquals("listItem", list.getContent().get(0).getType());
    }
    
    @Test
    void shouldCreateCodeBlock() {
        AdfNode codeBlock = AdfNode.codeBlock("java");
        
        assertEquals("codeBlock", codeBlock.getType());
        assertEquals("java", codeBlock.getAttrs().get("language"));
    }
    
    @Test
    void shouldAddMarksToText() {
        AdfNode text = AdfNode.text("Bold text")
            .addMark(AdfMark.strong());
        
        assertEquals(1, text.getMarks().size());
        assertEquals("strong", text.getMarks().get(0).getType());
    }
}