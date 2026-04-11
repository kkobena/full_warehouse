package com.kobe.warehouse.service.nav;

import com.kobe.warehouse.service.dto.nav.NavAssignDTO;
import com.kobe.warehouse.service.dto.nav.NavNodeDTO;
import com.kobe.warehouse.service.dto.nav.NavReorderDTO;
import java.util.List;
import java.util.Set;

/**
 * Service de navigation dynamique.
 * Construit l'arbre de navigation filtré selon les rôles de l'utilisateur.
 */
public interface NavItemService {

    /**
     * Construit l'arbre de navigation filtré pour les rôles donnés,
     * en appliquant l'ordre personnalisé de l'utilisateur.
     *
     * @param roles liste des rôles de l'utilisateur (ex: ["ROLE_ADMIN", "ROLE_USER"])
     * @param login login de l'utilisateur courant (clé de cache)
     * @return arbre de navigation trié et filtré
     */
    List<NavNodeDTO> buildTreeForRoles(Set<String> roles, String login);

    /**
     * Sauvegarde l'ordre personnalisé de l'utilisateur (préférences perso).
     *
     * @param login      login de l'utilisateur
     * @param reorderList liste des nouvelles positions
     */
    void saveUserOrder(String login, List<NavReorderDTO> reorderList);

    /**
     * Sauvegarde l'ordre global des items (admin — partagé pour tous).
     *
     * @param reorderList liste des nouvelles positions
     */
    void saveAdminOrder(List<NavReorderDTO> reorderList);

    /**
     * Assigne des NavItems à un rôle avec des permissions fines.
     *
     * @param dto DTO contenant le rôle et les permissions par item
     */
    void assignItemsToRole(NavAssignDTO dto);

    /**
     * Retourne tous les items de navigation (vue admin) — permissions vides (false).
     *
     * @return liste plate de tous les NavNodeDTO
     */
    List<NavNodeDTO> findAllItems();

    /**
     * Retourne tous les items avec les permissions existantes pour un rôle donné.
     * Les items sans assignation pour ce rôle ont toutes les permissions à false.
     *
     * @param roleName nom du rôle (ex: "ROLE_ADMIN")
     * @return liste plate avec permissions du rôle
     */
    List<NavNodeDTO> findAllItemsForRole(String roleName);

    /**
     * Met à jour le libellé affiché d'un item de navigation.
     *
     * @param id      identifiant du NavItem
     * @param libelle nouveau libellé (non vide)
     */
    void updateLibelle(Integer id, String libelle);
}

