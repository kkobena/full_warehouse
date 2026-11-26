package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.enumeration.CanalEnvoi;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EnvoiCommandeServiceImpl — tracer l'envoi au grossiste")
class EnvoiCommandeServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 11, 2);
    private static final CommandeId COMMANDE_ID = new CommandeId(12, ORDER_DATE);

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private StorageService storageService;

    private EnvoiCommandeServiceImpl service;
    private AppUser utilisateur;

    @BeforeEach
    void preparer() {
        service = new EnvoiCommandeServiceImpl(commandeRepository, storageService);
        utilisateur = new AppUser();
        utilisateur.setId(5);
        when(storageService.getUser()).thenReturn(utilisateur);
        when(commandeRepository.saveAndFlush(any(Commande.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Commande commande(OrderStatut statut) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        fournisseur.setLibelle("UBIPHARM");
        Commande commande = new Commande();
        commande.setId(12);
        commande.setOrderDate(ORDER_DATE);
        commande.setOrderReference("CMD-012");
        commande.setGrossAmount(90_000);
        commande.setOrderStatus(statut);
        commande.setFournisseur(fournisseur);
        when(commandeRepository.findById(COMMANDE_ID)).thenReturn(Optional.of(commande));
        return commande;
    }

    @Test
    @DisplayName("horodate l'envoi, son canal et son auteur")
    void horodaterLEnvoi() {
        Commande commande = commande(OrderStatut.REQUESTED);

        service.marquerEnvoyee(COMMANDE_ID, CanalEnvoi.TELEPHONE);

        assertThat(commande.estEnvoyee()).isTrue();
        assertThat(commande.getDateEnvoi()).isNotNull();
        assertThat(commande.getCanalEnvoi()).isEqualTo(CanalEnvoi.TELEPHONE);
        assertThat(commande.getEnvoiUser()).isSameAs(utilisateur);
    }

    @Test
    @DisplayName("ne rajeunit pas une commande deja envoyee")
    void marquageIdempotent() {
        // Relire le bon, ou le reexporter, ne doit pas effacer le retard deja constate.
        Commande commande = commande(OrderStatut.REQUESTED);
        LocalDateTime envoiInitial = LocalDateTime.of(2026, 11, 2, 9, 30);
        commande.setDateEnvoi(envoiInitial);
        commande.setCanalEnvoi(CanalEnvoi.MAIL);

        service.marquerEnvoyee(COMMANDE_ID, CanalEnvoi.PDF);

        assertThat(commande.getDateEnvoi()).isEqualTo(envoiInitial);
        assertThat(commande.getCanalEnvoi()).isEqualTo(CanalEnvoi.MAIL);
    }

    @Test
    @DisplayName("rend son etat de brouillon a une commande marquee par erreur")
    void annulerLEnvoi() {
        Commande commande = commande(OrderStatut.REQUESTED);
        commande.setDateEnvoi(LocalDateTime.now());
        commande.setCanalEnvoi(CanalEnvoi.PDF);
        commande.setEnvoiUser(utilisateur);

        service.annulerEnvoi(COMMANDE_ID);

        assertThat(commande.estEnvoyee()).isFalse();
        assertThat(commande.getCanalEnvoi()).isNull();
        assertThat(commande.getEnvoiUser()).isNull();
    }

    @Test
    @DisplayName("une commande receptionnee ne change plus d'etat d'envoi")
    void refuserUneCommandeReceptionnee() {
        // Son envoi est atteste par la livraison : le modifier fausserait le delai du grossiste.
        commande(OrderStatut.RECEIVED);

        assertThatThrownBy(() -> service.marquerEnvoyee(COMMANDE_ID, CanalEnvoi.MAIL))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("RECEIVED");
    }

    @Test
    @DisplayName("une commande introuvable remonte une erreur plutot qu'un NPE")
    void commandeIntrouvable() {
        when(commandeRepository.findById(COMMANDE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marquerEnvoyee(COMMANDE_ID, CanalEnvoi.MAIL))
                .isInstanceOf(GenericError.class);
    }
}
