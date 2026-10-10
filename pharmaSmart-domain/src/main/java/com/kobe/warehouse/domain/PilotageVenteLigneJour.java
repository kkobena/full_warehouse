package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.OctroiRemise;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
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
 * Lignes de vente clôturées et non annulées, agrégées par jour × produit × magasin × vendeur × nature × prescription ×
 * catégorie. Le coût porte sur la quantité demandée ; le prix d'achat est TTC, le coût HT se déduit au taux de la ligne.
 *
 * <p>Écrite par la fonction SQL de recalcul (migration V2.1.34), jamais par l'application.
 */
@Entity
@Immutable
@Table(name = "pilotage_vente_ligne_jour")
public class PilotageVenteLigneJour implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "jour")
    private LocalDate jour;

    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "magasin_id")
    private Integer magasinId;

    @Column(name = "vendeur_id")
    private Integer vendeurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nature_vente")
    private NatureVente natureVente;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_prescription")
    private TypePrescription typePrescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_ca")
    private CategorieChiffreAffaire categorieChiffreAffaire;

    @Column(name = "taux_remise")
    private short tauxRemise;

    @Enumerated(EnumType.STRING)
    @Column(name = "octroi_remise")
    private OctroiRemise octroiRemise;

    @Column(name = "autorisant_id")
    private Integer autorisantId;

    @Column(name = "quantite_demandee")
    private long quantiteDemandee;

    @Column(name = "quantite_servie")
    private long quantiteServie;

    @Column(name = "quantite_avoir")
    private long quantiteAvoir;

    @Column(name = "montant_ttc")
    private long montantTtc;

    @Column(name = "montant_ht")
    private long montantHt;

    @Column(name = "remise")
    private long remise;

    @Column(name = "cout_ttc")
    private long coutTtc;

    @Column(name = "cout_ht")
    private long coutHt;

    @Column(name = "nb_lignes")
    private long nbLignes;

    public Long getId() {
        return id;
    }

    public LocalDate getJour() {
        return jour;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    public Integer getVendeurId() {
        return vendeurId;
    }

    public NatureVente getNatureVente() {
        return natureVente;
    }

    public TypePrescription getTypePrescription() {
        return typePrescription;
    }

    public CategorieChiffreAffaire getCategorieChiffreAffaire() {
        return categorieChiffreAffaire;
    }

    /** Remise de la vente en % de son montant brut, arrondie ; 0 sans remise, au moins 1 dès qu'il y en a une. */
    public short getTauxRemise() {
        return tauxRemise;
    }

    public OctroiRemise getOctroiRemise() {
        return octroiRemise;
    }

    /** Propriétaire de la clé de sécurité, pour une remise autorisée. */
    public Integer getAutorisantId() {
        return autorisantId;
    }

    public long getQuantiteDemandee() {
        return quantiteDemandee;
    }

    public long getQuantiteServie() {
        return quantiteServie;
    }

    public long getQuantiteAvoir() {
        return quantiteAvoir;
    }

    public long getMontantTtc() {
        return montantTtc;
    }

    public long getMontantHt() {
        return montantHt;
    }

    public long getRemise() {
        return remise;
    }

    public long getCoutTtc() {
        return coutTtc;
    }

    public long getCoutHt() {
        return coutHt;
    }

    public long getNbLignes() {
        return nbLignes;
    }
}
