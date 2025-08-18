package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "client_tiers_payant",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tiers_payant_id", "assured_customer_id" }),
        @UniqueConstraint(columnNames = { "tiers_payant_id", "num" }),
    }
)
public class ClientTiersPayant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private TiersPayant tiersPayant;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "assured_customer_id", referencedColumnName = "id")
    private AssuredCustomer assuredCustomer;

    @Column(name = "num", nullable = false, length = 100)
    @NotNull
    private String num;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false)
    private PrioriteTiersPayant priorite = PrioriteTiersPayant.R0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private TiersPayantStatut statut = TiersPayantStatut.ACTIF;

    @NotNull
    @Column(name = "taux", nullable = false, columnDefinition = "int(3)")
    private int taux;

    @Column(name = "conso_mensuelle")
    private Long consoMensuelle;

    private transient double tauxValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", name = "consommation_json")
    private Set<Consommation> consommations = new HashSet<>();

    public ClientTiersPayant() {}

    public Long getId() {
        return id;
    }

    public ClientTiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public ClientTiersPayant setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public @NotNull AssuredCustomer getAssuredCustomer() {
        return assuredCustomer;
    }

    public ClientTiersPayant setAssuredCustomer(AssuredCustomer assuredCustomer) {
        this.assuredCustomer = assuredCustomer;
        return this;
    }

    public @NotNull String getNum() {
        return num;
    }

    public ClientTiersPayant setNum(String num) {
        this.num = num;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public ClientTiersPayant setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public ClientTiersPayant setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public @NotNull PrioriteTiersPayant getPriorite() {
        return priorite;
    }

    public ClientTiersPayant setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
        return this;
    }

    public @NotNull TiersPayantStatut getStatut() {
        return statut;
    }

    public ClientTiersPayant setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    public int getTaux() {
        return taux;
    }

    public ClientTiersPayant setTaux(int taux) {
        this.taux = taux;
        return this;
    }

    public Long getConsoMensuelle() {
        return consoMensuelle;
    }

    public ClientTiersPayant setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

    public Set<Consommation> getConsommations() {
        return consommations;
    }

    public ClientTiersPayant setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        return this;
    }

    public double getTauxValue() {
        tauxValue = (double) taux / 100;
        return tauxValue;
    }
}
