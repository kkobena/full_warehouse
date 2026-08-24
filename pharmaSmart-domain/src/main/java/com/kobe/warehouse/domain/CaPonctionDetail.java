package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Ce qu'une ponction a retiré à une vente donnée.
 *
 * <p>Cette table est ce qui rend la ponction <strong>justifiable</strong> : elle porte, vente par
 * vente, l'assiette retenue et le montant prélevé. C'est elle qu'imprime le justificatif, et c'est
 * d'elle que l'en-tête tire ses totaux — jamais l'inverse.
 *
 * <p>{@code saleId} et {@code saleDate} sont des colonnes simples plutôt qu'une relation vers
 * {@link Sales} : la clé étrangère est composite et la table cible est partitionnée, si bien qu'une
 * association obligerait Hibernate à charger la vente entière pour afficher une ligne d'historique.
 */
@Entity
@Table(name = "ca_ponction_detail")
@IdClass(CaPonctionDetailId.class)
public class CaPonctionDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ponction_id")
    private CaPonction ponction;

    @Id
    @Column(name = "sale_id")
    private Long saleId;

    @Id
    @Column(name = "sale_date")
    private LocalDate saleDate;

    /** Total de la vente : assiette du plafond par vente. */
    @Column(name = "montant_vente", nullable = false)
    private int montantVente;

    /** Part à TVA 0 de la vente : seule réellement ponctionnable. */
    @Column(name = "montant_base", nullable = false)
    private int montantBase;

    @Column(name = "montant_ponctionne", nullable = false)
    private int montantPonctionne;

    /** Rang dans l'ordre de prélèvement — assiette exonérée décroissante. */
    @Column(name = "rang", nullable = false)
    private int rang;

    /**
     * Référence de la vente, recopiée à la validation.
     *
     * <p>Dénormalisée à dessein : le justificatif est un document figé, qui doit rester lisible sans
     * dépendre d'une jointure sur une table partitionnée — et sans que la vente elle-même ait à être
     * relue des mois plus tard.
     */
    @Column(name = "numero_transaction", length = 20)
    private String numeroTransaction;

    public CaPonction getPonction() {
        return ponction;
    }

    public CaPonctionDetail setPonction(CaPonction ponction) {
        this.ponction = ponction;
        return this;
    }

    public Long getSaleId() {
        return saleId;
    }

    public CaPonctionDetail setSaleId(Long saleId) {
        this.saleId = saleId;
        return this;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public CaPonctionDetail setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
        return this;
    }

    public int getMontantVente() {
        return montantVente;
    }

    public CaPonctionDetail setMontantVente(int montantVente) {
        this.montantVente = montantVente;
        return this;
    }

    public int getMontantBase() {
        return montantBase;
    }

    public CaPonctionDetail setMontantBase(int montantBase) {
        this.montantBase = montantBase;
        return this;
    }

    public int getMontantPonctionne() {
        return montantPonctionne;
    }

    public CaPonctionDetail setMontantPonctionne(int montantPonctionne) {
        this.montantPonctionne = montantPonctionne;
        return this;
    }

    public String getNumeroTransaction() {
        return numeroTransaction;
    }

    public CaPonctionDetail setNumeroTransaction(String numeroTransaction) {
        this.numeroTransaction = numeroTransaction;
        return this;
    }

    public int getRang() {
        return rang;
    }

    public CaPonctionDetail setRang(int rang) {
        this.rang = rang;
        return this;
    }
}
