package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.errors.InvalidPhoneNumberException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssuredCustomerManagerImplTest {

    @Mock
    private AssuredCustomerRepository repository;

    private AssuredCustomerManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new AssuredCustomerManagerImpl(repository);
    }

    @Test
    void getAyantDroitFromId_NullRetourneOptionalVide() {
        assertEquals(Optional.empty(), manager.getAyantDroitFromId(null));
    }

    @Test
    void getAyantDroitFromId_ConstruitUneReference() {
        AssuredCustomer result = manager.getAyantDroitFromId(12).orElseThrow();
        assertEquals(12, result.getId());
    }

    @Test
    void updateAssuredCustomer_DtoNull_NeSauvegardePas() {
        manager.updateAssuredCustomer(new AssuredCustomer(), null);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAssuredCustomer_DonneesValides_MetAJourEtSauvegarde() {
        AssuredCustomer customer = new AssuredCustomer();
        AssuredCustomerDTO dto = new AssuredCustomerDTO();
        dto.setFirstName("Aya");
        dto.setLastName("Konan");
        dto.setPhone("+2250700000000");
        dto.setNumAyantDroit("AD-42");

        manager.updateAssuredCustomer(customer, dto);

        assertEquals("Aya", customer.getFirstName());
        assertEquals("Konan", customer.getLastName());
        assertEquals("+2250700000000", customer.getPhone());
        assertEquals("AD-42", customer.getNumAyantDroit());
        verify(repository).save(customer);
    }

    @Test
    void updateAssuredCustomer_TelephoneInvalide_RefuseSansSauvegarder() {
        AssuredCustomer customer = new AssuredCustomer();
        AssuredCustomerDTO dto = new AssuredCustomerDTO();
        dto.setFirstName("Aya");
        dto.setLastName("Konan");
        dto.setPhone("incorrect");

        assertThrows(InvalidPhoneNumberException.class,
            () -> manager.updateAssuredCustomer(customer, dto));

        verify(repository, never()).save(customer);
    }

    @Test
    void isSameCustomer_CompareLesIdentifiants() {
        AssuredCustomer customer = new AssuredCustomer();
        customer.setId(7);
        AssuredCustomerDTO same = new AssuredCustomerDTO();
        same.setId(7);
        AssuredCustomerDTO other = new AssuredCustomerDTO();
        other.setId(8);

        assertTrue(manager.isSameCustomer(customer, same));
        assertFalse(manager.isSameCustomer(customer, other));
    }
}

