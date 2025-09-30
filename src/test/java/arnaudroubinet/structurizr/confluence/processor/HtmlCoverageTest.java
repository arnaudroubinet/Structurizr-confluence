package arnaudroubinet.structurizr.confluence.processor;

import com.atlassian.adf.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Tests de couverture complète des balises HTML supportées par notre convertisseur.
 */
public class HtmlCoverageTest {

    @Test
    public void testStructuralElementsCoverage() throws Exception {
        String htmlContent = """
            <h1>Titre Principal</h1>
            <h2>Sous-titre</h2>
            <h3>Section</h3>
            <h4>Sous-section</h4>
            <h5>Petit titre</h5>
            <h6>Titre minimal</h6>
            
            <p>Paragraphe simple avec du texte.</p>
            <div>Division avec du contenu.</div>
            <section>Section sémantique.</section>
            <article>Article de contenu.</article>
            
            <hr>
            
            <main>Contenu principal de la page.</main>
            <header>En-tête de la page.</header>
            <footer>Pied de page.</footer>
            <nav>Navigation du site.</nav>
            <aside>Contenu latéral.</aside>
            """;
        
        testHtmlConversion("Structural Elements", htmlContent);
    }
    
    @Test
    public void testTextFormattingCoverage() throws Exception {
        String htmlContent = """
            <p>Texte avec <strong>gras</strong> et <b>gras alternatif</b>.</p>
            <p>Texte avec <em>italique</em> et <i>italique alternatif</i>.</p>
            <p>Texte avec <u>souligné</u> et <s>barré</s>.</p>
            <p>Texte avec <strike>barré ancien</strike> et <del>supprimé</del>.</p>
            <p>Texte avec <code>code inline</code> et <kbd>raccourci clavier</kbd>.</p>
            <p>Texte avec <samp>exemple de sortie</samp> et <var>variable</var>.</p>
            <p>Texte avec <mark>surlignage</mark> et <small>petit texte</small>.</p>
            <p>Texte avec <sub>indice</sub> et <sup>exposant</sup>.</p>
            """;
        
        testHtmlConversion("Text Formatting", htmlContent);
    }
    
    @Test
    public void testListsCoverage() throws Exception {
        String htmlContent = """
            <ul>
                <li>Premier élément de liste à puces</li>
                <li>Deuxième élément</li>
                <li>Troisième élément</li>
            </ul>
            
            <ol>
                <li>Premier élément numéroté</li>
                <li>Deuxième élément</li>
                <li>Troisième élément</li>
            </ol>
            
            <dl>
                <dt>Terme 1</dt>
                <dd>Description du terme 1</dd>
                <dt>Terme 2</dt>
                <dd>Description du terme 2</dd>
            </dl>
            """;
        
        testHtmlConversion("Lists", htmlContent);
    }
    
    @Test
    public void testTablesCoverage() throws Exception {
        String htmlContent = """
            <table>
                <caption>Titre du tableau</caption>
                <thead>
                    <tr>
                        <th>En-tête 1</th>
                        <th>En-tête 2</th>
                        <th>En-tête 3</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Cellule 1</td>
                        <td>Cellule 2</td>
                        <td>Cellule 3</td>
                    </tr>
                    <tr>
                        <td>Cellule 4</td>
                        <td>Cellule 5</td>
                        <td>Cellule 6</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td>Pied 1</td>
                        <td>Pied 2</td>
                        <td>Pied 3</td>
                    </tr>
                </tfoot>
            </table>
            """;
        
        testHtmlConversion("Tables", htmlContent);
    }
    
    @Test
    public void testFormElementsCoverage() throws Exception {
        String htmlContent = """
            <form>
                <fieldset>
                    <legend>Informations personnelles</legend>
                    
                    <label for="name">Nom:</label>
                    <input type="text" id="name" name="name">
                    
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email">
                    
                    <label for="message">Message:</label>
                    <textarea id="message" name="message" rows="4"></textarea>
                    
                    <label for="country">Pays:</label>
                    <select id="country" name="country">
                        <optgroup label="Europe">
                            <option value="fr">France</option>
                            <option value="de">Allemagne</option>
                        </optgroup>
                        <optgroup label="Amérique">
                            <option value="us">États-Unis</option>
                            <option value="ca">Canada</option>
                        </optgroup>
                    </select>
                    
                    <button type="submit">Envoyer</button>
                </fieldset>
            </form>
            """;
        
        testHtmlConversion("Form Elements", htmlContent);
    }
    
    @Test
    public void testMediaAndEmbedCoverage() throws Exception {
        String htmlContent = """
            <figure>
                <img src="image.jpg" alt="Description de l'image">
                <figcaption>Légende de l'image</figcaption>
            </figure>
            
            <audio controls>
                <source src="audio.mp3" type="audio/mpeg">
                Votre navigateur ne supporte pas l'audio.
            </audio>
            
            <video controls>
                <source src="video.mp4" type="video/mp4">
                <track kind="captions" src="captions.vtt" srclang="fr" label="Français">
                Votre navigateur ne supporte pas la vidéo.
            </video>
            
            <iframe src="https://example.com" title="Exemple d'iframe"></iframe>
            
            <embed src="document.pdf" type="application/pdf">
            
            <object data="flash-content.swf" type="application/x-shockwave-flash"></object>
            """;
        
        testHtmlConversion("Media and Embed", htmlContent);
    }
    
    @Test
    public void testInteractiveElementsCoverage() throws Exception {
        String htmlContent = """
            <details>
                <summary>Cliquez pour afficher les détails</summary>
                <p>Contenu des détails qui peut être affiché ou masqué.</p>
            </details>
            
            <dialog open>
                <p>Ceci est une boîte de dialogue.</p>
                <button type="button">Fermer</button>
            </dialog>
            
            <progress value="70" max="100">70%</progress>
            
            <meter value="6" min="0" max="10">6 sur 10</meter>
            """;
        
        testHtmlConversion("Interactive Elements", htmlContent);
    }
    
    @Test
    public void testCodeAndQuotesCoverage() throws Exception {
        String htmlContent = """
            <blockquote cite="https://example.com">
                <p>Ceci est une citation longue qui devrait être formatée comme un bloc de citation.</p>
                <footer>— <cite>Auteur de la citation</cite></footer>
            </blockquote>
            
            <pre><code>
function example() {
    console.log("Hello, World!");
    return true;
}
            </code></pre>
            
            <p>Citation courte : <q>Ceci est une citation courte</q>.</p>
            
            <p>Référence : <cite>Titre du livre</cite> par l'auteur.</p>
            """;
        
        testHtmlConversion("Code and Quotes", htmlContent);
    }
    
    @Test
    public void testTimeAndDataCoverage() throws Exception {
        String htmlContent = """
            <p>Publié le <time datetime="2025-09-17">17 septembre 2025</time>.</p>
            
            <p>Données avec attribut : <data value="12345">Référence produit</data>.</p>
            
            <p>Adresse : <address>
                123 Rue Example<br>
                75001 Paris<br>
                France
            </address></p>
            
            <p>Abbréviation : <abbr title="HyperText Markup Language">HTML</abbr>.</p>
            
            <p>Définition : <dfn>API</dfn> signifie Application Programming Interface.</p>
            """;
        
        testHtmlConversion("Time and Data", htmlContent);
    }
    
    /**
     * Méthode utilitaire pour tester la conversion HTML vers ADF.
     */
    private void testHtmlConversion(String testName, String htmlContent) throws Exception {
        System.out.println("\n=== Test : " + testName + " ===");
        System.out.println("HTML Input:");
        System.out.println(htmlContent);
        
        HtmlToAdfConverter converter = new HtmlToAdfConverter();
        Document adfDocument = converter.convertToAdf(htmlContent, testName);
        
        ObjectMapper objectMapper = new ObjectMapper();
        String adfJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adfDocument);
        
        System.out.println("\nADF Output:");
        System.out.println(adfJson);
        
        // Vérifications basiques
        assert adfDocument != null : "Le document ADF ne doit pas être null";
        assert adfJson.contains("\"type\" : \"doc\"") : "Le document doit être de type 'doc'";
        assert adfJson.contains("\"version\" : 1") : "Le document doit avoir la version 1";
        
        System.out.println("✅ Test " + testName + " réussi !");
    }
}