package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.TypeMenu;

public interface MenuSpecialisation {
    Long getId();

    Long getParent();

    TypeMenu getTypeMenu();

    String getLibelle();

    String getName();

    boolean isRoot();

    String getIconWeb();

    String getIconJavaClient();
}
