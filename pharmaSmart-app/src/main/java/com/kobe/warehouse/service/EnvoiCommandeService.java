package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.CanalEnvoi;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;

/**
 * Point de passage unique par lequel une commande devient « envoyée ».
 *
 * <p>Unique par nécessité : la date d'envoi sert de source à l'encours du réassort, au délai de
 * livraison réel et à l'alerte de retard. Si chaque chemin d'envoi l'écrivait à sa façon — EDI
 * ici, export PDF là, action manuelle ailleurs — ces trois mesures divergeraient selon la façon
 * dont l'officine travaille, et l'écart serait invisible.
 *
 * <p>Le marquage est idempotent : repasser par un envoi déjà tracé ne décale pas la date, sans
 * quoi un second téléchargement du bon rajeunirait l'attente et effacerait le retard.
 */
public interface EnvoiCommandeService {
    /**
     * Horodate l'envoi de la commande, au nom de l'utilisateur courant.
     *
     * @param canal par quel moyen elle est partie
     * @return la commande telle qu'elle est après marquage
     */
    CommandeLiteDTO marquerEnvoyee(CommandeId commandeId, CanalEnvoi canal);

    /**
     * Horodate l'envoi d'une commande déjà chargée, sans repasser par la base. Appelé depuis les
     * chemins qui tiennent déjà l'entité, l'envoi PharmaML en premier.
     */
    void marquerEnvoyee(Commande commande, CanalEnvoi canal);

    /**
     * Retire la trace d'envoi : la commande redevient un brouillon.
     *
     * <p>Nécessaire parce que le marquage peut être déclenché par un simple téléchargement du bon,
     * geste qu'on fait aussi pour relire une commande avant de l'envoyer.
     */
    CommandeLiteDTO annulerEnvoi(CommandeId commandeId);
}
