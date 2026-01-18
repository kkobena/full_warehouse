package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.DataMatrixInfo;
import java.util.Optional;

/**
 * Service for parsing barcodes commonly used in pharmaceutical products.
 * Supports:
 * <ul>
 *   <li>EAN-8 (8 digits)</li>
 *   <li>EAN-13 (13 digits)</li>
 *   <li>CIP-7 (7 digits)</li>
 *   <li>CIP-13 (13 digits, starts with 340)</li>
 *   <li>GS1 DataMatrix with Application Identifiers (AI)</li>
 * </ul>
 *
 * <p>GS1 DataMatrix codes contain Application Identifiers (AI) that identify data elements:
 * <ul>
 *   <li>AI 01: GTIN (Global Trade Item Number) - 14 digits</li>
 *   <li>AI 10: Batch/Lot number - variable length up to 20 chars</li>
 *   <li>AI 11: Production date (YYMMDD)</li>
 *   <li>AI 17: Expiration date (YYMMDD)</li>
 *   <li>AI 21: Serial number - variable length up to 20 chars</li>
 *   <li>AI 710-714: National Healthcare Reimbursement Number (NHRN) - used for CIP in France</li>
 * </ul>
 */
public interface DataMatrixParserService {

    /**
     * Parses any barcode string (EAN-8, EAN-13, CIP, or DataMatrix) and extracts product information.
     *
     * @param barcodeData the raw barcode string (EAN, CIP, or DataMatrix with FNC1/GS separators)
     * @return Optional containing parsed DataMatrixInfo if valid, empty otherwise
     */
    Optional<DataMatrixInfo> parse(String barcodeData);

    /**
     * Checks if the given string appears to be a valid GS1 DataMatrix code.
     *
     * @param code the code to validate
     * @return true if the code appears to be a GS1 DataMatrix format
     */
    boolean isValidDataMatrix(String code);

    /**
     * Checks if the given string is a valid EAN-8 code.
     *
     * @param code the code to validate
     * @return true if valid EAN-8 format with correct check digit
     */
    boolean isValidEan8(String code);

    /**
     * Checks if the given string is a valid EAN-13 code.
     *
     * @param code the code to validate
     * @return true if valid EAN-13 format with correct check digit
     */
    boolean isValidEan13(String code);

    /**
     * Checks if the given string is a valid CIP-13 code (French pharmaceutical code).
     *
     * @param code the code to validate
     * @return true if valid CIP-13 format (starts with 340)
     */
    boolean isValidCip13(String code);

    /**
     * Checks if the given string is a valid CIP-7 code (legacy French pharmaceutical code).
     *
     * @param code the code to validate
     * @return true if valid CIP-7 format (7 digits)
     */
    boolean isValidCip7(String code);

    /**
     * Detects the type of barcode from the scanned data.
     *
     * @param code the scanned barcode data
     * @return the detected barcode type
     */
    BarcodeType detectBarcodeType(String code);

    /**
     * Enumeration of supported barcode types.
     */
    enum BarcodeType {
        EAN_8,
        EAN_13,
        CIP_7,
        CIP_13,
        DATAMATRIX,
        UNKNOWN
    }
}
