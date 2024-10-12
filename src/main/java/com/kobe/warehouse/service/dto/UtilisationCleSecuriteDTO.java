package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class UtilisationCleSecuriteDTO {

    private UserDTO cleSecuriteOwner;
    private UserDTO connectedUser;
    private String caisse;

    @NotNull
    private String privilege;

    private LocalDateTime mvtDate;

    @NotNull
    private Long entityId;

    private String entityName;

    @NotNull
    private String actionAuthorityKey;

    private String commentaire;

    public String getActionAuthorityKey() {
        return actionAuthorityKey;
    }

    public UtilisationCleSecuriteDTO setActionAuthorityKey(String actionAuthorityKey) {
        this.actionAuthorityKey = actionAuthorityKey;
        return this;
    }

    public String getCaisse() {
        return caisse;
    }

    public UtilisationCleSecuriteDTO setCaisse(String caisse) {
        this.caisse = caisse;
        return this;
    }

    public UserDTO getCleSecuriteOwner() {
        return cleSecuriteOwner;
    }

    public UtilisationCleSecuriteDTO setCleSecuriteOwner(UserDTO cleSecuriteOwner) {
        this.cleSecuriteOwner = cleSecuriteOwner;
        return this;
    }

    public UserDTO getConnectedUser() {
        return connectedUser;
    }

    public UtilisationCleSecuriteDTO setConnectedUser(UserDTO connectedUser) {
        this.connectedUser = connectedUser;
        return this;
    }

    public Long getEntityId() {
        return entityId;
    }

    public UtilisationCleSecuriteDTO setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public UtilisationCleSecuriteDTO setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public LocalDateTime getMvtDate() {
        return mvtDate;
    }

    public UtilisationCleSecuriteDTO setMvtDate(LocalDateTime mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public String getPrivilege() {
        return privilege;
    }

    public UtilisationCleSecuriteDTO setPrivilege(String privilege) {
        this.privilege = privilege;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public UtilisationCleSecuriteDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }
}
