package com.kobe.warehouse.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ticket")
public class Ticket implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String code;
    @NotNull
    @Column(name = "montant_attendu", nullable = false)
    private Integer montantAttendu = 0;
    @NotNull
    @Column(name = "montant_paye", nullable = false)
    private Integer montantPaye = 0;
    @NotNull
    @Column(name = "montant_rendu", nullable = false)
    private Integer montantRendu = 0;
    @NotNull
    @Column(name = "montant_verse", nullable = false)
    private Integer montantVerse = 0;
    @NotNull
    @Column(name = "rest_to_pay", nullable = false)
    private Integer restToPay = 0;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant created;
    @NotNull
    @ManyToOne(optional = false)
    private User user;
    @ManyToOne
    private Sales sale;
    @Column(name = "part_assure", columnDefinition = "int default '0'")
    private Integer partAssure;
    @Column(name = "part_tiers_payant", columnDefinition = "int default '0'")
    private Integer partTiersPayant;
    @ManyToOne
    private Customer customer;
    @Column(length = 100)
    private String tva;
    @Column(name = "canceled", nullable = false, columnDefinition = "boolean default false")
    private Boolean canceled = false;

    public String getTva() {
        return tva;
    }

    public Ticket setTva(String tva) {
        this.tva = tva;
        return this;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public Ticket setCanceled(Boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public Ticket setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public Integer getPartAssure() {
        return partAssure;
    }

    public Ticket setPartAssure(Integer partAssure) {
        this.partAssure = partAssure;
        return this;
    }

    public Integer getPartTiersPayant() {
        return partTiersPayant;
    }

    public Ticket setPartTiersPayant(Integer partTiersPayant) {
        this.partTiersPayant = partTiersPayant;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Ticket setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Ticket setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getMontantAttendu() {
        return montantAttendu;
    }

    public Ticket setMontantAttendu(Integer montantAttendu) {
        this.montantAttendu = montantAttendu;
        return this;
    }

    public Integer getMontantPaye() {
        return montantPaye;
    }

    public Ticket setMontantPaye(Integer montantPaye) {
        this.montantPaye = montantPaye;
        return this;
    }

    public Integer getMontantRendu() {
        return montantRendu;
    }

    public Ticket setMontantRendu(Integer montantRendu) {
        this.montantRendu = montantRendu;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public Ticket setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Ticket setUser(User user) {
        this.user = user;
        return this;
    }

    public Sales getSale() {
        return sale;
    }

    public Ticket setSale(Sales sale) {
        this.sale = sale;
        return this;
    }


    public Integer getMontantVerse() {
        return montantVerse;
    }

    public Ticket setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return code.equals(ticket.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
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
