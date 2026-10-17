package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation of barcode parser supporting EAN-8, EAN-13, CIP, and GS1 DataMatrix.
 *
 * <p>Supports parsing of:
 * <ul>
 *   <li>EAN-8: 8-digit barcode with check digit</li>
 *   <li>EAN-13: 13-digit barcode with check digit</li>
 *   <li>CIP-7: 7-digit French pharmaceutical code</li>
 *   <li>CIP-13: 13-digit French pharmaceutical code (starts with 340)</li>
 *   <li>GTIN-14: contenu d'un AI 01 scanné sans en-tête GS1</li>
 *   <li>GS1 DataMatrix with Application Identifiers (AI)</li>
 * </ul>
 *
 * <p>GS1 DataMatrix Application Identifiers (AI):
 * <ul>
 *   <li>AI 01: GTIN (14 digits)</li>
 *   <li>AI 10: Batch/Lot number</li>
 *   <li>AI 11: Production date</li>
 *   <li>AI 17: Expiration date</li>
 *   <li>AI 21: Serial number</li>
 *   <li>AI 710-714: National Healthcare Reimbursement Numbers (CIP for France)</li>
 * </ul>
 */
@Service
public class DataMatrixParserServiceImpl implements DataMatrixParserService {

    private static final Logger LOG = LoggerFactory.getLogger(DataMatrixParserServiceImpl.class);

    // CIP-13 prefix for France
    private static final String CIP_13_PREFIX = "340";

    // GS1 Group Separator (ASCII 29) and common representations
    private static final char GS_CHAR = '\u001D';
    private static final String GS_STRING = String.valueOf(GS_CHAR);

    // Application Identifier codes
    private static final String AI_GTIN = "01";
    private static final String AI_BATCH = "10";
    private static final String AI_PRODUCTION_DATE = "11";
    private static final String AI_EXPIRY_DATE = "17";
    private static final String AI_SERIAL = "21";
    private static final String AI_QUANTITY = "37";
    private static final String AI_NHRN_GERMANY = "710";
    private static final String AI_NHRN_FRANCE = "711"; // CIP
    private static final String AI_NHRN_SPAIN = "712";
    private static final String AI_NHRN_BRAZIL = "713";
    private static final String AI_NHRN_PORTUGAL = "714";
    // Identification additionnelle du fabricant : AI des étiquettes internes de l'officine, où le
    // code du catalogue (dix caractères, non numérique) ne peut pas tenir dans un GTIN-14.
    private static final String AI_IDENTIFIANT_ADDITIONNEL = "240";

    // Fixed-length AI data lengths
    private static final int GTIN_LENGTH = 14;
    private static final int DATE_LENGTH = 6;

    // Date formatter for YYMMDD format
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    // Identifiants de symbologie AIM : ]d2/]d1 DataMatrix, ]C1 Code 128 GS1, ]Q3 QR GS1, ]e0 DataBar
    private static final Pattern GS1_PREFIX_PATTERN = Pattern.compile("^(\\]d2|\\]d1|\\]C1|\\]Q3|\\]e0)?(.*)$", Pattern.DOTALL);

    // FNC1 émis comme caractère imprimable par certains scanners (Datalogic)
    private static final char FNC1_IMPRIMABLE = 'è';

    // ── Motifs du mode proposition (codes en texte libre, sans AI GS1) ──────────────────────
    // Les bornes (?<!\d) et (?!\d) évitent de découper une suite de chiffres plus longue.
    private static final Pattern GTIN_14_NU_PATTERN = Pattern.compile("(?<!\\d)(\\d{14})(?!\\d)");
    private static final Pattern EAN_13_NU_PATTERN = Pattern.compile("(?<!\\d)(\\d{13})(?!\\d)");
    private static final Pattern DATE_AAAAMMJJ_PATTERN = Pattern.compile("(?<!\\d)(20\\d{2}(?:0[1-9]|1[0-2])(?:0\\d|[12]\\d|3[01]))(?!\\d)");
    private static final Pattern DATE_AAMMJJ_PATTERN = Pattern.compile("(?<!\\d)(\\d{2}(?:0[1-9]|1[0-2])(?:0\\d|[12]\\d|3[01]))(?!\\d)");
    // Le lot doit être étiqueté : deviner un lot dans une suite alphanumérique quelconque
    // reviendrait à inventer une traçabilité.
    // La barre oblique est exclue du lot : elle sépare les champs dans ces formats
    // (« LOT:ABC123/EXP:... »), l'inclure ferait avaler l'étiquette suivante.
    private static final Pattern LOT_ETIQUETE_PATTERN = Pattern.compile(
        "(?:LOT|BATCH|CHARGE)\\s*[:=#]?\\s*([A-Za-z0-9][A-Za-z0-9.\\-]{2,19})",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Optional<DataMatrixInfo> parse(String barcodeData) {
        if (!StringUtils.hasText(barcodeData)) {
            return Optional.empty();
        }

        // Normalisé une seule fois : les sous-parseurs recevaient la chaîne brute et stockaient
        // l'identifiant de symbologie avec le code (]C13400930000000 rangé tel quel dans cip13).
        String code = normalizeCode(barcodeData);

        return switch (detectBarcodeType(code)) {
            case EAN_8 -> parseEan8(code);
            case EAN_13 -> parseEan13(code);
            case CIP_7 -> parseCip7(code);
            case CIP_13 -> parseCip13(code);
            case GTIN_14 -> parseGtin14(code);
            case DATAMATRIX -> parseDataMatrix(code);
            // Code hors nomenclature connue : on tente d'y reconnaître des motifs (fabricants
            // asiatiques et africains encodent parfois en texte libre), puis on remonte le code brut pour que l'utilisateur puisse le mémoriser.
            case UNKNOWN -> parseHeuristique(code).or(() -> parseCodeBrut(code));
        };
    }

    @Override
    public BarcodeType detectBarcodeType(String code) {
        if (!StringUtils.hasText(code)) {
            return BarcodeType.UNKNOWN;
        }

        String normalized = normalizeCode(code);

        // Les longueurs d'un code 1D passent avant la détection GS1 : sans cela un EAN-8 ou un
        // EAN-13 dont les deux premiers chiffres forment un AI connu (préfixes GS1 01x, 10x, 11x)
        // serait pris pour un DataMatrix, puis échouerait au parsing.
        BarcodeType code1D = detecterCode1D(normalized);
        if (code1D != BarcodeType.UNKNOWN) {
            return code1D;
        }

        if (normalized.indexOf(GS_CHAR) >= 0 || isValidDataMatrix(normalized)) {
            return BarcodeType.DATAMATRIX;
        }

        // Le CIP-7 vient après : sept chiffres débutant par un AI connu sont le plus souvent un
        // GTIN tronqué (0134009), pas un code national.
        if (isValidCip7(normalized)) {
            return BarcodeType.CIP_7;
        }

        return BarcodeType.UNKNOWN;
    }

    /**
     * Type d'un code purement numérique dont la longueur est celle d'un code 1D connu.
     * La clé de contrôle sert d'arbitre : un EAN invalide rend UNKNOWN, ce qui laisse la main
     * à la détection GS1 (une trame « AI 17 + date » fait aussi huit chiffres).
     */
    private BarcodeType detecterCode1D(String code) {
        if (!code.matches("\\d+")) {
            return BarcodeType.UNKNOWN;
        }
        return switch (code.length()) {
            case 8 -> isValidEan8(code) ? BarcodeType.EAN_8 : BarcodeType.UNKNOWN;
            case 13 -> code.startsWith(CIP_13_PREFIX)
                ? BarcodeType.CIP_13
                : isValidEan13(code) ? BarcodeType.EAN_13 : BarcodeType.UNKNOWN;
            // Quatorze chiffres : GTIN-14 issu d'un AI 01 sans en-tête, ou ITF-14 de carton.
            // Sans identifiant de symbologie la distinction est impossible ; on retient le
            // GTIN-14, l'ITF-14 étant rare au comptoir pharmaceutique.
            case 14 -> BarcodeType.GTIN_14;
            default -> BarcodeType.UNKNOWN;
        };
    }

    @Override
    public boolean isValidEan8(String code) {
        if (code == null || code.length() != 8 || !code.matches("\\d{8}")) {
            return false;
        }
        return validateEanCheckDigit(code);
    }

    @Override
    public boolean isValidEan13(String code) {
        if (code == null || code.length() != 13 || !code.matches("\\d{13}")) {
            return false;
        }
        return validateEanCheckDigit(code);
    }

    @Override
    public boolean isValidCip13(String code) {
        if (code == null || code.length() != 13 || !code.matches("\\d{13}")) {
            return false;
        }
        return code.startsWith(CIP_13_PREFIX);
    }

    @Override
    public boolean isValidCip7(String code) {
        return code != null && code.length() == 7 && code.matches("\\d{7}");
    }

    /**
     * Parses an EAN-8 code.
     */
    private Optional<DataMatrixInfo> parseEan8(String code) {
        return Optional.of(DataMatrixInfo.builder()
            .ean13(code) // Store as ean13 field for consistency
            .build());
    }

    /**
     * Parses an EAN-13 code.
     */
    private Optional<DataMatrixInfo> parseEan13(String code) {
        DataMatrixInfo.Builder builder = DataMatrixInfo.builder().ean13(code);

        // Check if it's also a CIP-13
        if (code.startsWith(CIP_13_PREFIX)) {
            builder.cip13(code);
        }

        return Optional.of(builder.build());
    }

    /**
     * Parses a CIP-7 code.
     */
    private Optional<DataMatrixInfo> parseCip7(String code) {
        return Optional.of(DataMatrixInfo.builder()
            .cip13(code) // Store CIP-7 in cip13 field
            .build());
    }

    /**
     * Parses a CIP-13 code.
     */
    private Optional<DataMatrixInfo> parseCip13(String code) {
        return Optional.of(DataMatrixInfo.builder()
            .cip13(code)
            .ean13(code)
            .build());
    }

    /**
     * Lit un code encodé en texte libre, sans Application Identifier GS1 : certains fabricants
     * asiatiques et africains impriment {@code "LOT:ABC123/EXP:20250131/EAN:3400933399931"} ou
     * {@code "ABC123|250131|3400933399931"}.
     *
     * <p><b>Mode proposition.</b> Tout ce qui est reconnu ici l'est par motif, donc faillible :
     * le résultat porte {@code proposition = true}, ce qui interdit la création automatique de
     * lot et le ciblage d'un lot à la vente. Les valeurs ne servent qu'à pré-remplir l'écran,
     * où un humain les confirme.
     *
     * @return vide si aucun motif n'est reconnu, à charge de l'appelant de se rabattre sur le
     *     code brut
     */
    private Optional<DataMatrixInfo> parseHeuristique(String code) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }

        DataMatrixInfo.Builder builder = DataMatrixInfo.builder().proposition(true);
        boolean codeProduitTrouve = false;
        boolean donneeTrouvee = false;

        // GTIN-14 avant EAN-13 : les quatorze chiffres contiennent treize chiffres valides.
        Matcher gtin = GTIN_14_NU_PATTERN.matcher(code);
        if (gtin.find()) {
            String valeur = gtin.group(1);
            builder.gtin(valeur);
            String ean13 = extractEan13FromGtin(valeur);
            if (ean13 != null) {
                builder.ean13(ean13);
            }
            codeProduitTrouve = true;
        } else {
            Matcher ean = EAN_13_NU_PATTERN.matcher(code);
            while (ean.find() && !codeProduitTrouve) {
                String candidat = ean.group(1);
                // La clé de contrôle évite de prendre un numéro de dossier pour un code produit.
                if (isValidEan13(candidat)) {
                    builder.ean13(candidat);
                    if (candidat.startsWith(CIP_13_PREFIX)) {
                        builder.cip13(candidat);
                    }
                    codeProduitTrouve = true;
                }
            }
        }
        donneeTrouvee = codeProduitTrouve;

        String lot = extraireLot(code);
        if (lot != null) {
            builder.batchNumber(lot);
            donneeTrouvee = true;
        }

        LocalDate peremption = extrairePeremption(code);
        if (peremption != null) {
            builder.expiryDate(peremption);
            donneeTrouvee = true;
        }

        if (!donneeTrouvee) {
            return Optional.empty();
        }

        // Sans code produit, le scan ne peut rien résoudre : la chaîne entière est remontée pour
        // que l'utilisateur puisse la mémoriser comme code alternatif.
        if (!codeProduitTrouve) {
            builder.rawCode(code);
        }
        return Optional.of(builder.build());
    }

    /** Numéro de lot explicitement étiqueté. Un lot deviné sans étiquette serait trop risqué. */
    private String extraireLot(String code) {
        Matcher lot = LOT_ETIQUETE_PATTERN.matcher(code);
        return lot.find() ? lot.group(1).toUpperCase(Locale.ROOT) : null;
    }

    /**
     * Date de péremption : la plus proche parmi les dates futures reconnues, aux deux formats
     * rencontrés (AAMMJJ et AAAAMMJJ). Une date passée est ignorée — c'est plus probablement une
     * date de fabrication ou un numéro qui ressemble à une date.
     */
    private LocalDate extrairePeremption(String code) {
        LocalDate meilleure = null;
        LocalDate aujourdhui = LocalDate.now();

        Matcher annee4 = DATE_AAAAMMJJ_PATTERN.matcher(code);
        while (annee4.find()) {
            meilleure = plusProche(meilleure, parseDateSure(annee4.group(1), 4), aujourdhui);
        }
        Matcher annee2 = DATE_AAMMJJ_PATTERN.matcher(code);
        while (annee2.find()) {
            meilleure = plusProche(meilleure, parseDateSure(annee2.group(1), 2), aujourdhui);
        }
        return meilleure;
    }

    private LocalDate plusProche(LocalDate meilleure, LocalDate candidate, LocalDate aujourdhui) {
        if (candidate == null || !candidate.isAfter(aujourdhui)) {
            return meilleure;
        }
        return meilleure == null || candidate.isBefore(meilleure) ? candidate : meilleure;
    }

    /** Lit une date sans lever : un motif reconnu n'est pas forcément une date valide (3002). */
    private LocalDate parseDateSure(String valeur, int chiffresAnnee) {
        try {
            int annee = Integer.parseInt(valeur.substring(0, chiffresAnnee));
            int mois = Integer.parseInt(valeur.substring(chiffresAnnee, chiffresAnnee + 2));
            int jour = Integer.parseInt(valeur.substring(chiffresAnnee + 2));
            LocalDate premierDuMois = LocalDate.of(chiffresAnnee == 2 ? 2000 + annee : annee, mois, 1);
            // Comme pour l'AI 17, un jour à 00 désigne la fin du mois.
            return jour == 0 ? premierDuMois.withDayOfMonth(premierDuMois.lengthOfMonth()) : premierDuMois.withDayOfMonth(jour);
        } catch (DateTimeException | NumberFormatException e) {
            LOG.debug("Motif de date non exploitable : {}", valeur);
            return null;
        }
    }

    /**
     * Remonte un code qu'aucune nomenclature ne reconnaît, pour que l'appelant puisse quand même
     * le chercher en base. Rien n'en est déduit : ni lot, ni péremption, ni type de code.
     */
    private Optional<DataMatrixInfo> parseCodeBrut(String code) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }
        return Optional.of(DataMatrixInfo.builder().rawCode(code).build());
    }

    /**
     * Lit un GTIN-14 nu : le contenu d'un AI 01 scanné sans son en-tête GS1.
     */
    private Optional<DataMatrixInfo> parseGtin14(String code) {
        DataMatrixInfo.Builder builder = DataMatrixInfo.builder().gtin(code);
        String ean13 = extractEan13FromGtin(code);
        if (ean13 != null) {
            builder.ean13(ean13);
        }
        return Optional.of(builder.build());
    }

    /**
     * Parses a GS1 DataMatrix code with Application Identifiers.
     */
    private Optional<DataMatrixInfo> parseDataMatrix(String dataMatrixCode) {
        try {
            String code = normalizeCode(dataMatrixCode);
            DataMatrixInfo.Builder builder = DataMatrixInfo.builder();
            boolean foundAnyData = false;

            int position = 0;
            while (position < code.length()) {
                // Skip group separators
                if (code.charAt(position) == GS_CHAR) {
                    position++;
                    continue;
                }

                // Try to extract AI and data
                String remaining = code.substring(position);
                ParseResult result = parseNextElement(remaining);

                if (result == null) {
                    // Unknown AI or invalid format, try to skip to next GS
                    int nextGs = remaining.indexOf(GS_CHAR);
                    if (nextGs > 0) {
                        position += nextGs + 1;
                    } else {
                        break;
                    }
                    continue;
                }

                foundAnyData = applyToBuilder(builder, result) || foundAnyData;
                position += result.consumedLength();
            }

            if (foundAnyData) {
                return Optional.of(builder.build());
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse DataMatrix code: {}", dataMatrixCode, e);
        }

        return Optional.empty();
    }

    /**
     * Validates the check digit of an EAN code (works for both EAN-8 and EAN-13).
     */
    private boolean validateEanCheckDigit(String code) {
        int sum = 0;
        int length = code.length();

        for (int i = 0; i < length - 1; i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            // For EAN-13: odd positions (0,2,4...) multiply by 1, even (1,3,5...) multiply by 3
            // For EAN-8: same pattern but shorter
            int multiplier = ((length - 1 - i) % 2 == 0) ? 1 : 3;
            sum += digit * multiplier;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        int actualCheckDigit = Character.getNumericValue(code.charAt(length - 1));

        return checkDigit == actualCheckDigit;
    }

    @Override
    public boolean isValidDataMatrix(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        String normalized = normalizeCode(code);

        // Check for common GS1 AI prefixes
        return normalized.startsWith(AI_GTIN) ||
            normalized.startsWith(AI_BATCH) ||
            normalized.startsWith(AI_EXPIRY_DATE) ||
            normalized.startsWith(AI_PRODUCTION_DATE) ||
            normalized.startsWith(AI_SERIAL) ||
            normalized.startsWith(AI_NHRN_GERMANY) ||
            normalized.startsWith(AI_NHRN_FRANCE) ||
            normalized.startsWith(AI_NHRN_SPAIN) ||
            normalized.startsWith(AI_NHRN_BRAZIL) ||
            normalized.startsWith(AI_NHRN_PORTUGAL);
    }

    /**
     * Retire les identifiants de symbologie et ramène toutes les écritures du séparateur de
     * groupe au caractère GS réel. Idempotente : la chaîne peut être renormalisée sans dommage.
     */
    private String normalizeCode(String code) {
        String normalized = code.trim();

        // Caractères de transmission : FNC1 rendu imprimable (è, Datalogic) et caractères de
        // contrôle ajoutés par certains scanners (STX en tête, ETX en fin). Les données GS1
        // étant imprimables, les retirer ne peut pas amputer un lot ni un numéro de série.
        while (!normalized.isEmpty() && estCaractereDeTransmission(normalized.charAt(0))) {
            normalized = normalized.substring(1);
        }
        while (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) < ' ') {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        // Identifiant de symbologie AIM
        Matcher matcher = GS1_PREFIX_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            normalized = matcher.group(2);
        }

        // Toutes les notations textuelles du séparateur
        normalized = normalized
            .replace("<GS>", GS_STRING)
            .replace("{GS}", GS_STRING)
            .replace("^029", GS_STRING) // échappement décimal Honeywell
            .replace("%1D", GS_STRING); // URL-encodé, imports par fichier

        // GS en tête : Code 128 GS1 émis sans identifiant de symbologie
        while (!normalized.isEmpty() && normalized.charAt(0) == GS_CHAR) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    /** Le caractère est-il un artefact de transmission du scanner, et non une donnée ? */
    private boolean estCaractereDeTransmission(char caractere) {
        return caractere == FNC1_IMPRIMABLE || caractere < ' ';
    }

    /**
     * Parses the next AI element from the code string.
     */
    private ParseResult parseNextElement(String code) {
        if (code.length() < 2) {
            return null;
        }

        // Try 2-digit AIs first (most common)
        String ai2 = code.substring(0, 2);
        ParseResult result = switch (ai2) {
            case AI_GTIN -> parseFixedLength(code, AI_GTIN, GTIN_LENGTH);
            case AI_BATCH -> parseVariableLength(code, AI_BATCH);
            case AI_PRODUCTION_DATE -> parseFixedLength(code, AI_PRODUCTION_DATE, DATE_LENGTH);
            case AI_EXPIRY_DATE -> parseFixedLength(code, AI_EXPIRY_DATE, DATE_LENGTH);
            case AI_SERIAL -> parseVariableLength(code, AI_SERIAL);
            case AI_QUANTITY -> parseVariableLength(code, AI_QUANTITY);
            default -> null;
        };

        if (result != null) {
            return result;
        }

        // Try 3-digit AIs (NHRN codes)
        if (code.length() >= 3) {
            String ai3 = code.substring(0, 3);
            result = switch (ai3) {
                case AI_NHRN_GERMANY,
                    AI_NHRN_FRANCE,
                    AI_NHRN_SPAIN,
                    AI_NHRN_BRAZIL,
                    AI_NHRN_PORTUGAL,
                    AI_IDENTIFIANT_ADDITIONNEL -> parseVariableLength(code, ai3);
                default -> null;
            };
        }

        return result;
    }

    /**
     * Parses a fixed-length AI element.
     */
    private ParseResult parseFixedLength(String code, String ai, int dataLength) {
        int totalLength = ai.length() + dataLength;
        if (code.length() < totalLength) {
            return null;
        }

        String data = code.substring(ai.length(), totalLength);
        return new ParseResult(ai, data, totalLength);
    }

    /**
     * Parses a variable-length AI element (terminated by GS or end of string).
     */
    private ParseResult parseVariableLength(String code, String ai) {
        String remaining = code.substring(ai.length());
        int endIndex = remaining.indexOf(GS_CHAR);

        String data;
        int consumedLength;
        if (endIndex >= 0) {
            data = remaining.substring(0, endIndex);
            consumedLength = ai.length() + endIndex + 1; // +1 for GS char
        } else {
            data = remaining;
            consumedLength = code.length();
        }

        // Validate data is not empty
        if (data.isEmpty()) {
            return null;
        }

        return new ParseResult(ai, data, consumedLength);
    }

    /**
     * Applies the parsed result to the builder.
     *
     * @return true if data was applied, false otherwise
     */
    private boolean applyToBuilder(DataMatrixInfo.Builder builder, ParseResult result) {
        return switch (result.ai()) {
            case AI_GTIN -> {
                builder.gtin(result.data());
                // Extract EAN13 from GTIN-14 (remove leading 0 if present)
                String ean13 = extractEan13FromGtin(result.data());
                if (ean13 != null) {
                    builder.ean13(ean13);
                }
                yield true;
            }
            case AI_BATCH -> {
                builder.batchNumber(result.data());
                yield true;
            }
            case AI_PRODUCTION_DATE -> {
                LocalDate date = parseDate(result.data());
                if (date != null) {
                    builder.manufacturingDate(date);
                    yield true;
                }
                yield false;
            }
            case AI_EXPIRY_DATE -> {
                LocalDate date = parseDate(result.data());
                if (date != null) {
                    builder.expiryDate(date);
                    yield true;
                }
                yield false;
            }
            case AI_SERIAL -> {
                builder.serialNumber(result.data());
                yield true;
            }
            case AI_QUANTITY -> {
                try {
                    int qty = Integer.parseInt(result.data().strip());
                    if (qty > 0) {
                        builder.scannedQty(qty);
                    }
                } catch (NumberFormatException ignored) {}
                yield true;
            }
            case AI_NHRN_FRANCE -> {
                // CIP code for France
                builder.cip13(result.data());
                yield true;
            }
            case AI_NHRN_GERMANY, AI_NHRN_SPAIN, AI_NHRN_BRAZIL, AI_NHRN_PORTUGAL -> {
                // Other NHRN codes - store as CIP13 if no CIP already set
                builder.cip13(result.data());
                yield true;
            }
            case AI_IDENTIFIANT_ADDITIONNEL -> {
                // Code propre au fabricant — ici, celui du catalogue sur une étiquette interne.
                // Il n'appartient à aucune nomenclature : la résolution le confronte au catalogue.
                builder.rawCode(result.data());
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Extracts EAN-13 from GTIN-14.
     * GTIN-14 format: [indicator digit][GS1 Company Prefix + Item Reference][check digit]
     * If indicator digit is 0, the remaining 13 digits are EAN-13.
     */
    private String extractEan13FromGtin(String gtin) {
        if (gtin == null || gtin.length() != GTIN_LENGTH) {
            return null;
        }

        // If GTIN-14 starts with 0, extract EAN-13
        if (gtin.charAt(0) == '0') {
            return gtin.substring(1);
        }

        return null;
    }

    /**
     * Parses a date in YYMMDD format.
     * Handles special case where DD=00 means last day of month.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() != DATE_LENGTH) {
            return null;
        }

        try {
            int year = Integer.parseInt(dateStr.substring(0, 2));
            int month = Integer.parseInt(dateStr.substring(2, 4));
            int day = Integer.parseInt(dateStr.substring(4, 6));

            // Convert 2-digit year (assumes 20xx for years 00-99)
            int fullYear = 2000 + year;

            // Handle special case: day=00 means last day of month
            if (day == 0) {
                LocalDate firstOfMonth = LocalDate.of(fullYear, month, 1);
                return firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
            }

            return LocalDate.of(fullYear, month, day);
        } catch (DateTimeParseException | NumberFormatException e) {
            LOG.debug("Failed to parse date: {}", dateStr, e);
            return null;
        }
    }

    /**
     * Internal record for parse results.
     */
    private record ParseResult(String ai, String data, int consumedLength) {}
}
