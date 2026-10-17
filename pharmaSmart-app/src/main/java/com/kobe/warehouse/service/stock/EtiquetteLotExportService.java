package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import java.util.List;

/**
 * Deuxième type d'étiquette, à côté de l'étiquette standard en Code 39 : elle porte le lot, la
 * péremption et un GS1 DataMatrix réencodant le code du catalogue, pour que les scans suivants
 * lisent une étiquette que l'application maîtrise.
 */
public interface EtiquetteLotExportService {
    /** Une étiquette par lot reçu sur le bon de livraison, au format de planche par défaut. */
    byte[] exporterPourBon(List<OrderLine> orderLines, int startAt);

    /** Idem, sur le format de planche demandé. */
    byte[] exporterPourBon(List<OrderLine> orderLines, int startAt, FormatEtiquette format);

    /** Étiquettes d'un lot précis du catalogue, répétées {@code qty} fois. */
    byte[] exporterPourLot(Integer lotId, int qty, int startAt);

    /** Idem, sur le format de planche demandé. */
    byte[] exporterPourLot(Integer lotId, int qty, int startAt, FormatEtiquette format);

    /** Flux ZPL des étiquettes de lot d'un bon, pour une imprimante à rouleau pilotée en direct. */
    String genererZplPourBon(List<OrderLine> orderLines, FormatEtiquette format, ResolutionEtiquette resolution);

    /** Idem pour un lot précis, répété {@code qty} fois. */
    String genererZplPourLot(Integer lotId, int qty, FormatEtiquette format, ResolutionEtiquette resolution);
}
