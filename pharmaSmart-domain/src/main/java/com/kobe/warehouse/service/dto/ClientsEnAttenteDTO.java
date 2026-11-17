package com.kobe.warehouse.service.dto;

/**
 * Nombre de clients en attente d'un produit, pour l'afficher à côté de la ligne qu'on est en train
 * de commander ou de suggérer.
 *
 * <p>Compte <strong>tous</strong> les avoirs ouverts sur le produit, y compris ceux qu'une commande
 * déjà partie couvre : « 3 clients attendent ce produit » reste vrai même si le bon est en route,
 * et c'est ce qui justifie de ne pas diminuer la quantité.
 */
public record ClientsEnAttenteDTO(Integer produitId, long nbClients, long nbDemandes, long quantiteDue) {}
