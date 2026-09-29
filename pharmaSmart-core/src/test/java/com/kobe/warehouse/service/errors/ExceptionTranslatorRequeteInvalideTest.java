package com.kobe.warehouse.service.errors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Une requête mal formée est une erreur du client : elle garde son statut 4xx et un message qui dit
 * quoi corriger, et non le « 500, erreur interne » réservé aux défauts du logiciel.
 */
class ExceptionTranslatorRequeteInvalideTest {

    @RestController
    static class Controleur {

        @GetMapping("/rapport")
        String rapport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
            return "ok";
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Controleur()).setControllerAdvice(new ExceptionTranslator()).build();

    @Test
    void parametreObligatoireManquant() throws Exception {
        mvc
            .perform(get("/rapport"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Paramètre obligatoire manquant : startDate"));
    }

    @Test
    void verbeNonSupporte() throws Exception {
        mvc
            .perform(post("/rapport"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.message", containsString("POST")));
    }
}
