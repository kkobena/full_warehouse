package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.DashboardLayout;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import com.kobe.warehouse.repository.DashboardLayoutRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.DashboardLayoutService;
import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing Dashboard Layouts
 */
@Service
@Transactional
public class DashboardLayoutServiceImpl implements DashboardLayoutService {

    private final DashboardLayoutRepository dashboardLayoutRepository;
    private final UserRepository userRepository;

    public DashboardLayoutServiceImpl(DashboardLayoutRepository dashboardLayoutRepository, UserRepository userRepository) {
        this.dashboardLayoutRepository = dashboardLayoutRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DashboardLayoutDTO save(DashboardLayoutDTO dashboardLayoutDTO) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout dashboardLayout = new DashboardLayout();
        dashboardLayout.setName(dashboardLayoutDTO.getName());
        dashboardLayout.setDescription(dashboardLayoutDTO.getDescription());
        dashboardLayout.setUser(currentUser);
        dashboardLayout.setScope(dashboardLayoutDTO.getScope() != null ? dashboardLayoutDTO.getScope() : DashboardScope.PRIVATE);
        dashboardLayout.setIsDefault(dashboardLayoutDTO.getIsDefault() != null ? dashboardLayoutDTO.getIsDefault() : false);
        dashboardLayout.setLayoutConfig(dashboardLayoutDTO.getLayoutConfig());

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(dashboardLayout.getIsDefault())) {
            unsetOtherDefaults(currentUser);
        }

        dashboardLayout = dashboardLayoutRepository.save(dashboardLayout);
        return toDTO(dashboardLayout);
    }

    @Override
    public DashboardLayoutDTO update(DashboardLayoutDTO dashboardLayoutDTO) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout dashboardLayout = dashboardLayoutRepository
            .findById(dashboardLayoutDTO.getId())
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        // Security check: only owner can update
        if (!dashboardLayout.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to update this layout");
        }

        dashboardLayout.setName(dashboardLayoutDTO.getName());
        dashboardLayout.setDescription(dashboardLayoutDTO.getDescription());
        dashboardLayout.setScope(dashboardLayoutDTO.getScope());
        dashboardLayout.setIsDefault(dashboardLayoutDTO.getIsDefault());
        dashboardLayout.setLayoutConfig(dashboardLayoutDTO.getLayoutConfig());

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(dashboardLayout.getIsDefault())) {
            unsetOtherDefaults(currentUser);
        }

        dashboardLayout = dashboardLayoutRepository.save(dashboardLayout);
        return toDTO(dashboardLayout);
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
        return dashboardLayoutRepository.findByScope(DashboardScope.PUBLIC).stream().map(this::toDTO).collect(Collectors.toList());
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

    @Override
    public DashboardLayoutDTO setAsDefault(Integer id) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout dashboardLayout = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        // Security check
        if (!dashboardLayout.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to set this layout as default");
        }

        // Unset other defaults
        unsetOtherDefaults(currentUser);

        // Set as default
        dashboardLayout.setIsDefault(true);
        dashboardLayout = dashboardLayoutRepository.save(dashboardLayout);

        return toDTO(dashboardLayout);
    }

    @Override
    public void delete(Integer id) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout dashboardLayout = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        // Security check
        if (!dashboardLayout.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized to delete this layout");
        }

        dashboardLayoutRepository.delete(dashboardLayout);
    }

    @Override
    public DashboardLayoutDTO clone(Integer id, String newName) {
        AppUser currentUser = getCurrentUser();

        DashboardLayout original = dashboardLayoutRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Dashboard layout not found"));

        DashboardLayout clone = new DashboardLayout();
        clone.setName(newName);
        clone.setDescription("Clone of: " + original.getName());
        clone.setUser(currentUser);
        clone.setScope(DashboardScope.PRIVATE);
        clone.setIsDefault(false);
        clone.setLayoutConfig(original.getLayoutConfig());

        clone = dashboardLayoutRepository.save(clone);
        return toDTO(clone);
    }

    // Helper methods

    private AppUser getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("Current user login not found"));
        return userRepository.findOneByLogin(login).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void unsetOtherDefaults(AppUser user) {
        List<DashboardLayout> currentDefaults = dashboardLayoutRepository.findByUserAndScope(user, DashboardScope.PRIVATE);
        currentDefaults.forEach(layout -> {
            if (Boolean.TRUE.equals(layout.getIsDefault())) {
                layout.setIsDefault(false);
                dashboardLayoutRepository.save(layout);
            }
        });
    }

    private DashboardLayoutDTO toDTO(DashboardLayout dashboardLayout) {
        return new DashboardLayoutDTO(
            dashboardLayout.getId(),
            dashboardLayout.getName(),
            dashboardLayout.getDescription(),
            dashboardLayout.getUser() != null ? dashboardLayout.getUser().getId() : null,
            dashboardLayout.getUser() != null ? dashboardLayout.getUser().getLogin() : null,
            dashboardLayout.getScope(),
            dashboardLayout.getIsDefault(),
            dashboardLayout.getLayoutConfig(),
            dashboardLayout.getCreatedAt(),
            dashboardLayout.getUpdatedAt()
        );
    }
}
