package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
import lombok.Getter;
import org.hibernate.annotations.Type;

@Getter
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
    private LocalDateTime created;
    @Column(name = "updated", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();
    @NotNull
    @ManyToOne(optional = false)
    private User updatedBy;
    @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
    @Column(columnDefinition = "json", name = "consommation_json")
    private Set<Consommation> consommations = new HashSet<>();

    public TiersPayant() {
    }

  public TiersPayant setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        return this;
    }

  public TiersPayant setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

  public TiersPayant setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

  public TiersPayant setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

  public TiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

  public TiersPayant setName(String name) {
        this.name = name;
        return this;
    }

  public TiersPayant setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

  public TiersPayant setNbreBons(Integer nbreBons) {
        this.nbreBons = nbreBons;
        return this;
    }

  public TiersPayant setMontantMaxParFcture(Long montantMaxParFcture) {
        this.montantMaxParFcture = montantMaxParFcture;
        return this;
    }

  public TiersPayant setCodeOrganisme(String codeOrganisme) {
        this.codeOrganisme = codeOrganisme;
        return this;
    }

  public TiersPayant setCodeRegroupement(String coeRegroupement) {
        codeRegroupement = coeRegroupement;
        return this;
    }

  public TiersPayant setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

  public TiersPayant setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }

  public TiersPayant setAdresse(String adresse) {
        this.adresse = adresse;
        return this;
    }

  public TiersPayant setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

  public TiersPayant setTelephoneFixe(String telephoneFixe) {
        this.telephoneFixe = telephoneFixe;
        return this;
    }

  public TiersPayant setEmail(String email) {
        this.email = email;
        return this;
    }

  public TiersPayant setToBeExclude(Boolean toBeExclude) {
        this.toBeExclude = toBeExclude;
        return this;
    }

  public TiersPayant setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

  public TiersPayant setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

  public TiersPayant setCategorie(TiersPayantCategorie categorie) {
        this.categorie = categorie;
        return this;
    }

  public TiersPayant setRemiseForfaitaire(Long remiseForfaitaire) {
        this.remiseForfaitaire = remiseForfaitaire;
        return this;
    }

  public TiersPayant setNbreBordereaux(Integer nbreBordereaux) {
        this.nbreBordereaux = nbreBordereaux;
        return this;
    }

  public TiersPayant setGroupeTiersPayant(GroupeTiersPayant groupeTiersPayant) {
        this.groupeTiersPayant = groupeTiersPayant;
        return this;
    }
}
