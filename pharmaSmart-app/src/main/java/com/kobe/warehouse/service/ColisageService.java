package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.service.dto.AjustementColisage;

import java.util.Optional;

/**
 * Oppose les contraintes de conditionnement du fournisseur — colisage (PCB) et quantité minimale
 * de commande — aux quantités que l'officine décide elle-même.
 *
 * <p>La règle de calcul appartient à {@link FournisseurProduit#appliquerColisage(int)} ; ce service
 * l'applique et dit ce qu'il a changé.
 *
 * <p><b>Périmètre.</b> Ne sont concernés que les chemins où la quantité est de notre fait : saisie
 * à l'écran, commande rapide, reprise d'une suggestion, urgents SEMOIS, génération des suggestions.
 * Les quantités venues du grossiste — import de fichier, réponse de commande, bon de livraison —
 * ne sont jamais réécrites : ce sont ses données, et les réécrire ferait diverger notre bon de sa
 * réponse. Le reliquat et la fusion de commandes en sont également exclus, un reliquat de 7
 * manquants ne devant pas faire commander 12.
 */
public interface ColisageService {
    /**
     * Ajuste la quantité demandée de la ligne selon les contraintes de son fournisseur.
     *
     * @return le report de l'ajustement, vide si la quantité était déjà conforme
     */
    Optional<AjustementColisage> ajusterLigne(OrderLine orderLine);

    /** Ajuste une quantité nue, pour les calculs qui ne manipulent pas encore de ligne. */
    int ajusterQuantite(FournisseurProduit fournisseurProduit, int quantiteDemandee);
}
