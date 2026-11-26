package com.kobe.warehouse.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Poste;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.service.settings.dto.PosteRecord;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Le poste courant est retrouvé par son adresse ou son nom, et mis en cache 24 h. La réponse
 * « aucun poste » l'était aussi, et ni la création ni la suppression ne vidaient le cache : un
 * poste enregistré après le premier démarrage de l'appli de bureau restait introuvable une
 * journée, et un poste supprimé restait reconnu.
 */
@SpringJUnitConfig(PosteServiceImplCacheTest.Config.class)
@DisplayName("Postes — le cache suit les enregistrements")
class PosteServiceImplCacheTest {

    private static final String IP = "192.168.1.51";
    private static final String HOTE = "KOBENA";

    @Configuration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(EntityConstant.APP_POST_CONFIG);
        }

        @Bean
        PosteRepository posteRepository() {
            return mock(PosteRepository.class);
        }

        @Bean
        PosteService posteService(PosteRepository posteRepository) {
            return new PosteServiceImpl(posteRepository);
        }
    }

    @Autowired
    private PosteService posteService;

    @Autowired
    private PosteRepository posteRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void viderLeCache() {
        cacheManager.getCache(EntityConstant.APP_POST_CONFIG).clear();
        clearInvocations(posteRepository);
    }

    private static Poste poste(int id) {
        return new Poste().setId(id).setName("Caisse 1").setPosteNumber("P1").setAddress(IP);
    }

    @Test
    @DisplayName("un poste trouvé est servi par le cache")
    void posteTrouveEnCache() {
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.of(poste(8)));

        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).map(PosteRecord::id).contains(8);
        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).map(PosteRecord::id).contains(8);

        verify(posteRepository, times(1)).findFirstByAddressOrName(IP, HOTE);
    }

    @Test
    @DisplayName("« aucun poste » n'est pas mis en cache : un poste enregistré ensuite est trouvé")
    void posteInconnuPuisEnregistre() {
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.empty());
        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).isEmpty();

        // L'administrateur enregistre le poste — ici directement en base, sans passer par create().
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.of(poste(8)));

        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).map(PosteRecord::id).contains(8);
    }

    @Test
    @DisplayName("la suppression d'un poste vide le cache")
    void suppressionVideLeCache() {
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.of(poste(8)));
        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).isPresent();

        posteService.delete(8);
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.empty());

        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).isEmpty();
    }

    @Test
    @DisplayName("la modification d'un poste vide le cache")
    void modificationVideLeCache() {
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.of(poste(8)));
        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).map(PosteRecord::name).contains("Caisse 1");

        Poste renomme = poste(8).setName("Caisse comptoir");
        when(posteRepository.save(any(Poste.class))).thenReturn(renomme);
        posteService.create(new PosteRecord(8, "Caisse comptoir", "P1", IP));
        when(posteRepository.findFirstByAddressOrName(IP, HOTE)).thenReturn(Optional.of(renomme));

        assertThat(posteService.findFirstByAddressOrName(IP, HOTE)).map(PosteRecord::name).contains("Caisse comptoir");
    }
}
