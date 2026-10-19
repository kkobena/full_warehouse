package com.kobe.warehouse.repository.exports;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;

/**
 * Assemble la requête d'un export : la requête déclarée par {@link RequeteExport}, à laquelle s'ajoutent les prédicats des
 * filtres retenus, puis ne lie que les paramètres que la requête ainsi obtenue déclare — un export sans période n'a pas de
 * {@code :du}, un export sans filtre de famille n'a pas de {@code :familles}.
 */
@Repository
public class ExportDonneesRepositoryImpl implements ExportDonneesRepository {

    private static final int TAILLE_LOT = 500;
    private static final String RACINE_VENTE = "{v}";
    /** Un achat n'est exporté qu'une fois reçu ; le CA de l'officine a la même définition que le pilotage. */
    private static final List<OrderStatut> ACHATS_RECUS = List.of(OrderStatut.RECEIVED, OrderStatut.CLOSED);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Stream<Object[]> lireLignes(ExportDonnees export, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        RequeteExport requete = RequeteExport.lirePour(export);
        Map<FiltreExport, List<String>> retenus = lireFiltresRetenus(export, filtres);
        Query query = entityManager.createQuery(ecrire(requete, retenus));
        for (Parameter<?> parametre : query.getParameters()) {
            query.setParameter(parametre.getName(), lireValeur(parametre.getName(), du, au, retenus));
        }
        return query.setHint(HINT_FETCH_SIZE, TAILLE_LOT).setHint(HINT_READ_ONLY, true).getResultStream().map(ligne -> (Object[]) ligne);
    }

    /** Les filtres que l'export annonce et dont une valeur a été retenue ; les autres ne touchent pas la requête. */
    private Map<FiltreExport, List<String>> lireFiltresRetenus(ExportDonnees export, Map<FiltreExport, List<String>> filtres) {
        Map<FiltreExport, List<String>> retenus = new HashMap<>();
        if (filtres == null) {
            return retenus;
        }
        for (FiltreExport filtre : export.getFiltres()) {
            List<String> valeurs = filtres.get(filtre);
            if (valeurs != null && !valeurs.isEmpty()) {
                retenus.put(filtre, valeurs);
            }
        }
        return retenus;
    }

    private String ecrire(RequeteExport requete, Map<FiltreExport, List<String>> retenus) {
        List<String> conditions = new ArrayList<>();
        if (requete.getConditions() != null) {
            conditions.add(requete.getConditions());
        }
        for (FiltreExport filtre : retenus.keySet()) {
            conditions.add(filtre.getPredicat().replace(RACINE_VENTE, requete.getRacineVente()));
        }
        StringBuilder jpql = new StringBuilder(requete.getProjection()).append(' ').append(requete.getOrigine());
        if (!conditions.isEmpty()) {
            jpql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return jpql.append(' ').append(requete.getTri()).toString();
    }

    private Object lireValeur(String parametre, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> retenus) {
        switch (parametre) {
            case "du":
                return du;
            case "au":
                return au;
            case "statut":
                return SalesStatut.CLOSED;
            case "categories":
                return CategorieChiffreAffaire.officine();
            case "statuts":
                return ACHATS_RECUS;
            default:
                break;
        }
        for (Map.Entry<FiltreExport, List<String>> filtre : retenus.entrySet()) {
            if (filtre.getKey().getParametre().equals(parametre)) {
                return filtre.getValue().stream().map(filtre.getKey().getSource()::convertir).toList();
            }
        }
        throw new IllegalStateException("Paramètre d'export inconnu : " + parametre);
    }
}
