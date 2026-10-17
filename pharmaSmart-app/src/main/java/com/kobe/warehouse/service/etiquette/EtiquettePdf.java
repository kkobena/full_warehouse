package com.kobe.warehouse.service.etiquette;

import com.kobe.warehouse.service.license.DemoWatermark;
import com.kobe.warehouse.service.pdf.EtiquetteBarcodeReplacedElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextReplacedElementFactory;

/**
 * Rendu PDF d'une planche ou d'un rouleau d'étiquettes.
 *
 * <p>Le même montage pour tous les types d'étiquette : Flying Saucer, plus la fabrique qui
 * remplace les images par un Code 39 ou un GS1 DataMatrix selon ce que porte leur identifiant.
 * Écrit une fois ici plutôt que recopié dans chaque service d'impression.
 */
public final class EtiquettePdf {

    private EtiquettePdf() {}

    /**
     * @param html gabarit déjà rendu
     * @param codesBarres identifiants des images à remplacer par un symbole
     */
    public static byte[] rendre(String html, List<String> codesBarres) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext contexte = renderer.getSharedContext();
            EtiquetteBarcodeReplacedElement fabrique = new EtiquetteBarcodeReplacedElement(
                new ITextReplacedElementFactory(renderer.getOutputDevice())
            );
            fabrique.setBarcodesData(codesBarres);
            contexte.setReplacedElementFactory(fabrique);
            contexte.getTextRenderer().setSmoothingThreshold(0);
            contexte.setPrint(true);
            renderer.setDocumentFromString(DemoWatermark.apply(html));
            renderer.layout();
            renderer.createPDF(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF des étiquettes", e);
        }
    }
}
