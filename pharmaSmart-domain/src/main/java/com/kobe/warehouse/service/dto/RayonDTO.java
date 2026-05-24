package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.TypeZone;
import java.io.Serializable;

public class RayonDTO implements Serializable {

    private Integer id;
    private String code;
    private String libelle;
    private Integer storageId;
    private boolean exclude;
    private String storageLibelle;
    private String storageType;
    private long inventoryId;
    private TypeZone typeZone;
    private String position;

    public RayonDTO() {}

    public RayonDTO(Rayon rayon) {
        this.id = rayon.getId();
        this.code = rayon.getCode();
        this.libelle = rayon.getLibelle();
        Storage storage = rayon.getStorage();
        this.storageType = storage.getStorageType().getValue();
        this.storageId = storage.getId();
        this.storageLibelle = storage.getName();
        this.exclude = rayon.isExclude();
        this.typeZone = rayon.getTypeZone();
        this.position = rayon.getPosition();
    }

    public long getInventoryId() {
        return inventoryId;
    }

    public RayonDTO setInventoryId(long inventoryId) {
        this.inventoryId = inventoryId;
        return this;
    }

    public String getStorageType() {
        return storageType;
    }

    public RayonDTO setStorageType(String storageType) {
        this.storageType = storageType;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Integer getStorageId() {
        return storageId;
    }

    public void setStorageId(Integer storageId) {
        this.storageId = storageId;
    }

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public String getStorageLibelle() {
        return storageLibelle;
    }

    public void setStorageLibelle(String storageLibelle) {
        this.storageLibelle = storageLibelle;
    }

    public TypeZone getTypeZone() {
        return typeZone;
    }

    public RayonDTO setTypeZone(TypeZone typeZone) {
        this.typeZone = typeZone;
        return this;
    }

    public String getPosition() {
        return position;
    }

    public RayonDTO setPosition(String position) {
        this.position = position;
        return this;
    }
}
