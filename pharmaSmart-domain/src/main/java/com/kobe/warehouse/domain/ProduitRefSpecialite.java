package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;
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
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Proposition de rapprochement d'un produit du catalogue avec une spécialité du référentiel.
 *
 * <p>Les lignes sont écrites par la fonction SQL {@code ref_rapprocher_produits} ; l'application
 * n'y modifie que la décision ({@link #valider()}, {@link #rejeter()}). Une décision autre
 * qu'EN_ATTENTE n'est plus jamais recalculée.
 */
@Entity
@Table(
    name = "produit_ref_specialite",
    indexes = { @Index(name = "produit_ref_specialite_cis_idx", columnList = "cis") }
)
public class ProduitRefSpecialite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false, unique = true)
    private Produit produit;

    /** Nulle quand le statut est NON_TROUVE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cis")
    private RefSpecialite specialite;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 12)
    private StatutRapprochement statut;

    @Column(name = "score", nullable = false)
    private short score;

    @Column(name = "motif")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private DecisionRapprochement decision = DecisionRapprochement.EN_ATTENTE;

    @Column(name = "calcule_le", nullable = false, insertable = false, updatable = false)
    private LocalDateTime calculeLe;

    @Column(name = "decide_le")
    private LocalDateTime decideLe;

    /** Vrai quand l'acceptation automatique a posé les DCI du produit. Écrit par la base. */
    @Column(name = "dci_posee", nullable = false, insertable = false, updatable = false)
    private boolean dciPosee;

    protected ProduitRefSpecialite() {}

    public boolean isDciPosee() {
        return dciPosee;
    }

    public Integer getId() {
        return id;
    }

    public Produit getProduit() {
        return produit;
    }

    public RefSpecialite getSpecialite() {
        return specialite;
    }

    public StatutRapprochement getStatut() {
        return statut;
    }

    public short getScore() {
        return score;
    }

    public String getMotif() {
        return motif;
    }

    public DecisionRapprochement getDecision() {
        return decision;
    }

    public LocalDateTime getCalculeLe() {
        return calculeLe;
    }

    public LocalDateTime getDecideLe() {
        return decideLe;
    }

    public ProduitRefSpecialite valider() {
        this.decision = DecisionRapprochement.VALIDE;
        this.decideLe = LocalDateTime.now();
        return this;
    }

    public ProduitRefSpecialite rejeter() {
        this.decision = DecisionRapprochement.REJETE;
        this.decideLe = LocalDateTime.now();
        return this;
    }
}
