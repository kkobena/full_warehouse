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
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.DashboardLayoutService;
import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Dashboard Layouts
 */
@Service
@Transactional
public class DashboardLayoutServiceImpl implements DashboardLayoutService {

    private final DashboardLayoutRepository dashboardLayoutRepository;
    private final DashboardLayoutAuthorityRepository dashboardLayoutAuthorityRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    public DashboardLayoutServiceImpl(
        DashboardLayoutRepository dashboardLayoutRepository,
        DashboardLayoutAuthorityRepository dashboardLayoutAuthorityRepository,
        UserRepository userRepository,
        AuthorityRepository authorityRepository
    ) {
        this.dashboardLayoutRepository = dashboardLayoutRepository;
        this.dashboardLayoutAuthorityRepository = dashboardLayoutAuthorityRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
    }

    /** Eviction ciblée : seule l'entrée de l'utilisateur courant est invalidée. */
    private static final String CURRENT_USER_KEY =
        "T(com.kobe.warehouse.security.SecurityUtils).getCurrentUserLogin().orElse('')";

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public DashboardLayoutDTO save(DashboardLayoutDTO dto) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout layout = new DashboardLayout();
        layout.setName(dto.getName());
        layout.setDescription(dto.getDescription());
        layout.setUser(currentUser);
        layout.setScope(dto.getScope() != null ? dto.getScope() : DashboardScope.PRIVATE);
        layout.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        layout.setIsRoute(Boolean.TRUE.equals(dto.getIsRoute()));
        layout.setComponentKey(dto.getComponentKey() != null ?DashboardComponentKey.valueOf(dto.getComponentKey())  : DashboardComponentKey.ROUTE);
        layout.setLayoutConfig(dto.getLayoutConfig());

        if (Boolean.TRUE.equals(layout.getIsDefault())) {
            unsetOtherUserDefaults(currentUser);
        }

        return toDTO(dashboardLayoutRepository.save(layout));
    }

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public DashboardLayoutDTO update(DashboardLayoutDTO dto) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout layout = dashboardLayoutRepository
            .findById(dto.getId())
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        if (layout.getUser() != null && !layout.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to update this layout");
        }

        layout.setName(dto.getName());
        layout.setDescription(dto.getDescription());
        layout.setScope(dto.getScope());
        layout.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        layout.setIsRoute(Boolean.TRUE.equals(dto.getIsRoute()));
        layout.setComponentKey(dto.getComponentKey() != null ? DashboardComponentKey.valueOf(dto.getComponentKey()) : layout.getComponentKey());

        layout.setComponentKey(dto.getComponentKey() != null ?DashboardComponentKey.valueOf(dto.getComponentKey())  : DashboardComponentKey.ROUTE);
        layout.setLayoutConfig(dto.getLayoutConfig());

        if (Boolean.TRUE.equals(layout.getIsDefault()) && layout.getUser() != null) {
            unsetOtherUserDefaults(currentUser);
        }

        return toDTO(dashboardLayoutRepository.save(layout));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllForCurrentUser() {
        AppUser currentUser = getCurrentUser();
        return dashboardLayoutRepository.findByUserOrPublic(currentUser)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllPublic() {
        return dashboardLayoutRepository.findByScope(DashboardScope.PUBLIC)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardLayoutDTO> findOne(Integer id) {
        return dashboardLayoutRepository.findById(id).map(this::toDTO);
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
        return currentUser.getAuthorities().stream()
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

        DashboardLayout layout = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new GenericError("Tableau  de board inexistant"));

        if (layout.getUser() != null && !layout.getUser().getId().equals(currentUser.getId())) {
            throw new GenericError("Non autorisé à définir ce layout comme défaut");
        }

        unsetOtherUserDefaults(currentUser);
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
        authorityRepository.findById(authorityName)
            .orElseThrow(() -> new RuntimeException("Authority not found: " + authorityName));

        DashboardLayout layout = dashboardLayoutRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        // Retire is_default des autres entrées pour ce rôle
        dashboardLayoutAuthorityRepository.findByAuthorityName(authorityName).forEach(dla -> {
            if (Boolean.TRUE.equals(dla.getIsDefault())) {
                dla.setIsDefault(false);
                dashboardLayoutAuthorityRepository.save(dla);
            }
        });

        // Crée ou met à jour l'association pour ce layout + ce rôle
        DashboardLayoutAuthorityId assocId = new DashboardLayoutAuthorityId(layout.getId(), authorityName);
        DashboardLayoutAuthority association = dashboardLayoutAuthorityRepository.findById(assocId)
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

    @Override
    @CacheEvict(value = EntityConstant.DASHBOARD_LAYOUT_RESOLVED_CACHE, key = CURRENT_USER_KEY)
    public void delete(Integer id) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout layout = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        if (layout.getUser() != null && !layout.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to delete this layout");
        }

        dashboardLayoutRepository.delete(layout);
    }

    @Override
    public DashboardLayoutDTO clone(Integer id, String newName) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout original = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        DashboardLayout clone = new DashboardLayout();
        clone.setName(newName);
        clone.setDescription("Clone de : " + original.getName());
        clone.setUser(currentUser);
        clone.setScope(DashboardScope.PRIVATE);
        clone.setIsDefault(false);
        clone.setIsRoute(original.getIsRoute());
        clone.setLayoutConfig(original.getLayoutConfig());

        return toDTO(dashboardLayoutRepository.save(clone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardLayoutDTO> findAllForAuthority(String authorityName) {
        return dashboardLayoutAuthorityRepository.findByAuthorityName(authorityName)
            .stream()
            .map(dla -> toDTOWithIsDefault(dla.getLayout(), dla.getIsDefault()))
            .collect(Collectors.toList());
    }


    private AppUser getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("Current user login not found"));
        return userRepository.findOneByLogin(login)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void unsetOtherUserDefaults(AppUser user) {
        dashboardLayoutRepository.findByUserAndScope(user, DashboardScope.PRIVATE).forEach(layout -> {
            if (Boolean.TRUE.equals(layout.getIsDefault())) {
                layout.setIsDefault(false);
                dashboardLayoutRepository.save(layout);
            }
        });
    }

    private DashboardLayoutDTO toDTO(DashboardLayout layout) {
        List<String> authorityNames = dashboardLayoutAuthorityRepository.findByLayoutId(layout.getId())
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
            layout.getIsDefault(),
            layout.getIsRoute(),
            layout.getComponentKey().name(),
            layout.getLayoutConfig(),
            layout.getCreatedAt(),
            layout.getUpdatedAt()
        );
    }

    /**
     * Variante de toDTO utilisée lors de la résolution par rôle :
     * isDefault est tiré de l'association (DashboardLayoutAuthority), pas du layout lui-même.
     */
    private DashboardLayoutDTO toDTOWithIsDefault(DashboardLayout layout, Boolean isDefault) {
        List<String> authorityNames = dashboardLayoutAuthorityRepository.findByLayoutId(layout.getId())
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
            layout.getComponentKey().name(),
            layout.getLayoutConfig(),
            layout.getCreatedAt(),
            layout.getUpdatedAt()
        );
    }
}
