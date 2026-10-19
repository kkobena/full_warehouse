package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.ExportDonnees.ColonneExport;
import com.kobe.warehouse.domain.enumeration.TypeColonneExport;
import com.kobe.warehouse.service.exports.EcrivainExport;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

/**
 * PDF en flux (OpenPDF) : le tableau part au document tous les {@value #LIGNES_PAR_BLOC} lignes, l'en-tête se répète en tête de
 * chaque page, et rien ne reste en mémoire. Paysage, pour que les exports larges tiennent dans la page — au-delà de quelques
 * milliers de lignes, XLSX reste le format à conseiller.
 */
class EcrivainPdf implements EcrivainExport {

    private static final int LIGNES_PAR_BLOC = 200;
    private static final float MARGE = 24f;
    private static final float TAILLE_TITRE = 11f;
    private static final float TAILLE_ENTETE = 7.5f;
    private static final float TAILLE_CELLULE = 7f;
    private static final float PADDING = 3f;
    private static final float EPAISSEUR_BORDURE = 0.3f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<ColonneExport> colonnes;
    private final Document document;
    private final Font policeEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, TAILLE_ENTETE);
    private final Font policeCellule = FontFactory.getFont(FontFactory.HELVETICA, TAILLE_CELLULE);
    private PdfPTable tableau;
    private int lignesDuBloc;
    private boolean blocEnvoye;

    EcrivainPdf(List<ColonneExport> colonnes, String titre, OutputStream sortie) throws IOException {
        this.colonnes = colonnes;
        this.document = new Document(PageSize.A4.rotate(), MARGE, MARGE, MARGE, MARGE);
        try {
            PdfWriter.getInstance(document, sortie);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
        document.open();
        ajouter(new Paragraph(titre, FontFactory.getFont(FontFactory.HELVETICA_BOLD, TAILLE_TITRE)));
        ajouter(new Paragraph(" ", policeCellule));
        ouvrirTableau();
    }

    @Override
    public void ecrire(Object[] ligne) throws IOException {
        if (lignesDuBloc >= LIGNES_PAR_BLOC) {
            envoyerBloc();
        }
        for (int rang = 0; rang < colonnes.size(); rang++) {
            String texte = formater(rang < ligne.length ? ligne[rang] : null);
            tableau.addCell(cellule(texte, policeCellule, alignement(colonnes.get(rang).type())));
        }
        lignesDuBloc++;
    }

    /** Un export vide garde son en-tête et le dit : un tableau sans ligne de corps ne s'imprimerait pas. */
    @Override
    public void close() throws IOException {
        if (lignesDuBloc > 0) {
            envoyerBloc();
        } else if (!blocEnvoye) {
            tableau.setHeaderRows(0);
            ajouter(tableau);
            ajouter(new Paragraph("Aucune ligne pour ces critères.", policeCellule));
        }
        document.close();
    }

    private void ouvrirTableau() {
        tableau = new PdfPTable(colonnes.size());
        tableau.setWidthPercentage(100);
        tableau.setHeaderRows(1);
        for (ColonneExport colonne : colonnes) {
            tableau.addCell(cellule(colonne.libelle(), policeEntete, Element.ALIGN_LEFT));
        }
        lignesDuBloc = 0;
    }

    private void envoyerBloc() throws IOException {
        ajouter(tableau);
        blocEnvoye = true;
        ouvrirTableau();
    }

    private void ajouter(Element element) throws IOException {
        try {
            document.add(element);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    private static PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police));
        cellule.setHorizontalAlignment(alignement);
        cellule.setPadding(PADDING);
        cellule.setBorderWidth(EPAISSEUR_BORDURE);
        cellule.setBorderColor(Color.LIGHT_GRAY);
        return cellule;
    }

    private static int alignement(TypeColonneExport type) {
        return switch (type) {
            case ENTIER, MONTANT, DECIMAL -> Element.ALIGN_RIGHT;
            case DATE, DATE_HEURE -> Element.ALIGN_CENTER;
            case TEXTE -> Element.ALIGN_LEFT;
        };
    }

    /** Mêmes conventions qu'en CSV : dates à la française, virgule décimale, espace fine comme séparateur de milliers. */
    private static String formater(Object valeur) {
        return switch (valeur) {
            case null -> "";
            case LocalDateTime horodatage -> horodatage.format(DATE_HEURE);
            case LocalDate date -> date.format(DATE);
            case BigDecimal nombre -> nombres().format(nombre);
            case Number nombre -> nombres().format(nombre);
            case Boolean booleen -> booleen ? "Oui" : "Non";
            default -> valeur.toString();
        };
    }

    private static DecimalFormat nombres() {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRENCH);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symboles);
    }
}
