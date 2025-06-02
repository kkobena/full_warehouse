package com.kobe.warehouse.service.etiquette;

public abstract class AbstractJava2DReceiptEtiquetteService {

    protected static final int MARGIN = (int) ((0.5 / 2.54) * 72);
    protected static final int E_INTERSECTION_MARGIN = (int) ((0.2 / 2.54) * 72); // 0.5cm soit 14,17pt
    //38 *21,2
    protected static final int E_WIDTH = (int) ((3.8 / 2.54) * 72); // 3.8cm soit 80mm
    protected static final int E_HEIGHT = (int) ((2.12 / 2.54) * 72); // 2.12cm soit 80mm

    protected static final int A4_WIDTH = (int) ((((21 / 2.54) * 72) - (2 * MARGIN)) - (4 * E_INTERSECTION_MARGIN)); // Surface utile largeur = 21 cm − 2 × 0.5 cm = 20 cm
    protected static final int A4_HEIGHT = (int) ((((29.7 / 2.54) * 72) - (2 * MARGIN)) - (12 * E_INTERSECTION_MARGIN)); // Surface utile hauteur = 29.7 cm − 2 × 0.5 cm = 28.7 cm
}
