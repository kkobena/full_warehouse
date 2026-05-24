package com.kobe.warehouse.service.fne.model;

public record CertificationFneResult(int nbCertifiees, int nbEchecs) {

    public boolean hasErrors() {
        return nbEchecs > 0;
    }

    public boolean isFullSuccess() {
        return nbEchecs == 0 && nbCertifiees > 0;
    }

    public boolean isEmpty() {
        return nbCertifiees == 0 && nbEchecs == 0;
    }
}
