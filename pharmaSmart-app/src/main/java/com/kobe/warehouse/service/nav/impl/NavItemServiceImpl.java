package com.kobe.warehouse.service.nav.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.domain.nav.NavItemRole;
import com.kobe.warehouse.domain.nav.NavItemUserOrder;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.repository.nav.NavItemUserOrderRepository;
import com.kobe.warehouse.service.dto.nav.NavAssignDTO;
import com.kobe.warehouse.service.dto.nav.NavItemAssignmentDTO;
import com.kobe.warehouse.service.dto.nav.NavNodeDTO;
import com.kobe.warehouse.service.dto.nav.NavPermissionsDTO;
import com.kobe.warehouse.service.dto.nav.NavReorderDTO;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.service.nav.NavItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class NavItemServiceImpl implements NavItemService {

    private static final Logger LOG = LoggerFactory.getLogger(NavItemServiceImpl.class);

    private final NavItemRepository navItemRepository;
    private final NavItemRoleRepository navItemRoleRepository;
    private final NavItemUserOrderRepository navItemUserOrderRepository;
    private final LicenseService licenseService;

    public NavItemServiceImpl(
        NavItemRepository navItemRepository,
        NavItemRoleRepository navItemRoleRepository,
        NavItemUserOrderRepository navItemUserOrderRepository,
        LicenseService licenseService
    ) {
        this.navItemRepository = navItemRepository;
        this.navItemRoleRepository = navItemRoleRepository;
        this.navItemUserOrderRepository = navItemUserOrderRepository;
        this.licenseService = licenseService;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = EntityConstant.NAV_TREE_CACHE, key = "#login")
    public List<NavNodeDTO> buildTreeForRoles(Set<String> roles, String login) {
        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptyList();
        }

        //Charger tous les NavItem actifs pour les rôles
        List<NavItem> allItems = retainSubscribedFeatures(navItemRepository.findAllActiveByRoles(roles));
        if (CollectionUtils.isEmpty(allItems)) {
            return Collections.emptyList();
        }

        // Batch-charger toutes les NavItemRole correspondantes
        List<Integer> itemIds = allItems.stream().map(NavItem::getId).toList();
        List<NavItemRole> allRoles = navItemRoleRepository.findAllByNavItemIdInAndRoleNameIn(itemIds, roles);

        //Merger les permissions par OR logique (union des rôles)
        Map<Integer, NavPermissionsDTO> permissionsMap = buildPermissionsMap(allRoles);

        //Charger les ordres personnalisés de l'utilisateur
        Map<Integer, Integer> userOrderMap = buildUserOrderMap(login);

        // Construire l'arbre parent/enfant
        return buildTree(allItems, permissionsMap, userOrderMap);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findExecutableActionCodes(Set<String> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return Collections.emptySet();
        }
        return navItemRoleRepository.findExecutableCodesByRoles(roles, NavTargetType.ACTION);
    }

    @Override
    @CacheEvict(cacheNames = EntityConstant.NAV_TREE_CACHE, key = "#login")
    public void saveUserOrder(String login, List<NavReorderDTO> reorderList) {
        if (CollectionUtils.isEmpty(reorderList)) return;
        for (NavReorderDTO dto : reorderList) {
            NavItemUserOrder userOrder = navItemUserOrderRepository
                .findByUserLoginAndNavItemId(login, dto.navItemId())
                .orElseGet(() -> {
                    NavItem ref = navItemRepository.getReferenceById(dto.navItemId());
                    return new NavItemUserOrder().setUserLogin(login).setNavItem(ref);
                });
            userOrder.setOrdre(dto.newOrdre());
            navItemUserOrderRepository.save(userOrder);
        }
    }

    @Override
    @CacheEvict(cacheNames = EntityConstant.NAV_TREE_CACHE, allEntries = true)
    public void saveAdminOrder(List<NavReorderDTO> reorderList) {
        if (CollectionUtils.isEmpty(reorderList)) return;
        for (NavReorderDTO dto : reorderList) {
            NavItem item = navItemRepository.findById(dto.navItemId()).orElse(null);
            if (item == null) continue;
            item.setOrdre(dto.newOrdre());
            if (dto.newParentId() != null) {
                NavItem newParent = navItemRepository.getReferenceById(dto.newParentId());
                item.setParent(newParent);
            }
            navItemRepository.save(item);
        }
    }

    @Override
    @CacheEvict(cacheNames = EntityConstant.NAV_TREE_CACHE, allEntries = true)
    public void assignItemsToRole(NavAssignDTO dto) {
        if (dto == null || CollectionUtils.isEmpty(dto.assignments())) return;
        for (NavItemAssignmentDTO assignment : dto.assignments()) {
            NavItemRole role = navItemRoleRepository
                .findByNavItemIdAndRoleName(assignment.navItemId(), dto.roleName())
                .orElseGet(() -> {
                    NavItem navItem = navItemRepository.getReferenceById(assignment.navItemId());
                    return new NavItemRole().setNavItem(navItem).setRoleName(dto.roleName());
                });
            role.setCanDisplay(assignment.canDisplay())
                .setCanAccess(assignment.canAccess())
                .setCanCreate(assignment.canCreate())
                .setCanEdit(assignment.canEdit())
                .setCanDelete(assignment.canDelete())
                .setCanExport(assignment.canExport())
                .setCanExecute(assignment.canExecute());
            navItemRoleRepository.save(role);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NavNodeDTO> findAllItems() {
        List<NavItem> allItems = navItemRepository.findAllByActifTrueOrderByOrdreAsc();
        return buildAdminTree(allItems, Collections.emptyMap(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NavNodeDTO> findAllItemsForRole(String roleName) {
        List<NavItem> allItems = navItemRepository.findAllByActifTrueOrderByOrdreAsc();
        if (allItems.isEmpty()) return Collections.emptyList();

        // Charger les permissions existantes pour ce rôle
        List<Integer> itemIds = allItems.stream().map(NavItem::getId).toList();
        List<NavItemRole> rolePermissions =
            navItemRoleRepository.findAllByNavItemIdInAndRoleNameIn(itemIds, Set.of(roleName));

        // Construire une map itemId → permissions
        Map<Integer, NavPermissionsDTO> permMap = new HashMap<>();
        for (NavItemRole r : rolePermissions) {
            permMap.put(r.getNavItem().getId(), new NavPermissionsDTO(
                r.isCanDisplay(), r.isCanAccess(), r.isCanCreate(),
                r.isCanEdit(), r.isCanDelete(), r.isCanExport(), r.isCanExecute()
            ));
        }

        return buildAdminTree(allItems, permMap, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retire les items dont le module n'est pas couvert par la licence (§3.6).
     *
     * <p>Le filtre s'applique <strong>avant</strong> la construction de l'arbre, au même endroit que
     * le filtrage par rôles : un item écarté n'a pas de parent auquel se rattacher, ses descendants
     * ne sont donc jamais visités, et un groupe dont tous les enfants disparaissent est déjà écarté
     * par la règle « groupe sans enfant » de {@code buildTree}. Aucune modification du frontend
     * n'est nécessaire — les menus non souscrits <em>n'existent pas</em> dans la réponse.
     *
     * <p>Un libellé de feature inconnu est traité comme « aucune contrainte » : une valeur mal
     * saisie en base ne doit pas faire disparaître silencieusement un menu.
     *
     * <p><strong>Inerte à ce jour</strong> : {@code required_feature} n'est renseignée sur aucun
     * item — aucune migration ne la remplit — donc tous les menus existants continuent d'être
     * chargés. Le rattachement menu → module est une décision commerciale à prendre item par item,
     * pas une dérivation automatique : {@code nav_item.code} désigne un <em>menu</em>
     * (« comptabilite ») et {@code required_feature} un <em>module de licence</em>
     * (« COMPTABILITE ») ; rien ne garantit que les deux nommages coïncident.
     */
    private List<NavItem> retainSubscribedFeatures(List<NavItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return items;
        }
        return items.stream().filter(this::isFeatureSubscribed).toList();
    }

    private boolean isFeatureSubscribed(NavItem item) {
        String required = item.getRequiredFeature();
        if (required == null || required.isBlank()) {
            return true;
        }
        try {
            return licenseService.hasFeature(Feature.valueOf(required.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            LOG.warn("Module inconnu « {} » sur l'item de navigation « {} » : contrainte ignorée", required, item.getCode());
            return true;
        }
    }

    private Map<Integer, NavPermissionsDTO> buildPermissionsMap(List<NavItemRole> roles) {
        Map<Integer, NavPermissionsDTO> map = new HashMap<>();
        for (NavItemRole role : roles) {
            Integer itemId = role.getNavItem().getId();
            NavPermissionsDTO existing = map.get(itemId);
            NavPermissionsDTO current = new NavPermissionsDTO(
                role.isCanDisplay(),
                role.isCanAccess(),
                role.isCanCreate(),
                role.isCanEdit(),
                role.isCanDelete(),
                role.isCanExport(),
                role.isCanExecute()
            );
            map.put(itemId, existing == null ? current : existing.merge(current));
        }
        return map;
    }

    private Map<Integer, Integer> buildUserOrderMap(String login) {
        return navItemUserOrderRepository.findAllByUserLogin(login)
            .stream()
            .collect(Collectors.toMap(
                o -> o.getNavItem().getId(),
                NavItemUserOrder::getOrdre
            ));
    }

    private List<NavNodeDTO> buildTree(
        List<NavItem> allItems,
        Map<Integer, NavPermissionsDTO> permissionsMap,
        Map<Integer, Integer> userOrderMap
    ) {
        List<NavItem> roots = allItems.stream()
            .filter(i -> i.getParent() == null)
            .sorted((a, b) -> {
                int oa = userOrderMap.getOrDefault(a.getId(), a.getOrdre());
                int ob = userOrderMap.getOrDefault(b.getId(), b.getOrdre());
                return Integer.compare(oa, ob);
            })
            .toList();

        List<NavNodeDTO> tree = new ArrayList<>();
        for (NavItem root : roots) {
            NavPermissionsDTO perms = permissionsMap.get(root.getId());
            // Même règle que pour les enfants : un élément INTRA-PAGE (bouton, onglet) reste
            // dans l'arbre même masqué, parce que c'est de l'arbre que le front tire ses
            // habilitations. Sans cette exception, un item de ce type placé à la racine —
            // « nouvelle-vente » l'est — perdait toute permission dès qu'on le masquait, et
            // le bouton correspondant disparaissait des écrans qui le testent.
            boolean isInPageItem = root.getTargetType() == NavTargetType.SECTION
                                || root.getTargetType() == NavTargetType.ACTION;
            if (perms == null || (!perms.canDisplay() && !isInPageItem)) continue;
            List<NavNodeDTO> children = buildChildren(root, allItems, permissionsMap, userOrderMap);
            if (root.getTargetType() == NavTargetType.GROUP && children.isEmpty()) continue;
            tree.add(toNodeDTO(root, perms, children));
        }
        return tree;
    }

    private List<NavNodeDTO> buildChildren(
        NavItem parent,
        List<NavItem> allItems,
        Map<Integer, NavPermissionsDTO> permissionsMap,
        Map<Integer, Integer> userOrderMap
    ) {
        List<NavNodeDTO> result = new ArrayList<>();
        allItems.stream()
            .filter(i -> i.getParent() != null && Objects.equals(i.getParent().getId(), parent.getId()))
            .sorted((a, b) -> {
                int oa = userOrderMap.getOrDefault(a.getId(), a.getOrdre());
                int ob = userOrderMap.getOrDefault(b.getId(), b.getOrdre());
                return Integer.compare(oa, ob);
            })
            .forEach(child -> {
                NavPermissionsDTO perms = permissionsMap.get(child.getId());
                // Les items SECTION et ACTION sont des éléments intra-page (onglets/boutons).
                // On les inclut même avec canDisplay=false pour que le frontend puisse distinguer
                // "non configuré en base" (fallback permissif) de "configuré mais interdit" (caché).
                boolean isInPageItem = child.getTargetType() == NavTargetType.SECTION
                                    || child.getTargetType() == NavTargetType.ACTION;
                if (perms == null || (!perms.canDisplay() && !isInPageItem)) return;
                List<NavNodeDTO> grandChildren = buildChildren(child, allItems, permissionsMap, userOrderMap);
                result.add(toNodeDTO(child, perms, grandChildren));
            });
        return result;
    }

    private NavNodeDTO toNodeDTO(NavItem item, NavPermissionsDTO permissions, List<NavNodeDTO> children) {
        return new NavNodeDTO(
            item.getId(),
            item.getCode(),
            item.getLibelle(),
            item.getIcon(),
            item.getTitreLong(),
            item.getRouterLink(),
            item.getOrdre(),
            item.getBadgeType() != null ? item.getBadgeType().name() : "NONE",
            item.getTargetType() != null ? item.getTargetType().name() : "ROUTE",
            children,
            permissions
        );
    }

    @Override
    @CacheEvict(cacheNames = EntityConstant.NAV_TREE_CACHE, allEntries = true)
    public void updateLibelle(Integer id, String libelle) {
        NavItem item = navItemRepository.getReferenceById(id);
        item.setLibelle(libelle.trim());
        navItemRepository.save(item);
    }

    @Override
    @CacheEvict(cacheNames = EntityConstant.NAV_TREE_CACHE, allEntries = true)
    public void updateTitreLong(Integer id, String titreLong) {
        NavItem item = navItemRepository.getReferenceById(id);
        String valeur = titreLong == null ? null : titreLong.trim();
        // Une chaîne vide ferait diverger la barre du menu au premier renommage : on efface.
        item.setTitreLong(valeur == null || valeur.isEmpty() ? null : valeur);
        navItemRepository.save(item);
    }

    /**
     * Construit un arbre imbriqué pour la vue admin (tous les items actifs, permissions incluses).
     * Le frontend se charge d'aplatir l'arbre avec la profondeur calculée côté client.
     */
    private List<NavNodeDTO> buildAdminTree(
        List<NavItem> allItems,
        Map<Integer, NavPermissionsDTO> permMap,
        Integer parentId
    ) {
        List<NavNodeDTO> result = new ArrayList<>();
        allItems.stream()
            .filter(i -> parentId == null
                ? i.getParent() == null
                : i.getParent() != null && i.getParent().getId().equals(parentId))
            .sorted(Comparator.comparingInt(NavItem::getOrdre))
            .forEach(item -> {
                NavPermissionsDTO perms = permMap.getOrDefault(item.getId(), NavPermissionsDTO.noAccess());
                List<NavNodeDTO> children = buildAdminTree(allItems, permMap, item.getId());
                result.add(new NavNodeDTO(
                    item.getId(), item.getCode(), item.getLibelle(), item.getIcon(), item.getTitreLong(),
                    item.getRouterLink(),
                    item.getOrdre(),
                    item.getBadgeType() != null ? item.getBadgeType().name() : "NONE",
                    item.getTargetType() != null ? item.getTargetType().name() : "ROUTE",
                    children, perms
                ));
            });
        return result;
    }
}

