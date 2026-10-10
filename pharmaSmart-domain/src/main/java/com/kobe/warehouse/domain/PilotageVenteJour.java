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
 * En-têtes de vente clôturées (annulées comprises, marquées), agrégés par jour × heure × magasin × vendeur × caissier ×
 * nature × prescription × catégorie : fréquentation, taux d'annulation, part tiers payant.
 *
 * <p>Écrite par la fonction SQL de recalcul (migration V2.1.34), jamais par l'application.
 */
@Entity
@Immutable
@Table(name = "pilotage_vente_jour")
public class PilotageVenteJour implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "jour")
    private LocalDate jour;

    @Column(name = "heure")
    private short heure;

    @Column(name = "magasin_id")
    private Integer magasinId;

    @Column(name = "vendeur_id")
    private Integer vendeurId;

    @Column(name = "caissier_id")
    private Integer caissierId;

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

    @Column(name = "annulee")
    private boolean annulee;

    @Column(name = "nb_ventes")
    private long nbVentes;

    @Column(name = "montant_ttc")
    private long montantTtc;

    @Column(name = "montant_ht")
    private long montantHt;

    @Column(name = "montant_net")
    private long montantNet;

    @Column(name = "remise")
    private long remise;

    @Column(name = "part_tiers_payant")
    private long partTiersPayant;

    @Column(name = "nb_ventes_client_connu")
    private long nbVentesClientConnu;

    public Long getId() {
        return id;
    }

    public LocalDate getJour() {
        return jour;
    }

    public short getHeure() {
        return heure;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    public Integer getVendeurId() {
        return vendeurId;
    }

    public Integer getCaissierId() {
        return caissierId;
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

    public boolean isAnnulee() {
        return annulee;
    }

    public long getNbVentes() {
        return nbVentes;
    }

    public long getMontantTtc() {
        return montantTtc;
    }

    public long getMontantHt() {
        return montantHt;
    }

    public long getMontantNet() {
        return montantNet;
    }

    public long getRemise() {
        return remise;
    }

    public long getPartTiersPayant() {
        return partTiersPayant;
    }

    public long getNbVentesClientConnu() {
        return nbVentesClientConnu;
    }
}
