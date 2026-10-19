package com.kobe.warehouse.repository.exports;

import com.kobe.warehouse.domain.enumeration.SourceOptionsExport;
import com.kobe.warehouse.service.dto.exports.OptionFiltreDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Les référentiels tels qu'un filtre d'export les propose : l'identifiant en texte — c'est ce que la demande transporte — et le
 * libellé affiché.
 */
@Repository
public class OptionsFiltreExportRepositoryImpl implements OptionsFiltreExportRepository {

    private static final String DTO = "new com.kobe.warehouse.service.dto.exports.OptionFiltreDTO";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OptionFiltreDTO> lireOptions(SourceOptionsExport source) {
        String jpql =
            switch (source) {
                case FAMILLE -> DTO + "(cast(f.id AS String), f.libelle) FROM FamilleProduit f ORDER BY f.libelle";
                case LABORATOIRE -> DTO + "(cast(l.id AS String), l.libelle) FROM Laboratoire l ORDER BY l.libelle";
                case FOURNISSEUR -> DTO + "(cast(f.id AS String), f.libelle) FROM Fournisseur f ORDER BY f.libelle";
                case FORME -> DTO + "(cast(f.id AS String), f.libelle) FROM FormProduit f ORDER BY f.libelle";
                case GAMME -> DTO + "(cast(g.id AS String), g.libelle) FROM GammeProduit g ORDER BY g.libelle";
                case MAGASIN -> DTO + "(cast(m.id AS String), m.name) FROM Magasin m ORDER BY m.name";
                case UTILISATEUR -> DTO +
                "(cast(u.id AS String), concat(u.firstName, ' ', u.lastName)) FROM AppUser u WHERE u.activated = true ORDER BY u.lastName, u.firstName";
                case MODE_PAIEMENT -> DTO + "(p.code, p.libelle) FROM PaymentMode p ORDER BY p.libelle";
                default -> null;
            };
        return jpql == null ? List.of() : entityManager.createQuery("SELECT " + jpql, OptionFiltreDTO.class).getResultList();
    }
}
