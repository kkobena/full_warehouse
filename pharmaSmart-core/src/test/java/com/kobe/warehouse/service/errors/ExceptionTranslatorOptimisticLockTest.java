package com.kobe.warehouse.service.errors;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Un conflit sur une vente bloque un encaissement, client au comptoir : le message doit parler de la
 * vente, pas du stock. Selon le chemin emprunté par Hibernate, l'entité en cause arrive sous
 * plusieurs formes — chacune doit être reconnue.
 */
class ExceptionTranslatorOptimisticLockTest {

    private final ExceptionTranslator translator = new ExceptionTranslator();

    @Test
    void reconnaitUneVenteParLeNomDeSaClasse() {
        assertThat(detail(new ObjectOptimisticLockingFailureException(CashSale.class.getName(), 1L))).contains("vente");
        assertThat(detail(new ObjectOptimisticLockingFailureException(ThirdPartySales.class.getName(), 1L))).contains("vente");
    }

    @Test
    void reconnaitUneVenteParLInstanceEnConflit() {
        assertThat(detail(new OptimisticLockException("conflit", null, new CashSale()))).contains("vente");
    }

    @Test
    void reconnaitUneVenteDansUnLotJdbcParLOrdreSql() {
        var lot = new ObjectOptimisticLockingFailureException(
            "Batch update failed",
            new StaleStateException("Batch update returned unexpected row count [update sales set statut=? where id=? and sale_date=? and version=?]")
        );
        assertThat(detail(lot)).contains("vente");
    }

    @Test
    void neConfondPasLesLignesDeVenteAvecLaVente() {
        var lot = new ObjectOptimisticLockingFailureException(
            "Batch update failed",
            new StaleStateException("Batch update returned unexpected row count [update sales_line set quantity_sold=? where id=?]")
        );
        assertThat(detail(lot)).contains("stock");
    }

    @Test
    void garderLeMessageDuStockPourLesAutresEntites() {
        assertThat(detail(new ObjectOptimisticLockingFailureException(StoreInventoryLine.class.getName(), 1L))).contains("stock");
    }

    @Test
    void leMessageEstLisibleEtLaCleVaDansErrorKey() {
        ResponseEntity<Object> response = translator.handleOptimisticLock(
            new ObjectOptimisticLockingFailureException(CashSale.class.getName(), 1L),
            null
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasFieldOrPropertyWithValue("message", body.getDetail());
        assertThat(body).hasFieldOrPropertyWithValue("errorKey", "sale.concurrent.modification");
    }

    @Test
    void unVerrouNonObtenuDonneUn409Lisible() {
        ResponseEntity<Object> response = translator.handlePessimisticLock(
            new org.springframework.dao.CannotAcquireLockException("lock timeout"),
            null
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).contains("autre poste");
        assertThat(body).hasFieldOrPropertyWithValue("errorKey", "concurrent.lock");
    }

    private String detail(Exception ex) {
        ProblemDetail body = (ProblemDetail) translator.handleOptimisticLock(ex, null).getBody();
        return body == null ? null : body.getDetail();
    }
}
