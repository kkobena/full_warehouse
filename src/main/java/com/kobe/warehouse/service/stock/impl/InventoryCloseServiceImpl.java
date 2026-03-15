package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.HistoriqueInventaire;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.historique_inventaire.HistoriqueInventaireService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.InventoryCloseService;
import com.kobe.warehouse.service.stock.InventoryClosedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Tuple;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryCloseServiceImpl implements InventoryCloseService {

    private final StoreInventoryLineRepository storeInventoryLineRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final HistoriqueInventaireService historiqueInventaireService;
    private final AppConfigurationService appConfigurationService;
    private final EntityManager em;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryCloseServiceImpl(
        StoreInventoryLineRepository storeInventoryLineRepository,
        StoreInventoryRepository storeInventoryRepository,
        HistoriqueInventaireService historiqueInventaireService,
        AppConfigurationService appConfigurationService,
        EntityManager em,
        ApplicationEventPublisher eventPublisher
    ) {
        this.storeInventoryLineRepository = storeInventoryLineRepository;
        this.storeInventoryRepository = storeInventoryRepository;
        this.historiqueInventaireService = historiqueInventaireService;
        this.appConfigurationService = appConfigurationService;
        this.em = em;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ItemsCountRecord close(Long id) throws InventoryException {
        StoreInventory storeInventory = storeInventoryRepository.getReferenceById(id);
        if (storeInventory.getStatut() == InventoryStatut.CLOSED) {
            return new ItemsCountRecord(0);
        }
        long count = storeInventoryLineRepository.countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(id);
        if (count > 0) {
            throw new InventoryException();
        }
        StoreInventorySummaryRecord summary = fetchSummary(id);
        int rowCount = closeItems(id);
        storeInventory.setStatut(InventoryStatut.CLOSED);
        storeInventory.setUpdatedAt(LocalDateTime.now());
        storeInventory.setInventoryAmountBegin(toLong(summary.amountValueBegin()));
        storeInventory.setInventoryValueCostAfter(toLong(summary.costValueAfter()));
        storeInventory.setInventoryAmountAfter(toLong(summary.amountValueAfter()));
        storeInventory.setInventoryValueCostBegin(toLong(summary.costValueBegin()));
        storeInventory.setGapCost(toInt(summary.gapCost()));
        storeInventory.setGapAmount(toInt(summary.gapAmount()));
        storeInventory = storeInventoryRepository.save(storeInventory);
        this.historiqueInventaireService.save(new HistoriqueInventaire(storeInventory));
        eventPublisher.publishEvent(new InventoryClosedEvent(
            storeInventory.getId(),
            storeInventory.getInventoryCategory(),
            storeInventory.getStorage().getStorageType(),
            storeInventory.getStorage().getId(),
            storeInventory.getStorage().getMagasin().getId(),
            storeInventory.getUser().getId()
        ));
        return new ItemsCountRecord(rowCount);
    }

    private static long toLong(java.math.BigDecimal v) {
        return v != null ? v.longValue() : 0L;
    }

    private static int toInt(java.math.BigDecimal v) {
        return v != null ? v.intValue() : 0;
    }

    private int closeItems(Long id) {
        boolean gestionLot = appConfigurationService.useGestionLotInventaire();
        var spq = em.createStoredProcedureQuery("proc_close_inventory_v2")
            .registerStoredProcedureParameter("p_store_inventory_id", Long.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_gestion_lot", Boolean.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_nombre_ligne", Integer.class, ParameterMode.INOUT)
            .setParameter("p_store_inventory_id", id)
            .setParameter("p_gestion_lot", gestionLot)
            .setParameter("p_nombre_ligne", 0);
        spq.execute();
        return (Integer) spq.getOutputParameterValue("p_nombre_ligne");
    }

    private StoreInventorySummaryRecord fetchSummary(Long id) {
        return StoreInventoryLineFilterBuilder.buildSammary(
            (Tuple) em.createNativeQuery(StoreInventoryLineFilterBuilder.SUMMARY_SQL, Tuple.class)
                .setParameter(1, id)
                .getSingleResult()
        );
    }
}
