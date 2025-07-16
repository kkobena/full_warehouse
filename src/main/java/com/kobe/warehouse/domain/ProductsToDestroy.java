package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products_to_destroy")
public class ProductsToDestroy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numLot;

    @NotNull
    private Integer prixAchat;

    @NotNull
    private Integer prixUnit;

    @ManyToOne(optional = false)
    private FournisseurProduit fournisseurProduit;

    @Column(name = "quantity", nullable = false, columnDefinition = "int(6) ")
    @Min(1)
    private int quantity;

    @NotNull
    private LocalDate datePeremption;

    private LocalDate dateDestuction;

    @ManyToOne(optional = false)
    private User user;

    @Column(name = "destroyed", columnDefinition = "boolean default false")
    private boolean destroyed;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime updated;

    @ManyToOne(optional = false)
    private Magasin magasin;

    @Column(name = "editing", columnDefinition = "boolean default false")
    private boolean editing; // pour gerer les ajouts manuels

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDate getDateDestuction() {
        return dateDestuction;
    }

    public void setDateDestuction(LocalDate dateDestuction) {
        this.dateDestuction = dateDestuction;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumLot() {
        return numLot;
    }

    public void setNumLot(String lotNum) {
        this.numLot = lotNum;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public Integer getPrixUnit() {
        return prixUnit;
    }

    public void setPrixUnit(Integer prixUnit) {
        this.prixUnit = prixUnit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FournisseurProduit getFournisseurProduit() {
        return fournisseurProduit;
    }

    public void setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }
}
