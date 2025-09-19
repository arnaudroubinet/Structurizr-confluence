/**
 * Analyse de la couverture HTML dans HtmlToAdfConverter
 * 
 * Liste complète HTML W3Schools vs Notre implémentation
 */

// BALISES ACTUELLEMENT SUPPORTÉES (dans notre processElement switch):
// ✅ h1-h6 (headings)
// ✅ p, div, section, article (text blocks)  
// ✅ ul, ol (lists)
// ✅ table (tables)
// ✅ blockquote, pre (special blocks)
// ✅ hr, br (line breaks)
// ✅ span, strong, b, em, i, u, s, code (inline formatting)
// ✅ thead, tbody, tfoot, tr, td, th (table elements - handled in processTable)

// BALISES HTML COMPLÈTES (liste W3Schools):
// <!DOCTYPE>, <a>, <abbr>, <acronym>, <address>, <applet>, <area>, <article>, <aside>, <audio>,
// <b>, <base>, <basefont>, <bdi>, <bdo>, <big>, <blockquote>, <body>, <br>, <button>,
// <canvas>, <caption>, <center>, <cite>, <code>, <col>, <colgroup>, <data>, <datalist>,
// <dd>, <del>, <details>, <dfn>, <dialog>, <dir>, <div>, <dl>, <dt>, <em>, <embed>,
// <fieldset>, <figcaption>, <figure>, <font>, <footer>, <form>, <frame>, <frameset>,
// <h1>-<h6>, <head>, <header>, <hgroup>, <hr>, <html>, <i>, <iframe>, <img>, <input>,
// <ins>, <kbd>, <label>, <legend>, <li>, <link>, <main>, <map>, <mark>, <menu>, <meta>,
// <meter>, <nav>, <noframes>, <noscript>, <object>, <ol>, <optgroup>, <option>, <output>,
// <p>, <param>, <picture>, <pre>, <progress>, <q>, <rp>, <rt>, <ruby>, <s>, <samp>,
// <script>, <search>, <section>, <select>, <small>, <source>, <span>, <strike>, <strong>,
// <style>, <sub>, <summary>, <sup>, <svg>, <table>, <tbody>, <td>, <template>, <textarea>,
// <tfoot>, <th>, <thead>, <time>, <title>, <tr>, <track>, <tt>, <u>, <ul>, <var>, <video>, <wbr>

// BALISES MANQUANTES IMPORTANTES PAR CATÉGORIE:

// 1. ÉLÉMENTS SÉMANTIQUES MODERNES:
// ❌ <header>, <footer>, <main>, <nav>, <aside> 
// ❌ <figure>, <figcaption>
// ❌ <details>, <summary>
// ❌ <time>, <mark>

// 2. FORMULAIRES:
// ❌ <form>, <input>, <button>, <textarea>, <select>, <option>, <optgroup>
// ❌ <label>, <fieldset>, <legend>, <datalist>, <output>, <progress>, <meter>

// 3. MÉDIAS:
// ❌ <img>, <audio>, <video>, <picture>, <source>, <track>
// ❌ <canvas>, <svg>, <embed>, <object>, <iframe>

// 4. LISTES DE DESCRIPTION:
// ❌ <dl>, <dt>, <dd>

// 5. INLINE SÉMANTIQUES:
// ❌ <a> (links), <abbr>, <cite>, <dfn>, <kbd>, <samp>, <var>
// ❌ <ins>, <del>, <mark>, <q>, <small>, <sub>, <sup>
// ❌ <ruby>, <rt>, <rp>, <bdi>, <bdo>, <wbr>

// 6. MÉTADONNÉES/STRUCTURE:
// ❌ <meta>, <link>, <base>, <style>, <script>, <noscript>
// ❌ <template>, <slot>

// 7. BALISES OBSOLÈTES (pas nécessaires):
// <acronym>, <applet>, <basefont>, <big>, <center>, <dir>, <font>, <frame>, 
// <frameset>, <noframes>, <strike>, <tt>

// PRIORITÉS D'IMPLÉMENTATION:
// 1. HAUTE: Éléments sémantiques modernes (header, footer, main, nav, aside, figure)
// 2. HAUTE: Liens et images (a, img)  
// 3. MOYENNE: Listes de description (dl, dt, dd)
// 4. MOYENNE: Inline sémantiques (abbr, cite, dfn, ins, del, mark, q, small, sub, sup)
// 5. BASSE: Formulaires (converties en texte simple)
// 6. BASSE: Médias (références uniquement)