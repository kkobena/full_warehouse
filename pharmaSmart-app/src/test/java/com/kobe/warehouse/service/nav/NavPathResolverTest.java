package com.kobe.warehouse.service.nav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NavPathResolverTest {

    private final NavItemRepository repository = mock(NavItemRepository.class);
    private final NavPathResolver resolver = new NavPathResolver(repository);

    @Test
    void buildsFullPathFromMenuHierarchy() {
        NavItem groupe = item(1, "gestion-courante", "Gestion Courante", null);
        NavItem ventes = item(2, "ventes", "Ventes", groupe);
        NavItem proformas = item(3, "ventes.devis", "Proformas", ventes);
        when(repository.findAll()).thenReturn(List.of(groupe, ventes, proformas));

        Map<String, String> chemins = resolver.resolveAll();

        assertThat(chemins.get("ventes.devis"))
            .isEqualTo("Barre de navigation ▸ Gestion Courante ▸ Ventes ▸ Proformas");
        assertThat(chemins.get("ventes")).isEqualTo("Barre de navigation ▸ Gestion Courante ▸ Ventes");
    }

    /**
     * Raison d'être de tout ce mécanisme : un libellé renommé depuis l'écran d'administration
     * doit se répercuter dans le guide. Une chaîne recopiée dans le modèle ne le ferait pas.
     */
    @Test
    void followsRenamedLabels() {
        NavItem groupe = item(1, "gestion-courante", "Exploitation quotidienne", null);
        NavItem ventes = item(2, "ventes", "Comptoir", groupe);
        when(repository.findAll()).thenReturn(List.of(groupe, ventes));

        assertThat(resolver.resolveAll().get("ventes"))
            .isEqualTo("Barre de navigation ▸ Exploitation quotidienne ▸ Comptoir");
    }

    /** Un code absent ne doit pas propager de null au reste de la chaîne d'affichage. */
    @Test
    void returnsEmptyForUnknownCode() {
        when(repository.findByCode("inconnu")).thenReturn(null);

        assertThat(resolver.resolve("inconnu")).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve("  ")).isEmpty();
    }

    /**
     * Une boucle parent/enfant en base ne doit pas figer le serveur : le guide s'affiche avec
     * un chemin tronqué, ce qui reste préférable à une requête qui ne rend jamais la main.
     */
    @Test
    void stopsOnCyclicHierarchy() {
        NavItem a = item(1, "a", "A", null);
        NavItem b = item(2, "b", "B", a);
        a.setParent(b);
        when(repository.findByCode("b")).thenReturn(b);

        assertThat(resolver.resolve("b")).contains("Barre de navigation ▸ A ▸ B");
    }

    private NavItem item(Integer id, String code, String libelle, NavItem parent) {
        return new NavItem().setId(id).setCode(code).setLibelle(libelle).setParent(parent);
    }
}

