package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AjustDTO {

    private LocalDateTime dateMtv;
    private Long id;
    private Long storageId;
    private String commentaire;
    private String storageLibelle;
    private Long userId;
    private String userFullName;
    private List<AjustementDTO> ajustements = new ArrayList<>();

    public AjustDTO() {}

    public AjustDTO(Ajust ajust) {
        this.dateMtv = ajust.getDateMtv();
        this.id = ajust.getId();
        Storage storage = ajust.getStorage();
        this.commentaire = ajust.getCommentaire();
        if (storage != null) {
            this.storageId = storage.getId();
            this.storageLibelle = storage.getName();
        }
        User user = ajust.getUser();
        this.userId = user.getId();
        this.userFullName = user.getFirstName().concat(" ") + user.getLastName();
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public AjustDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Long getId() {
        return id;
    }

    public AjustDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getStorageId() {
        return storageId;
    }

    public AjustDTO setStorageId(Long storageId) {
        this.storageId = storageId;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public AjustDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public String getStorageLibelle() {
        return storageLibelle;
    }

    public AjustDTO setStorageLibelle(String storageLibelle) {
        this.storageLibelle = storageLibelle;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public AjustDTO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public AjustDTO setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public List<AjustementDTO> getAjustements() {
        return ajustements;
    }

    public AjustDTO setAjustements(List<AjustementDTO> ajustements) {
        this.ajustements = ajustements;
        return this;
    }
}
