package com.kobe.warehouse.service.cahier_recette;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.nav.NavPathResolver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Charge data/cahier-recette.json (généré depuis CAHIER_RECETTE côté TypeScript par `npm run
 * generate:cahier-recette`, cf. pharmaSmart-app/pom.xml) et le garde en mémoire.
 * <p>
 * Filtre les entrées `hidden` (même règle que `withoutHidden()` côté front,
 * cahier-recette.component.ts) et les fonctionnalités `roadmap` : le manuel documente ce qui est
 * livré, pas la feuille de route.
 * <p>
 * Les captures restent attachées à l'étape qu'elles illustrent : le manuel montre le geste à
 * l'endroit où il est décrit, et non une vue finale détachée de la marche à suivre.
 */
@Service
public class CahierRecetteDataService {

    private static final String RESOURCE_PATH = "data/cahier-recette.json";

    private final ObjectMapper objectMapper;
    private final NavPathResolver navPathResolver;
    private List<ModuleRecetteDTO> modules;

    public CahierRecetteDataService(ObjectMapper objectMapper, NavPathResolver navPathResolver) {
        this.objectMapper = objectMapper;
        this.navPathResolver = navPathResolver;
    }

    /**
     * Le JSON est mis en cache, pas les chemins de menu : ceux-ci sont recalculés à chaque
     * appel. Un libellé renommé ou une entrée déplacée doit se refléter dans le guide sans
     * redémarrage, sans quoi on aurait déplacé le problème du fichier vers la mémoire.
     */
    public List<ModuleRecetteDTO> getModules() {
        return withResolvedAcces(rawModules());
    }

    private synchronized List<ModuleRecetteDTO> rawModules() {
        if (modules == null) {
            modules = loadModules();
        }
        return modules;
    }

    private List<ModuleRecetteDTO> withResolvedAcces(List<ModuleRecetteDTO> source) {
        Map<String, String> chemins = navPathResolver.resolveAll();
        return source.stream()
            .map(module -> new ModuleRecetteDTO(module.id(), module.nom(), module.icone(),
                module.description(),
                module.fonctionnalites().stream().map(f -> withAcces(f, chemins)).toList()))
            .toList();
    }

    /**
     * Un code inconnu — entrée supprimée, faute de frappe — laisse la fonctionnalité sans
     * chemin plutôt que d'afficher le code brut : mieux vaut taire l'information que montrer
     * au pharmacien un identifiant technique qu'il ne trouvera nulle part dans l'écran.
     */
    private FonctionnaliteRecetteDTO withAcces(FonctionnaliteRecetteDTO fonctionnalite,
        Map<String, String> chemins) {
        String code = fonctionnalite.accesCode();
        if (code == null || code.isBlank()) {
            return fonctionnalite;
        }
        String chemin = chemins.get(code);
        if (chemin == null) {
            return fonctionnalite;
        }
        String suffixe = fonctionnalite.accesSuffixe();
        String complet = (suffixe == null || suffixe.isBlank())
            ? chemin
            : chemin + NavPathResolver.SEPARATEUR + suffixe;
        return new FonctionnaliteRecetteDTO(
            fonctionnalite.nom(),
            fonctionnalite.description(),
            complet,
            code,
            suffixe,
            fonctionnalite.scenarios(),
            fonctionnalite.hidden(),
            fonctionnalite.version(),
            fonctionnalite.roadmap()
        );
    }

    /**
     * Retourne uniquement les modules demandés. Une sélection vide désigne le manuel complet.
     * Les identifiants inconnus sont ignorés : ils ne peuvent ainsi ni élargir la sélection ni
     * provoquer une erreur de génération.
     */
    public List<ModuleRecetteDTO> getModules(Collection<String> moduleIds) {
        return getModules(moduleIds, List.of());
    }

    /**
     * Filtre le manuel par modules puis, si la sélection n'est pas vide, par scénarios. Cette
     * seconde maille permet d'imprimer une rubrique de parcours dépendants sans recopier le modèle.
     */
    public List<ModuleRecetteDTO> getModules(Collection<String> moduleIds,
        Collection<String> scenarioIds) {
        List<ModuleRecetteDTO> selectedModules;
        if (moduleIds == null || moduleIds.isEmpty()) {
            selectedModules = getModules();
        } else {
            Set<String> selection = normalizedSelection(moduleIds);
            selectedModules = getModules().stream()
                .filter(module -> selection.contains(module.id()))
                .toList();
        }

        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return selectedModules;
        }
        Set<String> selectedScenarios = normalizedSelection(scenarioIds);
        return selectedModules.stream()
            .map(module -> filterScenarios(module, selectedScenarios))
            .filter(module -> !module.fonctionnalites().isEmpty())
            .toList();
    }

    private Set<String> normalizedSelection(Collection<String> ids) {
        return ids.stream()
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    private ModuleRecetteDTO filterScenarios(ModuleRecetteDTO module, Set<String> scenarioIds) {
        List<FonctionnaliteRecetteDTO> features = module.fonctionnalites().stream()
            .map(feature -> withScenarios(feature, feature.scenarios().stream()
                .filter(scenario -> scenarioIds.contains(scenario.id()))
                .toList()))
            .filter(feature -> !feature.scenarios().isEmpty())
            .toList();
        return new ModuleRecetteDTO(module.id(), module.nom(), module.icone(), module.description(),
            features);
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
        return withScenarios(fonctionnalite, scenarios);
    }

    private FonctionnaliteRecetteDTO withScenarios(FonctionnaliteRecetteDTO fonctionnalite,
        List<ScenarioRecetteDTO> scenarios) {
        return new FonctionnaliteRecetteDTO(
            fonctionnalite.nom(),
            fonctionnalite.description(),
            fonctionnalite.acces(),
            fonctionnalite.accesCode(),
            fonctionnalite.accesSuffixe(),
            scenarios,
            fonctionnalite.hidden(),
            fonctionnalite.version(),
            fonctionnalite.roadmap()
        );
    }
}
