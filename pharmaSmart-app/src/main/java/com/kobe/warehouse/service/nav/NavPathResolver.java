package com.kobe.warehouse.service.nav;

import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconstruit le chemin d'accès d'une entrée de menu — « Barre de navigation ▸ Gestion Courante
 * ▸ Ventes ▸ Proformas » — à partir de la hiérarchie {@code nav_item}.
 * <p>
 * Le guide des fonctionnalités désignait ce chemin par une chaîne recopiée à la main. Or les
 * libellés se renomment (PATCH /api/admin/nav/items/{id}/libelle) et les entrées se déplacent :
 * la chaîne devenait fausse sans que rien ne le signale. Seul le code d'une entrée est stable,
 * le libellé et la position ne le sont pas — c'est donc le code que le guide référence, et ce
 * service qui en dérive le libellé au moment de l'affichage.
 * <p>
 * La résolution ignore volontairement {@code actif} et les permissions de rôle : un manuel
 * décrit l'arborescence de l'application, pas ce que l'utilisateur qui le consulte a le droit
 * de voir. Un pharmacien qui lit la fiche « Annulation de vente » doit connaître son
 * emplacement même si son propre profil ne lui en donne pas l'accès.
 */
@Service
public class NavPathResolver {

    /** Séparateur des niveaux de menu, identique à celui affiché dans le guide. */
    public static final String SEPARATEUR = " ▸ ";

    /** Racine implicite : le premier niveau de nav_item est déjà dans la barre. */
    private static final String RACINE = "Barre de navigation";

    /**
     * Garde-fou : une hiérarchie de menus se compte en unités de profondeur. Une valeur plus
     * élevée trahirait un cycle parent/enfant en base, qui boucherait sinon indéfiniment.
     */
    private static final int PROFONDEUR_MAX = 10;

    private final NavItemRepository navItemRepository;

    public NavPathResolver(NavItemRepository navItemRepository) {
        this.navItemRepository = navItemRepository;
    }

    /**
     * Chemin complet de chaque entrée, indexé par code. Une seule lecture de la table : les
     * appelants en résolvent des dizaines d'un coup (le guide compte une entrée par rubrique),
     * les interroger une par une multiplierait les allers-retours pour le même résultat.
     */
    @Transactional(readOnly = true)
    public Map<String, String> resolveAll() {
        List<NavItem> items = navItemRepository.findAll();
        Map<String, String> chemins = new HashMap<>(items.size());
        for (NavItem item : items) {
            chemins.put(item.getCode(), buildPath(item));
        }
        return chemins;
    }

    /**
     * Chemin d'une entrée, ou {@code Optional.empty()} si le code n'existe pas — un code
     * disparu ne doit pas faire échouer l'écran qui l'affiche, seulement priver le lecteur
     * d'une indication.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolve(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(navItemRepository.findByCode(code)).map(this::buildPath);
    }


    private String buildPath(NavItem item) {
        List<String> libelles = new ArrayList<>();
        Set<Integer> visites = new HashSet<>();
        NavItem courant = item;
        int profondeur = 0;

        while (courant != null && profondeur++ < PROFONDEUR_MAX && visites.add(courant.getId())) {
            libelles.addFirst(courant.getLibelle());
            courant = courant.getParent();
        }

        libelles.addFirst(RACINE);
        return String.join(SEPARATEUR, libelles);
    }
}

