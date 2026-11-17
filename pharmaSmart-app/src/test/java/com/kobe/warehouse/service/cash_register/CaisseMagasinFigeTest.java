package com.kobe.warehouse.service.cash_register;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.service.portee.PorteeMagasinService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.impl.CashRegisterServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le magasin d'une caisse est figé à l'ouverture, et non déduit de {@code user.getMagasin()} à la
 * lecture. Ces deux essais tiennent les deux conséquences de ce choix, qui ne vont pas de soi.
 *
 * <p><b>La délégation.</b> Quand l'allocation automatique du fonds est active et que le caissier
 * n'a pas de caisse ouverte, c'est l'<b>admin</b> qui l'ouvre pour lui — {@code
 * openCashRegister(admin, caissier)}. La caisse appartient alors au caissier, et son magasin doit
 * être celui du caissier. Estampiller depuis l'utilisateur connecté aurait donné celui de l'admin.
 *
 * <p><b>La réouverture.</b> Une caisse ne se réaffecte pas : après le billetage, le caissier en
 * ouvre une autre. Chaque session prend donc le magasin du caissier <i>au moment de son
 * ouverture</i>. C'est cette maille qui protège l'historique : un caissier déplacé laisse ses
 * sessions passées là où elles ont eu lieu.
 */
@DisplayName("Caisse — le magasin est figé à l'ouverture")
class CaisseMagasinFigeTest {

    private CashRegisterRepository cashRegisterRepository;
    private CashFundService cashFundService;
    private UserService userService;
    private CashRegisterServiceImpl service;

    @BeforeEach
    void preparer() throws Exception {
        cashRegisterRepository = mock(CashRegisterRepository.class);
        cashFundService = mock(CashFundService.class);
        AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
        userService = mock(UserService.class);

        AppConfiguration allocationAutomatique = new AppConfiguration();
        allocationAutomatique.setValue("1");
        allocationAutomatique.setOtherValue("10000");
        when(appConfigurationService.findOneById(any())).thenReturn(Optional.of(allocationAutomatique));
        when(cashFundService.allocateCashFund(anyInt(), any(CashFundType.class), any(AppUser.class), any(AppUser.class)))
            .thenReturn(new CashFund());
        when(cashRegisterRepository.save(any(CashRegister.class))).thenAnswer(appel -> appel.getArgument(0));

        service = new CashRegisterServiceImpl(cashRegisterRepository, appConfigurationService, cashFundService, userService, mock(PorteeMagasinService.class));
    }

    @Test
    @DisplayName("ouverte par l'admin pour un caissier, la caisse prend le magasin du caissier")
    void laDelegationSuitLeProprietaire() throws Exception {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        Magasin parapharmacie = magasin(7, TypeMagasin.UNITE_GESTION);
        AppUser admin = utilisateur(1, "admin", officine);
        AppUser caissier = utilisateur(2, "awa", parapharmacie);
        when(userService.getUser()).thenReturn(admin);
        when(cashRegisterRepository.existsByUserIdAndStatut(anyInt(), any(CashRegisterStatut.class))).thenReturn(false);

        CashRegister caisse = service.openCashRegister(admin, caissier);

        assertThat(caisse.getUser().getLogin()).isEqualTo("awa");
        assertThat(caisse.getMagasin().getId()).isEqualTo(7);
    }

    @Test
    @DisplayName("la session rouverte après billetage prend le magasin courant du caissier")
    void laReouvertureReprendLeMagasinCourant() throws Exception {
        Magasin officine = magasin(1, TypeMagasin.OFFICINE);
        AppUser caissier = utilisateur(2, "awa", officine);
        when(userService.getUser()).thenReturn(caissier);
        when(cashRegisterRepository.existsByUserIdAndStatut(anyInt(), any(CashRegisterStatut.class))).thenReturn(false);

        CashRegister premiere = service.openCashRegister(caissier, caissier);
        assertThat(premiere.getMagasin().getId()).isEqualTo(1);

        // Le caissier change d'affectation entre deux sessions : la suivante naît ailleurs, et la
        // precedente ne bouge pas.
        caissier.setMagasin(magasin(7, TypeMagasin.UNITE_GESTION));
        CashRegister seconde = service.openCashRegister(caissier, caissier);

        assertThat(seconde.getMagasin().getId()).isEqualTo(7);
        assertThat(premiere.getMagasin().getId()).isEqualTo(1);
    }

    // ===== jeu d'essai =====

    private AppUser utilisateur(int id, String login, Magasin magasin) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setLogin(login);
        user.setMagasin(magasin);
        return user;
    }

    private Magasin magasin(int id, TypeMagasin type) {
        Magasin magasin = new Magasin();
        magasin.setId(id);
        magasin.setName("MAGASIN " + id);
        magasin.setFullName("MAGASIN COMPLET " + id);
        magasin.setTypeMagasin(type);
        return magasin;
    }
}
