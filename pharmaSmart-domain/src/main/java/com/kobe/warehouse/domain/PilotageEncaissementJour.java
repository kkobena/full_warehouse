package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

/**
 * Encaissements agrégés par jour × type de transaction × mode de paiement × caissier × catégorie.
 *
 * <p>Écrite par la fonction SQL de recalcul (migration V2.1.34), jamais par l'application.
 */
@Entity
@Immutable
@Table(name = "pilotage_encaissement_jour")
public class PilotageEncaissementJour implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "jour")
    private LocalDate jour;

    @Column(name = "type_transaction")
    private String typeTransaction;

    @Column(name = "mode_paiement")
    private String modePaiement;

    @Column(name = "caissier_id")
    private Integer caissierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_ca")
    private CategorieChiffreAffaire categorieChiffreAffaire;

    @Column(name = "montant")
    private long montant;

    @Column(name = "nb_transactions")
    private long nbTransactions;

    public Long getId() {
        return id;
    }

    public LocalDate getJour() {
        return jour;
    }

    public String getTypeTransaction() {
        return typeTransaction;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public Integer getCaissierId() {
        return caissierId;
    }

    public CategorieChiffreAffaire getCategorieChiffreAffaire() {
        return categorieChiffreAffaire;
    }

    public long getMontant() {
        return montant;
    }

    public long getNbTransactions() {
        return nbTransactions;
    }
}
