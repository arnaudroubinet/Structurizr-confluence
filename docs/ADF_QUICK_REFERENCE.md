# Référence Rapide : HTML vers Confluence ADF

## Types de nœuds ADF principaux

### Nœuds de bloc (Block Nodes)
```json
{"type": "doc"}              // Document racine
{"type": "paragraph"}        // Paragraphe
{"type": "heading"}          // Titre (level: 1-6)
{"type": "bulletList"}       // Liste à puces
{"type": "orderedList"}      // Liste numérotée  
{"type": "listItem"}         // Élément de liste
{"type": "codeBlock"}        // Bloc de code
{"type": "blockquote"}       // Citation
{"type": "rule"}            // Ligne horizontale
{"type": "table"}           // Tableau
{"type": "tableRow"}        // Ligne de tableau
{"type": "tableCell"}       // Cellule de tableau
{"type": "tableHeader"}     // En-tête de tableau
```

### Nœuds inline (Inline Nodes)
```json
{"type": "text"}            // Texte simple
{"type": "hardBreak"}       // Saut de ligne
{"type": "mention"}         // Mention d'utilisateur
{"type": "emoji"}           // Emoji
{"type": "date"}            // Date
{"type": "status"}          // Statut
```

### Marques de formatage (Text Marks)
```json
{"type": "strong"}          // Gras
{"type": "em"}              // Italique
{"type": "code"}            // Code inline
{"type": "strike"}          // Barré
{"type": "underline"}       // Souligné
{"type": "subsup"}          // Exposant/indice
{"type": "textColor"}       // Couleur du texte
{"type": "link"}            // Lien hypertexte
```

## Correspondances HTML → ADF les plus communes

| HTML | ADF Type | Notes |
|------|----------|-------|
| `<h1>` - `<h6>` | `heading` | ✅ Support natif |
| `<p>` | `paragraph` | ✅ Support natif |
| `<ul>` | `bulletList` | ✅ Support natif |
| `<ol>` | `orderedList` | ⚠️ Converti en bulletList |
| `<li>` | `listItem` | ✅ Support natif |
| `<table>` | `table` | ✅ Support natif + post-processing |
| `<pre>` | `codeBlock` | ✅ Support natif |
| `<a>` | text avec format fallback | 🔄 `text (url)` |
| `<strong>` | text simple | 🔄 Pas de marque forte |
| `<em>` | text simple | 🔄 Pas de marque italique |
| `<hr>` | `rule` | ✅ Support natif |

## Exemples d'implémentation

### Texte avec lien (fallback actuel)
```java
// HTML: <a href="https://example.com">link text</a>
// Résultat: "link text (https://example.com)"
```

### Texte avec formatage riche (futur)
```json
{
  "type": "text",
  "text": "texte en gras",
  "marks": [
    {
      "type": "strong"
    }
  ]
}
```

### Lien hypertexte natif (futur)
```json
{
  "type": "text",
  "text": "cliquez ici",
  "marks": [
    {
      "type": "link",
      "attrs": {
        "href": "https://example.com",
        "title": "Titre optionnel"
      }
    }
  ]
}
```

## Structures ADF avancées

### Panel d'information
```json
{
  "type": "panel",
  "attrs": {
    "panelType": "info"
  },
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Information importante"
        }
      ]
    }
  ]
}
```

### Extension (macro Confluence)
```json
{
  "type": "extension",
  "attrs": {
    "extensionType": "com.atlassian.confluence.macro.core",
    "extensionKey": "info",
    "parameters": {
      "title": "Titre de l'info"
    }
  },
  "content": [
    {
      "type": "paragraph",
      "content": [
        {
          "type": "text",
          "text": "Contenu de l'extension"
        }
      ]
    }
  ]
}
```

### Mention d'utilisateur
```json
{
  "type": "mention",
  "attrs": {
    "id": "user-id",
    "text": "@username",
    "userType": "DEFAULT"
  }
}
```

### Statut
```json
{
  "type": "status",
  "attrs": {
    "text": "En cours",
    "color": "blue",
    "localId": "status-id"
  }
}
```

## Limitations de l'implémentation actuelle

### ❌ Non supporté
- Extensions Confluence natives
- Formatage inline (gras, italique, etc.)
- Liens hypertexte natifs
- Panels d'information
- Mentions d'utilisateurs
- Statuts
- Dates formatées
- Médias avec métadonnées Atlassian

### 🟡 Support partiel
- Liens → format `text (url)`
- Listes ordonnées → listes à puces avec numéros
- Éléments multimedia → descriptions textuelles
- Formulaires → descriptions textuelles

### ✅ Bien supporté
- Structure de document de base
- Titres H1-H6
- Paragraphes avec préservation des espaces
- Tableaux complets avec en-têtes
- Blocs de code
- Listes à puces
- Règles horizontales

## Améliorer l'implémentation

### Ajout du support des marques
```java
// Au lieu de:
return doc.paragraph(getElementText(element));

// Utiliser:
return doc.paragraph(createInlineNodesWithMarks(element));
```

### Ajout du support des liens natifs
```java
// Créer des nœuds inline avec marques de liens
InlineNode textWithLink = InlineNode.text("texte")
    .withMark(LinkMark.create("https://example.com"));
```

### Ajout du support des extensions
```java
// Utiliser des extensions pour du contenu spécialisé
Extension infoPanel = Extension.create("info")
    .withParameter("title", "Information")
    .withContent(paragraphContent);
```

---

**Référence pour :** `HtmlToAdfConverter.java`  
**Version ADF :** 1  
**Date :** Septembre 2025