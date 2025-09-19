# Documentation de Correspondance HTML vers Confluence ADF

## Vue d'ensemble

Ce document définit la correspondance entre les éléments HTML et leur équivalent dans le format Atlassian Document Format (ADF) utilisé par Confluence Cloud. Cette correspondance est basée sur l'implémentation dans `HtmlToAdfConverter.java`.

## Structure de base ADF

Tous les documents ADF suivent cette structure de base :

```json
{
  "version": 1,
  "type": "doc",
  "content": [
    // Contenu du document
  ]
}
```

## Correspondances par catégorie

### 1. Éléments de titre (Headings)

| HTML | Confluence ADF | Exemple |
|------|---------------|---------|
| `<h1>` | `{"type": "heading", "attrs": {"level": 1}}` | ✅ Support natif |
| `<h2>` | `{"type": "heading", "attrs": {"level": 2}}` | ✅ Support natif |
| `<h3>` | `{"type": "heading", "attrs": {"level": 3}}` | ✅ Support natif |
| `<h4>` | `{"type": "heading", "attrs": {"level": 4}}` | ✅ Support natif |
| `<h5>` | `{"type": "heading", "attrs": {"level": 5}}` | ✅ Support natif |
| `<h6>` | `{"type": "heading", "attrs": {"level": 6}}` | ✅ Support natif |

**Exemple ADF :**
```json
{
  "type": "heading",
  "attrs": {
    "level": 1
  },
  "content": [
    {
      "type": "text",
      "text": "Titre Principal"
    }
  ]
}
```

### 2. Éléments de texte et paragraphes

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<p>` | `{"type": "paragraph"}` | ✅ Support natif avec gestion des liens |
| `<div>` | Traitement récursif des enfants | ⚠️ Pas de nœud ADF direct |
| `<section>` | Traitement récursif des enfants | ⚠️ Pas de nœud ADF direct |
| `<article>` | Traitement récursif des enfants | ⚠️ Pas de nœud ADF direct |
| `<span>` | `{"type": "paragraph"}` | 🔄 Converti en paragraphe |

**Exemple ADF pour paragraphe :**
```json
{
  "type": "paragraph",
  "content": [
    {
      "type": "text",
      "text": "Contenu du paragraphe"
    }
  ]
}
```

### 3. Formatage de texte inline

| HTML | Confluence ADF | Statut |
|------|---------------|--------|
| `<strong>`, `<b>` | `{"type": "text", "marks": [{"type": "strong"}]}` | 🔄 Actuellement texte simple |
| `<em>`, `<i>` | `{"type": "text", "marks": [{"type": "em"}]}` | 🔄 Actuellement texte simple |
| `<u>` | `{"type": "text", "marks": [{"type": "underline"}]}` | 🔄 Actuellement texte simple |
| `<s>`, `<del>` | `{"type": "text", "marks": [{"type": "strike"}]}` | 🔄 Actuellement texte simple |
| `<code>` | `{"type": "text", "marks": [{"type": "code"}]}` | 🔄 Actuellement texte simple |

**Note :** L'implémentation actuelle convertit ces éléments en texte simple dans des paragraphes.

### 4. Listes

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<ul>` | `{"type": "bulletList"}` | ✅ Support natif |
| `<ol>` | `{"type": "orderedList"}` | 🔄 Converti en bulletList avec numéros |
| `<li>` | `{"type": "listItem"}` | ✅ Support natif |
| `<dl>` | Titre + paragraphes | 🔄 Format personnalisé |
| `<dt>` | `"Terme: {text}"` | 🔄 Format personnalisé |
| `<dd>` | `"Définition: {text}"` | 🔄 Format personnalisé |

**Exemple ADF pour liste :**
```json
{
  "type": "bulletList",
  "content": [
    {
      "type": "listItem",
      "content": [
        {
          "type": "paragraph",
          "content": [
            {
              "type": "text",
              "text": "Premier élément"
            }
          ]
        }
      ]
    }
  ]
}
```

### 5. Tableaux

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<table>` | `{"type": "table"}` | ✅ Support natif avec post-processing |
| `<thead>` | Structure intégrée | ✅ Support natif |
| `<tbody>` | Structure intégrée | ✅ Support natif |
| `<tfoot>` | Structure intégrée | ✅ Support natif |
| `<tr>` | `{"type": "tableRow"}` | ✅ Support natif |
| `<th>` | `{"type": "tableHeader"}` | ✅ Support natif |
| `<td>` | `{"type": "tableCell"}` | ✅ Support natif |
| `<caption>` | Ignoré | ❌ Non supporté |

**Exemple ADF pour tableau :**
```json
{
  "type": "table",
  "content": [
    {
      "type": "tableRow",
      "content": [
        {
          "type": "tableHeader",
          "content": [
            {
              "type": "paragraph",
              "content": [
                {
                  "type": "text",
                  "text": "En-tête"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 6. Liens et médias

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<a>` | Format fallback : `text (url)` | 🔄 Converti en texte lisible |
| `<img>` | `"Image: {src} ({alt})"` | 🔄 Description textuelle |
| `<audio>` | `"Source: {src} ({type})"` | 🔄 Description textuelle |
| `<video>` | `"Source: {src} ({type})"` | 🔄 Description textuelle |
| `<iframe>` | Ignoré | ❌ Non supporté |
| `<embed>` | Ignoré | ❌ Non supporté |
| `<object>` | Ignoré | ❌ Non supporté |

**Note :** Les vrais nœuds de liens ADF nécessiteraient le format :
```json
{
  "type": "text",
  "text": "texte du lien",
  "marks": [
    {
      "type": "link",
      "attrs": {
        "href": "https://example.com"
      }
    }
  ]
}
```

### 7. Blocs de code et citations

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<blockquote>` | `{"type": "paragraph"}` | 🔄 Texte simple avec préfixe |
| `<pre>` | `{"type": "codeBlock"}` | ✅ Support natif |
| `<code>` (block) | `{"type": "codeBlock"}` | ✅ Support natif |
| `<q>` | `"{text}"` (entre guillemets) | 🔄 Texte simple |
| `<cite>` | `"Citation: {text}"` | 🔄 Texte simple |

**Exemple ADF pour bloc de code :**
```json
{
  "type": "codeBlock",
  "attrs": {
    "language": null
  },
  "content": [
    {
      "type": "text",
      "text": "function example() {\n    return true;\n}"
    }
  ]
}
```

### 8. Éléments interactifs

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<details>` | Titre h4 + contenu | 🔄 Format personnalisé |
| `<summary>` | `{"type": "heading", "level": 4}` | 🔄 Converti en titre |
| `<dialog>` | `{"type": "paragraph"}` | 🔄 Texte simple |
| `<progress>` | `"{value}%"` ou description | 🔄 Texte simple |
| `<meter>` | Description textuelle | 🔄 Texte simple |

### 9. Éléments sémantiques

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<time>` | Texte formaté | 🔄 Extraction de datetime |
| `<abbr>` | `"{text}"` (title ignoré) | 🔄 Texte simple |
| `<dfn>` | `"Definition: {text}"` | 🔄 Texte avec préfixe |
| `<mark>` | `"Highlighted: {text}"` | 🔄 Texte avec préfixe |
| `<kbd>` | `"Keyboard: {text}"` | 🔄 Texte avec préfixe |
| `<samp>` | `"Sample: {text}"` | 🔄 Texte avec préfixe |
| `<var>` | `"Variable: {text}"` | 🔄 Texte avec préfixe |
| `<sub>` | `"{text} (subscript)"` | 🔄 Texte avec suffixe |
| `<sup>` | `"{text} (superscript)"` | 🔄 Texte avec suffixe |
| `<ins>` | `"Inserted: {text}"` | 🔄 Texte avec préfixe |
| `<small>` | `"Small text: {text}"` | 🔄 Texte avec préfixe |

### 10. Éléments de formulaire

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<form>` | Titre "Form" + contenu | 🔄 Format personnalisé |
| `<input>` | Description selon type | 🔄 Texte descriptif |
| `<textarea>` | `"Textarea: {placeholder}"` | 🔄 Texte descriptif |
| `<select>` | `"Select: {options}"` | 🔄 Texte descriptif |
| `<button>` | `"Button: {text}"` | 🔄 Texte descriptif |
| `<fieldset>` | Titre + contenu | 🔄 Format personnalisé |
| `<legend>` | `{"type": "heading", "level": 4}` | 🔄 Converti en titre |
| `<label>` | `"Label: {text}"` | 🔄 Texte descriptif |

### 11. Éléments structurels

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<header>` | Traitement récursif | ⚠️ Pas de nœud direct |
| `<footer>` | Traitement récursif | ⚠️ Pas de nœud direct |
| `<main>` | Traitement récursif | ⚠️ Pas de nœud direct |
| `<nav>` | Traitement récursif | ⚠️ Pas de nœud direct |
| `<aside>` | Traitement récursif | ⚠️ Pas de nœud direct |
| `<figure>` | Titre "Figure" + contenu | 🔄 Format personnalisé |
| `<figcaption>` | `"Caption: {text}"` | 🔄 Texte avec préfixe |
| `<address>` | `{"type": "paragraph"}` | 🔄 Texte simple |

### 12. Éléments divers

| HTML | Confluence ADF | Implémentation |
|------|---------------|----------------|
| `<hr>` | `{"type": "rule"}` | ✅ Support natif |
| `<br>` | Paragraphe vide | 🔄 Format personnalisé |
| `<wbr>` | Ignoré | ❌ Non supporté |

## Limitations et améliorations possibles

### 🔴 Non supportés actuellement
- **Extensions Confluence** : `extension`, `inlineExtension`, `bodiedExtension`
- **Médias riches** : `media` avec attributs Atlassian
- **Emojis** : Support partiel pour Unicode standard uniquement
- **Panneau d'informations** : `panel`, `infoPanel`
- **Statuts** : `status`
- **Mentions** : `mention`
- **Dates** : `date`

### 🟡 Support partiel
- **Liens** : Convertis en format lisible `text (url)` au lieu de vrais liens hypertexte
- **Formatage inline** : `strong`, `em`, etc. convertis en texte simple
- **Listes ordonnées** : Convertis en listes à puces avec numéros manuels

### ✅ Bien supportés
- **Titres** : Support natif complet H1-H6
- **Paragraphes** : Support natif avec gestion des liens
- **Tableaux** : Support natif avec post-processing
- **Blocs de code** : Support natif
- **Listes à puces** : Support natif
- **Règles horizontales** : Support natif

## Exemples d'utilisation

### Conversion d'un document complet

**HTML d'entrée :**
```html
<h1>Architecture Document</h1>
<p>This document follows the <a href="https://arc42.org/overview">arc42</a> template.</p>
<ul>
  <li>Introduction</li>
  <li>Architecture Overview</li>
</ul>
```

**ADF de sortie :**
```json
{
  "version": 1,
  "type": "doc",
  "content": [
    {
      "type": "heading",
      "attrs": {"level": 1},
      "content": [{"type": "text", "text": "Architecture Document"}]
    },
    {
      "type": "paragraph",
      "content": [{"type": "text", "text": "This document follows the arc42 (https://arc42.org/overview) template."}]
    },
    {
      "type": "bulletList",
      "content": [
        {
          "type": "listItem",
          "content": [
            {
              "type": "paragraph",
              "content": [{"type": "text", "text": "Introduction"}]
            }
          ]
        }
      ]
    }
  ]
}
```

## Notes de développement

### Gestion des espaces et du formatage
- Les espaces sont préservés dans la conversion des liens
- Le texte est nettoyé via `cleanText()` sauf pour les nœuds de texte des liens
- Les éléments inline sont convertis en paragraphes séparés

### Post-processing
- Les tableaux bénéficient d'un post-processing spécial via `AdfTablePostProcessor`
- Conversion des marqueurs de tableau en structures ADF natives

### Fallbacks
- Éléments non reconnus : convertis en paragraphes avec leur contenu textuel
- Éléments structurels : traitement récursif des enfants
- Éléments multimedia : descriptions textuelles

---

**Version :** 1.0.0  
**Dernière mise à jour :** Septembre 2025  
**Implémentation :** `HtmlToAdfConverter.java`