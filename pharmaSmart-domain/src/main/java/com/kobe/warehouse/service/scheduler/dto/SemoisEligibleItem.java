package com.kobe.warehouse.service.scheduler.dto;

import java.time.LocalDateTime;

public record SemoisEligibleItem(
    Integer produitId,
    Integer fpPrincipalId,
    Integer fournisseurId,
    Integer qteColis,
    Integer qteMinimaleCommande,
    long totalStock,
    Integer stockObjectifCalcule,
    Integer qtySeuilMini,
    Integer qtyAppro,
    LocalDateTime exclusionDate,
    Integer exclusionDureeJours
) {
    /**
     * Constructeur auxiliaire pour Hibernate JPQL : SUM(Integer) retourne Long (spec JPA).
     * Hibernate appelle ce constructeur via réflexion ; il délègue au constructeur canonique
     * en convertissant Long → long.
     */
    public SemoisEligibleItem(
        Integer produitId, Integer fpPrincipalId, Integer fournisseurId,
        Integer qteColis, Integer qteMinimaleCommande, Long totalStock,
        Integer stockObjectifCalcule, Integer qtySeuilMini, Integer qtyAppro,
        LocalDateTime exclusionDate, Integer exclusionDureeJours
    ) {
        this(produitId, fpPrincipalId, fournisseurId, qteColis, qteMinimaleCommande,
            totalStock != null ? totalStock : 0L,
            stockObjectifCalcule, qtySeuilMini, qtyAppro, exclusionDate, exclusionDureeJours);
    }

    public int appliquerColisage(int qty) {
        int colis = (qteColis != null && qteColis > 1) ? qteColis : 1;
        int minimum = (qteMinimaleCommande != null && qteMinimaleCommande > 0) ? qteMinimaleCommande : 0;
        int arrondi = colis == 1 ? qty : (int) Math.ceil((double) qty / colis) * colis;
        return Math.max(Math.max(1, arrondi), minimum);
    }

    public boolean isExcluActif() {
        return exclusionDate != null
            && LocalDateTime.now().isBefore(
                exclusionDate.plusDays(exclusionDureeJours != null ? exclusionDureeJours : 30));
    }
}
