package com.kobe.warehouse.domain.enumeration;

/** CSV : UTF-8 avec BOM, séparateur « ; », décimales à la française (Excel en français l'ouvre tel quel). */
public enum FormatExport {
    CSV("csv", "text/csv"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String typeMime;

    FormatExport(String extension, String typeMime) {
        this.extension = extension;
        this.typeMime = typeMime;
    }

    public String getExtension() {
        return extension;
    }

    public String getTypeMime() {
        return typeMime;
    }
}
