package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "third_party_saleLine",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"client_tiers_payant_id", "sale_id"})})
public class ThirdPartySaleLine implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @ManyToOne(optional = false)
  private ThirdPartySales sale;

  @Column(name = "num_bon", length = 50)
  private String numBon;

  @NotNull
  @ManyToOne(optional = false)
  private ClientTiersPayant clientTiersPayant;

  @NotNull
  @Column(name = "montant", nullable = false)
  private Integer montant;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private Instant created;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private Instant updated = Instant.now();

  @NotNull
  @Column(name = "effective_update_date", nullable = false)
  private Instant effectiveUpdateDate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false)
  private ThirdPartySaleStatut statut = ThirdPartySaleStatut.ACTIF;

  @NotNull
  @Column(name = "taux", nullable = false)
  private short taux;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "invoice_statut", nullable = false)
  private ThirdPartySaleStatut invoiceStatut = ThirdPartySaleStatut.ACTIF;

  public short getTaux() {
    return taux;
  }

  public ThirdPartySaleStatut getInvoiceStatut() {
    return invoiceStatut;
  }

  public ThirdPartySaleLine setInvoiceStatut(ThirdPartySaleStatut invoiceStatut) {
    this.invoiceStatut = invoiceStatut;
    return this;
  }

  public ThirdPartySaleLine setTaux(short taux) {
    this.taux = taux;
    return this;
  }

  public Long getId() {
    return id;
  }

  public ThirdPartySaleLine setId(Long id) {
    this.id = id;
    return this;
  }

  public ThirdPartySales getSale() {
    return sale;
  }

  public ThirdPartySaleLine setSale(ThirdPartySales sale) {
    this.sale = sale;
    return this;
  }

  public String getNumBon() {
    return numBon;
  }

  public ThirdPartySaleLine setNumBon(String numBon) {
    this.numBon = numBon;
    return this;
  }

  public ClientTiersPayant getClientTiersPayant() {
    return clientTiersPayant;
  }

  public ThirdPartySaleLine setClientTiersPayant(ClientTiersPayant clientTiersPayant) {
    this.clientTiersPayant = clientTiersPayant;
    return this;
  }

  public Integer getMontant() {
    return montant;
  }

  public ThirdPartySaleLine setMontant(Integer montant) {
    this.montant = montant;
    return this;
  }

  public Instant getCreated() {
    return created;
  }

  public ThirdPartySaleLine setCreated(Instant created) {
    this.created = created;
    return this;
  }

  public Instant getUpdated() {
    return updated;
  }

  public ThirdPartySaleLine setUpdated(Instant updated) {
    this.updated = updated;
    return this;
  }

  public Instant getEffectiveUpdateDate() {
    return effectiveUpdateDate;
  }

  public ThirdPartySaleLine setEffectiveUpdateDate(Instant effectiveUpdateDate) {
    this.effectiveUpdateDate = effectiveUpdateDate;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ThirdPartySaleLine that = (ThirdPartySaleLine) o;

    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  public ThirdPartySaleStatut getStatut() {
    return statut;
  }

  public ThirdPartySaleLine setStatut(ThirdPartySaleStatut statut) {
    this.statut = statut;
    return this;
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace(System.err);
      return null;
    }
  }
}
