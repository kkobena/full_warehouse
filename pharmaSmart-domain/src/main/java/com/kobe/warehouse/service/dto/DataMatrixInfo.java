package com.kobe.warehouse.service.dto;

import java.time.LocalDate;

/**
 * DTO containing parsed information from a GS1 DataMatrix barcode.
 * Commonly used in pharmaceutical products for tracking batch/lot numbers and expiration dates.
 */
public record DataMatrixInfo(
    String gtin,
    String cip13,
    String ean13,
    String batchNumber,
    LocalDate expiryDate,
    LocalDate manufacturingDate,
    String serialNumber,
    int scannedQty,
    String rawCode,
    boolean proposition
) {
    public boolean hasProductCode() {
        return gtin != null || cip13 != null || ean13 != null || rawCode != null;
    }

    public boolean hasBatchInfo() {
        return batchNumber != null;
    }

    /**
     * Lot et péremption sûrs, c'est-à-dire lus dans un Application Identifier GS1.
     *
     * <p>Les valeurs d'un code non-GS1 sont déduites par reconnaissance de motif : elles ne
     * valent que comme proposition à l'écran, jamais comme source d'une écriture de stock.
     */
    public boolean hasBatchInfoCertaine() {
        return batchNumber != null && !proposition;
    }

    public boolean hasExpiryDate() {
        return expiryDate != null;
    }

    /**
     * Meilleur code produit disponible : CIP-13, puis EAN-13, puis GTIN-14, puis le code brut.
     *
     * <p>Le code brut n'est renseigné que pour un code hors nomenclature connue — typiquement le
     * code interne d'un grossiste ivoirien. Il n'est pas interprété : c'est à l'appelant de le
     * confronter au catalogue et aux codes alternatifs.
     */
    public String getProductCode() {
        if (cip13 != null) {
            return cip13;
        }
        if (ean13 != null) {
            return ean13;
        }
        return gtin != null ? gtin : rawCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String gtin;
        private String cip13;
        private String ean13;
        private String batchNumber;
        private LocalDate expiryDate;
        private LocalDate manufacturingDate;
        private String serialNumber;
        private int scannedQty = 1;
        private String rawCode;
        private boolean proposition;

        public Builder gtin(String gtin) {
            this.gtin = gtin;
            return this;
        }

        public Builder cip13(String cip13) {
            this.cip13 = cip13;
            return this;
        }

        public Builder ean13(String ean13) {
            this.ean13 = ean13;
            return this;
        }

        public Builder batchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder manufacturingDate(LocalDate manufacturingDate) {
            this.manufacturingDate = manufacturingDate;
            return this;
        }

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder scannedQty(int scannedQty) {
            this.scannedQty = scannedQty;
            return this;
        }

        public Builder rawCode(String rawCode) {
            this.rawCode = rawCode;
            return this;
        }

        /** Marque les valeurs comme déduites par motif, donc à confirmer par un humain. */
        public Builder proposition(boolean proposition) {
            this.proposition = proposition;
            return this;
        }

        public DataMatrixInfo build() {
            return new DataMatrixInfo(
                gtin,
                cip13,
                ean13,
                batchNumber,
                expiryDate,
                manufacturingDate,
                serialNumber,
                scannedQty,
                rawCode,
                proposition
            );
        }
    }
}
