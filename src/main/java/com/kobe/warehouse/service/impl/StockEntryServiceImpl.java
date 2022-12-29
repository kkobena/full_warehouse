package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.StockEntryService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockEntryServiceImpl implements StockEntryService {
    private final Logger log = LoggerFactory.getLogger(StockEntryServiceImpl.class);
    private final CommandeRepository commandeRepository;

    public StockEntryServiceImpl(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    @Override
    public Commande saveSaisieEntreeStock(CommandeDTO commandeDTO) {
        Commande commande = commandeRepository.getReferenceById(commandeDTO.getId());
        commande.setReceiptDate(commandeDTO.getReceiptDate())
            .setReceiptAmount(commandeDTO.getReceiptAmount())
            .taxAmount(commandeDTO.getTaxAmount())
            .setReceiptRefernce(commandeDTO.getReceiptRefernce())
            .setSequenceBon(commandeDTO.getSequenceBon());
        return commandeRepository.saveAndFlush(commande);
    }

}
