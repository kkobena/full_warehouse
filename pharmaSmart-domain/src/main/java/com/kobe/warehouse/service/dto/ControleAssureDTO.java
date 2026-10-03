package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * Contrôle anticipé de la saisie d'un assuré, avant l'enregistrement.
 *
 * @param numeroDejaUtilise  le numéro de carte est déjà porté, pour cet organisme, par un autre dossier
 * @param titulaireDuNumero  nom du dossier qui porte ce numéro, pour que le caissier tranche (carte présentée deux fois, ou fraude)
 * @param dossierExistant     un client porte déjà ces nom, prénom ET numéro de matricule (quel que soit l'organisme) : la saisie est bloquée
 * @param titulaireDuDossier nom du dossier existant
 * @param homonymes          dossiers de même nom et prénom : un AVERTISSEMENT, jamais un blocage — deux personnes peuvent porter le même nom
 */
public record ControleAssureDTO(
    boolean numeroDejaUtilise,
    String titulaireDuNumero,
    boolean dossierExistant,
    String titulaireDuDossier,
    List<String> homonymes
) {}
