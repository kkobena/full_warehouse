package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.PrivilegeException;

public interface UtilisationCleSecuriteService {
    boolean hasPrivilege(String privilegeName, String AuthorityName);

    void save(User owner, UtilisationCleSecuriteDTO utilisationCleSecuriteDTO);

    void authorizeAction(UtilisationCleSecuriteDTO utilisationCleSecuriteDTO, Object action) throws PrivilegeException;
}
