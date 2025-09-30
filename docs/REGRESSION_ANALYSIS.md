# Rapport détaillé de régression - Structurizr Confluence Exporter

## Contexte du problème

L'utilisateur signale deux régressions principales:
1. **Structure des pages**: Le `parentPageId` ne fonctionne plus comme avant (créer des sous-pages sans modifier la page parent)
2. **Export des diagrammes**: La récupération des diagrammes ne fonctionne plus correctement

## Analyse chronologique des changements

### Version originale (commit fdbcfd4 - Premier commit fonctionnel)

#### Structure de pages créée
```
Page principale (nom = branchName)
├── Documentation (page de sommaire)
│   ├── Section 1 (titre extrait du contenu)
│   ├── Section 2 (titre extrait du contenu)
│   └── Section N (titre extrait du contenu)
├── Views (exportViews - plusieurs pages)
├── Model (exportModel)
├── Schémas (exportDiagramsPage - page dédiée avec liste)
└── ADRs (exportDecisions)
```

#### Code de export() - Version originale
```java
public void export(Workspace workspace, String branchName) {
    // 1. Page principale
    String mainPageId = confluenceClient.createOrUpdatePage(mainPageTitle, ...);
    
    // 2. Page Documentation SOUS la page principale
    String documentationPageId = confluenceClient.createOrUpdatePage(
        documentationPageTitle, ..., mainPageId);  // ← mainPageId comme parent
    
    // 3. Sections SOUS la page Documentation
    exportWorkspaceDocumentationSections(workspace, documentationPageId, branchName);
    
    // 4. Autres pages SOUS la page principale
    exportViews(workspace, mainPageId);
    exportModel(workspace, mainPageId);
    exportDecisions(workspace, mainPageId, branchName);
    exportDiagramsPage(mainPageId, workspace);  // ← Page Schémas
}
```

#### Méthode exportWorkspaceDocumentationSections - Version originale
```java
public void exportWorkspaceDocumentationSections(..., String parentPageId, ...) {
    for (Section section : workspace.getDocumentation().getSections()) {
        // Créer UNE PAGE PAR SECTION
        String pageTitle = branchName + " - " + actualTitle;
        String pageId = confluenceClient.createOrUpdatePage(
            pageTitle, adfJson, parentPageId);  // ← parentPageId = documentationPageId
    }
}
```

**Point clé**: Chaque section créait sa propre page Confluence sous Documentation.

#### Méthode exportDiagramsPage - Version originale
```java
private void exportDiagramsPage(String parentPageId, Workspace workspace) {
    Document diagramsDoc = Document.create().h1("Schémas");
    // Ajoute description et liste de toutes les vues avec métadonnées
    for (View view : views.getViews()) {
        diagramsDoc.h2(diagramName);
        diagramsDoc.bulletList(...);
        // Ajoute lien vers image PNG
    }
    String diagramsPageId = confluenceClient.createOrUpdatePage(
        "Schémas", ..., parentPageId);  // ← parentPageId = mainPageId
}
```

**Point clé**: Une page dédiée "Schémas" listait tous les diagrammes.

---

### Commit 2d0a3e9 - "Complete Structurizr to Confluence formatting preservation with Puppeteer-based diagram export"

#### Changements majeurs
1. **Documentation sections inline**: Les sections ne créent plus de sous-pages
2. **Introduction de Puppeteer**: Export des diagrammes via script Node.js
3. **Suppression de exportDiagramsPage**: Plus de page "Schémas" dédiée

#### Code modifié dans export()
```java
public void export(Workspace workspace, String branchName) {
    // 1. Export diagrams FIRST avec Puppeteer
    List<File> exportedDiagrams = diagramExporter.exportDiagrams(workspace);
    
    // 2. Page principale
    String mainPageId = ...;
    
    // 3. Page Documentation
    String documentationPageId = ...;
    
    // 4. INLINE des sections dans Documentation
    // Boucle sur les sections et concatène le contenu dans documentationNode
    for (Section section : ...) {
        // Convertir et ajouter au contenu de la page Documentation
        docContent.add(child);
    }
    confluenceClient.updatePageById(documentationPageId, ..., finalDocumentationJson);
    // Commentaire: "No longer create sub-pages for sections"
    
    // 5. Reste des exports
    exportAllViewsSinglePage(workspace, mainPageId);  // ← Nouvelle méthode
    exportModel(workspace, mainPageId);
    exportDecisions(workspace, mainPageId, branchName);
    // exportDiagramsPage SUPPRIMÉ
}
```

**Régression 1**: Les sections ne créent plus de sous-pages, tout est inline dans Documentation.

**Régression 2**: La page "Schémas" n'existe plus, remplacée par "Views" (exportAllViewsSinglePage).

---

### Commit f58004a - "Complete CLI cleanup, Puppeteer to Playwright Java migration"

#### Changement majeur
- **Migration Puppeteer → Playwright**: Export via script Node.js remplacé par code Java

#### Code DiagramExporter - Avant (Puppeteer)
```java
// Lancer script Node.js externe
ProcessBuilder processBuilder = new ProcessBuilder(
    "node", "export-diagrams.js", workspaceUrl, "png", username, password);
Process process = processBuilder.start();
```

#### Code DiagramExporter - Après (Playwright)
```java
try (Playwright playwright = Playwright.create()) {
    Browser browser = playwright.chromium().launch(...);
    Page page = context.newPage();
    // Authentification
    signIn(page, workspaceUrl);
    // Navigation vers /diagrams
    page.navigate(viewerUrl);
    // Attente du frame Structurizr
    Frame structurizrFrame = findStructurizrFrame(page);
    // Attente du rendu des diagrammes
    structurizrFrame.waitForFunction("() => window.structurizr...");
    // Export via scripting API
    Object viewsResult = structurizrFrame.evaluate("() => window.structurizr.scripting.getViews()");
}
```

**Problème potentiel**: La migration Playwright peut avoir des bugs:
- Timeouts incorrects
- Sélecteurs CSS qui ne fonctionnent pas
- API Structurizr non disponible
- Problèmes d'authentification

---

## Régressions identifiées

### Régression 1: Structure des pages Documentation
**Quand**: Commit 2d0a3e9  
**Quoi**: 
- Avant: Chaque section = 1 page sous Documentation
- Après: Toutes les sections inline dans 1 seule page Documentation
- Impact: Perte de la hiérarchie de navigation, page Documentation très longue

### Régression 2: Page Schémas disparue
**Quand**: Commit 2d0a3e9  
**Quoi**:
- Avant: Page "Schémas" avec liste descriptive des diagrammes
- Après: Page "Views" avec images inline (exportAllViewsSinglePage)
- Impact: Changement de présentation, métadonnées différentes

### Régression 3: Export des diagrammes potentiellement cassé
**Quand**: Commit f58004a  
**Quoi**:
- Avant: Script Puppeteer Node.js externe
- Après: Playwright Java intégré
- Impact: Potentiellement des erreurs dans l'export (timeouts, sélecteurs, API)

### Régression 4: Comportement du parentPageId changé
**Quand**: Commit 2d0a3e9  
**Quoi**:
- Avant: parentPageId utilisé pour créer des sous-pages séparées
- Après: parentPageId utilisé mais contenu inline dans la page
- Impact: L'utilisateur ne peut plus créer la structure hiérarchique qu'il souhaite

---

## Commits importants

1. **fdbcfd4** (Dec 2024): Version initiale fonctionnelle avec structure hiérarchique complète
2. **2d0a3e9** (Dec 2024): Changement vers inline des sections + introduction de Puppeteer
3. **f58004a** (Jan 2025): Migration Puppeteer → Playwright + CLI Quarkus
4. **f7552ba** (Jan 2025): Transformation complète en application CLI
5. **4ce9dd1** (HEAD): Optimisation Playwright (installation Chromium seulement)

---

## Différences détaillées entre versions

### Comportement de exportWorkspaceDocumentationSections

**Version originale (fdbcfd4)**:
```java
public void exportWorkspaceDocumentationSections(Workspace workspace, String parentPageId, String branchName) {
    for (Section section : workspace.getDocumentation().getSections()) {
        String pageTitle = branchName + " - " + actualTitle;
        // Crée une PAGE SÉPARÉE pour chaque section
        String pageId = confluenceClient.createOrUpdatePage(pageTitle, adfJson, parentPageId);
    }
}
```

**Version actuelle (depuis 2d0a3e9)**:
```java
// La méthode existe toujours mais n'est PLUS APPELÉE
// À la place, dans export():
for (Section section : workspace.getDocumentation().getSections()) {
    // Convertit et CONCATÈNE tout dans une seule page
    docContent.add(child);
}
// Une seule mise à jour de la page Documentation
confluenceClient.updatePageById(documentationPageId, documentationPageTitle, finalDocumentationJson);
```

### Page Schémas/Diagrams

**Version originale (fdbcfd4)**:
```java
private void exportDiagramsPage(String parentPageId, Workspace workspace) {
    Document diagramsDoc = Document.create().h1("Schémas");
    diagramsDoc.paragraph("Liste des schémas...");
    
    for (View view : views.getViews()) {
        diagramsDoc.h2(diagramName);
        diagramsDoc.bulletList(list -> {
            list.item("Clé : " + diagramKey);
            list.item("Type : " + view.getClass().getSimpleName());
            list.item("Description : " + description);
        });
        // Ajoute lien vers l'image PNG
    }
    
    confluenceClient.createOrUpdatePage("Schémas", convertDocumentToJson(diagramsDoc), parentPageId);
}
```

**Version actuelle (depuis 2d0a3e9)**:
```java
private void exportAllViewsSinglePage(Workspace workspace, String parentPageId) {
    String viewsPageId = confluenceClient.createOrUpdatePage("Views", "{...}", parentPageId);
    
    Document viewsDoc = Document.create();
    // Pour chaque catégorie de vues
    viewsDoc = addViewsWithImages(viewsDoc, views.getSystemLandscapeViews(), "System Landscape Views");
    viewsDoc = addViewsWithImages(viewsDoc, views.getSystemContextViews(), "System Context Views");
    // ...
    
    // Ajoute les IMAGES directement dans la page
    confluenceClient.updatePageById(viewsPageId, "Views", convertDocumentToJson(viewsDoc));
}
```

---

## Recommandations pour restaurer le comportement original

### Option 1: Restauration complète
Revenir au comportement de fdbcfd4:
1. Restaurer `exportWorkspaceDocumentationSections` pour créer des sous-pages
2. Restaurer `exportDiagramsPage` avec page "Schémas"
3. Supprimer l'inline des sections dans `export()`

### Option 2: Approche hybride
Offrir le choix via paramètre:
1. Ajouter flag `--inline-documentation` (défaut: false pour compatibilité)
2. Si false: créer sous-pages comme avant
3. Si true: inline comme actuellement

### Option 3: Debugging de Playwright
Identifier et corriger les problèmes d'export des diagrammes:
1. Ajouter logs détaillés dans DiagramExporter
2. Vérifier les timeouts et sélecteurs
3. Tester l'authentification Structurizr
4. Comparer avec l'ancien script Puppeteer

---

## Conclusion

Les modifications principales ont été introduites dans le commit **2d0a3e9** ("Complete Structurizr to Confluence formatting preservation with Puppeteer-based diagram export") en décembre 2024. Ce commit a:
1. Changé la structure des pages en inline au lieu de hiérarchique
2. Remplacé la page "Schémas" par "Views"
3. Introduit Puppeteer pour l'export des diagrammes

Le commit **f58004a** a ensuite migré de Puppeteer vers Playwright, ce qui peut avoir introduit des bugs dans l'export des diagrammes.

Pour restaurer le comportement original, il faudrait revenir au code de **fdbcfd4** ou adapter le code actuel pour recréer la structure hiérarchique.
