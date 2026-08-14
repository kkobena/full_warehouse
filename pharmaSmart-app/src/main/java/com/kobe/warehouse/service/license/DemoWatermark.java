package com.kobe.warehouse.service.license;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

/**
 * Appose un filigrane « DÉMONSTRATION — SANS VALEUR LÉGALE » sur les documents PDF lorsque
 * l'installation tourne sous une licence de démonstration (cf. PLAN-GESTION-LICENCE §3.5).
 *
 * <p>Sans lui, un ticket ou une facture issus d'une démo seraient indiscernables d'un document
 * réel et pourraient être présentés comme justificatifs — c'est précisément ce qui ferait d'une
 * licence `DEMO`, installable partout par construction, le vecteur de fraude idéal.
 *
 */
@Component
public final class DemoWatermark {

    private static final String TEXT = "DÉMONSTRATION — SANS VALEUR LÉGALE";


    private static final String STYLE =
        """
        <style>
          .ps-demo-watermark {
            position: fixed;
            top: 42%;
            left: 0;
            width: 100%;
            text-align: center;
            font-size: 30pt;
            font-weight: bold;
            letter-spacing: 2px;
            color: #d8d8d8;
            transform: rotate(-30deg);
          }
        </style>
        """;

    private static final String MARKUP = "<div class=\"ps-demo-watermark\">" + TEXT + "</div>";

    /** Remplacé au démarrage par le service de licence. Défaut sûr : aucun filigrane. */
    private static volatile BooleanSupplier demoFlag = () -> false;

    public DemoWatermark(LicenseService licenseService) {
        demoFlag = licenseService::isDemo;
    }

    /**
     * @param html document prêt à être rendu
     * @return le même document, filigrané si la licence est une démonstration ; l'entrée inchangée
     *     sinon — l'appel est donc sans effet et sans coût en exploitation normale
     */
    public static String apply(String html) {
        if (html == null || html.isBlank() || !demoFlag.getAsBoolean()) {
            return html;
        }
        return injectMarkup(injectStyle(html));
    }

    private static String injectStyle(String html) {
        int head = indexOfIgnoreCase(html, "</head>");
        return head < 0 ? STYLE + html : html.substring(0, head) + STYLE + html.substring(head);
    }

    private static String injectMarkup(String html) {
        int body = indexOfIgnoreCase(html, "</body>");
        return body < 0 ? html + MARKUP : html.substring(0, body) + MARKUP + html.substring(body);
    }

    /** Les gabarits ne garantissent pas la casse des balises : la recherche y est insensible. */
    private static int indexOfIgnoreCase(String html, String needle) {
        return html.toLowerCase(Locale.ROOT).indexOf(needle);
    }
}
