package com.kobe.warehouse.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat du traitement batch de retours fournisseur depuis des lots périmés.
 *
 */
public class RetourBonBatchResultDTO {

    private int totalCreated = 0;
    private int totalErrors = 0;
    private List<RetourBonDTO> created = new ArrayList<>();
    private List<LotError> errors = new ArrayList<>();

    public static class LotError {
        private Integer lotId;
        private String lotNumero;
        private String message;

        public LotError(Integer lotId, String lotNumero, String message) {
            this.lotId = lotId;
            this.lotNumero = lotNumero;
            this.message = message;
        }

        public Integer getLotId() { return lotId; }
        public String getLotNumero() { return lotNumero; }
        public String getMessage() { return message; }
    }

    public int getTotalCreated() { return totalCreated; }
    public void setTotalCreated(int totalCreated) { this.totalCreated = totalCreated; }

    public int getTotalErrors() { return totalErrors; }
    public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }

    public List<RetourBonDTO> getCreated() { return created; }
    public void setCreated(List<RetourBonDTO> created) { this.created = created; }

    public List<LotError> getErrors() { return errors; }
    public void setErrors(List<LotError> errors) { this.errors = errors; }

    public void addCreated(RetourBonDTO retourBon) {
        this.created.add(retourBon);
        this.totalCreated++;
    }

    public void addError(Integer lotId, String lotNumero, String message) {
        this.errors.add(new LotError(lotId, lotNumero, message));
        this.totalErrors++;
    }
}

