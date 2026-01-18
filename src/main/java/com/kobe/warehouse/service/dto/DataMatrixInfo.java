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
    String serialNumber
) {
    public boolean hasProductCode() {
        return gtin != null || cip13 != null || ean13 != null;
    }

    public boolean hasBatchInfo() {
        return batchNumber != null;
    }

    public boolean hasExpiryDate() {
        return expiryDate != null;
    }

    /**
     * Returns the best available product code (CIP13 > EAN13 > GTIN).
     */
    public String getProductCode() {
        if (cip13 != null) {
            return cip13;
        }
        if (ean13 != null) {
            return ean13;
        }
        return gtin;
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

        public DataMatrixInfo build() {
            return new DataMatrixInfo(gtin, cip13, ean13, batchNumber, expiryDate, manufacturingDate, serialNumber);
        }
    }
}
