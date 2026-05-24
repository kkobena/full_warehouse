package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.AuthorityDTO;

import java.util.List;

public interface AuthorityService {
    void save(AuthorityDTO authority);

    void delete(String name);

    List<AuthorityDTO> fetch(String search);

    AuthorityDTO fetchOne(String name);

    List<AuthorityDTO> fetchAll();
}
