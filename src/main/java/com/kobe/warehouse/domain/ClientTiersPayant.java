package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.service.dto.Consommation;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@TypeDef(
    name = "json", typeClass = JsonStringType.class
)
@Table(name = "client_tiers_payant", uniqueConstraints = {@UniqueConstraint(columnNames = {"tiers_payant_id", "assured_customer_id"}),
    @UniqueConstraint(columnNames = {"tiers_payant_id", "num"})

})
public class ClientTiersPayant implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @ManyToOne(optional = false)
    private TiersPayant tiersPayant;
    @NotNull
    @ManyToOne(optional = false)
    private AssuredCustomer assuredCustomer;
    @Column(name = "num", nullable = false, length = 100)
    @NotNull
    private String num;
    @Column(name = "plafond_conso")
    private Long plafondConso;
    @Column(name = "plafond_journalier")
    private Long plafondJournalier;
    @Column(name = "created", nullable = false)
    private Instant created;
    @Column(name = "updated", nullable = false)
    private Instant updated = Instant.now();
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priorite", nullable = false)
    private PrioriteTiersPayant priorite = PrioriteTiersPayant.T0;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut", nullable = false)
    private TiersPayantStatut statut = TiersPayantStatut.ACTIF;
    @NotNull
    @Column(name = "taux", nullable = false)
    private Integer taux;
    @Column(name = "conso_mensuelle")
    private Long consoMensuelle;
    private transient double tauxValue;
    @Column(name = "plafond_absolu")
    private Boolean plafondAbsolu = false;
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "consommation_json")
    private Set<Consommation> consommations=new HashSet<>();
    public Boolean getPlafondAbsolu() {
        return plafondAbsolu;
    }

    public Set<Consommation> getConsommations() {
        return consommations;
    }

    public ClientTiersPayant setConsommations(Set<Consommation> consommations) {
        this.consommations = consommations;
        return this;
    }

    public ClientTiersPayant setPlafondAbsolu(Boolean plafondAbsolu) {
        this.plafondAbsolu = plafondAbsolu;
        return this;
    }

    public Long getConsoMensuelle() {
        return consoMensuelle;
    }

    public ClientTiersPayant setConsoMensuelle(Long consoMensuelle) {
        this.consoMensuelle = consoMensuelle;
        return this;
    }

    public Long getId() {
        return id;
    }

    public ClientTiersPayant setId(Long id) {
        this.id = id;
        return this;
    }

    public TiersPayant getTiersPayant() {
        return tiersPayant;
    }

    public ClientTiersPayant setTiersPayant(TiersPayant tiersPayant) {
        this.tiersPayant = tiersPayant;
        return this;
    }

    public AssuredCustomer getAssuredCustomer() {
        return assuredCustomer;
    }

    public ClientTiersPayant setAssuredCustomer(AssuredCustomer assuredCustomer) {
        this.assuredCustomer = assuredCustomer;
        return this;
    }

    public String getNum() {
        return num;
    }

    public ClientTiersPayant setNum(String num) {
        this.num = num;
        return this;
    }

    public Long getPlafondConso() {
        return plafondConso;
    }

    public ClientTiersPayant setPlafondConso(Long plafondConso) {
        this.plafondConso = plafondConso;
        return this;
    }

    public Long getPlafondJournalier() {
        return plafondJournalier;
    }

    public ClientTiersPayant setPlafondJournalier(Long plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public ClientTiersPayant setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Instant getUpdated() {
        return updated;
    }

    public ClientTiersPayant setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    public PrioriteTiersPayant getPriorite() {
        return priorite;
    }

    public ClientTiersPayant setPriorite(PrioriteTiersPayant priorite) {
        this.priorite = priorite;
        return this;
    }

    public TiersPayantStatut getStatut() {
        return statut;
    }

    public double getTauxValue() {
        tauxValue = Double.valueOf(taux) / 100;
        return tauxValue;
    }

    public Integer getTaux() {
        return taux;
    }

    public ClientTiersPayant setTaux(Integer taux) {
        this.taux = taux;
        return this;
    }

    public ClientTiersPayant setStatut(TiersPayantStatut statut) {
        this.statut = statut;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ClientTiersPayant{");
        sb.append("id=").append(id);
        sb.append(", num='").append(num).append('\'');
        sb.append(", plafondConso=").append(plafondConso);
        sb.append(", plafondJournalier=").append(plafondJournalier);
        sb.append(", created=").append(created);
        sb.append(", updated=").append(updated);
        sb.append(", priorite=").append(priorite);
        sb.append(", statut=").append(statut);
        sb.append(", taux=").append(taux);
        sb.append(", consoMensuelle=").append(consoMensuelle);
        sb.append(", tauxValue=").append(tauxValue);
        sb.append(", plafondAbsolu=").append(plafondAbsolu);
        sb.append('}');
        return sb.toString();
    }

    public ClientTiersPayant() {
    }
}
