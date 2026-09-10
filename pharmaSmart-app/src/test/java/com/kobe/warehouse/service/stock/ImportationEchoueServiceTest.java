package com.kobe.warehouse.service.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.kobe.warehouse.domain.ImportationEchoue;
import com.kobe.warehouse.domain.ImportationEchoueLigne;
import com.kobe.warehouse.repository.ImportationEchoueLigneRepository;
import com.kobe.warehouse.repository.ImportationEchoueRepository;
import com.kobe.warehouse.service.dto.OrderItem;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportationEchoueService")
class ImportationEchoueServiceTest {

    @Mock
    private ImportationEchoueRepository importationEchoueRepository;

    @Mock
    private ImportationEchoueLigneRepository importationEchoueLigneRepository;

    private ImportationEchoueService service;

    @BeforeEach
    void setUp() {
        service = new ImportationEchoueService(importationEchoueRepository, importationEchoueLigneRepository);
    }

    private static OrderItem orderItem() {
        return new OrderItem()
            .setProduitCip("1234567")
            .setProduitEan("3400931234567")
            .setQuantityReceived(12)
            .setPrixAchat(850)
            .setPrixUn(1200d)
            .setUg(2)
            .setTva(18d)
            .setDatePeremption("2027-06-30");
    }

    private ImportationEchoue capturerEnregistrement() {
        ArgumentCaptor<ImportationEchoue> captor = ArgumentCaptor.forClass(ImportationEchoue.class);
        verify(importationEchoueRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("enregistre l en-tete avec l objet d origine et le drapeau commande")
    void enregistreLEnTeteCommande() {
        service.save(77, true, List.of());

        ImportationEchoue saved = capturerEnregistrement();
        assertThat(saved.getObjectId()).isEqualTo(77);
        assertThat(saved.isCommande()).isTrue();
        assertThat(saved.getImportationEchoueLignes()).isEmpty();
    }

    @Test
    @DisplayName("enregistre l en-tete avec le drapeau commande a faux pour une entree de stock")
    void enregistreLEnTeteEntreeStock() {
        service.save(78, false, List.of());

        assertThat(capturerEnregistrement().isCommande()).isFalse();
    }

    @Test
    @DisplayName("accepte un identifiant d objet nul")
    void objetIdNul() {
        service.save(null, true, List.of());

        assertThat(capturerEnregistrement().getObjectId()).isNull();
    }

    @Test
    @DisplayName("mappe chaque colonne de la ligne rejetee")
    void mappeLesColonnes() {
        service.save(77, true, List.of(orderItem()));

        ImportationEchoueLigne ligne = capturerEnregistrement().getImportationEchoueLignes().getFirst();
        assertThat(ligne.getProduitCip()).isEqualTo("1234567");
        assertThat(ligne.getProduitEan()).isEqualTo("3400931234567");
        assertThat(ligne.getQuantityReceived()).isEqualTo(12);
        assertThat(ligne.getPrixAchat()).isEqualTo(850);
        assertThat(ligne.getPrixUn()).isEqualTo(1200);
        assertThat(ligne.getUg()).isEqualTo(2);
        assertThat(ligne.getCodeTva()).isEqualTo(18);
        assertThat(ligne.getDatePeremption()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    @DisplayName("rattache chaque ligne a son en-tete")
    void rattacheLesLignes() {
        service.save(77, true, List.of(orderItem(), orderItem()));

        ImportationEchoue saved = capturerEnregistrement();
        assertThat(saved.getImportationEchoueLignes()).hasSize(2);
        assertThat(saved.getImportationEchoueLignes()).allSatisfy(l -> assertThat(l.getImportationEchoue()).isSameAs(saved));
    }

    @Test
    @DisplayName("tronque un prix unitaire decimal vers l entier inferieur")
    void prixUnitaireTronque() {
        service.save(77, true, List.of(orderItem().setPrixUn(1299.99d)));

        assertThat(capturerEnregistrement().getImportationEchoueLignes().getFirst().getPrixUn()).isEqualTo(1299);
    }

    @Test
    @DisplayName("tronque un taux de TVA decimal")
    void tvaTronquee() {
        service.save(77, true, List.of(orderItem().setTva(5.5d)));

        assertThat(capturerEnregistrement().getImportationEchoueLignes().getFirst().getCodeTva()).isEqualTo(5);
    }

    @ParameterizedTest(name = "une TVA nulle laisse le code TVA vide")
    @NullSource
    void tvaNulle(Double tva) {
        service.save(77, true, List.of(orderItem().setTva(tva)));

        assertThat(capturerEnregistrement().getImportationEchoueLignes().getFirst().getCodeTva()).isNull();
    }

    @ParameterizedTest(name = "une date de peremption [{0}] laisse la colonne vide")
    @ValueSource(strings = { "", "   " })
    void datePeremptionBlanche(String date) {
        service.save(77, true, List.of(orderItem().setDatePeremption(date)));

        assertThat(capturerEnregistrement().getImportationEchoueLignes().getFirst().getDatePeremption()).isNull();
    }

    @Test
    @DisplayName("une date de peremption nulle laisse la colonne vide")
    void datePeremptionNulle() {
        service.save(77, true, List.of(orderItem().setDatePeremption(null)));

        assertThat(capturerEnregistrement().getImportationEchoueLignes().getFirst().getDatePeremption()).isNull();
    }

    @Test
    @DisplayName("une date de peremption non ISO remonte en erreur")
    void datePeremptionInvalide() {
        List<OrderItem> items = List.of(orderItem().setDatePeremption("30/06/2027"));

        assertThatThrownBy(() -> service.save(77, true, items)).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("le depot des lignes n est pas sollicite directement")
    void aucunAccesDirectAuDepotDesLignes() {
        service.save(77, true, List.of(orderItem()));

        verifyNoInteractions(importationEchoueLigneRepository);
    }
}
