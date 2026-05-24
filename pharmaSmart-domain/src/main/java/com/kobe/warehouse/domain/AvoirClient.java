package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "avoir_client")
public class AvoirClient implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "reference", length = 30, unique = true, nullable = false)
    private String reference;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "cloture_le")
    private LocalDateTime clotureLe;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 15)
    private AvoirClientStatut statut = AvoirClientStatut.OUVERT;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_cloture", length = 30)
    private ModeClotureAvoir modeCloture;

    @NotNull
    @Column(name = "quantite", nullable = false)
    private int quantite;

    @NotNull
    @Column(name = "montant", nullable = false)
    private int montant;

    @Column(name = "montant_utilise", nullable = false, columnDefinition = "int default 0")
    private int montantUtilise = 0;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "sales_line_id", referencedColumnName = "id"),
        @JoinColumn(name = "sales_line_date", referencedColumnName = "sale_date")
    })
    private SalesLine salesLine;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "commande_id", referencedColumnName = "id"),
        @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date")
    })
    private Commande commande;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @ManyToOne
    @JoinColumn(name = "closed_by_id")
    private AppUser closedBy;

    public Integer getId() { return id; }
    public AvoirClient setId(Integer id) { this.id = id; return this; }

    public String getReference() { return reference; }
    public AvoirClient setReference(String reference) { this.reference = reference; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public AvoirClient setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public LocalDateTime getClotureLe() { return clotureLe; }
    public AvoirClient setClotureLe(LocalDateTime clotureLe) { this.clotureLe = clotureLe; return this; }

    public AvoirClientStatut getStatut() { return statut; }
    public AvoirClient setStatut(AvoirClientStatut statut) { this.statut = statut; return this; }

    public ModeClotureAvoir getModeCloture() { return modeCloture; }
    public AvoirClient setModeCloture(ModeClotureAvoir modeCloture) { this.modeCloture = modeCloture; return this; }

    public int getQuantite() { return quantite; }
    public AvoirClient setQuantite(int quantite) { this.quantite = quantite; return this; }

    public int getMontant() { return montant; }
    public AvoirClient setMontant(int montant) { this.montant = montant; return this; }

    public int getMontantUtilise() { return montantUtilise; }
    public AvoirClient setMontantUtilise(int montantUtilise) { this.montantUtilise = montantUtilise; return this; }

    public int getMontantRestant() { return montant - montantUtilise; }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public AvoirClient setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; return this; }

    public String getCommentaire() { return commentaire; }
    public AvoirClient setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public Customer getCustomer() { return customer; }
    public AvoirClient setCustomer(Customer customer) { this.customer = customer; return this; }

    public Produit getProduit() { return produit; }
    public AvoirClient setProduit(Produit produit) { this.produit = produit; return this; }

    public SalesLine getSalesLine() { return salesLine; }
    public AvoirClient setSalesLine(SalesLine salesLine) { this.salesLine = salesLine; return this; }

    public Commande getCommande() { return commande; }
    public AvoirClient setCommande(Commande commande) { this.commande = commande; return this; }

    public AppUser getCreatedBy() { return createdBy; }
    public AvoirClient setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; return this; }

    public AppUser getClosedBy() { return closedBy; }
    public AvoirClient setClosedBy(AppUser closedBy) { this.closedBy = closedBy; return this; }
}
