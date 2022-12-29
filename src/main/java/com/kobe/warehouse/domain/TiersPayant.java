package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity

@Table(name = "tiers_payant", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"}),
    @UniqueConstraint(columnNames = {"full_name"})
})
public class TiersPayant implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @Column(name = "name", nullable = false, length = 100)
    @NotNull
    private String name;
    @NotNull
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "nbre_bons_max_sur_fact")
    private Integer nbreBons;
    @Column(name = "montant_max_sur_fact")
    private Long montantMaxParFcture;
    @Column(name = "code_organisme", length = 100)
    private String codeOrganisme;
    @Column(name = "code_regroupement", length = 100)
    private String codeRegroupement;
    @Column(name = "conso_mensuelle")
    private Long consoMensuelle;
    @Column(name = "plafond_absolu")
    private Boolean plafondAbsolu = false;
    @Column(name = "adresse", length = 200)
    private String adresse;
    @Column(name = "telephone", length = 15)
    private String telephone;
    @Column(name = "telephone_fixe", length = 15)
    private String telephoneFixe;
    @Column(name = "email", length = 50)
    private String email;
    @Column(name = "to_be_exclude", columnDefinition = "boolean default false")
    private Boolean toBeExclude = Boolean.FALSE;
    @Column(name = "plafond_conso")
    private Long plafondConso;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private TiersPayantStatut statut = TiersPayantStatut.ACTIF;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private TiersPayantCategorie categorie;
    @Column(name = "remise_forfaitaire")
    private Long remiseForfaitaire;
    @Column(name = "nbre_bordereau", nullable = false, columnDefinition = "int default '1'")
    private Integer nbreBordereaux = 1;
    @ManyToOne
    private GroupeTiersPayant groupeTiersPayant;
    @Column(name = "created", nullable = false)
    private Instant created;
    @Column(name = "updated", nullable = false)
    private Instant updated = Instant.now();
    @NotNull
    @ManyToOne(optional = false)
    private User updatedBy;
    @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
    @Column(columnDefinition = "json", name = "consommation_json")
    private Set<Consommation> consommations = new HashSet<>();

    public TiersPayant() {
    }

    public Set<Consommation> getConsommations() {
        return consommations;
    }

    public TiersPayant setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        return this;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public TiersPayant setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public TiersPayant setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public TiersPayant setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public Long getId() {
        return id;
    }

    public TiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public TiersPayant setName(String name) {
        this.name = name;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public TiersPayant setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Integer getNbreBons() {
        return nbreBons;
    }

    public TiersPayant setNbreBons(Integer nbreBons) {
        this.nbreBons = nbreBons;
        return this;
    }

    public Long getMontantMaxParFcture() {
        return montantMaxParFcture;
    }

    public TiersPayant setMontantMaxParFcture(Long montantMaxParFcture) {
        this.montantMaxParFcture = montantMaxParFcture;
        return this;
    }

    public String getCodeOrganisme() {
        return codeOrganisme;
    }

    public TiersPayant setCodeOrganisme(String codeOrganisme) {
        this.codeOrganisme = codeOrganisme;
        return this;
    }

    public String getCodeRegroupement() {
        return codeRegroupement;
    }

    public TiersPayant setCodeRegroupement(String coeRegroupement) {
        codeRegroupement = coeRegroupement;
        return this;
    }

    public Long getConsoMensuelle() {
        return consoMensuelle;
    }

    public TiersPayant setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

    public Boolean getPlafondAbsolu() {
        return plafondAbsolu;
    }

    public TiersPayant setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }

    public String getAdresse() {
        return adresse;
    }

    public TiersPayant setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public TiersPayant setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getTelephoneFixe() {
        return telephoneFixe;
    }

    public TiersPayant setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TiersPayant setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getToBeExclude() {
        return toBeExclude;
    }

    public TiersPayant setToBeExclude(Boolean toBeExclude) {
        this.toBeExclude = toBeExclude;
        return this;
    }

    public Long getPlafondConso() {
        return plafondConso;
    }

    public TiersPayant setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

    public TiersPayantStatut getStatut() {
        return statut;
    }

    public TiersPayant setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public TiersPayantCategorie getCategorie() {
        return categorie;
    }

    public TiersPayant setCategorie(TiersPayantCategorie categorie) {
        this.categorie = categorie;
        return this;
    }

    public Long getRemiseForfaitaire() {
        return remiseForfaitaire;
    }

    public TiersPayant setRemiseForfaitaire(Long remiseForfaitaire) {
        this.remiseForfaitaire = remiseForfaitaire;
        return this;
    }

    public Integer getNbreBordereaux() {
        return nbreBordereaux;
    }

    public TiersPayant setNbreBordereaux(Integer nbreBordereaux) {
        this.nbreBordereaux = nbreBordereaux;
        return this;
    }

    public GroupeTiersPayant getGroupeTiersPayant() {
        return groupeTiersPayant;
    }

    public TiersPayant setGroupeTiersPayant(GroupeTiersPayant groupeTiersPayant) {
        this.groupeTiersPayant = groupeTiersPayant;
        return this;
    }
}
