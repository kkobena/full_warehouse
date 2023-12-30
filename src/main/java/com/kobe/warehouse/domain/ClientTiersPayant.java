package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "client_tiers_payant",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"tiers_payant_id", "assured_customer_id"}),
      @UniqueConstraint(columnNames = {"tiers_payant_id", "num"})
    })
public class ClientTiersPayant implements Serializable {

  private static final long serialVersionUID = 1L;

  @Getter
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @SequenceGenerator(name = "sequenceGenerator")
  private Long id;

  @Getter
  @NotNull
  @ManyToOne(optional = false)
  private TiersPayant tiersPayant;

  @Getter
  @NotNull
  @ManyToOne(optional = false)
  private AssuredCustomer assuredCustomer;

  @Getter
  @Column(name = "num", nullable = false, length = 100)
  @NotNull
  private String num;

  @Getter
  @Column(name = "plafond_conso")
  private Long plafondConso;

  @Getter
  @Column(name = "plafond_journalier")
  private Long plafondJournalier;

  @Getter
  @Column(name = "created", nullable = false)
  private LocalDateTime created;

  @Getter
  @Column(name = "updated", nullable = false)
  private LocalDateTime updated = LocalDateTime.now();

  @Getter
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "priorite", nullable = false)
  private PrioriteTiersPayant priorite = PrioriteTiersPayant.T0;

  @Getter
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut", nullable = false)
  private TiersPayantStatut statut = TiersPayantStatut.ACTIF;

  @Getter
  @NotNull
  @Column(name = "taux", nullable = false)
  private Integer taux;

  @Getter
  @Column(name = "conso_mensuelle")
  private Long consoMensuelle;

  private transient double tauxValue;

  @Getter
  @Column(name = "plafond_absolu")
  private Boolean plafondAbsolu = false;

  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", name = "consommation_json")
  private Set<Consommation> consommations = new HashSet<>();

  public ClientTiersPayant() {}

  public ClientTiersPayant setPlafondAbsolu(Boolean plafondAbsolu) {
    this.plafondAbsolu = plafondAbsolu;
    return this;
  }

  public ClientTiersPayant setConsommations(Set<Consommation> consommations) {
    this.consommations = consommations;
    return this;
  }

  public ClientTiersPayant setConsoMensuelle(Long consoMensuelle) {
    this.consoMensuelle = consoMensuelle;
    return this;
  }

  public ClientTiersPayant setId(Long id) {
    this.id = id;
    return this;
  }

  public ClientTiersPayant setTiersPayant(TiersPayant tiersPayant) {
    this.tiersPayant = tiersPayant;
    return this;
  }

  public ClientTiersPayant setAssuredCustomer(AssuredCustomer assuredCustomer) {
    this.assuredCustomer = assuredCustomer;
    return this;
  }

  public ClientTiersPayant setNum(String num) {
    this.num = num;
    return this;
  }

  public ClientTiersPayant setPlafondConso(Long plafondConso) {
    this.plafondConso = plafondConso;
    return this;
  }

  public ClientTiersPayant setPlafondJournalier(Long plafondJournalier) {
    this.plafondJournalier = plafondJournalier;
    return this;
  }

  public ClientTiersPayant setCreated(LocalDateTime created) {
    this.created = created;
    return this;
  }

  public ClientTiersPayant setUpdated(LocalDateTime updated) {
    this.updated = updated;
    return this;
  }

  public ClientTiersPayant setPriorite(PrioriteTiersPayant priorite) {
    this.priorite = priorite;
    return this;
  }

  public ClientTiersPayant setStatut(TiersPayantStatut statut) {
    this.statut = statut;
    return this;
  }

  public double getTauxValue() {
    tauxValue = Double.valueOf(taux) / 100;
    return tauxValue;
  }

  public ClientTiersPayant setTaux(Integer taux) {
    this.taux = taux;
    return this;
  }

  @Override
  public String toString() {
    String sb =
        "ClientTiersPayant{"
            + "id="
            + id
            + ", num='"
            + num
            + '\''
            + ", plafondConso="
            + plafondConso
            + ", plafondJournalier="
            + plafondJournalier
            + ", created="
            + created
            + ", updated="
            + updated
            + ", priorite="
            + priorite
            + ", statut="
            + statut
            + ", taux="
            + taux
            + ", consoMensuelle="
            + consoMensuelle
            + ", tauxValue="
            + tauxValue
            + ", plafondAbsolu="
            + plafondAbsolu
            + '}';
    return sb;
  }
}
