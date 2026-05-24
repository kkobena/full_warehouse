package com.kobe.warehouse.service.dto.projection;

import java.time.LocalDate;

public interface ReponseRetourBonItemProjection {
    Integer getAcceptedQty();

    Integer getFournisseurId();

    Integer getValeurAchat();

    LocalDate getDateMtv();
}
