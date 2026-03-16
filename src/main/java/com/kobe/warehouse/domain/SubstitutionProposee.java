package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.SubstitutionStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "substitution_proposee")
public class SubstitutionProposee implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumns({
        @JoinColumn(name = "commande_id",   referencedColumnName = "id"),
        @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date"),
    })
    private Commande commande;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumns({
        @JoinColumn(name = "order_line_id",  referencedColumnName = "id"),
        @JoinColumn(name = "commande_date",  referencedColumnName = "order_date"),
    })
    private OrderLine orderLine;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @NotNull
    @Column(name = "cip_propose", length = 13, nullable = false)
    private String cipPropose;

    @Column(name = "designation", length = 255)
    private String designation;

    @Column(name = "type_codification", length = 10)
    private String typeCodification;

    @NotNull
    @Column(name = "quantite", nullable = false)
    private int quantite;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private SubstitutionStatut statut = SubstitutionStatut.EN_ATTENTE;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getId() { return id; }

    public Commande getCommande() { return commande; }
    public SubstitutionProposee setCommande(Commande commande) { this.commande = commande; return this; }

    public OrderLine getOrderLine() { return orderLine; }
    public SubstitutionProposee setOrderLine(OrderLine orderLine) { this.orderLine = orderLine; return this; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public SubstitutionProposee setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; return this; }

    public String getCipPropose() { return cipPropose; }
    public SubstitutionProposee setCipPropose(String cipPropose) { this.cipPropose = cipPropose; return this; }

    public String getDesignation() { return designation; }
    public SubstitutionProposee setDesignation(String designation) { this.designation = designation; return this; }

    public String getTypeCodification() { return typeCodification; }
    public SubstitutionProposee setTypeCodification(String typeCodification) { this.typeCodification = typeCodification; return this; }

    public int getQuantite() { return quantite; }
    public SubstitutionProposee setQuantite(int quantite) { this.quantite = quantite; return this; }

    public SubstitutionStatut getStatut() { return statut; }
    public SubstitutionProposee setStatut(SubstitutionStatut statut) { this.statut = statut; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
