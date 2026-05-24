package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.InventoryLot;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.repository.InventoryLotRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.InventoryLotRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLotLineRecord;
import com.kobe.warehouse.service.stock.InventoryLotService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryLotServiceImpl implements InventoryLotService {

    private final InventoryLotRepository inventoryLotRepository;
    private final StoreInventoryLineRepository storeInventoryLineRepository;
    private final LotRepository lotRepository;
    private final EntityManager em;

    public InventoryLotServiceImpl(
        InventoryLotRepository inventoryLotRepository,
        StoreInventoryLineRepository storeInventoryLineRepository,
        LotRepository lotRepository,
        EntityManager em
    ) {
        this.inventoryLotRepository = inventoryLotRepository;
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.lotRepository = lotRepository;
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLotRecord> findByStoreInventoryLineId(Long storeInventoryLineId) {
        return inventoryLotRepository.findAllByStoreInventoryLineId(storeInventoryLineId)
            .stream()
            .map(this::toRecord)
            .toList();
    }

    @Override
    public InventoryLotRecord save(InventoryLotRecord record) {
        StoreInventoryLine line = storeInventoryLineRepository.getReferenceById(record.storeInventoryLineId());
        Lot lot = resolveLot(record, line);

        InventoryLot entity = new InventoryLot();
        entity.setStoreInventoryLine(line);
        entity.setLot(lot);
        entity.setQuantityOnHand(record.quantityOnHand());
        entity.setQuantityInit(lot.getCurrentQuantity());
        entity.setGap(record.quantityOnHand() != null
            ? record.quantityOnHand() - lot.getCurrentQuantity() : 0);
        entity.setUpdated(record.quantityOnHand() != null);
        entity.setLastUnitPrice(record.lastUnitPrice());
        entity.setUpdatedAt(LocalDateTime.now());

        entity = inventoryLotRepository.saveAndFlush(entity);
        return toRecord(entity);
    }

    /**
     * Résout le lot à partir du record :
     * - Si lotId renseigné : utilise le lot existant (même current_quantity = 0)
     * - Sinon : cherche par numLot, ou crée un nouveau lot si introuvable
     */
    private Lot resolveLot(InventoryLotRecord record, StoreInventoryLine line) {
        if (record.lotId() != null) {
            return lotRepository.getReferenceById(record.lotId());
        }
        if (record.numLot() != null) {
            return lotRepository.findByNumLot(record.numLot())
                .orElseGet(() -> {
                    Lot newLot = new Lot();
                    newLot.setNumLot(record.numLot());
                    newLot.setExpiryDate(record.expiryDate());
                    newLot.setProduit(line.getProduit());
                    newLot.setCurrentQuantity(0);
                    newLot.setStatut(StatutLot.AVAILABLE);
                    return lotRepository.saveAndFlush(newLot);
                });
        }
        throw new IllegalArgumentException("lotId ou numLot obligatoire pour créer un inventory_lot");
    }

    @Override
    public InventoryLotRecord update(InventoryLotRecord record) {
        InventoryLot entity = inventoryLotRepository.getReferenceById(record.id());
        entity.setQuantityOnHand(record.quantityOnHand());
        if (record.quantityOnHand() != null && entity.getQuantityInit() != null) {
            entity.setGap(record.quantityOnHand() - entity.getQuantityInit());
        }
        entity.setUpdated(true);
        entity.setUpdatedAt(LocalDateTime.now());

        entity = inventoryLotRepository.saveAndFlush(entity);

        // Update the parent StoreInventoryLine.quantityOnHand = SUM(inventory_lot.quantity_on_hand)
        syncParentLineQuantity(entity.getStoreInventoryLine().getId());

        return toRecord(entity);
    }

    @Override
    public void delete(Long inventoryLotId) {
        inventoryLotRepository.deleteById(inventoryLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreInventoryLotLineRecord> findLotFlatPage(StoreInventoryLineFilterRecord filter, Pageable pageable) {
        long count = countLotFlat(filter);
        if (count == 0) {
            return Page.empty(pageable);
        }
        List<Tuple> tuples = fetchLotFlatTuples(filter, pageable);
        List<StoreInventoryLotLineRecord> records = tuples.stream().map(this::toLotLineRecord).toList();
        return new PageImpl<>(records, pageable, count);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void syncParentLineQuantity(Long storeInventoryLineId) {
        StoreInventoryLine line = storeInventoryLineRepository.getReferenceById(storeInventoryLineId);
        List<InventoryLot> lots = inventoryLotRepository.findAllByStoreInventoryLineId(storeInventoryLineId);
        int sumQty = lots.stream()
            .filter(l -> l.getQuantityOnHand() != null)
            .mapToInt(InventoryLot::getQuantityOnHand)
            .sum();
        line.setQuantityOnHand(sumQty);
        if (line.getQuantityInit() != null) {
            line.setGap(sumQty - line.getQuantityInit());
        }
        line.setUpdated(true);
        line.setUpdatedAt(LocalDateTime.now());
        storeInventoryLineRepository.saveAndFlush(line);
    }

    private long countLotFlat(StoreInventoryLineFilterRecord filter) {
        try {
            String sql = StoreInventoryLineFilterBuilder.lotQuery(filter).buildCount();
            Object result = em.createNativeQuery(sql)
                .setParameter(1, filter.storeInventoryId())
                .getSingleResult();
            return result instanceof Number n ? n.longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> fetchLotFlatTuples(StoreInventoryLineFilterRecord filter, Pageable pageable) {
        try {
            String sql = StoreInventoryLineFilterBuilder.lotQuery(filter)
                .withAbcPareto(true)
                .buildPage();
            return em.createNativeQuery(sql, Tuple.class)
                .setParameter(1, filter.storeInventoryId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private StoreInventoryLotLineRecord toLotLineRecord(Tuple t) {
        return new StoreInventoryLotLineRecord(
            t.get("id", Long.class),
            t.get("store_inventory_line_id", Long.class),
            t.get("produit_id", Integer.class),
            t.get("code_cip", String.class),
            t.get("libelle", String.class),
            t.get("num_lot", String.class),
            t.get("expiry_date", LocalDate.class),
            t.get("quantity_on_hand", Integer.class),
            t.get("quantity_init", Integer.class),
            t.get("gap", Integer.class),
            Boolean.TRUE.equals(t.get("updated", Boolean.class)),
            t.get("classe_pareto", String.class)
        );
    }


    private InventoryLotRecord toRecord(InventoryLot entity) {
        Lot lot = entity.getLot();
        return new InventoryLotRecord(
            entity.getId(),
            entity.getStoreInventoryLine().getId(),
            lot.getId(),
            lot.getNumLot(),
            lot.getExpiryDate(),
            entity.getQuantityOnHand(),
            entity.getQuantityInit(),
            entity.getGap(),
            Boolean.TRUE.equals(entity.getUpdated()),
            entity.getLastUnitPrice()
        );
    }
}
