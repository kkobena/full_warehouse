package com.kobe.warehouse.service.cahier_recette;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Charge data/cahier-recette.json (généré depuis CAHIER_RECETTE côté TypeScript par `npm run
 * generate:cahier-recette`, cf. pharmaSmart-app/pom.xml) et le garde en mémoire.
 * <p>
 * Filtre les entrées `hidden` (même règle que `withoutHidden()` côté front,
 * cahier-recette.component.ts) et les fonctionnalités `roadmap` : le PDF documente ce qui est
 * livré, pas la feuille de route.
 */
@Service
public class CahierRecetteDataService {

    private static final String RESOURCE_PATH = "data/cahier-recette.json";

    private final ObjectMapper objectMapper;
    private List<ModuleRecetteDTO> modules;

    public CahierRecetteDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized List<ModuleRecetteDTO> getModules() {
        if (modules == null) {
            modules = loadModules();
        }
        return modules;
    }

    private List<ModuleRecetteDTO> loadModules() {
        try (InputStream is = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            List<ModuleRecetteDTO> raw = objectMapper.readValue(is, new TypeReference<>() {
            });
            return raw.stream().map(this::withoutHiddenOrRoadmap)
                .filter(m -> !m.fonctionnalites().isEmpty()).toList();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + RESOURCE_PATH, e);
        }
    }

    private ModuleRecetteDTO withoutHiddenOrRoadmap(ModuleRecetteDTO module) {
        List<FonctionnaliteRecetteDTO> fonctionnalites = module
            .fonctionnalites()
            .stream()
            .filter(f -> !Boolean.TRUE.equals(f.hidden()) && !Boolean.TRUE.equals(f.roadmap()))
            .map(this::withoutHiddenScenarios)
            .filter(f -> !f.scenarios().isEmpty())
            .toList();
        return new ModuleRecetteDTO(module.id(), module.nom(), module.icone(), module.description(),
            fonctionnalites);
    }

    private FonctionnaliteRecetteDTO withoutHiddenScenarios(
        FonctionnaliteRecetteDTO fonctionnalite) {
        List<ScenarioRecetteDTO> scenarios = fonctionnalite
            .scenarios()
            .stream()
            .filter(s -> !Boolean.TRUE.equals(s.hidden()))
            .toList();
        return new FonctionnaliteRecetteDTO(
            fonctionnalite.nom(),
            fonctionnalite.description(),
            scenarios,
            fonctionnalite.hidden(),
            fonctionnalite.version(),
            fonctionnalite.roadmap()
        );
    }
}
