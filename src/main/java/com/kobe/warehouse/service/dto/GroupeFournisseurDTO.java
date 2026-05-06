package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GroupeFournisseur;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * A DTO for the {@link com.kobe.warehouse.domain.GroupeFournisseur} entity.
 */
public class GroupeFournisseurDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6447988428721557444L;

    private Integer id;

    @NotNull
    private String libelle;

    private String addresspostale;

    private String numFaxe;

    private String email;

    private String tel;

    private Integer odre = 100;
    private String codeRecepteurPharmaMl;
    private String codeOfficePharmaMl;
    private String urlPharmaMl;
    private String idRecepteurPharmaMl;
    private Integer delaiLivraisonJours;
    private Integer frequenceCommandeJours;
    private Integer joursCredit;
    private Integer joursCritique;
    private Long palierRfa;
    private Integer tauxRfa;

    public GroupeFournisseurDTO() {
    }

    public GroupeFournisseurDTO(GroupeFournisseur groupeFournisseur) {
        id = groupeFournisseur.getId();
        libelle = groupeFournisseur.getLibelle();
        addresspostale = groupeFournisseur.getAddresspostale();
        numFaxe = groupeFournisseur.getNumFaxe();
        email = groupeFournisseur.getEmail();
        tel = groupeFournisseur.getTel();
        odre = groupeFournisseur.getOdre();
        codeRecepteurPharmaMl = groupeFournisseur.getCodeRecepteurPharmaMl();
        codeOfficePharmaMl = groupeFournisseur.getCodeOfficePharmaMl();
        urlPharmaMl = groupeFournisseur.getUrlPharmaMl();
        idRecepteurPharmaMl = groupeFournisseur.getIdRecepteurPharmaMl();
        delaiLivraisonJours = groupeFournisseur.getDelaiLivraisonJours();
        frequenceCommandeJours = groupeFournisseur.getFrequenceCommandeJours();
        joursCredit = groupeFournisseur.getJoursCredit();
        joursCritique = groupeFournisseur.getJoursCritique();
        palierRfa = groupeFournisseur.getPalierRfa();
        tauxRfa = groupeFournisseur.getTauxRfa();
    }

    public Integer getDelaiLivraisonJours() {
        return delaiLivraisonJours;
    }

    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) {
        this.delaiLivraisonJours = delaiLivraisonJours;
    }

    public Integer getFrequenceCommandeJours() {
        return frequenceCommandeJours;
    }

    public void setFrequenceCommandeJours(Integer frequenceCommandeJours) {
        this.frequenceCommandeJours = frequenceCommandeJours;
    }

    public String getUrlPharmaMl() {
        return urlPharmaMl;
    }

    public GroupeFournisseurDTO setUrlPharmaMl(String urlPharmaMl) {
        this.urlPharmaMl = urlPharmaMl;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public GroupeFournisseurDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getCodeOfficePharmaMl() {
        return codeOfficePharmaMl;
    }

    public GroupeFournisseurDTO setCodeOfficePharmaMl(String codeOfficePharmaMl) {
        this.codeOfficePharmaMl = codeOfficePharmaMl;
        return this;
    }

    public String getCodeRecepteurPharmaMl() {
        return codeRecepteurPharmaMl;
    }

    public GroupeFournisseurDTO setCodeRecepteurPharmaMl(String codeRecepteurPharmaMl) {
        this.codeRecepteurPharmaMl = codeRecepteurPharmaMl;
        return this;
    }

    public String getIdRecepteurPharmaMl() {
        return idRecepteurPharmaMl;
    }

    public GroupeFournisseurDTO setIdRecepteurPharmaMl(String idRecepteurPharmaMl) {
        this.idRecepteurPharmaMl = idRecepteurPharmaMl;
        return this;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public GroupeFournisseurDTO setLibelle(@NotNull String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public GroupeFournisseurDTO setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
        return this;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public GroupeFournisseurDTO setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public GroupeFournisseurDTO setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getTel() {
        return tel;
    }

    public GroupeFournisseurDTO setTel(String tel) {
        this.tel = tel;
        return this;
    }

    public Integer getOdre() {
        return odre;
    }

    public GroupeFournisseurDTO setOdre(Integer odre) {
        this.odre = odre;
        return this;
    }

    public Integer getJoursCredit() { return joursCredit; }
    public GroupeFournisseurDTO setJoursCredit(Integer joursCredit) { this.joursCredit = joursCredit; return this; }

    public Integer getJoursCritique() { return joursCritique; }
    public GroupeFournisseurDTO setJoursCritique(Integer joursCritique) { this.joursCritique = joursCritique; return this; }

    public Long getPalierRfa() { return palierRfa; }
    public GroupeFournisseurDTO setPalierRfa(Long palierRfa) { this.palierRfa = palierRfa; return this; }

    public Integer getTauxRfa() { return tauxRfa; }
    public GroupeFournisseurDTO setTauxRfa(Integer tauxRfa) { this.tauxRfa = tauxRfa; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupeFournisseurDTO)) {
            return false;
        }

        return id != null && id.equals(((GroupeFournisseurDTO) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
