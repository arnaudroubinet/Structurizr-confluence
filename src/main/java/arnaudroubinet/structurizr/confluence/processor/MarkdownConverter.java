package arnaudroubinet.structurizr.confluence.processor;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.Arrays;

/**
 * Robust Markdown to HTML converter using flexmark-java with common extensions (tables,
 * strikethrough, task lists). The output HTML is then fed into the HtmlToAdfConverter for ADF
 * conversion and title extraction.
 */
public class MarkdownConverter {

  private final Parser parser;
  private final HtmlRenderer renderer;

  public MarkdownConverter() {
    MutableDataSet options = new MutableDataSet();
    options.set(
        Parser.EXTENSIONS,
        Arrays.asList(
            TablesExtension.create(), StrikethroughExtension.create(), TaskListExtension.create()));

    // Soft breaks to spaces (closer to our basic converter behavior)
    options.set(HtmlRenderer.SOFT_BREAK, " \n");

    this.parser = Parser.builder(options).build();
    this.renderer = HtmlRenderer.builder(options).escapeHtml(false).build();
  }

  public String toHtml(String markdown) {
    if (markdown == null || markdown.trim().isEmpty()) return "";
    Node doc = parser.parse(markdown);
    return renderer.render(doc);
  }
}
