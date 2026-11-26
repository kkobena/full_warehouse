package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.CanalEnvoi;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.EnvoiCommandeService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnvoiCommandeServiceImpl implements EnvoiCommandeService {

    private final CommandeRepository commandeRepository;
    private final StorageService storageService;

    public EnvoiCommandeServiceImpl(CommandeRepository commandeRepository, StorageService storageService) {
        this.commandeRepository = commandeRepository;
        this.storageService = storageService;
    }

    @Override
    public CommandeLiteDTO marquerEnvoyee(CommandeId commandeId, CanalEnvoi canal) {
        Commande commande = lireCommandeEnAttente(commandeId);
        marquerEnvoyee(commande, canal);
        return new CommandeLiteDTO(commandeRepository.saveAndFlush(commande));
    }

    @Override
    public void marquerEnvoyee(Commande commande, CanalEnvoi canal) {
        // Idempotent : un second passage ne rajeunit pas l'attente, sans quoi relire le bon
        // effacerait le retard déjà constaté.
        if (commande.estEnvoyee()) {
            return;
        }
        commande.setDateEnvoi(LocalDateTime.now());
        commande.setCanalEnvoi(canal);
        commande.setEnvoiUser(storageService.getUser());
        commande.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public CommandeLiteDTO annulerEnvoi(CommandeId commandeId) {
        Commande commande = lireCommandeEnAttente(commandeId);
        commande.setDateEnvoi(null);
        commande.setCanalEnvoi(null);
        commande.setEnvoiUser(null);
        commande.setUpdatedAt(LocalDateTime.now());
        return new CommandeLiteDTO(commandeRepository.saveAndFlush(commande));
    }

    /**
     * Une commande déjà réceptionnée ou clôturée ne se remet pas en question : son envoi est
     * attesté par la livraison, et le modifier fausserait le délai mesuré pour le grossiste.
     */
    private Commande lireCommandeEnAttente(CommandeId commandeId) {
        Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new GenericError("Commande introuvable", "commandeNotFound"));
        if (commande.getOrderStatus() != OrderStatut.REQUESTED) {
            throw new GenericError(
                    "Une commande au statut " + commande.getOrderStatus() + " ne peut plus changer d'état d'envoi",
                    "commande");
        }
        return commande;
    }
}
