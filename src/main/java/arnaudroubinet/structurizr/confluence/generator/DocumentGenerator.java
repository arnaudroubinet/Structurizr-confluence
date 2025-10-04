package arnaudroubinet.structurizr.confluence.generator;

import com.atlassian.adf.Document;
import com.structurizr.Workspace;
import com.structurizr.view.*;
import java.util.Collection;

/**
 * Generates ADF documents from Structurizr workspace elements. Separates document generation
 * concerns from export orchestration.
 */
public class DocumentGenerator {

  /**
   * Generates a document containing workspace overview with description and views summary.
   *
   * @param workspace the workspace to document
   * @param branchName the branch name (for context)
   * @return ADF document
   */
  public Document generateWorkspaceDocumentation(Workspace workspace, String branchName) {
    Document doc = Document.create();

    // Description
    if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
      doc.paragraph(workspace.getDescription());
    }

    // Add views section overview only (no manual table of contents)
    ViewSet views = workspace.getViews();

    // Views Overview (sections only, no global title)
    if (hasViews(views)) {
      addViewsOverview(doc, views.getSystemLandscapeViews(), "System Landscape Views");
      addViewsOverview(doc, views.getSystemContextViews(), "System Context Views");
      addViewsOverview(doc, views.getContainerViews(), "Container Views");
      addViewsOverview(doc, views.getComponentViews(), "Component Views");
      addViewsOverview(doc, views.getDeploymentViews(), "Deployment Views");
    }

    return doc;
  }

  /**
   * Checks if the view set contains any views.
   *
   * @param views the view set to check
   * @return true if any views exist
   */
  private boolean hasViews(ViewSet views) {
    return (views.getSystemLandscapeViews().size() > 0
        || views.getSystemContextViews().size() > 0
        || views.getContainerViews().size() > 0
        || views.getComponentViews().size() > 0
        || views.getDeploymentViews().size() > 0);
  }

  /**
   * Adds an overview of views to the document.
   *
   * @param doc the document to add to
   * @param views the views to document
   * @param title the section title
   */
  private void addViewsOverview(Document doc, Collection<? extends View> views, String title) {
    if (!views.isEmpty()) {
      doc.h3(title);

      for (View view : views) {
        String viewTitle = view.getTitle();
        if (viewTitle == null || viewTitle.trim().isEmpty()) {
          viewTitle = view.getKey() != null ? view.getKey() : "Untitled View";
        }
        doc.h4(viewTitle);

        if (view.getDescription() != null && !view.getDescription().trim().isEmpty()) {
          doc.paragraph(view.getDescription());
        }

        // Add view key information
        doc.paragraph("Key: " + view.getKey());
      }
    }
  }
}
