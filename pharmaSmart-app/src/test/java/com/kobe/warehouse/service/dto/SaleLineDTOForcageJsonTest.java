package com.kobe.warehouse.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.enumeration.MotifForcageStock;
import org.junit.jupiter.api.Test;

/** Contrat JSON du motif de forçage — lot 2 de PLAN-VENTE-SUR-STOCK-ERRONE. */
class SaleLineDTOForcageJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void motifIsAcceptedFromTheFrontAndImpliesForcing() throws Exception {
        SaleLineDTO dto = mapper.readValue("{\"produitId\":1,\"quantityRequested\":3,\"motifForcage\":\"ECART_INVENTAIRE\"}", SaleLineDTO.class);

        assertEquals(MotifForcageStock.ECART_INVENTAIRE, dto.getMotifForcage());
        assertTrue(dto.isForcage(), "un motif explicite vaut forçage");
        assertFalse(dto.isForceStock(), "le drapeau historique reste ce que le front a envoyé");
    }

    @Test
    void motifAndEffectiveForcingAreNeverSentBack() throws Exception {
        SaleLineDTO dto = new SaleLineDTO().setMotifForcage(MotifForcageStock.RUPTURE_AVOIR);

        String json = mapper.writeValueAsString(dto);

        assertFalse(json.contains("motifForcage"), json);
        assertFalse(json.contains("\"forcage\""), json);
        assertTrue(json.contains("\"forceStock\":false"), "un DTO relu puis renvoyé ne force rien : " + json);
    }

    @Test
    void historicFlagAloneStillForces() {
        SaleLineDTO dto = new SaleLineDTO().setForceStock(true);

        assertTrue(dto.isForcage());
    }
}
