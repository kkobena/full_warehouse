package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.URL;

import java.io.Serial;
import java.io.Serializable;

/**
 * A GroupeFournisseur.
 */
@Entity
@Table(name = "groupe_fournisseur")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GroupeFournisseur implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;

    @Column(name = "addresspostale")
    private String addresspostale;

    @Column(name = "num_faxe")
    private String numFaxe;

    @Column(name = "email")
    private String email;

    @Column(name = "tel", length = 15)
    private String tel;

    @NotNull
    @Column(name = "odre", nullable = false)
    private Integer odre = 100;

    /**
     * Délai de livraison par défaut (jours) pour tous les fournisseurs de ce groupe.
     */
    @NotNull
    @Column(name = "delai_livraison_jours", nullable = false)
    private Integer delaiLivraisonJours = 7;

    @Column(name = "frequence_commande_jours", nullable = false)
    private Integer frequenceCommandeJours = 7;

    /*
   utilser dans pharmaMl code grossiste RECEPTEUR(code)
   DESTINATAIRE(code_societe)
     */

    @Column(name = "code_recepteur_pharma_ml", length = 50)
    private String codeRecepteurPharmaMl;

    /*
     Code de l'officine chez le grossiste dans EMETTEUR(id,Id_Client)
     */

    @Column(name = "code_office_pharma_ml", length = 50)
    private String codeOfficePharmaMl;

    @URL(message = "URL PharmaMl n'est pas valide")
    @Column(name = "url_pharma_ml", length = 150)
    private String urlPharmaMl;

    @Column(name = "id_recepteur_pharma_ml", length = 50)
    private String idRecepteurPharmaMl; // Code de l'officine chez le grossiste dans EMETTEUR(id,Id_Client)

    /** Délai de paiement en jours. Null = utiliser la valeur par défaut de l'application. */
    @Column(name = "jours_credit")
    private Integer joursCredit;

    /** Délai supplémentaire (en jours après l'échéance) avant de passer en statut CRITIQUE. Null = valeur par défaut. */
    @Column(name = "jours_critique")
    private Integer joursCritique;

    /** Seuil CA annuel (FCFA) à atteindre pour déclencher la RFA. Null = pas de RFA configurée. */
    @Column(name = "palier_rfa")
    private Long palierRfa;

    /** Taux RFA en % entier (ex : 2 = 2 %). Null = pas de taux configuré. */
    @Column(name = "taux_rfa")
    private Integer tauxRfa;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodeOfficePharmaMl() {
        return codeOfficePharmaMl;
    }

    public Integer getJoursCredit() {
        return joursCredit;
    }

    public GroupeFournisseur setJoursCredit(Integer joursCredit) {
        this.joursCredit = joursCredit;
        return this;
    }

    public Integer getJoursCritique() {
        return joursCritique;
    }

    public GroupeFournisseur setJoursCritique(Integer joursCritique) {
        this.joursCritique = joursCritique;
        return this;
    }

    public Long getPalierRfa() {
        return palierRfa;
    }

    public GroupeFournisseur setPalierRfa(Long palierRfa) {
        this.palierRfa = palierRfa;
        return this;
    }

    public Integer getTauxRfa() {
        return tauxRfa;
    }

    public GroupeFournisseur setTauxRfa(Integer tauxRfa) {
        this.tauxRfa = tauxRfa;
        return this;
    }

    public GroupeFournisseur setCodeOfficePharmaMl(String codeOfficePharmaMl) {
        this.codeOfficePharmaMl = codeOfficePharmaMl;
        return this;
    }

    public String getIdRecepteurPharmaMl() {
        return idRecepteurPharmaMl;
    }

    public GroupeFournisseur setIdRecepteurPharmaMl(String idRecepteurPharmaMl) {
        this.idRecepteurPharmaMl = idRecepteurPharmaMl;
        return this;
    }

    public String getCodeRecepteurPharmaMl() {
        return codeRecepteurPharmaMl;
    }

    public GroupeFournisseur setCodeRecepteurPharmaMl(String codeRecepteurPharmaMl) {
        this.codeRecepteurPharmaMl = codeRecepteurPharmaMl;
        return this;
    }

    public String getUrlPharmaMl() {
        return urlPharmaMl;
    }

    public GroupeFournisseur setUrlPharmaMl(String urlPharmaMl) {
        this.urlPharmaMl = urlPharmaMl;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public GroupeFournisseur libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public GroupeFournisseur id(Integer id) {
        this.id = id;
        return this;
    }

    public String getAddresspostale() {
        return addresspostale;
    }

    public void setAddresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
    }

    public GroupeFournisseur addresspostale(String addresspostale) {
        this.addresspostale = addresspostale;
        return this;
    }

    public String getNumFaxe() {
        return numFaxe;
    }

    public void setNumFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
    }

    public GroupeFournisseur numFaxe(String numFaxe) {
        this.numFaxe = numFaxe;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public GroupeFournisseur email(String email) {
        this.email = email;
        return this;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public GroupeFournisseur tel(String tel) {
        this.tel = tel;
        return this;
    }

    public Integer getOdre() {
        return odre;
    }

    public void setOdre(Integer odre) {
        this.odre = odre;
    }

    public GroupeFournisseur odre(Integer odre) {
        this.odre = odre;
        return this;
    }

    public Integer getDelaiLivraisonJours() {
        if (delaiLivraisonJours == null) {
            delaiLivraisonJours = 7;
        }
        return delaiLivraisonJours;
    }

    public GroupeFournisseur setDelaiLivraisonJours(Integer delaiLivraisonJours) {
        this.delaiLivraisonJours = delaiLivraisonJours;
        return this;
    }

    public Integer getFrequenceCommandeJours() {
        if (frequenceCommandeJours == null) {
            frequenceCommandeJours = 7;
        }
        return frequenceCommandeJours;
    }

    public GroupeFournisseur setFrequenceCommandeJours(Integer frequenceCommandeJours) {
        this.frequenceCommandeJours = frequenceCommandeJours;
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupeFournisseur)) {
            return false;
        }
        return id != null && id.equals(((GroupeFournisseur) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GroupeFournisseur{"
            + "id="
            + getId()
            + ", libelle='"
            + getLibelle()
            + "'"
            + ", addresspostale='"
            + getAddresspostale()
            + "'"
            + ", numFaxe='"
            + getNumFaxe()
            + "'"
            + ", email='"
            + getEmail()
            + "'"
            + ", tel='"
            + getTel()
            + "'"
            + ", odre="
            + getOdre()
            + "}";
    }
}
