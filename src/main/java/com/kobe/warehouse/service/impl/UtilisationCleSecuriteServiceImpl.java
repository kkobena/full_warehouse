package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.UtilisationCleSecurite;
import com.kobe.warehouse.repository.AuthorityPrivilegeRepository;
import com.kobe.warehouse.repository.PrivilegeRepository;
import com.kobe.warehouse.repository.UtilisationCleSecuriteRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.PrivilegeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisationCleSecuriteServiceImpl implements UtilisationCleSecuriteService {

    private final AuthorityPrivilegeRepository authorityPrivilegeRepository;
    private final UtilisationCleSecuriteRepository utilisationCleSecuriteRepository;
    private final PrivilegeRepository privilegeRepository;
    private final UserService userService;

    // private Function<String, String> passwordEncoder = (password) -> password;
    public UtilisationCleSecuriteServiceImpl(
        AuthorityPrivilegeRepository authorityPrivilegeRepository,
        UtilisationCleSecuriteRepository utilisationCleSecuriteRepository,
        PrivilegeRepository privilegeRepository,
        UserService userService
    ) {
        this.authorityPrivilegeRepository = authorityPrivilegeRepository;
        this.utilisationCleSecuriteRepository = utilisationCleSecuriteRepository;
        this.privilegeRepository = privilegeRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPrivilege(String privilegeName, String AuthorityName) {
        return this.authorityPrivilegeRepository.existsByPrivilegeNameAndAuthorityName(privilegeName, AuthorityName);
    }

    @Override
    public void save(User owner, UtilisationCleSecuriteDTO utilisationCleSecuriteDTO) {
        User connectedUser = this.userService.getUser();
        UtilisationCleSecurite utilisationCleSecurite = new UtilisationCleSecurite()
            .setCleSecuriteOwner(owner)
            .setConnectedUser(connectedUser)
            .setCaisse(utilisationCleSecuriteDTO.getCaisse())
            .setPrivilege(this.privilegeRepository.getReferenceById(utilisationCleSecuriteDTO.getPrivilege()))
            .setEntityId(utilisationCleSecuriteDTO.getEntityId())
            .setCommentaire(utilisationCleSecuriteDTO.getCommentaire())
            .setEntityName(utilisationCleSecuriteDTO.getEntityName());
        this.utilisationCleSecuriteRepository.save(utilisationCleSecurite);
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
