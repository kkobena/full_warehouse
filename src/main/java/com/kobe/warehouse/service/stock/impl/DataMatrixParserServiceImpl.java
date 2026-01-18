package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final String AI_NHRN_GERMANY = "710";
    private static final String AI_NHRN_FRANCE = "711"; // CIP
    private static final String AI_NHRN_SPAIN = "712";
    private static final String AI_NHRN_BRAZIL = "713";
    private static final String AI_NHRN_PORTUGAL = "714";

    // Fixed-length AI data lengths
    private static final int GTIN_LENGTH = 14;
    private static final int DATE_LENGTH = 6;

    // Date formatter for YYMMDD format
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    // Pattern for detecting GS1 DataMatrix symbology identifier
    private static final Pattern GS1_PREFIX_PATTERN = Pattern.compile("^(\\]d2|\\]C1|\\x1D)?(.*)$");

    @Override
    public Optional<DataMatrixInfo> parse(String barcodeData) {
        if (!StringUtils.hasText(barcodeData)) {
            return Optional.empty();
        }

        String code = barcodeData.trim();
        BarcodeType type = detectBarcodeType(code);

        return switch (type) {
            case EAN_8 -> parseEan8(code);
            case EAN_13 -> parseEan13(code);
            case CIP_7 -> parseCip7(code);
            case CIP_13 -> parseCip13(code);
            case DATAMATRIX -> parseDataMatrix(code);
            case UNKNOWN -> Optional.empty();
        };
    }

    @Override
    public BarcodeType detectBarcodeType(String code) {
        if (!StringUtils.hasText(code)) {
            return BarcodeType.UNKNOWN;
        }

        String trimmed = code.trim();

        // Check for DataMatrix first (contains non-numeric or AI prefixes)
        if (isValidDataMatrix(trimmed)) {
            return BarcodeType.DATAMATRIX;
        }

        // Check numeric-only codes by length
        if (trimmed.matches("\\d+")) {
            int length = trimmed.length();
            if (length == 7) {
                return BarcodeType.CIP_7;
            } else if (length == 8 && isValidEan8(trimmed)) {
                return BarcodeType.EAN_8;
            } else if (length == 13) {
                if (trimmed.startsWith(CIP_13_PREFIX)) {
                    return BarcodeType.CIP_13;
                } else if (isValidEan13(trimmed)) {
                    return BarcodeType.EAN_13;
                }
            }
        }

        return BarcodeType.UNKNOWN;
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
     * Normalizes the DataMatrix code by removing symbology identifiers and standardizing separators.
     */
    private String normalizeCode(String code) {
        String normalized = code.trim();

        // Remove GS1 symbology identifiers (]d2 for DataMatrix, ]C1 for Code 128)
        Matcher matcher = GS1_PREFIX_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            normalized = matcher.group(2);
        }

        // Replace common GS representations with actual GS char
        normalized = normalized.replace("<GS>", GS_STRING);
        normalized = normalized.replace("{GS}", GS_STRING);

        return normalized;
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
            default -> null;
        };

        if (result != null) {
            return result;
        }

        // Try 3-digit AIs (NHRN codes)
        if (code.length() >= 3) {
            String ai3 = code.substring(0, 3);
            result = switch (ai3) {
                case AI_NHRN_GERMANY, AI_NHRN_FRANCE, AI_NHRN_SPAIN, AI_NHRN_BRAZIL, AI_NHRN_PORTUGAL ->
                    parseVariableLength(code, ai3);
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
