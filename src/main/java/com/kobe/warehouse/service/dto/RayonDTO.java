package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.Storage;

import java.io.Serializable;

public class RayonDTO implements Serializable {
    private Long id;
    private String code;
    private String libelle;
    private Long storageId;
    private boolean exclude;
    private String storageLibelle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Long getStorageId() {
        return storageId;
    }

    public void setStorageId(Long storageId) {
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

    public RayonDTO() {
    }

    public RayonDTO(Rayon rayon) {
        this.id = rayon.getId();
        this.code = rayon.getCode();
        this.libelle = rayon.getLibelle();
        Storage storage = rayon.getStorage();
        this.storageId = storage.getId();
        this.storageLibelle = storage.getName();
        this.exclude = rayon.isExclude();

    }
}
