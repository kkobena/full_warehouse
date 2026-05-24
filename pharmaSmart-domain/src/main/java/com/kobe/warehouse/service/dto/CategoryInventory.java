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
            case PERIME -> "Inventaire des produits périmés";
            case ALERTE_PEREMPTION -> "Inventaire des produits en alerte péremption";
            case VENDU -> "Inventaire des produits vendus";
            case INVENDU -> "Inventaire des produits invendus";
            case SOUS_SEUIL -> "Inventaire des produits sous seuil";
            case EN_RUPTURE -> "Inventaire des produits en rupture";
            case SELECTION_PRODUIT -> "Inventaire des produits ";
            case GROSSISTE -> "Inventaire d'un grossite";
            case ABC -> "Inventaire des produits de classe A/B/C (Pareto)";
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
