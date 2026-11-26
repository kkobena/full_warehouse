package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

/**
 * Un produit promis à des clients et qu'aucune commande partie chez le grossiste ne couvre.
 *
 * <p>C'est la question du matin : qu'ai-je promis que je n'ai pas encore commandé ? Le circuit
 * d'avoir client était jusqu'ici purement réactif — il exploitait la réception pour rapprocher les
 * avoirs des produits reçus — sans jamais déclencher ni orienter la commande.
 *
 * @param nbClients nombre de clients <em>identifiés</em> distincts. Un même client qui a demandé
 *     deux fois le produit compte pour un ; les avoirs au porteur, sans client, n'y figurent pas.
 * @param nbDemandes nombre d'avoirs ouverts. Il dépasse {@code nbClients} dès qu'un avoir est au
 *     porteur ou qu'un client a demandé deux fois : les deux chiffres ne mesurent pas la même
 *     chose et l'écart entre eux est une information, pas une incohérence.
 * @param quantiteDue somme des unités restant à servir, déduction faite de ce qui a déjà été remis.
 * @param attenteDepuis date du plus ancien avoir ouvert : c'est elle qui ordonne la liste.
 * @param stockDisponible stock actuel, tous emplacements confondus. <strong>Positif</strong>, il
 *     signale un dû servable tout de suite, sans commande. Il peut être négatif : une vente forcée
 *     sort la quantité demandée même sans stock, et ce négatif est la dette d'avoir elle-même.
 */
public record ProduitDuNonCommandeDTO(
        Integer produitId,
        String produitLibelle,
        String codeEan,
        long nbClients,
        long nbDemandes,
        long quantiteDue,
        LocalDateTime attenteDepuis,
        int stockDisponible) {}
