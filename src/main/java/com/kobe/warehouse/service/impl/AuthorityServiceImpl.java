package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.AuthorityPrivilege;
import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.domain.Privilege;
import com.kobe.warehouse.repository.AuthorityPrivilegeRepository;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.MenuRepository;
import com.kobe.warehouse.repository.PrivilegeRepository;
import com.kobe.warehouse.service.AuthorityService;
import com.kobe.warehouse.service.dto.AuthorityDTO;
import com.kobe.warehouse.service.dto.PrivillegesDTO;
import com.kobe.warehouse.service.dto.PrivillegesWrapperDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthorityServiceImpl implements AuthorityService {

    final BiPredicate<Menu, String> privilegesSearchPredicate = (menu, s) ->
        Strings.CI.contains(menu.getName(), s) ||
            Strings.CI.contains(menu.getLibelle(), s);
    private final AuthorityRepository authorityRepository;
    private final MenuRepository menuRepository;
    private final PrivilegeRepository privilegeRepository;
    private final AuthorityPrivilegeRepository authorityPrivilegeRepository;

    public AuthorityServiceImpl(
        AuthorityRepository authorityRepository,
        MenuRepository menuRepository,
        PrivilegeRepository privilegeRepository,
        AuthorityPrivilegeRepository authorityPrivilegeRepository
    ) {
        this.authorityRepository = authorityRepository;
        this.menuRepository = menuRepository;
        this.privilegeRepository = privilegeRepository;
        this.authorityPrivilegeRepository = authorityPrivilegeRepository;
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
        if (CollectionUtils.isEmpty(authorityDTO.privilleges())) {
            authority.setMenus(Collections.emptySet());
            authority.setPrivileges(Collections.emptySet());
        } else {
            authority.setMenus(
                authorityDTO
                    .privilleges()
                    .stream()
                    .map(this.menuRepository::findMenuByName)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet())
            );
        }
        this.authorityRepository.save(authority);
    }

    @Override
    public void setPrivilleges(AuthorityDTO authorityDto) {
        Authority authority = this.authorityRepository.getReferenceById(authorityDto.name());
        authority.setMenus(
            authorityDto
                .privilleges()
                .stream()
                .filter(s -> !StringUtils.startsWithIgnoreCase(s, "PR_"))
                .map(this.menuRepository::findMenuByName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet())
        );
        saveActionPrivilege(
            authority,
            authorityDto.privilleges().stream().filter(s -> StringUtils.startsWithIgnoreCase(s, "PR_")).collect(Collectors.toSet())
        );
        this.authorityRepository.save(authority);
    }

    private void saveActionPrivilege(String actionName, Authority authority) {
        if (this.authorityPrivilegeRepository.findOneByPrivilegeNameAndAuthorityName(actionName, authority.getName()).isEmpty()) {
            Privilege privilege = this.privilegeRepository.findById(actionName).orElseThrow();
            AuthorityPrivilege authorityPrivilege = new AuthorityPrivilege().setAuthority(authority).setPrivilege(privilege);
            this.authorityPrivilegeRepository.save(authorityPrivilege);
        }
    }

    private void saveActionPrivilege(Authority authority, Set<String> actions) {
        List<AuthorityPrivilege> authorityPrivileges =
            this.authorityPrivilegeRepository.findAllAuthorityPrivilegeByAuthorityName(authority.getName());
        if (!CollectionUtils.isEmpty(authorityPrivileges)) {
            this.authorityPrivilegeRepository.deleteAll(
                    authorityPrivileges
                        .stream()
                        .filter(authorityPrivilege -> !actions.contains(authorityPrivilege.getPrivilege().getName()))
                        .collect(Collectors.toSet())
                );
        }
        actions.forEach(actionName -> saveActionPrivilege(actionName, authority));
    }

    @Override
    public void delete(String name) {
        Authority authority = this.authorityRepository.findOneByName(name);
        authority.setMenus(Collections.emptySet());
        this.authorityRepository.delete(this.authorityRepository.saveAndFlush(authority));
    }

    @Override
    public List<AuthorityDTO> fetch(String search) {
        if (StringUtils.hasLength(search)) {
            return this.authorityRepository.findAll()
                .stream()
                .filter(authority -> authority.getName().contains(search.toUpperCase()))
                .map(this::buildAutorityDTO)
                .toList();
        }

        return this.authorityRepository.findAll().stream().map(this::buildAutorityDTO).toList();
    }

    @Override
    public AuthorityDTO fetchOne(String name) {
        return Optional.ofNullable(authorityRepository.findOneByName(name)).map(this::buildAutorityDTO).orElseThrow();
    }

    @Override
    public PrivillegesWrapperDTO fetchPrivillegesByRole(String roleName) {
        List<PrivillegesDTO> privilleges = new ArrayList<>(buildFromAction(roleName));
        privilleges.addAll(
            this.authorityRepository.findOneByName(roleName)
                .getMenus()
                .stream()
                .filter(Menu::isEnable)
                .map(this::buildAllPrivilleges)
                .toList()
        );
        List<PrivillegesDTO> privillegesAll = new ArrayList<>();
        List<PrivillegesDTO> privillegesMenus = privilleges.stream().filter(PrivillegesDTO::isMenu).toList();
        List<PrivillegesDTO> privillegesActions = privilleges.stream().filter(e -> !e.isMenu()).toList();
        List<Menu> autres = menuRepository.findAll();
        for (Menu menu : autres) {
            boolean isExist = false;
            for (PrivillegesDTO p : privillegesMenus) {
                if (menu.getId() == Long.parseLong(p.id() + "")) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                privillegesAll.add(buildAllPrivilleges(menu));
            }
        }

        List<Privilege> allActions = privilegeRepository.findAll();
        for (Privilege action : allActions) {
            boolean isExist = false;
            for (PrivillegesDTO p : privillegesActions) {
                if (action.getName().equals(p.name())) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                privillegesAll.add(buildFromAction(action));
            }
        }
        return new PrivillegesWrapperDTO(privilleges, privillegesAll);
    }

    @Override
    public List<PrivillegesDTO> fetchPrivilleges(String search) {
        List<PrivillegesDTO> privilleges = new ArrayList<>(buildActionsPrivilleges(search));
        if (StringUtils.hasLength(search)) {
            Set<Menu> menus =
                this.menuRepository.findAll()
                    .stream()
                    .filter(menu -> privilegesSearchPredicate.test(menu, search))
                    .collect(Collectors.toSet());
            Map<Boolean, List<Menu>> map = menus.stream().collect(Collectors.partitioningBy(menu -> Objects.isNull(menu.getParent())));
            List<Menu> parents = map.remove(true);
            if (CollectionUtils.isEmpty(parents)) {
                List<Menu> items = map.remove(false);
                if (!CollectionUtils.isEmpty(items)) {
                    items
                        .stream()
                        .collect(Collectors.groupingBy(Menu::getParent))
                        .forEach(((menu, menus1) -> privilleges.add(buildPrivilleges(menu, menus1))));
                }
            } else {
                List<Menu> items = map.remove(false);
                if (!CollectionUtils.isEmpty(items)) {
                    Map<Menu, List<Menu>> parentItemsMap = items.stream().collect(Collectors.groupingBy(Menu::getParent));
                    for (Menu menu : parents) {
                        List<Menu> menus1 = parentItemsMap.remove(menu);
                        privilleges.add(buildPrivilleges(menu, Optional.ofNullable(menus1).orElse(Collections.emptyList())));
                    }
                } else {
                    for (Menu menu : parents) {
                        privilleges.add(buildPrivilleges(menu, Collections.emptyList()));
                    }
                }
            }
        } else {
            privilleges.addAll(
                this.menuRepository.findAll()
                    .stream()
                    .filter(menu -> Objects.nonNull(menu.getParent()))
                    .map(this::buildPrivilleges)
                    .toList()
            );
        }
        return privilleges;
    }

    @Override
    public boolean hasAuthority(String authorityName, String privillegeName) {
        return authorityPrivilegeRepository.existsByPrivilegeNameAndAuthorityName(privillegeName, authorityName);
    }

    private AuthorityDTO buildAutorityDTO(Authority authority) {
        return new AuthorityDTO(
            authority.getName(),
            authority.getLibelle(),
            authority.getMenus().stream().map(Menu::getName).collect(Collectors.toSet())
        );
    }

    private PrivillegesDTO buildPrivilleges(Menu menu, Long parentId) {
        return new PrivillegesDTO(
            menu.getId(),
            menu.getName(),
            menu.getLibelle(),
            menu.isRoot(),
            parentId.intValue(),
            menu.isEnable(),
            Collections.emptySet(),
            true
        );
    }

    private PrivillegesDTO buildPrivilleges(Menu menu) {
        return new PrivillegesDTO(
            menu.getId(),
            menu.getName(),
            menu.getLibelle(),
            menu.isRoot(),
            null,
            menu.isEnable(),
            menu.getMenus().stream().map(it -> buildPrivilleges(it, menu.getId())).collect(Collectors.toSet()),
            true
        );
    }

    private PrivillegesDTO buildPrivilleges(Menu menu, List<Menu> menus) {
        return new PrivillegesDTO(
            menu.getId(),
            menu.getName(),
            menu.getLibelle(),
            menu.isRoot(),
            null,
            menu.isEnable(),
            menus.stream().map(it -> buildPrivilleges(it, menu.getId())).collect(Collectors.toSet()),
            true
        );
    }

    private PrivillegesDTO buildAllPrivilleges(Menu menu) {
        return new PrivillegesDTO(
            menu.getId(),
            menu.getName(),
            menu.getLibelle(),
            menu.isRoot(),
            null,
            menu.isEnable(),
            Collections.emptySet(),
            true
        );
    }

    private List<PrivillegesDTO> buildActionsPrivilleges(String search) {
        if (!StringUtils.hasLength(search)) {
            return this.privilegeRepository.findAllPrivilege()
                .stream()
                .map(actionPrivilegeDTO ->
                    new PrivillegesDTO(
                        actionPrivilegeDTO.getName(),
                        actionPrivilegeDTO.getName(),
                        actionPrivilegeDTO.getLibelle(),
                        false,
                        null,
                        true,
                        Collections.emptySet(),
                        false
                    )
                )
                .toList();
        }
        return this.privilegeRepository.findAllPrivilege()
            .stream()
            .filter(privilege -> privilege.getName().contains(search.toUpperCase()))
            .map(actionPrivilegeDTO ->
                new PrivillegesDTO(
                    actionPrivilegeDTO.getName(),
                    actionPrivilegeDTO.getName(),
                    actionPrivilegeDTO.getLibelle(),
                    false,
                    null,
                    true,
                    Collections.emptySet(),
                    false
                )
            )
            .toList();
    }

    private List<PrivillegesDTO> buildFromAction(String roleName) {
        return this.authorityPrivilegeRepository.findAllAuthorityName(roleName)
            .stream()
            .map(authorityPrivilege ->
                new PrivillegesDTO(
                    authorityPrivilege.getName(),
                    authorityPrivilege.getName(),
                    authorityPrivilege.getLibelle(),
                    false,
                    null,
                    true,
                    Collections.emptySet(),
                    false
                )
            )
            .toList();
    }

    private PrivillegesDTO buildFromAction(Privilege privilege) {
        return new PrivillegesDTO(
            privilege.getName(),
            privilege.getName(),
            privilege.getLibelle(),
            false,
            null,
            true,
            Collections.emptySet(),
            false
        );
    }
}
