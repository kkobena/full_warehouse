package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Résultat de la pré-résolution d'un lot avant d'ouvrir le dialogue "Retour fournisseur".
 * Permet au frontend d'afficher le bon état dès le premier rendu (sans flash de chargement).
 */
public class RetourBonLotResolutionDTO {

    public enum ResolutionStatut {
        /** Lot rattaché à une commande → retour standard pré-rempli */
        COMMANDE_TROUVEE,
        /** Lot sans orderLine, 1 seul fournisseur associé au produit → retour hors commande auto */
        HORS_COMMANDE_UN_FOURN,
        /** Lot sans orderLine, plusieurs fournisseurs possibles → l'utilisateur doit choisir */
        HORS_COMMANDE_MULTI,
        /** Lot sans orderLine et aucun fournisseur associé → action bloquante */
        FOURNISSEUR_INCONNU
    }

    public record FournisseurSimple(Integer id, String libelle) {}

    private ResolutionStatut statut;

    // Renseignés si statut = COMMANDE_TROUVEE
    private Integer commandeId;
    private LocalDate commandeOrderDate;
    private String commandeReference;

    // Renseignés si statut = HORS_COMMANDE_UN_FOURN
    private Integer fournisseurId;
    private String fournisseurLibelle;

    // Renseigné si statut = HORS_COMMANDE_MULTI
    private List<FournisseurSimple> fournisseurs;

    public RetourBonLotResolutionDTO() {}

    /** Constructeur — commande trouvée */
    public static RetourBonLotResolutionDTO commandeTrouvee(Integer commandeId, LocalDate commandeOrderDate, String reference) {
        RetourBonLotResolutionDTO dto = new RetourBonLotResolutionDTO();
        dto.statut = ResolutionStatut.COMMANDE_TROUVEE;
        dto.commandeId = commandeId;
        dto.commandeOrderDate = commandeOrderDate;
        dto.commandeReference = reference;
        return dto;
    }

    /** Constructeur — hors commande, un seul fournisseur */
    public static RetourBonLotResolutionDTO horsCommandeUnFournisseur(Integer fournisseurId, String fournisseurLibelle) {
        RetourBonLotResolutionDTO dto = new RetourBonLotResolutionDTO();
        dto.statut = ResolutionStatut.HORS_COMMANDE_UN_FOURN;
        dto.fournisseurId = fournisseurId;
        dto.fournisseurLibelle = fournisseurLibelle;
        return dto;
    }

    /** Constructeur — hors commande, plusieurs fournisseurs */
    public static RetourBonLotResolutionDTO horsCommandeMultiFournisseurs(List<FournisseurSimple> fournisseurs) {
        RetourBonLotResolutionDTO dto = new RetourBonLotResolutionDTO();
        dto.statut = ResolutionStatut.HORS_COMMANDE_MULTI;
        dto.fournisseurs = fournisseurs;
        return dto;
    }

    /** Constructeur — fournisseur inconnu */
    public static RetourBonLotResolutionDTO fournisseurInconnu() {
        RetourBonLotResolutionDTO dto = new RetourBonLotResolutionDTO();
        dto.statut = ResolutionStatut.FOURNISSEUR_INCONNU;
        return dto;
    }

    public ResolutionStatut getStatut() { return statut; }
    public Integer getCommandeId() { return commandeId; }
    public LocalDate getCommandeOrderDate() { return commandeOrderDate; }
    public String getCommandeReference() { return commandeReference; }
    public Integer getFournisseurId() { return fournisseurId; }
    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public List<FournisseurSimple> getFournisseurs() { return fournisseurs; }
}

