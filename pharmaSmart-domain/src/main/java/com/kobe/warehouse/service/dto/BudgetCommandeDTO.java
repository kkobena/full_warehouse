package com.kobe.warehouse.service.dto;

/**
 * DTO pour le contrôle budgétaire des suggestions de commande.
 *
 * @param budgetMensuel      Budget mensuel paramétré (0 = illimité)
 * @param montantEstime      Montant total estimé des suggestions actives
 * @param montantCommande    Montant déjà commandé ce mois (commandes REQUESTED + RECEIVED)
 * @param budgetRestant      budgetMensuel - montantCommande (négatif si dépassement)
 * @param enDepassement      true si montantEstime + montantCommande > budgetMensuel (et budgetMensuel > 0)
 * @param budgetIllimite     true si budgetMensuel == 0
 */
public record BudgetCommandeDTO(
    long budgetMensuel,
    long montantEstime,
    long montantCommande,
    long budgetRestant,
    boolean enDepassement,
    boolean budgetIllimite
) {}
