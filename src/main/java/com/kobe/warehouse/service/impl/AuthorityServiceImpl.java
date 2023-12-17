package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.MenuRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthorityServiceImpl implements AuthorityService {
  final BiPredicate<Menu, String> privilegesSearchPredicate =
      (menu, s) ->
          org.apache.commons.lang3.StringUtils.containsIgnoreCase(menu.getName(), s)
              || org.apache.commons.lang3.StringUtils.containsIgnoreCase(menu.getLibelle(), s);
  private final AuthorityRepository authorityRepository;
  private final MenuRepository menuRepository;

  public AuthorityServiceImpl(
      AuthorityRepository authorityRepository, MenuRepository menuRepository) {
    this.authorityRepository = authorityRepository;
    this.menuRepository = menuRepository;
  }

  @Override
  public void save(AuthorityDTO authorityDTO) {
    Authority authority = new Authority();
    authority.setName(authorityDTO.name());
    authority.setLibelle(authorityDTO.libelle());
    if (CollectionUtils.isEmpty(authorityDTO.privilleges())) {
      authority.setMenus(Collections.emptySet());
    } else {
      authority.setMenus(
          authorityDTO.privilleges().stream()
              .map(this.menuRepository::findMenuByName)
              .flatMap(Optional::stream)
              .collect(Collectors.toSet()));
    }
    this.authorityRepository.save(authority);
  }

  @Override
  public void setPrivilleges(AuthorityDTO authorityDto) {
    Authority authority = this.authorityRepository.getReferenceById(authorityDto.name());
    authority.setMenus(
        authorityDto.privilleges().stream()
            .map(this.menuRepository::findMenuByName)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet()));
    this.authorityRepository.save(authority);
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
      return this.authorityRepository.findAll().stream()
          .filter(authority -> authority.getName().contains(search.toUpperCase()))
          .map(this::buildAutorityDTO)
          .toList();
    }

    return this.authorityRepository.findAll().stream().map(this::buildAutorityDTO).toList();
  }

  @Override
  public AuthorityDTO fetchOne(String name) {
    return Optional.ofNullable(authorityRepository.findOneByName(name))
        .map(this::buildAutorityDTO)
        .orElseThrow();
  }

  @Override
  public PrivillegesWrapperDTO fetchPrivillegesByRole(String roleName) {

    List<PrivillegesDTO> privilleges =
        this.authorityRepository.findOneByName(roleName).getMenus().stream()
            .filter(menu -> menu.isEnable())
            .map(this::buildAllPrivilleges)
            .toList();
    List<PrivillegesDTO> privillegesAll = new ArrayList<>();
    List<Menu> autres = menuRepository.findAll();
    for (Menu menu : autres) {
      boolean isExist = false;
      for (PrivillegesDTO p : privilleges) {
        if (menu.getId().intValue() == p.id()) {
          isExist = true;
          break;
        }
      }
      if (!isExist) {
        privillegesAll.add(buildAllPrivilleges(menu));
      }
    }
    return new PrivillegesWrapperDTO(privilleges, privillegesAll);
  }

  @Override
  public List<PrivillegesDTO> fetchPrivilleges(String search) {
    if (StringUtils.hasLength(search)) {
      List<PrivillegesDTO> privilleges = new ArrayList<>();
      Set<Menu> menus =
          this.menuRepository.findAll().stream()
              .filter(menu -> privilegesSearchPredicate.test(menu, search))
              .collect(Collectors.toSet());
      Map<Boolean, List<Menu>> map =
          menus.stream()
              .collect(Collectors.partitioningBy(menu -> Objects.isNull(menu.getParent())));
      List<Menu> parents = map.remove(true);
      if (CollectionUtils.isEmpty(parents)) {
        List<Menu> items = map.remove(false);
        if (!CollectionUtils.isEmpty(items)) {
          items.stream()
              .collect(Collectors.groupingBy(Menu::getParent))
              .forEach(((menu, menus1) -> privilleges.add(buildPrivilleges(menu, menus1))));
        }
      } else {
        List<Menu> items = map.remove(false);
        if (!CollectionUtils.isEmpty(items)) {
          Map<Menu, List<Menu>> parentItemsMap =
              items.stream().collect(Collectors.groupingBy(Menu::getParent));
          for (Menu menu : parents) {
            List<Menu> menus1 = parentItemsMap.remove(menu);
            privilleges.add(
                buildPrivilleges(
                    menu, Optional.ofNullable(menus1).orElse(Collections.emptyList())));
          }
        } else {
          for (Menu menu : parents) {
            privilleges.add(buildPrivilleges(menu, Collections.emptyList()));
          }
        }
      }
      return privilleges;
    } else {
      return this.menuRepository.findAll().stream()
          .filter(menu -> Objects.nonNull(menu.getParent()))
          .map(this::buildPrivilleges)
          .toList();
    }
  }

  private AuthorityDTO buildAutorityDTO(Authority authority) {
    return new AuthorityDTO(
        authority.getName(),
        authority.getLibelle(),
        authority.getMenus().stream().map(Menu::getName).collect(Collectors.toSet()));
  }

  private PrivillegesDTO buildPrivilleges(Menu menu, Long parentId) {
    return new PrivillegesDTO(
        menu.getId().intValue(),
        menu.getName(),
        menu.getLibelle(),
        menu.isRoot(),
        parentId.intValue(),
        menu.isEnable(),
        Collections.emptySet());
  }

  private PrivillegesDTO buildPrivilleges(Menu menu) {
    return new PrivillegesDTO(
        menu.getId().intValue(),
        menu.getName(),
        menu.getLibelle(),
        menu.isRoot(),
        null,
        menu.isEnable(),
        menu.getMenus().stream()
            .map(it -> buildPrivilleges(it, menu.getId()))
            .collect(Collectors.toSet()));
  }

  private PrivillegesDTO buildPrivilleges(Menu menu, List<Menu> menus) {
    return new PrivillegesDTO(
        menu.getId().intValue(),
        menu.getName(),
        menu.getLibelle(),
        menu.isRoot(),
        null,
        menu.isEnable(),
        menus.stream().map(it -> buildPrivilleges(it, menu.getId())).collect(Collectors.toSet()));
  }

  private PrivillegesDTO buildAllPrivilleges(Menu menu) {
    return new PrivillegesDTO(
        menu.getId().intValue(),
        menu.getName(),
        menu.getLibelle(),
        menu.isRoot(),
        null,
        menu.isEnable(),
        Collections.emptySet());
  }
}
