package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "retour_client")
public class RetourClient implements Serializable {

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

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "motif", nullable = false, length = 30)
    private MotifRetourClient motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_reglement", length = 30)
    private ModeReglementRetour modeReglement;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @NotNull
    @Column(name = "montant_total", nullable = false)
    private int montantTotal;

    @Column(name = "original_sale_id")
    private Long originalSaleId;

    @Column(name = "original_sale_date")
    private LocalDate originalSaleDate;

    @Column(name = "original_sale_ref", length = 50)
    private String originalSaleRef;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @ManyToOne
    @JoinColumn(name = "validated_by_id")
    private AppUser validatedBy;

    @OneToMany(mappedBy = "retourClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RetourClientLine> lines = new ArrayList<>();

    public Integer getId() { return id; }
    public RetourClient setId(Integer id) { this.id = id; return this; }

    public String getReference() { return reference; }
    public RetourClient setReference(String reference) { this.reference = reference; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public RetourClient setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public RetourClient setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; return this; }

    public MotifRetourClient getMotif() { return motif; }
    public RetourClient setMotif(MotifRetourClient motif) { this.motif = motif; return this; }

    public ModeReglementRetour getModeReglement() { return modeReglement; }
    public RetourClient setModeReglement(ModeReglementRetour modeReglement) { this.modeReglement = modeReglement; return this; }

    public String getCommentaire() { return commentaire; }
    public RetourClient setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }

    public int getMontantTotal() { return montantTotal; }
    public RetourClient setMontantTotal(int montantTotal) { this.montantTotal = montantTotal; return this; }

    public Long getOriginalSaleId() { return originalSaleId; }
    public RetourClient setOriginalSaleId(Long originalSaleId) { this.originalSaleId = originalSaleId; return this; }

    public LocalDate getOriginalSaleDate() { return originalSaleDate; }
    public RetourClient setOriginalSaleDate(LocalDate originalSaleDate) { this.originalSaleDate = originalSaleDate; return this; }

    public String getOriginalSaleRef() { return originalSaleRef; }
    public RetourClient setOriginalSaleRef(String originalSaleRef) { this.originalSaleRef = originalSaleRef; return this; }

    public Customer getCustomer() { return customer; }
    public RetourClient setCustomer(Customer customer) { this.customer = customer; return this; }

    public AppUser getCreatedBy() { return createdBy; }
    public RetourClient setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; return this; }

    public AppUser getValidatedBy() { return validatedBy; }
    public RetourClient setValidatedBy(AppUser validatedBy) { this.validatedBy = validatedBy; return this; }

    public List<RetourClientLine> getLines() { return lines; }
    public RetourClient setLines(List<RetourClientLine> lines) { this.lines = lines; return this; }
}
