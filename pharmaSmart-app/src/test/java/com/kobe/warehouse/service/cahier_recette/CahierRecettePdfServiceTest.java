package com.kobe.warehouse.service.cahier_recette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.nav.NavPathResolver;
import java.time.Year;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class CahierRecettePdfServiceTest {

    @Test
    @Timeout(60)
    void generatesSelectedModuleWithCopyright() throws Exception {
        CahierRecetteDataService dataService = mock(CahierRecetteDataService.class);
        List<String> selection = List.of("VTE");
        when(dataService.getModules(selection, List.of())).thenReturn(List.of(module()));
        CahierRecettePdfService service = new CahierRecettePdfService(templateEngine(), dataService);

        byte[] pdf = service.generatePdf(selection);

        verify(dataService).getModules(selection, List.of());
        assertThat(pdf).startsWith("%PDF".getBytes());
        try (PdfReader reader = new PdfReader(pdf)) {
            String firstPage = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(firstPage)
                .contains("Guide pratique — Point de vente")
                .contains("Kobena")
                .contains(String.valueOf(Year.now().getValue()));
            // Le chemin d'accès est la première chose que cherche un pharmacien qui découvre
            // une fonctionnalité : s'il n'est pas imprimé, le guide perd son rôle de mode d'emploi.
            assertThat(extractText(reader))
                .contains("Où le trouver")
                .contains("Gestion Courante");
        }
    }

    /**
     * Le manuel s'adresse au pharmacien : il montre les gestes, pas les références internes du
     * cahier de recette. Aucun identifiant de scénario ne doit apparaître dans le document.
     */
    @Test
    @Timeout(60)
    void printsStepsWithoutInternalScenarioReferences() throws Exception {
        // Résolveur neutre : ce test porte sur le rendu, pas sur la dérivation du chemin de
        // menu. Une table nav_item vide laisse simplement les rubriques sans « Où le trouver ».
        NavPathResolver navPathResolver = mock(NavPathResolver.class);
        when(navPathResolver.resolveAll()).thenReturn(Map.of());
        CahierRecetteDataService dataService =
            new CahierRecetteDataService(new ObjectMapper(), navPathResolver);
        CahierRecettePdfService service = new CahierRecettePdfService(templateEngine(), dataService);
        List<String> scenarios = List.of(
            "HOME-12", "HOME-13", "HOME-14", "HOME-15", "HOME-16", "HOME-17"
        );

        byte[] pdf = service.generatePdf(List.of("HOME"), scenarios);

        try (PdfReader reader = new PdfReader(pdf)) {
            // Les intitulés de section sont mis en capitales par la feuille de style : le texte
            // extrait du PDF porte le glyphe dessiné, pas la casse du gabarit.
            assertThat(extractText(reader))
                .containsIgnoringCase("Marche à suivre")
                .contains("Consulter ses dernières transactions")
                .doesNotContain("HOME-12")
                .doesNotContain("HOME-17")
                .doesNotContain("Cette vue commune illustre les scénarios")
                .doesNotContain("Ce que ce module apporte à l’officine");
        }
    }

    /** Texte de toutes les pages : le guide est paginé, une assertion par page serait fragile. */
    private String extractText(PdfReader reader) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page));
        }
        return text.toString();
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private ModuleRecetteDTO module() {
        ScenarioRecetteDTO scenario = new ScenarioRecetteDTO(
            "VTE-01",
            "Encaisser une vente",
            "Encaisser le client",
            "Le total est calculé automatiquement.",
            null,
            List.of("Ouvrir la vente", "Valider l’encaissement"),
            "La vente est enregistrée.",
            false,
            List.of()
        );
        // Chemin déjà résolu : la dérivation depuis nav_item est vérifiée à part, ici on ne
        // teste que son impression.
        FonctionnaliteRecetteDTO feature = new FonctionnaliteRecetteDTO(
            "Point de vente",
            "Encaissement au comptoir",
            "Barre de navigation ▸ Gestion Courante ▸ Ventes",
            "ventes",
            null,
            List.of(scenario),
            false,
            null,
            false
        );
        return new ModuleRecetteDTO(
            "VTE",
            "Ventes",
            "pi pi-shopping-cart",
            "Vendre et encaisser.",
            List.of(feature)
        );
    }
}
