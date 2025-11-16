package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kobe.warehouse.service.dto.Consommation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsommationService Tests")
class ConsommationServiceTest {

    private ConsommationService consommationService;

    @BeforeEach
    void setUp() {
        consommationService = new ConsommationService();
    }

    @Test
    @DisplayName("Should create new consommation when none exists")
    void testUpdateConsommation_NewConsommation() {
        // Given
        TestEntity entity = new TestEntity();
        Integer montant = 1000;
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 15, 10, 0);
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, montant, dateTime, saver);

        // Then
        assertEquals(1, entity.getConsommations().size(), "Should create one consommation");
        Consommation conso = entity.getConsommations().iterator().next();
        // assertEquals(montant, conso.getConsommation());
        assertEquals(10, conso.getMonth());
        assertEquals(2025, conso.getYear());
        assertEquals(202510, conso.getId());
        assertEquals(montant, entity.getConsoMensuelle());
        assertNotNull(entity.getUpdated());
        verify(saver).accept(entity);
    }

    @Test
    @DisplayName("Should update existing consommation for same month")
    void testUpdateConsommation_UpdateExisting() {
        // Given
        TestEntity entity = new TestEntity();
        LocalDateTime dateTime = LocalDateTime.of(2025, 10, 15, 10, 0);
        int consommationId = 202510;

        Consommation existingConso = new Consommation();
        existingConso.setId(consommationId);
        existingConso.setConsommation(500);
        existingConso.setMonth((short) 10);
        existingConso.setYear(2025);

        Set<Consommation> consommations = new HashSet<>();
        consommations.add(existingConso);
        entity.setConsommations(consommations);
        entity.setConsoMensuelle(500);

        Integer additionalMontant = 300;
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, additionalMontant, dateTime, saver);

        // Then
        assertEquals(1, entity.getConsommations().size(), "Should still have one consommation");
        Consommation updatedConso = entity.getConsommations().iterator().next();
        assertEquals(800, updatedConso.getConsommation(), "Should add to existing consommation");
        assertEquals(800, entity.getConsoMensuelle(), "Monthly consumption should be updated");
        verify(saver).accept(entity);
    }

    @Test
    @DisplayName("Should handle multiple consommations for different months")
    void testUpdateConsommation_MultipleMonths() {
        // Given
        TestEntity entity = new TestEntity();

        Consommation octConso = new Consommation();
        octConso.setId(202510);
        octConso.setConsommation(1000);
        octConso.setMonth((short) 10);
        octConso.setYear(2025);

        Set<Consommation> consommations = new HashSet<>();
        consommations.add(octConso);
        entity.setConsommations(consommations);
        entity.setConsoMensuelle(1000);

        LocalDateTime novDateTime = LocalDateTime.of(2025, 11, 10, 10, 0);
        Integer novMontant = 500;
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, novMontant, novDateTime, saver);

        // Then
        assertEquals(2, entity.getConsommations().size(), "Should have two consommations");
        assertEquals(1500, entity.getConsoMensuelle(), "Should sum both months");
        assertTrue(entity.getConsommations().stream().anyMatch(c -> c.getId() == 202510));
        assertTrue(entity.getConsommations().stream().anyMatch(c -> c.getId() == 202511));
    }

    @Test
    @DisplayName("Should handle null montant gracefully")
    void testUpdateConsommation_NullMontant() {
        // Given
        TestEntity entity = new TestEntity();
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, null, LocalDateTime.now(), saver);

        // Then
        assertTrue(entity.getConsommations().isEmpty(), "Should not create consommation");
        verify(saver, never()).accept(any());
    }

    @Test
    @DisplayName("Should handle Integer consoMensuelle type")
    void testUpdateConsommation_IntegerType() {
        // Given
        TestEntityWithInteger entity = new TestEntityWithInteger();
        entity.setConsoMensuelle(100);

        Integer montant = 50;
        Consumer<TestEntityWithInteger> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, montant, LocalDateTime.now(), saver);

        // Then
        assertEquals(150, entity.getConsoMensuelle());
        assertTrue(entity.getConsoMensuelle() instanceof Integer, "Should preserve Integer type");
    }

    @Test
    @DisplayName("Should handle Long consoMensuelle type")
    void testUpdateConsommation_LongType() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setConsoMensuelle(100L);

        Integer montant = 50;
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, montant, LocalDateTime.now(), saver);

        // Then
        assertEquals(150L, entity.getConsoMensuelle());
        assertTrue(entity.getConsoMensuelle() instanceof Long, "Should preserve Long type");
    }

    @Test
    @DisplayName("Should handle null consoMensuelle as Integer by default")
    void testUpdateConsommation_NullConsoMensuelle() {
        // Given
        TestEntity entity = new TestEntity(); // consoMensuelle is null
        Integer montant = 100;
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, montant, LocalDateTime.now(), saver);

        // Then
        assertEquals(100, entity.getConsoMensuelle());
        assertTrue(entity.getConsoMensuelle() instanceof Integer, "Should default to Integer");
    }

    @Test
    @DisplayName("Should build correct consommation ID")
    void testBuildConsommationId() {
        // When
        int id = consommationService.buildConsommationId();

        // Then
        LocalDate now = LocalDate.now();
        int expectedId = Integer.parseInt(String.format("%04d%02d", now.getYear(), now.getMonthValue()));
        assertEquals(expectedId, id, "Should create ID in yyyyMM format");
    }

    @Test
    @DisplayName("Should build consommation ID for specific date")
    void testBuildConsommationId_SpecificDate() {
        // Given
        TestEntity entity = new TestEntity();
        LocalDateTime specificDate = LocalDateTime.of(2024, 3, 15, 10, 0);
        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, 100, specificDate, saver);

        // Then
        Consommation conso = entity.getConsommations().iterator().next();
        assertEquals(202403, conso.getId(), "Should use specific date for ID");
    }

    @Test
    @DisplayName("Should update timestamp when updating consommation")
    void testUpdateConsommation_UpdatesTimestamp() {
        // Given
        TestEntity entity = new TestEntity();
        LocalDateTime oldTimestamp = LocalDateTime.now().minusHours(1);
        entity.setUpdated(oldTimestamp);

        Consumer<TestEntity> saver = mock(Consumer.class);

        // When
        consommationService.updateConsommation(entity, 100, LocalDateTime.now(), saver);

        // Then
        assertNotNull(entity.getUpdated());
        assertTrue(entity.getUpdated().isAfter(oldTimestamp), "Should update timestamp");
    }

    // Test entities

    private static class TestEntity implements ConsommationService.HasConsommation {

        private Set<Consommation> consommations = new HashSet<>();
        private Number consoMensuelle;
        private LocalDateTime updated;

        @Override
        public Set<Consommation> getConsommations() {
            return consommations;
        }

        @Override
        public void setConsommations(Set<Consommation> consommations) {
            this.consommations = consommations;
        }

        @Override
        public Number getConsoMensuelle() {
            return consoMensuelle;
        }

        @Override
        public void setConsoMensuelle(Number consoMensuelle) {
            if (consoMensuelle != null) {
                // Mimic the behavior of real entities (Long type)
                this.consoMensuelle = consoMensuelle.longValue();
            } else {
                this.consoMensuelle = null;
            }
        }

        @Override
        public void setUpdated(LocalDateTime updated) {
            this.updated = updated;
        }

        public LocalDateTime getUpdated() {
            return updated;
        }
    }

    private static class TestEntityWithInteger implements ConsommationService.HasConsommation {

        private Set<Consommation> consommations = new HashSet<>();
        private Number consoMensuelle;
        private LocalDateTime updated;

        @Override
        public Set<Consommation> getConsommations() {
            return consommations;
        }

        @Override
        public void setConsommations(Set<Consommation> consommations) {
            this.consommations = consommations;
        }

        @Override
        public Number getConsoMensuelle() {
            return consoMensuelle;
        }

        @Override
        public void setConsoMensuelle(Number consoMensuelle) {
            if (consoMensuelle != null) {
                // Mimic Integer type behavior
                this.consoMensuelle = consoMensuelle.intValue();
            } else {
                this.consoMensuelle = null;
            }
        }

        @Override
        public void setUpdated(LocalDateTime updated) {
            this.updated = updated;
        }
    }
}
