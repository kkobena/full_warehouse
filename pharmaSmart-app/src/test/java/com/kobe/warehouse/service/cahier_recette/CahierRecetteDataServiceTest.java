package com.kobe.warehouse.service.cahier_recette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.nav.NavPathResolver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CahierRecetteDataServiceTest {

    /**
     * Résolveur neutre : ces tests portent sur la sélection des scénarios et le rattachement des
     * captures, pas sur la dérivation du chemin de menu — celle-ci a ses propres tests. Une table
     * {@code nav_item} vide laisse simplement les rubriques sans « Où le trouver ».
     */
    private final CahierRecetteDataService service = new CahierRecetteDataService(new ObjectMapper(), navPathResolver());

    private static NavPathResolver navPathResolver() {
        NavPathResolver resolver = mock(NavPathResolver.class);
        when(resolver.resolveAll()).thenReturn(Map.of());
        return resolver;
    }

    @Test
    void filtersAGroupOfDependentScenariosWithoutLosingTheirCaptures() {
        List<String> scenarioIds = List.of("STK-04", "STK-05", "STK-07");

        List<ModuleRecetteDTO> modules = service.getModules(List.of("STK"), scenarioIds);

        assertThat(modules).singleElement().satisfies(module -> {
            assertThat(module.id()).isEqualTo("STK");
            assertThat(module.fonctionnalites()).singleElement().satisfies(feature -> {
                assertThat(feature.scenarios()).extracting(ScenarioRecetteDTO::id)
                    .containsExactlyElementsOf(scenarioIds);
                assertThat(feature.scenarios())
                    .anySatisfy(scenario -> assertThat(scenario.captures()).isNotEmpty());
            });
        });
    }

    /**
     * Chaque étape décrite doit pouvoir porter son écran : c'est ce lien qui permet au manuel
     * d'illustrer le geste là où il est expliqué, plutôt qu'une vue finale isolée.
     */
    @Test
    void bindsEachCaptureToTheStepItIllustrates() {
        ScenarioRecetteDTO scenario = service.getModules(List.of("VTE")).stream()
            .flatMap(module -> module.fonctionnalites().stream())
            .flatMap(feature -> feature.scenarios().stream())
            .filter(candidate -> candidate.captures() != null && !candidate.captures().isEmpty())
            .findFirst()
            .orElseThrow();

        List<EtapeIllustreeDTO> etapes = scenario.etapesIllustrees();

        assertThat(etapes).hasSizeGreaterThanOrEqualTo(scenario.etapes().size());
        assertThat(etapes).extracting(EtapeIllustreeDTO::numero).startsWith(1).isSorted();
        assertThat(etapes).anyMatch(EtapeIllustreeDTO::illustree);
        assertThat(etapes)
            .filteredOn(EtapeIllustreeDTO::illustree)
            .allSatisfy(etape -> assertThat(etape.capture().ordre()).isEqualTo(etape.numero()));
    }

    @Test
    void keepsEveryStepEvenWhenNoCaptureExists() {
        ScenarioRecetteDTO scenario = new ScenarioRecetteDTO(
            "VTE-99", "Titre", "Besoin", "Fonctionnement", null,
            List.of("Premier geste", "Second geste"), "Résultat", false, null);

        assertThat(scenario.etapesIllustrees())
            .extracting(EtapeIllustreeDTO::texte)
            .containsExactly("Premier geste", "Second geste");
        assertThat(scenario.etapesIllustrees()).noneMatch(EtapeIllustreeDTO::illustree);
    }
}
