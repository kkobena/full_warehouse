package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "tiers_payant",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }), @UniqueConstraint(columnNames = { "full_name" }) }
)
public class TiersPayant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(name = "name", nullable = false, length = 150)
    @NotNull
    private String name;

    @NotEmpty
    @NotNull
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "nbre_bons_max_sur_fact")
    private Integer nbreBons;

    @Column(name = "montant_max_sur_fact")
    private Long montantMaxParFcture;

    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    @Column(name = "code_organisme", length = 100)
    private String codeOrganisme;

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
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    @ManyToOne
    private User updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "consommation_json")
    private Set<Consommation> consommations = new HashSet<>();

    @Column(name = "model_facture", length = 20)
    private String modelFacture;

    @Transient
    private String modelFilePath;

    public TiersPayant() {}

    public String getModelFacture() {
        return modelFacture;
    }

    public TiersPayant setModelFacture(String modelFacture) {
        this.modelFacture = modelFacture;
        return this;
    }

    public Long getId() {
        return id;
    }

    public TiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull String getName() {
        return name;
    }

    public TiersPayant setName(String name) {
        this.name = name;
        return this;
    }

    public @NotNull String getFullName() {
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

    public @NotNull TiersPayantStatut getStatut() {
        return statut;
    }

    public TiersPayant setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public @NotNull TiersPayantCategorie getCategorie() {
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

    public LocalDateTime getCreated() {
        return created;
    }

    public TiersPayant setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public TiersPayant setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public TiersPayant setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    public Set<Consommation> getConsommations() {
        return consommations;
    }

    public TiersPayant setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        return this;
    }

    @Override
    public String toString() {
        return "TiersPayant{" + "fullName='" + fullName + '\'' + ", id=" + id + ", name='" + name + '\'' + '}';
    }

    public String getModelFilePath() {
        this.modelFilePath = Objects.requireNonNullElse(modelFacture, "default");
        return modelFilePath;
    }
}
