package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.service.AuthorityService;
import com.kobe.warehouse.service.dto.AuthorityDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorityServiceImpl implements AuthorityService {


    private final AuthorityRepository authorityRepository;

    public AuthorityServiceImpl(
        AuthorityRepository authorityRepository
    ) {
        this.authorityRepository = authorityRepository;
    }

    @Override
    public void save(AuthorityDTO authorityDTO) {
        Authority authority = new Authority();
        String roleName = authorityDTO.name();
        if (!StringUtils.startsWithIgnoreCase(roleName, "ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        authority.setName(roleName.toUpperCase());
        authority.setLibelle(authorityDTO.libelle());
        this.authorityRepository.save(authority);
    }


    @Override
    public void delete(String name) {
        Authority authority = this.authorityRepository.findOneByName(name);
        this.authorityRepository.delete(this.authorityRepository.saveAndFlush(authority));
    }

    @Override
    public List<AuthorityDTO> fetch(String search) {
        Set<String> roleToExclude = Set.of("ROLE_SUPER_USER", "ROLE_ADMIN", "ROLE_USER");
        if (StringUtils.hasLength(search)) {
            return this.authorityRepository.findAll()
                .stream()
                .filter(authority -> !roleToExclude.contains(authority.getName()))
                .filter(authority -> authority.getName().contains(search.toUpperCase()))
                .map(this::buildAutorityDTO)
                .toList();
        }

        return this.authorityRepository.findAll().stream().filter(authority -> !authority.getName().equals("ROLE_SUPER_USER")).map(this::buildAutorityDTO).toList();
    }

    @Override
    public AuthorityDTO fetchOne(String name) {
        return Optional.ofNullable(authorityRepository.findOneByName(name)).map(this::buildAutorityDTO).orElseThrow();
    }


    @Override
    public List<AuthorityDTO> fetchAll() {
        Set<String> roleToExclude = Set.of("ROLE_SUPER_USER", "ROLE_ADMIN", "ROLE_USER");
        return authorityRepository.findNameAndLibelle()
            .stream()
            .filter(authority -> !roleToExclude.contains(authority.code()))
            .map(authority -> new AuthorityDTO(authority.code(), authority.libelle(), Set.of()))
            .collect(Collectors.toList());

    }

    private AuthorityDTO buildAutorityDTO(Authority authority) {
        return new AuthorityDTO(
            authority.getName(),
            authority.getLibelle(), Set.of());
    }


}
