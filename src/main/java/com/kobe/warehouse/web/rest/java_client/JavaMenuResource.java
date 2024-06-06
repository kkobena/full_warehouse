package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.service.dto.MenuDTO;
import com.kobe.warehouse.service.menu.MenuService;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing {@link Menu}. */
@RestController
@RequestMapping("/java-client")
@Transactional
public class JavaMenuResource {
  final MenuService menuService;

  public JavaMenuResource(MenuService menuService) {
    this.menuService = menuService;
  }

  /**
   * {@code GET /menus} : get all the menus.
   *
   * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of menus in body.
   */
  @GetMapping("/menus/users")
  public Set<MenuDTO> getConnectedUserMenus() {

    return menuService.getConnectedUserMenus();
  }
}
