package com.kobe.warehouse.service.etiquette.zpl;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import java.util.List;

/**
 * Rend des étiquettes en ZPL, pour les imprimantes qui n'ont pas de pilote exploitable — Zebra,
 * TSC, Godex, Xprinter.
 *
 * <p>Le PDF au format de l'étiquette couvre tout ce qui a un pilote Windows. Le ZPL sert ailleurs,
 * et donne en prime le contrôle du contraste et de la vitesse : l'imprimante trace elle-même les
 * symboles au lieu de recevoir une image.
 *
 * <p>Ne vaut que pour un rouleau : une planche A4 passe par le PDF.
 */
public interface ZplEtiquetteService {
    /**
     * Flux ZPL d'une série d'étiquettes, une étiquette par bloc {@code ^XA … ^XZ}.
     *
     * @param type ce qui est écrit sur l'étiquette : c'est lui, et non le format, qui décide de
     *     la mise en page
     */
    String rendreEtiquettes(
        List<EtiquetteDTO> etiquettes,
        TypeEtiquette type,
        FormatEtiquette format,
        ResolutionEtiquette resolution
    );
}
