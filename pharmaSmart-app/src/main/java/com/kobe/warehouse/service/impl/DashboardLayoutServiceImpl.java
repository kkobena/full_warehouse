package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.DashboardLayout;
import com.kobe.warehouse.domain.DashboardLayoutAuthority;
import com.kobe.warehouse.domain.DashboardLayoutAuthorityId;
import com.kobe.warehouse.domain.enumeration.DashboardComponentKey;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.DashboardLayoutAuthorityRepository;
import com.kobe.warehouse.repository.DashboardLayoutRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.DashboardLayoutService;
import com.kobe.warehouse.service.dashboard.widget.WidgetAuthorizationService;
import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Dashboard Layouts
 *
 * <p>Un layout <b>système</b> n'a pas de propriétaire : livré par migration, il sert l'accueil par
 * rôle. Seul un administrateur peut le modifier ou le supprimer.
 */
@Service
@Transactional
public class DashboardLayoutServiceImpl implements DashboardLayoutService {

    private final DashboardLayoutRepository dashboardLayoutRepository;
    private final DashboardLayoutAuthorityRepository dashboardLayoutAuthorityRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final WidgetAuthorizationService widgetAuthorizationService;

    public DashboardLayoutServiceImpl(
        DashboardLayoutRepository dashboardLayoutRepository,
        DashboardLayoutAuthorityRepository dashboardLayoutAuthorityRepository,
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        WidgetAuthorizationService widgetAuthorizationService
    ) {
        this.dashboardLayoutRepository = dashboardLayoutRepository;
        this.dashboardLayoutAuthorityRepository = dashboardLayoutAuthorityRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.widgetAuthorizationService = widgetAuthorizationService;
    }

    /** Eviction ciblée : seule l'entrée de l'utilisateur courant est invalidée. */
    private static final String CURRENT_USER_KEY =
        "T(com.kobe.warehouse.security.SecurityUtils).getCurrentUserLogin().orElse('')";

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public DashboardLayoutDTO save(DashboardLayoutDTO dto) {
        AppUser currentUser = getCurrentUser();
        boolean isRoute = Boolean.TRUE.equals(dto.getIsRoute());
        if (!isRoute) {
            widgetAuthorizationService.checkLayoutConfig(dto.getLayoutConfig());
        }

        DashboardLayout layout = new DashboardLayout();
        layout.setName(dto.getName());
        layout.setDescription(dto.getDescription());
        layout.setUser(currentUser);
        layout.setScope(dto.getScope() != null ? dto.getScope() : DashboardScope.PRIVATE);
        layout.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        layout.setIsRoute(isRoute);
        layout.setComponentKey(resolveComponentKey(dto.getComponentKey(), isRoute ? DashboardComponentKey.ROUTE : DashboardComponentKey.CUSTOM));
        layout.setLayoutConfig(dto.getLayoutConfig());

        if (Boolean.TRUE.equals(layout.getIsDefault())) {
            unsetOtherUserDefaults(currentUser, null);
        }

        return toDTO(dashboardLayoutRepository.save(layout));
    }

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public DashboardLayoutDTO update(DashboardLayoutDTO dto) {
        AppUser currentUser = getCurrentUser();
        DashboardLayout layout = findLayout(dto.getId());
        checkCanModify(layout, currentUser);

        boolean isRoute = Boolean.TRUE.equals(dto.getIsRoute());
        if (!isRoute) {
            widgetAuthorizationService.checkLayoutConfig(dto.getLayoutConfig());
        }

        layout.setName(dto.getName());
        layout.setDescription(dto.getDescription());
        if (dto.getScope() != null) {
            layout.setScope(dto.getScope());
        }
        layout.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        layout.setIsRoute(isRoute);
        // Sans clé dans la requête, on garde celle du layout : l'écraser ferait basculer l'accueil
        layout.setComponentKey(resolveComponentKey(dto.getComponentKey(), layout.getComponentKey()));
        layout.setLayoutConfig(dto.getLayoutConfig());

        if (Boolean.TRUE.equals(layout.getIsDefault()) && layout.getUser() != null) {
            unsetOtherUserDefaults(layout.getUser(), layout.getId());
        }

        return toDTO(dashboardLayoutRepository.save(layout));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllForCurrentUser() {
        AppUser currentUser = getCurrentUser();
        return dashboardLayoutRepository.findByUserOrPublic(currentUser).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllPublic() {
        return dashboardLayoutRepository.findPublicUserLayouts().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardLayoutDTO> findOne(Integer id) {
        AppUser currentUser = getCurrentUser();
        return dashboardLayoutRepository.findById(id).filter(layout -> isReadable(layout, currentUser)).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardLayoutDTO> findDefaultForCurrentUser() {
        AppUser currentUser = getCurrentUser();
        return dashboardLayoutRepository.findByUserAndIsDefaultTrue(currentUser).map(this::toDTO);
    }

    /**
     * Résolution en 2 niveaux :
     *  1. Layout personnel (user_id = currentUser, is_default = true)
     *  2. Layout par rôle  (dashboard_layout_authority.is_default = true pour le premier rôle trouvé)
     *
     * Mis en cache par login (TTL 24h).
     * Invalidé automatiquement sur toute modification (save, update, delete, setAsDefault).
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public Optional<DashboardLayoutDTO> resolveForCurrentUser() {
        AppUser currentUser = getCurrentUser();

        // Niveau 1 — layout personnel
        Optional<DashboardLayout> personal = dashboardLayoutRepository.findByUserAndIsDefaultTrue(currentUser);
        if (personal.isPresent()) {
            return personal.map(this::toDTO);
        }

        // Niveau 2 — layout par rôle via la table d'association
        // AppUser.getAuthorities() retourne Set<Authority> directement
        return currentUser
            .getAuthorities()
            .stream()
            .map(auth -> dashboardLayoutAuthorityRepository.findDefaultByAuthorityName(auth.getName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .map(dla -> toDTOWithIsDefault(dla.getLayout(), true));
    }

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public DashboardLayoutDTO setAsDefault(Integer id) {
        AppUser currentUser = getCurrentUser();
        DashboardLayout layout = findLayout(id);

        // Le drapeau d'un layout système n'est lu par personne : l'accueil par rôle passe par l'association
        if (layout.getUser() == null) {
            throw new GenericError("Un tableau de bord système ne peut pas devenir votre accueil ; dupliquez-le d'abord.", "layoutSysteme");
        }
        if (!isOwner(layout, currentUser)) {
            throw new ForbiddenOperationException(
                "Seul le propriétaire peut faire de ce tableau de bord son accueil ; dupliquez-le d'abord.",
                "layoutNonProprietaire"
            );
        }

        unsetOtherUserDefaults(currentUser, layout.getId());
        layout.setIsDefault(true);
        return toDTO(dashboardLayoutRepository.save(layout));
    }

    /**
     * Eviction totale : changer le layout d'un rôle affecte tous les utilisateurs
     * de ce rôle — on ne peut pas cibler une entrée précise sans connaître tous les logins.
     */
    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, allEntries = true)
    public DashboardLayoutDTO setAsDefaultForAuthority(Integer id, String authorityName) {
        requireAdmin(getCurrentUser());
        authorityRepository.findById(authorityName).orElseThrow(() -> new GenericError("Rôle inexistant : " + authorityName));

        DashboardLayout layout = findLayout(id);

        // Retire is_default des autres entrées pour ce rôle
        dashboardLayoutAuthorityRepository
            .findByAuthorityName(authorityName)
            .forEach(dla -> {
                if (Boolean.TRUE.equals(dla.getIsDefault())) {
                    dla.setIsDefault(false);
                    dashboardLayoutAuthorityRepository.save(dla);
                }
            });

        // Crée ou met à jour l'association pour ce layout + ce rôle
        DashboardLayoutAuthorityId assocId = new DashboardLayoutAuthorityId(layout.getId(), authorityName);
        DashboardLayoutAuthority association = dashboardLayoutAuthorityRepository
            .findById(assocId)
            .orElseGet(() -> {
                DashboardLayoutAuthority newAssoc = new DashboardLayoutAuthority();
                newAssoc.setId(assocId);
                newAssoc.setLayout(layout);
                authorityRepository.findById(authorityName).ifPresent(newAssoc::setAuthority);
                return newAssoc;
            });
        association.setIsDefault(true);
        dashboardLayoutAuthorityRepository.save(association);

        return toDTO(layout);
    }

    /** Eviction totale : le layout supprimé peut être l'accueil d'un rôle entier. */
    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, allEntries = true)
    public void delete(Integer id) {
        AppUser currentUser = getCurrentUser();
        DashboardLayout layout = findLayout(id);
        checkCanModify(layout, currentUser);
        dashboardLayoutRepository.delete(layout);
    }

    @Override
    public DashboardLayoutDTO clone(Integer id, String newName) {
        AppUser currentUser = getCurrentUser();
        DashboardLayout original = findLayout(id);
        if (!isReadable(original, currentUser)) {
            throw new GenericError("Tableau de bord inexistant", "layoutInexistant");
        }
        if (!Boolean.TRUE.equals(original.getIsRoute())) {
            widgetAuthorizationService.checkLayoutConfig(original.getLayoutConfig());
        }

        DashboardLayout clone = new DashboardLayout();
        clone.setName(newName);
        clone.setDescription("Clone de : " + original.getName());
        clone.setUser(currentUser);
        clone.setScope(DashboardScope.PRIVATE);
        clone.setIsDefault(false);
        clone.setIsRoute(original.getIsRoute());
        clone.setComponentKey(original.getComponentKey());
        clone.setLayoutConfig(original.getLayoutConfig());

        return toDTO(dashboardLayoutRepository.save(clone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllForAuthority(String authorityName) {
        requireAdmin(getCurrentUser());
        return dashboardLayoutAuthorityRepository
            .findByAuthorityName(authorityName)
            .stream()
            .map(dla -> toDTOWithIsDefault(dla.getLayout(), dla.getIsDefault()))
            .collect(Collectors.toList());
    }

    private AppUser getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new GenericError("Utilisateur non connecté"));
        return userRepository.findOneByLogin(login).orElseThrow(() -> new GenericError("Utilisateur introuvable"));
    }

    private DashboardLayout findLayout(Integer id) {
        return dashboardLayoutRepository.findById(id).orElseThrow(() -> new GenericError("Tableau de bord inexistant", "layoutInexistant"));
    }

    private static boolean isOwner(DashboardLayout layout, AppUser user) {
        return layout.getUser() != null && Objects.equals(layout.getUser().getId(), user.getId());
    }

    private static boolean isAdmin(AppUser user) {
        return user.getAuthorities().stream().anyMatch(a -> AuthoritiesConstants.ADMIN.equals(a.getName()));
    }

    /** Lisible : le sien, un layout système, ou un layout que son auteur a partagé. */
    private static boolean isReadable(DashboardLayout layout, AppUser user) {
        return layout.getUser() == null || isOwner(layout, user) || layout.getScope() != DashboardScope.PRIVATE;
    }

    private static void checkCanModify(DashboardLayout layout, AppUser user) {
        if (layout.getUser() == null) {
            requireAdmin(user);
        } else if (!isOwner(layout, user) && !isAdmin(user)) {
            throw new ForbiddenOperationException("Seul le propriétaire peut modifier ce tableau de bord.", "layoutNonProprietaire");
        }
    }

    private static void requireAdmin(AppUser user) {
        if (!isAdmin(user)) {
            throw new ForbiddenOperationException("Opération réservée à l'administrateur.", "adminRequis");
        }
    }

    private static DashboardComponentKey resolveComponentKey(String value, DashboardComponentKey fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return DashboardComponentKey.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new GenericError("Type de tableau de bord inconnu : " + value, "componentKeyInconnu");
        }
    }

    /** Un seul accueil personnel par utilisateur, quelle que soit la portée de ses layouts. */
    private void unsetOtherUserDefaults(AppUser user, Integer keptLayoutId) {
        dashboardLayoutRepository
            .findAllByUserAndIsDefaultTrue(user)
            .stream()
            .filter(layout -> !Objects.equals(layout.getId(), keptLayoutId))
            .forEach(layout -> {
                layout.setIsDefault(false);
                dashboardLayoutRepository.save(layout);
            });
    }

    private DashboardLayoutDTO toDTO(DashboardLayout layout) {
        return toDTOWithIsDefault(layout, layout.getIsDefault());
    }

    /**
     * Variante de toDTO utilisée lors de la résolution par rôle :
     * isDefault est tiré de l'association (DashboardLayoutAuthority), pas du layout lui-même.
     */
    private DashboardLayoutDTO toDTOWithIsDefault(DashboardLayout layout, Boolean isDefault) {
        List<String> authorityNames = dashboardLayoutAuthorityRepository
            .findByLayoutId(layout.getId())
            .stream()
            .map(dla -> dla.getAuthority().getName())
            .collect(Collectors.toList());

        return new DashboardLayoutDTO(
            layout.getId(),
            layout.getName(),
            layout.getDescription(),
            layout.getUser() != null ? layout.getUser().getId() : null,
            layout.getUser() != null ? layout.getUser().getLogin() : null,
            authorityNames,
            layout.getScope(),
            isDefault,
            layout.getIsRoute(),
            layout.getComponentKey() != null ? layout.getComponentKey().name() : null,
            layout.getLayoutConfig(),
            layout.getCreatedAt(),
            layout.getUpdatedAt()
        );
    }
}
