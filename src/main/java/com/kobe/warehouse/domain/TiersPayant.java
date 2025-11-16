package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import com.kobe.warehouse.service.sale.impl.ConsommationService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "tiers_payant",
    indexes = {
        @Index(columnList = "full_name", name = "tiers_payant_full_name_index"),
        @Index(columnList = "name", name = "tiers_payant_name_index"),
        @Index(columnList = "statut", name = "tiers_payant_statut_index"),
        @Index(columnList = "categorie", name = "tiers_payant_categorie_index"),
    },
    uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }), @UniqueConstraint(columnNames = { "full_name" }) }
)
public class TiersPayant implements Serializable, ConsommationService.HasConsommation {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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

    @ColumnDefault("false")
    @Column(name = "plafond_absolu")
    private boolean plafondAbsolu;

    @Column(name = "adresse", length = 200)
    private String adresse;

    @Column(name = "telephone", length = 15)
    private String telephone;

    @Column(name = "telephone_fixe", length = 15)
    private String telephoneFixe;

    @Column(name = "email", length = 100)
    private String email;

    @ColumnDefault("false")
    @Column(name = "to_be_exclude")
    private boolean beExclude;

    @Column(name = "plafond_conso")
    private Long plafondConso;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 10)
    private TiersPayantStatut statut = TiersPayantStatut.ACTIF;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 10)
    private TiersPayantCategorie categorie;

    @ColumnDefault("0")
    @Column(name = "remise_forfaitaire")
    private int remiseForfaitaire;

    @Column(name = "nbre_bordereau")
    private int nbreBordereaux;

    @ManyToOne
    @JoinColumn(name = "groupe_tiers_payant_id", referencedColumnName = "id")
    private GroupeTiersPayant groupeTiersPayant;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "consommation_json")
    private Set<Consommation> consommations = new HashSet<>();

    @Column(name = "model_facture", length = 20)
    private String modelFacture;

    @Column(name = "plafond_conso_client")
    private Integer plafondConsoClient;

    @Column(name = "plafond_journalier_client")
    private Integer plafondJournalierClient;

    @ColumnDefault("false")
    @Column(name = "plafond_absolu_client")
    private boolean plafondAbsoluClient;

    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    @Column(name = "ncc", length = 100)
    private String ncc; //Identifiant contribuable

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

    public Integer getId() {
        return id;
    }

    public TiersPayant setId(Integer id) {
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

    public boolean isBeExclude() {
        return beExclude;
    }

    public TiersPayant setBeExclude(boolean beExclude) {
        this.beExclude = beExclude;
        return this;
    }

    public boolean isPlafondAbsoluClient() {
        return plafondAbsoluClient;
    }

    public TiersPayant setPlafondAbsoluClient(boolean plafondAbsoluClient) {
        this.plafondAbsoluClient = plafondAbsoluClient;
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

    @Override
    @JsonIgnore
    public void setConsoMensuelle(Number consoMensuelle) {
        this.consoMensuelle = (consoMensuelle != null) ? consoMensuelle.longValue() : null;
    }

    public boolean isPlafondAbsolu() {
        return plafondAbsolu;
    }

    public TiersPayant setPlafondAbsolu(boolean plafondAbsolu) {
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

    public int getRemiseForfaitaire() {
        return remiseForfaitaire;
    }

    public TiersPayant setRemiseForfaitaire(int remiseForfaitaire) {
        this.remiseForfaitaire = remiseForfaitaire;
        return this;
    }

    public int getNbreBordereaux() {
        return nbreBordereaux;
    }

    public TiersPayant setNbreBordereaux(int nbreBordereaux) {
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

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public AppUser getUser() {
        return user;
    }

    public TiersPayant setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public Set<Consommation> getConsommations() {
        return consommations;
    }

    public void setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        //  return this;
    }

    public Integer getPlafondConsoClient() {
        return plafondConsoClient;
    }

    public TiersPayant setPlafondConsoClient(Integer plafondConsoClient) {
        this.plafondConsoClient = plafondConsoClient;
        return this;
    }

    public Integer getPlafondJournalierClient() {
        return plafondJournalierClient;
    }

    public TiersPayant setPlafondJournalierClient(Integer plafondJournalierClient) {
        this.plafondJournalierClient = plafondJournalierClient;
        return this;
    }

    @Override
    public String toString() {
        return "TiersPayant{" + "fullName='" + fullName + '\'' + ", id=" + id + ", name='" + name + '\'' + '}';
    }

    public String getNcc() {
        return ncc;
    }

    public TiersPayant setNcc(String ncc) {
        this.ncc = ncc;
        return this;
    }

    public String getModelFilePath() {
        this.modelFilePath = Objects.requireNonNullElse(modelFacture, "default");
        return modelFilePath;
    }
}
