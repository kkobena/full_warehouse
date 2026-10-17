package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.DeliveryTotalsDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface StockEntryDataService {
    Page<DeliveryReceiptDTO> fetchAllReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    List<DeliveryReceiptDTO> fetchAllDeliveryReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    Optional<DeliveryReceiptDTO> findOneById(CommandeId id);

    byte[] printEtiquette(CommandeId id, int startAt, FormatEtiquette format);

    /** Second type d'étiquette : une par lot reçu, avec lot, péremption et GS1 DataMatrix. */
    byte[] printEtiquetteLot(CommandeId id, int startAt, FormatEtiquette format);

    /** Étiquettes standard du bon en ZPL, pour une imprimante à rouleau pilotée en direct. */
    String printEtiquetteZpl(CommandeId id, FormatEtiquette format, ResolutionEtiquette resolution);

    /** Étiquettes « lot » du bon en ZPL. */
    String printEtiquetteLotZpl(CommandeId id, FormatEtiquette format, ResolutionEtiquette resolution);

    byte[] exportToPdf(CommandeId id);

    Slice<DeliveryReceiptProjection> fetchAllReceipts(String searchTerm);

    List<DeliveryReceiptItemProjection> findAllByCommandeIdAndCommandeOrderDate(CommandeId id);

    Page<DeliveryReceiptDTO> fetchAllWithoutDetail(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    DeliveryTotalsDTO computeTotals(DeliveryReceiptFilterDTO filter);

    long countByOrderStatusAndType(OrderStatut statut);
}
