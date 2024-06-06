package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.repository.MenuRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

public class MenuResourceProxy {

  private static final String ENTITY_NAME = "menu";
  private final Logger log = LoggerFactory.getLogger(MenuResourceProxy.class);
  private final MenuRepository menuRepository;
  private final UserService userService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public MenuResourceProxy(MenuRepository menuRepository, UserService userService) {
    this.menuRepository = menuRepository;
    this.userService = userService;
  }

  public ResponseEntity<Menu> createMenu(Menu menu) throws URISyntaxException {
    log.debug("REST request to save Menu : {}", menu);
    if (menu.getId() != null) {
      throw new BadRequestAlertException(
          "A new menu cannot already have an ID", ENTITY_NAME, "idexists");
    }
    Menu result = menuRepository.save(menu);
    return ResponseEntity.created(new URI("/api/menus/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  public ResponseEntity<Menu> updateMenu(Menu menu) throws URISyntaxException {
    log.debug("REST request to update Menu : {}", menu);
    if (menu.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    Menu result = menuRepository.save(menu);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, menu.getId().toString()))
        .body(result);
  }

  public List<Menu> getAllMenus() {
    log.debug("REST request to get all Menus");
    return menuRepository.findAll();
  }

  public ResponseEntity<Menu> getMenu(Long id) {
    log.debug("REST request to get Menu : {}", id);
    Optional<Menu> menu = menuRepository.findById(id);
    return ResponseUtil.wrapOrNotFound(menu);
  }

  public ResponseEntity<Void> deleteMenu(Long id) {
    log.debug("REST request to delete Menu : {}", id);
    menuRepository.deleteById(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }

  public Set<Menu> getConnectedUserMenus() {
    log.debug("REST request to get all Menus");
    return userService.getUserWithAuthorities().get().getAuthorities().stream()
        .map(Authority::getMenus)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }
}
