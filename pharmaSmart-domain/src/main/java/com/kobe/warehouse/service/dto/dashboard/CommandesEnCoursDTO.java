package com.kobe.warehouse.service.dto.dashboard;

public record CommandesEnCoursDTO(
    Integer enAttente,
    Integer aReceptionner,
    Long totalMontant
) {
}
