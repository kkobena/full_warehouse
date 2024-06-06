package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Menu;
import com.kobe.warehouse.domain.enumeration.TypeMenu;
import java.util.HashSet;
import java.util.Set;

public class MenuDTO {
  private Set<MenuDTO> menus = new HashSet<>();
  private TypeMenu typeMenu;
  private String libelle;
  private String name;
  private boolean root;
  private boolean enable;
  private String iconWeb;
  private String iconJavaClient;

  public MenuDTO() {}

  public MenuDTO(Menu menu) {
    this.typeMenu = menu.getTypeMenu();
    this.libelle = menu.getLibelle();
    this.name = menu.getName();
    this.root = menu.isRoot();
    this.enable = menu.isEnable();
    this.iconWeb = menu.getIconWeb();
    this.iconJavaClient = menu.getIconJavaClient();
    menus.addAll(menu.getMenus().stream().map(MenuDTO::new).toList());
  }

  public Set<MenuDTO> getMenus() {
    return menus;
  }

  public void setMenus(Set<MenuDTO> menus) {
    this.menus = menus;
  }

  public TypeMenu getTypeMenu() {
    return typeMenu;
  }

  public void setTypeMenu(TypeMenu typeMenu) {
    this.typeMenu = typeMenu;
  }

  public String getLibelle() {
    return libelle;
  }

  public void setLibelle(String libelle) {
    this.libelle = libelle;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isRoot() {
    return root;
  }

  public void setRoot(boolean root) {
    this.root = root;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public String getIconWeb() {
    return iconWeb;
  }

  public void setIconWeb(String iconWeb) {
    this.iconWeb = iconWeb;
  }

  public String getIconJavaClient() {
    return iconJavaClient;
  }

  public void setIconJavaClient(String iconJavaClient) {
    this.iconJavaClient = iconJavaClient;
  }

  @Override
  public String toString() {
    return "MenuDTO{"
        + "menus="
        + menus
        + ", typeMenu="
        + typeMenu
        + ", libelle='"
        + libelle
        + '\''
        + ", name='"
        + name
        + '\''
        + ", root="
        + root
        + ", enable="
        + enable
        + ", iconWeb='"
        + iconWeb
        + '\''
        + ", iconJavaClient='"
        + iconJavaClient
        + '\''
        + '}';
  }
}
