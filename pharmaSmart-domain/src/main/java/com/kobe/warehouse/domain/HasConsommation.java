package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.service.dto.Consommation;
import java.time.LocalDateTime;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface HasConsommation {
    Set<Consommation> getConsommations();
    void setConsommations(Set<Consommation> consommations);
    Number getConsoMensuelle();
    void setConsoMensuelle(Number consoMensuelle);
    void setUpdated(LocalDateTime updated);
}
