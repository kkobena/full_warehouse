package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.RetourBonService;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link RetourBon}.
 */
@Service
@Transactional
public class RetourBonServiceImpl implements RetourBonService {

    private final Logger log = LoggerFactory.getLogger(RetourBonServiceImpl.class);

    private final RetourBonRepository retourBonRepository;
    private final RetourBonItemRepository retourBonItemRepository;
    private final CommandeRepository commandeRepository;
    private final OrderLineRepository orderLineRepository;
    private final MotifRetourProduitRepository motifRetourProduitRepository;
    private final UserRepository userRepository;

    public RetourBonServiceImpl(
        RetourBonRepository retourBonRepository,
        RetourBonItemRepository retourBonItemRepository,
        CommandeRepository commandeRepository,
        OrderLineRepository orderLineRepository,
        MotifRetourProduitRepository motifRetourProduitRepository,
        UserRepository userRepository
    ) {
        this.retourBonRepository = retourBonRepository;
        this.retourBonItemRepository = retourBonItemRepository;
        this.commandeRepository = commandeRepository;
        this.orderLineRepository = orderLineRepository;
        this.motifRetourProduitRepository = motifRetourProduitRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RetourBonDTO create(RetourBonDTO retourBonDTO) {
        log.debug("Request to create RetourBon : {}", retourBonDTO);

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.PROCESSING);
        retourBon.setCommentaire(retourBonDTO.getCommentaire());

        // Set user
        AppUser currentUser = userRepository
            .findOneByLogin(SecurityUtils.getCurrentUserLogin().orElseThrow())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        retourBon.setUser(currentUser);

        // Set commande
        if (retourBonDTO.getCommandeId() != null && retourBonDTO.getCommandeOrderDate() != null) {
            CommandeId commandeId = new CommandeId(retourBonDTO.getCommandeId(), retourBonDTO.getCommandeOrderDate());
            Commande commande = commandeRepository
                .findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande not found"));
            retourBon.setCommande(commande);
        }

        retourBon = retourBonRepository.save(retourBon);

        // Create return items
        if (retourBonDTO.getRetourBonItems() != null && !retourBonDTO.getRetourBonItems().isEmpty()) {
            RetourBon finalRetourBon = retourBon;
            List<RetourBonItem> items = retourBonDTO.getRetourBonItems().stream()
                .map(itemDTO -> createRetourBonItem(itemDTO, finalRetourBon))
                .collect(Collectors.toList());
            retourBonItemRepository.saveAll(items);
        }

        return new RetourBonDTO(retourBonRepository.findById(retourBon.getId()).orElseThrow());
    }

    @Override
    public RetourBonDTO update(RetourBonDTO retourBonDTO) {
        log.debug("Request to update RetourBon : {}", retourBonDTO);

        RetourBon retourBon = retourBonRepository
            .findById(retourBonDTO.getId())
            .orElseThrow(() -> new RuntimeException("RetourBon not found"));

        retourBon.setCommentaire(retourBonDTO.getCommentaire());
        if (retourBonDTO.getStatut() != null) {
            retourBon.setStatut(retourBonDTO.getStatut());
        }

        retourBon = retourBonRepository.save(retourBon);

        // Update items
        if (retourBonDTO.getRetourBonItems() != null) {
            // Delete existing items
            retourBonItemRepository.deleteAll(retourBonItemRepository.findAllByRetourBonId(retourBon.getId()));

            // Create new items
            RetourBon finalRetourBon = retourBon;
            List<RetourBonItem> items = retourBonDTO.getRetourBonItems().stream()
                .map(itemDTO -> createRetourBonItem(itemDTO, finalRetourBon))
                .toList();
            retourBonItemRepository.saveAll(items);
        }

        return new RetourBonDTO(retourBonRepository.findById(retourBon.getId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAll(Pageable pageable) {
        log.debug("Request to get all RetourBons");
        return retourBonRepository.findAllByOrderByDateMtvDesc(pageable).map(RetourBonDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAllByStatut(RetourStatut statut, Pageable pageable) {
        log.debug("Request to get all RetourBons by status : {}", statut);
        return retourBonRepository.findAllByStatutOrderByDateMtvDesc(statut, pageable).map(RetourBonDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate) {
        log.debug("Request to get all RetourBons by commande : {}, {}", commandeId, orderDate);
        return retourBonRepository.findAllByCommandeId(commandeId).stream()
            .map(RetourBonDTO::new)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAllByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Request to get all RetourBons by date range : {} - {}", startDate, endDate);
        return retourBonRepository.findAllByDateMtvBetween(startDate, endDate, pageable).map(RetourBonDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetourBonDTO> findOne(Integer id) {
        log.debug("Request to get RetourBon : {}", id);
        return retourBonRepository.findById(id).map(RetourBonDTO::new);
    }

    @Override
    public void delete(Integer id) {
        log.debug("Request to delete RetourBon : {}", id);
        // Delete all items first
        retourBonItemRepository.deleteAll(retourBonItemRepository.findAllByRetourBonId(id));
        // Delete the return document
        retourBonRepository.deleteById(id);
    }

    @Override
    public RetourBonDTO validate(Integer id) {
        log.debug("Request to validate RetourBon : {}", id);
        RetourBon retourBon = retourBonRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("RetourBon not found"));

        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon = retourBonRepository.save(retourBon);

        return new RetourBonDTO(retourBon);
    }

    private RetourBonItem createRetourBonItem(RetourBonItemDTO itemDTO, RetourBon retourBon) {
        RetourBonItem item = new RetourBonItem();
        item.setDateMtv(LocalDateTime.now());
        item.setRetourBon(retourBon);
        item.setQtyMvt(itemDTO.getQtyMvt());
        item.setInitStock(itemDTO.getInitStock());
        item.setAfterStock(itemDTO.getAfterStock());

        // Set motif retour
        if (itemDTO.getMotifRetourId() != null) {
            MotifRetourProduit motif = motifRetourProduitRepository
                .findById(itemDTO.getMotifRetourId())
                .orElseThrow(() -> new RuntimeException("MotifRetourProduit not found"));
            item.setMotifRetour(motif);
        }

        // Set order line
        if (itemDTO.getOrderLineId() != null && itemDTO.getOrderLineOrderDate() != null) {
            OrderLineId orderLineId = new OrderLineId(itemDTO.getOrderLineId(), itemDTO.getOrderLineOrderDate());
            OrderLine orderLine = orderLineRepository
                .findById(orderLineId)
                .orElseThrow(() -> new RuntimeException("OrderLine not found"));
            item.setOrderLine(orderLine);
        }

        return item;
    }
}
