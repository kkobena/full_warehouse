package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisation_cle_securite")
public class UtilisationCleSecurite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private User cleSecuriteOwner;

    @NotNull
    @ManyToOne(optional = false)
    private User connectedUser;

    @NotNull
    @Column(name = "caisse", nullable = false)
    private String caisse;

    @Column(name = "mvt_date", nullable = false)
    @NotNull
    private LocalDateTime mvtDate = LocalDateTime.now();

    @NotNull
    @ManyToOne(optional = false)
    private Privilege privilege;

    private String commentaire;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name")
    private String entityName;

    public Long getId() {
        return id;
    }

    public UtilisationCleSecurite setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull User getCleSecuriteOwner() {
        return cleSecuriteOwner;
    }

    public UtilisationCleSecurite setCleSecuriteOwner(@NotNull User cleSecuriteOwner) {
        this.cleSecuriteOwner = cleSecuriteOwner;
        return this;
    }

    public @NotNull User getConnectedUser() {
        return connectedUser;
    }

    public UtilisationCleSecurite setConnectedUser(@NotNull User connectedUser) {
        this.connectedUser = connectedUser;
        return this;
    }

    public @NotNull String getCaisse() {
        return caisse;
    }

    public UtilisationCleSecurite setCaisse(@NotNull String caisse) {
        this.caisse = caisse;
        return this;
    }

    public @NotNull LocalDateTime getMvtDate() {
        return mvtDate;
    }

    public UtilisationCleSecurite setMvtDate(@NotNull LocalDateTime mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public @NotNull Privilege getPrivilege() {
        return privilege;
    }

    public UtilisationCleSecurite setPrivilege(@NotNull Privilege privilege) {
        this.privilege = privilege;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public UtilisationCleSecurite setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public Long getEntityId() {
        return entityId;
    }

    public UtilisationCleSecurite setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public UtilisationCleSecurite setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }
}
