package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO.StyleSerie;
import com.kobe.warehouse.service.pilotage.GraphiquePilotageService;
import com.kobe.warehouse.service.pilotage.calcul.FormatAxeRapport;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

@Service
public class GraphiquePilotageServiceImpl implements GraphiquePilotageService {

    // L'image est rendue au double de sa taille de dessin pour rester nette à l'impression.
    private static final int ECHELLE = 2;

    private static final Color REALISE = new Color(0x3989c6);
    private static final Color REFERENCE = new Color(0x6b7280);
    private static final Color OBJECTIF = new Color(0xc2410c);
    /** L'accent à 30 % sur blanc. */
    private static final Color FANTOME = new Color(0xc3dcee);
    // Barres classées : la hauteur suit le nombre d'éléments, comme à l'écran.
    private static final int HAUTEUR_PAR_BARRE = 22;
    private static final int MARGES_CLASSEMENT = 70;
    private static final int HAUTEUR_CLASSEMENT_MIN = 260;
    private static final double ETENDUE_MINIMALE = 0.1;
    /** Couleurs de série du thème clair de l'écran, l'accent du rapport en tête. */
    private static final Color[] PALETTE = {
        REALISE,
        new Color(0xc2410c),
        new Color(0xa21caf),
        new Color(0x1d4ed8),
        new Color(0xb45309),
        new Color(0x4338ca),
        new Color(0xb91c1c),
        new Color(0x0e7490),
    };
    private static final Color GRILLE = new Color(0xe5e7eb);
    private static final Font POLICE = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private static final float[] POINTILLE = { 6f, 4f };
    private static final float[] TIRETS = { 2f, 3f };

    @Override
    public String dessinerCourbes(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format) {
        LineAndShapeRenderer traceur = new LineAndShapeRenderer(true, true);
        for (int rang = 0; rang < series.size(); rang++) {
            StyleSerie style = series.get(rang).style();
            traceur.setSeriesPaint(rang, lireCouleur(style, rang));
            traceur.setSeriesStroke(rang, lireTrait(style));
            traceur.setSeriesShapesVisible(rang, style != StyleSerie.OBJECTIF);
        }
        // Comme Chart.js : l'axe suit les valeurs, sans partir de zéro, pour que la tendance se lise.
        return encoder(habiller(tracer(libelles, series, unite, traceur, false), true), format);
    }

    @Override
    public String dessinerBarres(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format) {
        return encoder(habiller(tracerBarres(libelles, series, unite, new BarRenderer()), series.size() > 1), format);
    }

    @Override
    public String dessinerBarresEmpilees(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format) {
        return encoder(habiller(tracerBarres(libelles, series, unite, new StackedBarRenderer()), series.size() > 1), format);
    }

    @Override
    public String dessinerClassement(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite) {
        CategoryPlot trace = tracerBarres(libelles, series, unite, new BarRenderer());
        trace.setOrientation(PlotOrientation.HORIZONTAL);
        int hauteur = Math.max(HAUTEUR_CLASSEMENT_MIN, libelles.size() * HAUTEUR_PAR_BARRE + MARGES_CLASSEMENT);
        return encoder(habiller(trace, true), FormatGraphe.LARGE.getLargeur(), hauteur);
    }

    private static CategoryPlot tracerBarres(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, BarRenderer traceur) {
        traceur.setBarPainter(new StandardBarPainter());
        traceur.setShadowVisible(false);
        traceur.setItemMargin(0.05);
        for (int rang = 0; rang < series.size(); rang++) {
            traceur.setSeriesPaint(rang, lireCouleur(series.get(rang).style(), rang));
        }
        return tracer(libelles, series, unite, traceur, true);
    }

    private static CategoryPlot tracer(
        List<String> libelles,
        List<SerieGraphiqueDTO> series,
        UniteIndicateur unite,
        AbstractCategoryItemRenderer traceur,
        boolean depuisZero
    ) {
        DefaultCategoryDataset donnees = new DefaultCategoryDataset();
        double plusGrande = 0;
        for (SerieGraphiqueDTO serie : series) {
            for (int tranche = 0; tranche < libelles.size(); tranche++) {
                Double valeur = tranche < serie.valeurs().size() ? serie.valeurs().get(tranche) : null;
                donnees.addValue(valeur, serie.libelle(), libelles.get(tranche));
                plusGrande = valeur == null ? plusGrande : Math.max(plusGrande, Math.abs(valeur));
            }
        }
        CategoryAxis axeX = new CategoryAxis();
        axeX.setTickLabelFont(POLICE);
        NumberAxis axeY = new NumberAxis();
        axeY.setTickLabelFont(POLICE);
        axeY.setNumberFormatOverride(new FormatAxeRapport(unite));
        axeY.setAutoRangeIncludesZero(depuisZero);
        // Une courbe plate n'étire pas l'axe sur un écart infime : les graduations s'écriraient toutes pareil.
        axeY.setAutoRangeMinimumSize(Math.max(1, plusGrande * ETENDUE_MINIMALE));
        return new CategoryPlot(donnees, axeX, axeY, traceur);
    }

    private static Color lireCouleur(StyleSerie style, int rang) {
        return switch (style) {
            case REALISE -> REALISE;
            case REFERENCE -> REFERENCE;
            case OBJECTIF -> OBJECTIF;
            case PALETTE, PALETTE_POINTILLE -> PALETTE[rang % PALETTE.length];
            case FANTOME -> FANTOME;
        };
    }

    private static Stroke lireTrait(StyleSerie style) {
        return switch (style) {
            case REALISE, PALETTE, FANTOME -> new BasicStroke(2f);
            case REFERENCE, PALETTE_POINTILLE -> new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, POINTILLE, 0f);
            case OBJECTIF -> new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, TIRETS, 0f);
        };
    }

    /** @param avecLegende faux pour des barres d'une seule série : la légende ne dirait que le titre, l'écran la masque aussi */
    private static JFreeChart habiller(CategoryPlot trace, boolean avecLegende) {
        trace.setBackgroundPaint(Color.WHITE);
        trace.setOutlineVisible(false);
        trace.setRangeGridlinePaint(GRILLE);
        trace.setDomainGridlinesVisible(false);
        JFreeChart graphe = new JFreeChart(null, POLICE, trace, avecLegende);
        graphe.setBackgroundPaint(Color.WHITE);
        if (!avecLegende) {
            return graphe;
        }
        LegendTitle legende = graphe.getLegend();
        legende.setItemFont(POLICE);
        legende.setFrame(BlockBorder.NONE);
        legende.setPosition(RectangleEdge.TOP);
        return graphe;
    }

    private static String encoder(JFreeChart graphe, FormatGraphe format) {
        return encoder(graphe, format.getLargeur(), format.getHauteur());
    }

    private static String encoder(JFreeChart graphe, int largeur, int hauteur) {
        try {
            byte[] png = ChartUtils.encodeAsPNG(graphe.createBufferedImage(largeur * ECHELLE, hauteur * ECHELLE, largeur, hauteur, null));
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
