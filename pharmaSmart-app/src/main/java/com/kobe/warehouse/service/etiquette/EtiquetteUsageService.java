package com.kobe.warehouse.service.etiquette;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.service.dto.etiquette.EtiquettePreparationDTO;

/**
 * Les trois usages qui ne sont pas l'étiquette d'une boîte : la réglette de gondole, la posologie
 * remise au patient et la préparation magistrale.
 *
 * <p>Chacun a son contenu — ce n'est pas une affaire de format. Ils partagent en revanche le
 * gabarit commun des usages et, de là, tout le catalogue de formats et les deux chemins
 * d'impression, PDF et ZPL.
 */
public interface EtiquetteUsageService {
    /** Réglette d'étagère : le prix en gros, le code en petit, pas de lot. */
    byte[] imprimerReglette(Produit produit, int qty, int startAt, FormatEtiquette format);

    String genererZplReglette(Produit produit, int qty, FormatEtiquette format, ResolutionEtiquette resolution);

    /**
     * Posologie d'une ordonnance : une étiquette par ligne, ou une seule si {@code ligneId} est
     * précisé. Patient, prescripteur et instructions viennent de l'ordonnance — rien à ressaisir.
     */
    byte[] imprimerPosologie(Integer ordonnanceId, Integer ligneId, int qty, int startAt, FormatEtiquette format);

    String genererZplPosologie(Integer ordonnanceId, Integer ligneId, int qty, FormatEtiquette format, ResolutionEtiquette resolution);

    /** Préparation magistrale, saisie à l'impression : rien n'est conservé. */
    byte[] imprimerPreparation(EtiquettePreparationDTO saisie, int qty, int startAt, FormatEtiquette format);

    String genererZplPreparation(EtiquettePreparationDTO saisie, int qty, FormatEtiquette format, ResolutionEtiquette resolution);
}
