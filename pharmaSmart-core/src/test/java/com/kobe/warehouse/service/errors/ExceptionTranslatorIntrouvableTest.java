package com.kobe.warehouse.service.errors;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Une vente supprimée par une transformation, relue par un écran qui garde son ancien identifiant : l'erreur doit dire
 * « introuvable » (404), pas « erreur interne » (500).
 */
class ExceptionTranslatorIntrouvableTest {

    private final ExceptionTranslator translator = new ExceptionTranslator();

    @Test
    void uneEntiteIntrouvableRendUn404ExplicitePlutotQueLErreurInterne() {
        ResponseEntity<Object> reponse = translator.handleEntityNotFound(
            new EntityNotFoundException("No row with the given identifier exists for entity [ThirdPartySales with id '4151']")
        );

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        var corps = (ProblemDetail) reponse.getBody();
        assertThat(corps).isNotNull();
        assertThat(corps.getDetail()).contains("introuvable").contains("transformé");
    }

    @Test
    void laTraceTechniqueNeRemonteAuCaissierQueDansLeJournal() {
        ResponseEntity<Object> reponse = translator.handleEntityNotFound(new EntityNotFoundException("[ThirdPartySales with id '4151']"));

        assertThat(((ProblemDetail) reponse.getBody()).getDetail()).doesNotContain("ThirdPartySales").doesNotContain("4151");
    }
}
