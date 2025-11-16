package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Menu;
import java.util.HashSet;
import java.util.Set;

public class MenuDTO {

    private final Set<ActionPrivilegeDTO> privileges = new HashSet<>();
    private Set<MenuDTO> menus = new HashSet<>();

    private String libelle;
    private String name;
    private boolean root;
    private boolean enable;

    public MenuDTO() {}

    public MenuDTO(Menu menu) {
        this.libelle = menu.getLibelle();
        this.name = menu.getName();
        this.root = menu.isRoot();
        this.enable = menu.isEnable();

        menus.addAll(menu.getMenus().stream().map(MenuDTO::new).toList());
    }

    public Set<ActionPrivilegeDTO> getPrivileges() {
        return privileges;
    }

    public Set<MenuDTO> getMenus() {
        return menus;
    }

    public void setMenus(Set<MenuDTO> menus) {
        this.menus = menus;
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
}
