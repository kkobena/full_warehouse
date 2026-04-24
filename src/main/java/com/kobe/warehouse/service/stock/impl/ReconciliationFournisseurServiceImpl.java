package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.ReconciliationFactureFournisseur;
import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.dto.ReconciliationFactureDTO;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReconciliationFournisseurServiceImpl implements ReconciliationFournisseurService {

    private final CommandeRepository commandeRepository;

    public ReconciliationFournisseurServiceImpl(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    @Override
    public ReconciliationFactureDTO save(CommandeId commandeId, ReconciliationCommand cmd) {
        Commande commande = commandeRepository.findById(commandeId)
            .orElseThrow(() -> new EntityNotFoundException("Commande introuvable : " + commandeId));

        ReconciliationFactureFournisseur recon = commande.getReconciliation();
        if (recon == null) {
            recon = new ReconciliationFactureFournisseur();
        } else {
            recon.setUpdatedAt(LocalDateTime.now());
        }

        int blHT = commande.getGrossAmount() != null ? commande.getGrossAmount() : 0;
        int blTVA = commande.getTaxAmount();
        int facHT = cmd.factureMontantHT() != null ? cmd.factureMontantHT() : 0;
        int facTVA = cmd.factureTVA() != null ? cmd.factureTVA() : 0;
        int ecartHT = facHT - blHT;
        int ecartTVA = facTVA - blTVA;

        recon.setFactureReference(cmd.factureReference())
            .setFactureDate(cmd.factureDate())
            .setFactureMontantHT(facHT)
            .setFactureTVA(facTVA)
            .setBlMontantHT(blHT)
            .setBlTVA(blTVA)
            .setEcartHT(ecartHT)
            .setEcartTVA(ecartTVA)
            .setStatut(ecartHT == 0 && ecartTVA == 0 ? ReconciliationStatut.RECONCILIEE : ReconciliationStatut.ECART);

        commande.setReconciliation(recon);
        commandeRepository.save(commande);
        return new ReconciliationFactureDTO(recon);
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationFactureDTO findByCommandeId(CommandeId commandeId) {
        Commande commande = commandeRepository.findById(commandeId)
            .orElseThrow(() -> new EntityNotFoundException("Commande introuvable : " + commandeId));
        ReconciliationFactureFournisseur recon = commande.getReconciliation();
        return recon != null ? new ReconciliationFactureDTO(recon) : null;
    }
}
