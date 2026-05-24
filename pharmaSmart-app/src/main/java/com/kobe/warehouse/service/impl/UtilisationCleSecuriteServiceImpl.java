package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.UtilisationCleSecurite;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.UtilisationCleSecuriteRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.PrivilegeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisationCleSecuriteServiceImpl implements UtilisationCleSecuriteService {

    private final NavItemRepository navItemRepository;
    private final UtilisationCleSecuriteRepository utilisationCleSecuriteRepository;

    private final UserService userService;
    private final LogsService logService;

    // private Function<String, String> passwordEncoder = (password) -> password;
    public UtilisationCleSecuriteServiceImpl(
        UtilisationCleSecuriteRepository utilisationCleSecuriteRepository,
         NavItemRepository navItemRepository,
        UserService userService,
        LogsService logService
    ) {
        this.navItemRepository = navItemRepository;
        this.utilisationCleSecuriteRepository = utilisationCleSecuriteRepository;
        this.userService = userService;
        this.logService = logService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPrivilege(String privilegeName, String AuthorityName) {
        return this.navItemRepository.existByCodeAndRoleName(privilegeName, AuthorityName);
    }

    @Override
    public void save(AppUser owner, UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) {
        AppUser connectedUser = this.userService.getUser();
        UtilisationCleSecurite utilisationCleSecurite = new UtilisationCleSecurite()
            .setCleSecuriteOwner(owner)
            .setConnectedUser(connectedUser)
            .setCaisse(utilisationCleSecuriteDTO.getCaisse())
            .setNavItem(this.navItemRepository.findByCode(utilisationCleSecuriteDTO.getPrivilege()))
            .setEntityId(utilisationCleSecuriteDTO.getEntityId())
            .setCommentaire(utilisationCleSecuriteDTO.getCommentaire())
            .setEntityName(utilisationCleSecuriteDTO.getEntityName());
        utilisationCleSecurite = this.utilisationCleSecuriteRepository.save(utilisationCleSecurite);
        this.logService.create(
                TransactionType.ACTIVATION_PRIVILEGE,
                utilisationCleSecuriteDTO.getCommentaire(),
                utilisationCleSecurite.getId().toString()
            );
    }

    @Override
    public void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO, Object action) throws PrivilegeException {
        this.userService.getUserByPwdOrSecurityKey(utilisationCleSecuriteDTO.getActionAuthorityKey()).ifPresentOrElse(
                user -> {
                    if (
                        user
                            .getAuthorities()
                            .stream()
                            .anyMatch(role -> this.hasPrivilege(utilisationCleSecuriteDTO.getPrivilege(), role.getName()))
                    ) {
                        utilisationCleSecuriteDTO.setEntityName(action.getClass().getName());
                        this.save(user, utilisationCleSecuriteDTO);
                    } else {
                        throw new PrivilegeException();
                    }
                },
                () -> {
                    throw new PrivilegeException();
                }
            );
    }
}
