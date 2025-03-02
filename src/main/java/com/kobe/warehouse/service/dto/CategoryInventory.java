package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.InventoryCategory;

public class CategoryInventory {

    private InventoryCategory name;
    private String label;

    public CategoryInventory(InventoryCategory name) {
        this.name = name;
        this.label = switch (name) {
            case RAYON -> "Inventaire d'un rayon";
            case MAGASIN -> "Inventaire global";
            case STORAGE -> "Inventaire d'un emplacement";
            case FAMILLY -> "Inventaire d'une famille de produit";
        };
    }

    public InventoryCategory getName() {
        return name;
    }

    public CategoryInventory setName(InventoryCategory name) {
        this.name = name;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public CategoryInventory setLabel(String label) {
        this.label = label;
        return this;
    }
}
