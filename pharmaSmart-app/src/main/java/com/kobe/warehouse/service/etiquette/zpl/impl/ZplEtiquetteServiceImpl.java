package com.kobe.warehouse.service.etiquette.zpl.impl;

import static com.kobe.warehouse.service.etiquette.zpl.ZplFormatage.assainir;
import static com.kobe.warehouse.service.etiquette.zpl.ZplFormatage.construireDonneeDuCode;
import static com.kobe.warehouse.service.etiquette.zpl.ZplFormatage.convertirEnDots;
import static com.kobe.warehouse.service.etiquette.zpl.ZplFormatage.convertirTrameEnDonnee;

import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.domain.enumeration.TypeEtiquette;
import com.kobe.warehouse.service.dto.EtiquetteDTO;
import com.kobe.warehouse.service.etiquette.zpl.ZplEtiquetteService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Écrit le flux ZPL d'une série d'étiquettes de rouleau.
 *
 * <p>Deux règles portent tout le reste :
 *
 * <ol>
 *   <li><b>Une étiquette, un bloc {@code ^XA … ^XZ}</b>, et la page déclarée à chaque bloc
 *       ({@code ^PW}, {@code ^LL}) : l'imprimante garde en mémoire la dernière configuration
 *       reçue, et une étiquette imprimée après un travail d'un autre format sortirait à la
 *       longueur de celui-ci.
 *   <li><b>Le Code 39 n'est tracé que s'il tient</b> à 0,25 mm de module. Sinon c'est un
 *       DataMatrix du code qui est posé : sur un 30 × 20, un Code 39 de dix caractères
 *       demanderait 39 mm de large. Un code-barres illisible ne vaut pas mieux qu'un symbole 2D.
 * </ol>
 */
@Service
public class ZplEtiquetteServiceImpl implements ZplEtiquetteService {

    /** Marge intérieure, la même que le gabarit PDF du rouleau. */
    private static final double MARGE_MM = 1.0;

    /** Hauteur des lignes de texte : l'équivalent du 8 px du gabarit PDF. */
    private static final double HAUTEUR_TEXTE_MM = 2.6;

    /** Le prix, un cran plus gros : c'est ce que le client lit. */
    private static final double HAUTEUR_PRIX_MM = 3.2;

    private static final double INTERLIGNE_MM = 0.4;

    /** Côté du DataMatrix, comme sur planche : 0,55 mm par module sur une trame 20 × 20. */
    private static final double SYMBOLE_MM = 11.0;

    private static final double MODULE_DATAMATRIX_MM = 0.55;

    /** Blanc entre le symbole et la colonne de texte qui l'accompagne. */
    private static final double ECART_SYMBOLE_TEXTE_MM = 1.0;

    /** Plancher de lisibilité du module d'un Code 39, au-delà du minimum GS1 de 0,19 mm. */
    private static final double MODULE_CODE39_MINI_MM = 0.25;

    /**
     * Largeur d'un caractère Code 39, en modules, au rapport 2 : six éléments étroits, trois
     * larges, plus la séparation inter-caractère.
     */
    private static final int MODULES_PAR_CARACTERE_CODE39 = 13;

    /** Le Code 39 encadre la donnée de deux caractères de service. */
    private static final int CARACTERES_DE_SERVICE_CODE39 = 2;

    /** En deçà, le nom de l'officine pousse le prix hors de la découpe — règle du gabarit PDF. */
    private static final double HAUTEUR_MAGASIN_MINI_MM = 25.0;

    /** Le prix d'une réglette se lit debout devant le rayon : c'est ce qu'on y cherche. */
    private static final double HAUTEUR_PRIX_GONDOLE_MM = 6.0;

    /** Le code d'une réglette est une vérification, pas une information : il reste discret. */
    private static final double HAUTEUR_CODE_DISCRET_MM = 2.2;

    /** Au-delà, un texte d'ordonnance déborderait : l'imprimante coupe le bloc. */
    private static final int LIGNES_MAX_BLOC = 4;

    /** Largeur d'un caractère, en part de la hauteur de police : de quoi estimer les lignes d'un bloc. */
    private static final double LARGEUR_CARACTERE = 0.55;

    @Override
    public String rendreEtiquettes(
        List<EtiquetteDTO> etiquettes,
        TypeEtiquette type,
        FormatEtiquette format,
        ResolutionEtiquette resolution
    ) {
        StringBuilder flux = new StringBuilder();
        for (EtiquetteDTO etiquette : etiquettes) {
            // Les cases blanches d'une planche entamée n'ont pas d'équivalent en rouleau : les
            // imprimer ferait dérouler du consommable vierge.
            if (etiquette.isPrint()) {
                ecrireEtiquette(flux, etiquette, type, format, resolution.getDpi());
            }
        }
        return flux.toString();
    }

    private void ecrireEtiquette(StringBuilder flux, EtiquetteDTO etiquette, TypeEtiquette type, FormatEtiquette format, int dpi) {
        flux.append("^XA\n");
        // ^CI28 : UTF-8. Sans lui, un libellé accentué sort en caractères de remplacement.
        flux.append("^CI28\n");
        flux.append("^PW").append(convertirEnDots(format.getLargeurMm(), dpi)).append('\n');
        flux.append("^LL").append(convertirEnDots(format.getHauteurMm(), dpi)).append('\n');
        flux.append("^LH0,0\n");

        switch (type) {
            case REGLETTE -> ecrireReglette(flux, etiquette, format, dpi);
            case POSOLOGIE -> ecrirePosologie(flux, etiquette, format, dpi);
            case PREPARATION -> ecrirePreparation(flux, etiquette, format, dpi);
            default -> ecrireEtiquetteArticle(flux, etiquette, format, dpi);
        }
        flux.append("^XZ\n");
    }

    /** Étiquette posée sur la boîte : standard en Code 39, ou lot en GS1 DataMatrix. */
    private void ecrireEtiquetteArticle(StringBuilder flux, EtiquetteDTO etiquette, FormatEtiquette format, int dpi) {
        double y = MARGE_MM;
        if (format.getHauteurMm() >= HAUTEUR_MAGASIN_MINI_MM) {
            y = ecrireLigne(flux, etiquette.getMagasin(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        }
        y = ecrireLigne(flux, etiquette.getLibelle(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);

        if (StringUtils.hasText(etiquette.getCodeDataMatrix())) {
            ecrireDataMatrix(flux, etiquette, convertirTrameEnDonnee(etiquette.getCodeDataMatrix()), y, dpi);
        } else if (code39Tient(etiquette.getCode(), format, dpi)) {
            ecrireCode39(flux, etiquette, format, y, dpi);
        } else {
            ecrireDataMatrix(flux, etiquette, construireDonneeDuCode(etiquette.getCode()), y, dpi);
        }
    }

    /** Réglette de gondole : le prix en gros, le code en petit, pas de lot. */
    private void ecrireReglette(StringBuilder flux, EtiquetteDTO etiquette, FormatEtiquette format, int dpi) {
        double y = ecrireLigne(flux, etiquette.getLibelle(), MARGE_MM, MARGE_MM, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireLigne(flux, etiquette.getPrix(), MARGE_MM, y, HAUTEUR_PRIX_GONDOLE_MM, dpi);
        if (code39Tient(etiquette.getCode(), format, dpi)) {
            ecrireCode39(flux, etiquette, format, y, dpi);
        } else {
            // Le code n'est qu'une vérification au rayon : s'il ne tient pas en barres, il reste
            // en clair plutôt que d'encombrer la réglette d'un symbole 2D.
            ecrireLigne(flux, etiquette.getCode(), MARGE_MM, y, HAUTEUR_CODE_DISCRET_MM, dpi);
        }
    }

    /** Posologie remise au patient : ni prix, ni code-barres — c'est une consigne de prise. */
    private void ecrirePosologie(StringBuilder flux, EtiquetteDTO etiquette, FormatEtiquette format, int dpi) {
        double y = ecrireLigne(flux, etiquette.getPatient(), MARGE_MM, MARGE_MM, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireLigne(flux, etiquette.getLibelle(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireBloc(flux, etiquette.getPosologie(), y, format, dpi);
        y = ecrireLigne(flux, etiquette.getDuree(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireLigne(flux, etiquette.getPrescripteur(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        ecrireLigne(flux, etiquette.getMagasin(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
    }

    /** Préparation magistrale : composition et dates, le contenu que rien d'autre ne porte. */
    private void ecrirePreparation(StringBuilder flux, EtiquetteDTO etiquette, FormatEtiquette format, int dpi) {
        double y = ecrireLigne(flux, etiquette.getLibelle(), MARGE_MM, MARGE_MM, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireBloc(flux, etiquette.getComposition(), y, format, dpi);
        y = ecrireLigne(flux, etiquette.getPatient(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        y = ecrireBloc(flux, etiquette.getPosologie(), y, format, dpi);
        if (StringUtils.hasText(etiquette.getDate())) {
            y = ecrireLigne(flux, "Préparé le " + etiquette.getDate(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        }
        if (StringUtils.hasText(etiquette.getExpiryDate())) {
            y = ecrireLigne(flux, "À utiliser avant le " + etiquette.getExpiryDate(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        }
        y = ecrireLigne(flux, etiquette.getPrescripteur(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
        ecrireLigne(flux, etiquette.getMention(), MARGE_MM, y, HAUTEUR_TEXTE_MM, dpi);
    }

    /**
     * Écrit un texte sur plusieurs lignes et rend l'ordonnée de la suivante.
     *
     * <p>Une posologie ou une composition ne tient pas sur une ligne, et ZPL ne replie rien tout
     * seul : {@code ^FB} déclare la largeur du bloc et le nombre de lignes au-delà duquel le
     * texte est coupé — mieux vaut couper que déborder sur l'étiquette suivante.
     */
    private double ecrireBloc(StringBuilder flux, String texte, double y, FormatEtiquette format, int dpi) {
        if (!StringUtils.hasText(texte)) {
            return y;
        }
        double largeurMm = format.getLargeurMm() - 2 * MARGE_MM;
        int lignes = compterLignes(texte, largeurMm);
        flux
            .append("^FO")
            .append(convertirEnDots(MARGE_MM, dpi))
            .append(',')
            .append(convertirEnDots(y, dpi))
            .append("^A0N,")
            .append(convertirEnDots(HAUTEUR_TEXTE_MM, dpi))
            .append(',')
            .append(convertirEnDots(HAUTEUR_TEXTE_MM, dpi))
            .append("^FB")
            .append(convertirEnDots(largeurMm, dpi))
            .append(',')
            .append(lignes)
            .append(",0,L,0^FD")
            .append(assainir(texte))
            .append("^FS\n");
        return y + lignes * (HAUTEUR_TEXTE_MM + INTERLIGNE_MM);
    }

    /** Nombre de lignes qu'occupera un texte, pour savoir où écrire la suite. */
    private int compterLignes(String texte, double largeurMm) {
        double largeurCaractereMm = HAUTEUR_TEXTE_MM * LARGEUR_CARACTERE;
        int caracteresParLigne = Math.max(1, (int) (largeurMm / largeurCaractereMm));
        int lignes = (texte.length() + caracteresParLigne - 1) / caracteresParLigne;
        return Math.clamp(lignes, 1, LIGNES_MAX_BLOC);
    }

    /** Écrit une ligne de texte et rend l'ordonnée de la suivante. */
    private double ecrireLigne(StringBuilder flux, String texte, double x, double y, double hauteurMm, int dpi) {
        if (!StringUtils.hasText(texte)) {
            return y;
        }
        int hauteurDots = convertirEnDots(hauteurMm, dpi);
        flux
            .append("^FO")
            .append(convertirEnDots(x, dpi))
            .append(',')
            .append(convertirEnDots(y, dpi))
            .append("^A0N,")
            .append(hauteurDots)
            .append(',')
            .append(hauteurDots)
            .append("^FD")
            .append(assainir(texte))
            .append("^FS\n");
        return y + hauteurMm + INTERLIGNE_MM;
    }

    /** Symbole 2D à gauche, informations du lot à sa droite — la mise en page du gabarit PDF. */
    private void ecrireDataMatrix(StringBuilder flux, EtiquetteDTO etiquette, String donnee, double y, int dpi) {
        flux
            .append("^FO")
            .append(convertirEnDots(MARGE_MM, dpi))
            .append(',')
            .append(convertirEnDots(y, dpi))
            // ^BX : module, qualité 200 (ECC 200), puis le caractère d'échappement qui porte les
            // FNC1 de la trame GS1, et le rapport 1 pour un symbole carré.
            .append("^BXN,")
            .append(Math.max(2, convertirEnDots(MODULE_DATAMATRIX_MM, dpi)))
            .append(",200,,,,_,1^FD")
            .append(donnee)
            .append("^FS\n");

        double x = MARGE_MM + SYMBOLE_MM + ECART_SYMBOLE_TEXTE_MM;
        double ligne = y;
        ligne = ecrireLigne(flux, etiquette.getCode(), x, ligne, HAUTEUR_TEXTE_MM, dpi);
        if (StringUtils.hasText(etiquette.getNumLot())) {
            ligne = ecrireLigne(flux, "Lot " + etiquette.getNumLot(), x, ligne, HAUTEUR_TEXTE_MM, dpi);
        }
        if (StringUtils.hasText(etiquette.getExpiryDate())) {
            ligne = ecrireLigne(flux, "Exp " + etiquette.getExpiryDate(), x, ligne, HAUTEUR_TEXTE_MM, dpi);
        }
        ecrireLigne(flux, etiquette.getPrix(), x, ligne, HAUTEUR_PRIX_MM, dpi);
    }

    /** Code 39 pleine largeur, puis le code en clair et le prix — l'étiquette standard. */
    private void ecrireCode39(StringBuilder flux, EtiquetteDTO etiquette, FormatEtiquette format, double y, int dpi) {
        int moduleDots = moduleCode39EnDots(dpi);
        flux
            .append("^FO")
            .append(convertirEnDots(MARGE_MM, dpi))
            .append(',')
            .append(convertirEnDots(y, dpi))
            .append("^BY")
            .append(moduleDots)
            .append(",2.0^B3N,N,")
            .append(convertirEnDots(format.getHauteurCodeBarresMm(), dpi))
            // Pas de ligne d'interprétation : le code est écrit juste en dessous, à notre police.
            .append(",N,N^FD")
            .append(assainir(etiquette.getCode()))
            .append("^FS\n");

        double ligne = y + format.getHauteurCodeBarresMm() + INTERLIGNE_MM;
        ligne = ecrireLigne(flux, etiquette.getCode(), MARGE_MM, ligne, HAUTEUR_TEXTE_MM, dpi);
        ecrireLigne(flux, etiquette.getPrix(), MARGE_MM, ligne, HAUTEUR_PRIX_MM, dpi);
    }

    /** Le Code 39 du code tient-il dans la laize, au module minimal ? */
    private boolean code39Tient(String code, FormatEtiquette format, int dpi) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        double module = moduleCode39EnDots(dpi) * 25.4 / dpi;
        int modules = (code.length() + CARACTERES_DE_SERVICE_CODE39) * MODULES_PAR_CARACTERE_CODE39;
        return modules * module <= format.getLargeurCodeBarresMm();
    }

    /** Module du Code 39 : le plancher de lisibilité, arrondi au point de la tête d'impression. */
    private int moduleCode39EnDots(int dpi) {
        return Math.max(2, convertirEnDots(MODULE_CODE39_MINI_MM, dpi));
    }
}
