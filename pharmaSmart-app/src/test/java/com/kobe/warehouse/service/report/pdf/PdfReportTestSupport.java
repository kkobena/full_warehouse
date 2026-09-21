package com.kobe.warehouse.service.report.pdf;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Le câblage commun aux tests des générateurs PDF.
 *
 * <p>Ce qu'on éprouve de ces services est le <b>modèle</b> qu'ils remettent au gabarit, pas le
 * document produit : un chiffre faux dans le modèle donne un PDF impeccablement mis en page et
 * faux, et c'est le seul défaut qui compte. Le moteur de gabarits est donc simulé et rend un
 * document minimal — Flying Saucer le met en page en quelques millisecondes, sans qu'on ait à
 * charger les vrais gabarits ni leurs ressources.
 *
 * <p>Le magasin et son registre du commerce viennent du {@link StorageService} : ce sont eux qui
 * alimentent le pied de page légal que tous les documents portent.
 */
final class PdfReportTestSupport {

    static final String REGISTRE = "RC-1";

    private PdfReportTestSupport() {}

    static FileStorageProperties proprietes() {
        return mock(FileStorageProperties.class);
    }

    /** Un moteur de gabarits qui rend toujours un document minimal mais valide. */
    static SpringTemplateEngine moteurDeGabarits() {
        SpringTemplateEngine moteur = mock(SpringTemplateEngine.class);
        when(moteur.process(anyString(), any(Context.class))).thenReturn("<html><body>document</body></html>");
        return moteur;
    }

    /** Un utilisateur rattaché à une officine dont le registre alimente le pied de page. */
    static StorageService storageService() {
        Magasin magasin = new Magasin();
        magasin.setRegistre(REGISTRE);
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUser()).thenReturn(utilisateur);
        return storageService;
    }
}
