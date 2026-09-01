package com.kobe.warehouse.service.cahier_recette;

import com.kobe.warehouse.service.license.DemoWatermark;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.pdf.PagePosition;

/**
 * Génère le PDF "Guide des fonctionnalités" (table des matières paginée + bookmarks) à partir des
 * données de {@link CahierRecetteDataService}.
 * <p>
 * Rendu en deux passes, technique classique avec Flying Saucer pour une table des matières à
 * numéros de page réels :
 * <ol>
 *   <li>1ʳᵉ passe : rend le document (TOC comprise, numéros de page vides), écrit un PDF
 *   jetable puis {@link ITextRenderer#findPagePositionsByID} donne le numéro de page réel de
 *   chaque module et fonctionnalité. ⚠ {@code layout()} seul ne suffit pas :
 *   {@code findPagePositionsByID} lit {@code ITextOutputDevice._root}, qui n'est affecté que
 *   par {@code createPDF} (via {@code setRoot}) — il faut donc avoir écrit un PDF avant de
 *   pouvoir l'appeler (vérifié en inspectant le jar flying-saucer-pdf).</li>
 *   <li>2ᵉ passe : rend exactement le même template avec ces numéros injectés dans la TOC, et
 *   produit le PDF final. Comme seul le texte des numéros change (pas la structure), la
 *   pagination ne bouge pas entre les deux passes.</li>
 * </ol>
 * Les bookmarks PDF (panneau de navigation) sont générés automatiquement par Flying Saucer
 * depuis la hiérarchie {@code h1}/{@code h2}/{@code h3} du template — aucun code dédié requis.
 * Le template évite délibérément tout {@code h4} : Flying Saucer bookmarke TOUTE balise de
 * titre (h1-h6), un titre par sous-section ("Le besoin", "Étapes"...) noierait le panneau de
 * navigation sous des centaines d'entrées.
 */
@Service
public class CahierRecettePdfService {

    private static final String TEMPLATE = "cahier-recette/main";
    private static final Pattern TOC_ID_PATTERN = Pattern.compile("^(module|feature)-.*");

    /** Les actifs Angular (dont content/) atterrissent sous static/ dans le classpath. */
    private static final String CLASSPATH_PREFIX = "static/";

    private final SpringTemplateEngine templateEngine;
    private final CahierRecetteDataService dataService;

    public CahierRecettePdfService(SpringTemplateEngine templateEngine,
        CahierRecetteDataService dataService) {
        this.templateEngine = templateEngine;
        this.dataService = dataService;
    }

    public byte[] generatePdf() {
        return generatePdf(List.of(), List.of());
    }

    public byte[] generatePdf(Collection<String> moduleIds) {
        return generatePdf(moduleIds, List.of());
    }

    public byte[] generatePdf(Collection<String> moduleIds, Collection<String> scenarioIds) {
        try {
            List<ModuleRecetteDTO> modules = dataService.getModules(moduleIds, scenarioIds);

            String htmlPass1 = renderHtml(modules, Map.of());
            Map<String, Integer> pageNumbers = findPageNumbers(htmlPass1);

            String htmlPass2 = renderHtml(modules, pageNumbers);
            return renderPdf(htmlPass2);
        } catch (Exception e) {
            throw new RuntimeException("Error generating cahier de recette PDF: " + e.getMessage(),
                e);
        }
    }

    private String renderHtml(List<ModuleRecetteDTO> modules, Map<String, Integer> pageNumbers) {
        Context context = new Context(Locale.FRENCH);
        context.setVariable("modules", modules);
        context.setVariable("pageNumbers", pageNumbers);
        context.setVariable("currentYear", Year.now().getValue());
        context.setVariable("editionTitle", resolveEditionTitle(modules));
        return templateEngine.process(TEMPLATE, context);
    }

    private String resolveEditionTitle(List<ModuleRecetteDTO> modules) {
        if (modules.size() != 1) {
            return "Manuel d’utilisation PharmaSmart";
        }
        ModuleRecetteDTO module = modules.getFirst();
        if (module.fonctionnalites().size() == 1) {
            return "Guide pratique — " + module.fonctionnalites().getFirst().nom();
        }
        return "Manuel d’utilisation — " + module.nom();
    }

    /**
     * Renderer dont l'agent utilisateur sait résoudre les images depuis le CLASSPATH.
     * <p>
     * {@link ITextRenderer#setDocumentFromString(String)} est appelé sans URL de base : sans
     * cet agent, une {@code <img src="content/captures/...">} du guide ne serait résolue nulle
     * part et le PDF sortirait avec des images manquantes — sans erreur, ce qui est le pire des
     * cas. Passer une URL de base {@code file:} ne conviendrait pas non plus : les captures
     * vivent dans les ressources, qui ne sont pas des fichiers une fois l'application packagée
     * en jar.
     * <p>
     * Les chemins du guide sont relatifs et servis par Angular depuis {@code content/} ; côté
     * backend, ils se retrouvent dans le classpath sous {@code static/}, d'où le préfixe.
     */
    private ITextRenderer newRenderer() {
        ITextOutputDevice outputDevice = new ITextOutputDevice(ITextRenderer.DEFAULT_DOTS_PER_POINT);
        ITextUserAgent userAgent = new ITextUserAgent(outputDevice,
            ITextRenderer.DEFAULT_DOTS_PER_PIXEL) {
            @Override
            public String resolveURI(String uri) {
                // Laisse les chemins relatifs intacts : c'est openStream qui les résout.
                return uri;
            }

            @Override
            protected InputStream openStream(String uri) throws IOException {
                ClassPathResource resource = new ClassPathResource(CLASSPATH_PREFIX + uri);
                if (resource.exists()) {
                    return resource.getInputStream();
                }
                return super.openStream(uri);
            }
        };
        return new ITextRenderer(outputDevice, userAgent);
    }

    // Instance dédiée à cette passe : elle rend le HTML "numéros de page vides" et son
    // createPDF() jetable ne sert qu'à peupler les positions de page (cf. javadoc de
    // classe) — on ne la réutilise pas pour la passe finale, qui charge un HTML différent
    // (numéros remplis) sur un writer PDF déjà écrit une première fois.
    private Map<String, Integer> findPageNumbers(String html) throws Exception {
        ITextRenderer renderer = newRenderer();
        renderer.setDocumentFromString(DemoWatermark.apply(html));
        renderer.layout();
        try (ByteArrayOutputStream discard = new ByteArrayOutputStream()) {
            renderer.createPDF(discard);
        }
        return renderer
            .findPagePositionsByID(TOC_ID_PATTERN)
            .stream()
            .collect(Collectors.toMap(PagePosition::getId, PagePosition::getPageNo, (a, _) -> a));
    }

    // Nouvelle instance (pas celle de findPageNumbers) : elle charge le HTML final, avec
    // les numéros de page réels injectés dans la TOC — un document différent de celui de
    // la 1ʳᵉ passe, sur un renderer qui n'a pas encore écrit de PDF.
    private byte[] renderPdf(String html) throws Exception {
        ITextRenderer renderer = newRenderer();
        renderer.setDocumentFromString(DemoWatermark.apply(html));
        renderer.layout();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }
}
