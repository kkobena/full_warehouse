package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.CustomerAccount;
import com.kobe.warehouse.domain.CustomerAllergie;
import com.kobe.warehouse.domain.CustomerDossierSante;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.errors.GenericError;
import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Doublons du fichier client et leur fusion (docs/PLAN-FICHE-CLIENT.md, lot 5), sur le modèle de
 * la fusion de produits : tout l'historique des fiches source passe sur la fiche conservée, puis
 * les sources sont désactivées — jamais supprimées.
 *
 * <p>Les rattachements passent par des mises à jour en masse : {@code AssuredCustomer} porte
 * {@code orphanRemoval} sur ses tiers payants et ses ayants droit, et les déplacer par les
 * collections les supprimerait.
 */
@Service
@Transactional
public class FusionClientService {

    static final String ASSURE = "ASSURE";
    static final String STANDARD = "STANDARD";
    private static final int MAX_GROUPES = 500;
    private static final int CHIFFRES_TELEPHONE = 8;

    private final EntityManager em;
    private final LogsService logsService;

    public FusionClientService(EntityManager em, LogsService logsService) {
        this.em = em;
        this.logsService = logsService;
    }

    // ── Détection ──

    /** Fiches actives rapprochées par le nom (dans n'importe quel ordre), ou par téléphone et date de naissance. */
    @Transactional(readOnly = true)
    public List<DoublonClientGroupeDTO> doublons() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em
            .createNativeQuery(
                "SELECT id, code, first_name, last_name, phone, dat_naiss, type_assure, dtype FROM customer WHERE status = 'ENABLE'"
            )
            .getResultList();
        List<Fiche> fiches = rows
            .stream()
            .map(r ->
                new Fiche(
                    ((Number) r[0]).intValue(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    r[5] instanceof Date d ? d.toLocalDate() : (LocalDate) r[5],
                    (String) r[6],
                    "AssuredCustomer".equals(r[7]) ? ASSURE : STANDARD
                )
            )
            .toList();

        Regroupement regroupement = new Regroupement(fiches.size());
        Map<String, Integer> premiers = new HashMap<>();
        for (int i = 0; i < fiches.size(); i++) {
            Fiche f = fiches.get(i);
            for (String cle : f.cles()) {
                Integer premier = premiers.putIfAbsent(cle, i);
                if (premier != null) {
                    regroupement.unir(premier, i);
                }
            }
        }
        Map<Integer, List<Fiche>> groupes = new LinkedHashMap<>();
        for (int i = 0; i < fiches.size(); i++) {
            groupes.computeIfAbsent(regroupement.racine(i), k -> new ArrayList<>()).add(fiches.get(i));
        }
        List<List<Fiche>> candidats = groupes.values().stream().filter(g -> g.size() > 1).toList();
        Map<Integer, Object[]> activite = activite(candidats.stream().flatMap(List::stream).map(Fiche::id).toList());

        return candidats
            .stream()
            .map(g -> groupe(g, activite))
            .sorted(
                Comparator.comparingInt((DoublonClientGroupeDTO g) -> -g.criteres().size()).thenComparing(g ->
                    g.clients().getFirst().lastName() + " " + g.clients().getFirst().firstName()
                )
            )
            .limit(MAX_GROUPES)
            .toList();
    }

    private static DoublonClientGroupeDTO groupe(List<Fiche> fiches, Map<Integer, Object[]> activite) {
        List<String> criteres = new ArrayList<>();
        if (auMoinsDeux(fiches.stream().map(Fiche::nom).toList())) {
            criteres.add("Même nom");
        }
        if (auMoinsDeux(fiches.stream().map(Fiche::telephone).toList())) {
            criteres.add("Même téléphone");
        }
        if (auMoinsDeux(fiches.stream().map(Fiche::datNaiss).toList())) {
            criteres.add("Même date de naissance");
        }
        List<DoublonClientDTO> clients = fiches
            .stream()
            .map(f -> {
                Object[] a = activite.get(f.id());
                return new DoublonClientDTO(
                    f.id(),
                    f.code(),
                    f.firstName(),
                    f.lastName(),
                    f.phone(),
                    f.datNaiss(),
                    f.typeAssure(),
                    a == null ? 0 : ((Number) a[1]).longValue(),
                    a == null ? null : a[2] instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) a[2]
                );
            })
            // La fiche la plus utilisée en tête : c'est d'ordinaire celle à conserver.
            .sorted(Comparator.comparingLong(DoublonClientDTO::nombreAchats).reversed())
            .toList();
        return new DoublonClientGroupeDTO(fiches.getFirst().type(), criteres, clients);
    }

    private static boolean auMoinsDeux(List<?> valeurs) {
        List<?> renseignees = valeurs.stream().filter(Objects::nonNull).toList();
        return renseignees.size() > new LinkedHashSet<>(renseignees).size();
    }

    private Map<Integer, Object[]> activite(List<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em
            .createNativeQuery(
                "SELECT customer_id, COUNT(*), MAX(updated_at) FROM sales WHERE customer_id IN (:ids) AND statut = 'CLOSED' AND NOT canceled GROUP BY customer_id"
            )
            .setParameter("ids", ids)
            .getResultList();
        return rows.stream().collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> r));
    }

    // ── Fusion ──

    @Transactional(readOnly = true)
    public FusionClientApercuDTO apercu(Integer targetId, List<Integer> sourceIds) {
        Customer target = cible(targetId);
        Map<Integer, String> rejets = new LinkedHashMap<>();
        List<Customer> sources = sources(target, sourceIds, rejets);
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<String> avertissements = new ArrayList<>();
        for (Customer source : sources) {
            Integer id = source.getId();
            counts.merge("ventes", compter("SELECT COUNT(s) FROM Sales s WHERE s.customer.id = :id", id), Integer::sum);
            counts.merge("ventesAyantDroit", compter("SELECT COUNT(s) FROM ThirdPartySales s WHERE s.ayantDroit.id = :id", id), Integer::sum);
            counts.merge("reglementsDifferes", compter("SELECT COUNT(p) FROM DifferePayment p WHERE p.differeCustomer.id = :id", id), Integer::sum);
            counts.merge("avoirs", compter("SELECT COUNT(a) FROM AvoirClient a WHERE a.customer.id = :id", id), Integer::sum);
            counts.merge("retours", compter("SELECT COUNT(r) FROM RetourClient r WHERE r.customer.id = :id", id), Integer::sum);
            counts.merge("ayantsDroit", compter("SELECT COUNT(c) FROM AssuredCustomer c WHERE c.assurePrincipal.id = :id", id), Integer::sum);
            counts.merge("tiersPayants", tiersPayants(id).size(), Integer::sum);
            counts.merge("allergies", allergies(id).size(), Integer::sum);
        }
        if (!sources.isEmpty() && dossier(target.getId()).isPresent() && sources.stream().anyMatch(s -> dossier(s.getId()).isPresent())) {
            avertissements.add(
                "Plusieurs fiches ont un dossier santé : pathologies et notes sont réunies, le reste vient du dossier le plus récent. À relire."
            );
        }
        if (target instanceof UninsuredCustomer && compteCarnet(target.getId()) != null && sources.stream().anyMatch(s -> compteCarnet(s.getId()) != null)) {
            avertissements.add("Plusieurs fiches ont un compte carnet : les soldes sont additionnés sur celui de la fiche conservée.");
        }
        return new FusionClientApercuDTO(targetId, sources.stream().map(Customer::getId).toList(), rejets, counts, avertissements);
    }

    public FusionClientResultDTO fusionner(FusionClientRequestDTO request) {
        Customer target = cible(request.targetId());
        Map<Integer, String> rejets = new LinkedHashMap<>();
        List<Customer> sources = sources(target, request.sourceIds(), rejets);
        if (!rejets.isEmpty()) {
            throw new GenericError(String.join(" ; ", rejets.values()), "fusionClientRejetee");
        }
        if (sources.isEmpty()) {
            throw new GenericError("Aucune fiche à fusionner", "fusionClientVide");
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Customer source : sources) {
            completerIdentite(target, source);
            fusionnerDossierSante(target.getId(), source.getId(), counts);
            fusionnerCompteCarnet(target, source, counts);
            source.setStatus(Status.DISABLE);
            source.setUpdatedAt(LocalDateTime.now());
        }
        target.setUpdatedAt(LocalDateTime.now());
        em.flush();

        for (Customer source : sources) {
            Customer from = em.getReference(Customer.class, source.getId());
            Customer to = em.getReference(Customer.class, target.getId());
            counts.merge("ventes", deplacer("UPDATE Sales s SET s.customer = :to WHERE s.customer = :from", from, to), Integer::sum);
            counts.merge(
                "reglementsDifferes",
                deplacer("UPDATE DifferePayment p SET p.differeCustomer = :to WHERE p.differeCustomer = :from", from, to),
                Integer::sum
            );
            counts.merge("avoirs", deplacer("UPDATE AvoirClient a SET a.customer = :to WHERE a.customer = :from", from, to), Integer::sum);
            counts.merge("retours", deplacer("UPDATE RetourClient r SET r.customer = :to WHERE r.customer = :from", from, to), Integer::sum);
            if (source instanceof AssuredCustomer) {
                AssuredCustomer assureFrom = em.getReference(AssuredCustomer.class, source.getId());
                AssuredCustomer assureTo = em.getReference(AssuredCustomer.class, target.getId());
                counts.merge(
                    "ventesAyantDroit",
                    deplacer("UPDATE ThirdPartySales s SET s.ayantDroit = :to WHERE s.ayantDroit = :from", assureFrom, assureTo),
                    Integer::sum
                );
                counts.merge(
                    "ayantsDroit",
                    deplacer("UPDATE AssuredCustomer c SET c.assurePrincipal = :to WHERE c.assurePrincipal = :from", assureFrom, assureTo),
                    Integer::sum
                );
                counts.merge("tiersPayants", fusionnerTiersPayants(target.getId(), source.getId()), Integer::sum);
            }
            // Traces : elles suivent le patient.
            deplacer("UPDATE AlerteSanteDerogation d SET d.customerId = :to WHERE d.customerId = :from", source.getId(), target.getId());
            deplacer("UPDATE LimiteCreditDerogation d SET d.customerId = :to WHERE d.customerId = :from", source.getId(), target.getId());
            deplacer("UPDATE RelanceDiffere r SET r.customerId = :to WHERE r.customerId = :from", source.getId(), target.getId());
        }
        em.flush();
        em.clear();

        List<Integer> mergedIds = sources.stream().map(Customer::getId).toList();
        logsService.create(
            TransactionType.MERGE_CUSTOMER,
            "Fusion des clients " + mergedIds + " dans le client " + target.getId() + " (" + target.getCode() + ") : " + counts,
            target.getId().toString()
        );
        return new FusionClientResultDTO(target.getId(), mergedIds, counts);
    }

    private Customer cible(Integer targetId) {
        Customer target = Optional.ofNullable(targetId)
            .map(id -> em.find(Customer.class, id))
            .orElseThrow(() -> new GenericError("Client à conserver introuvable", "customerNotFound"));
        if (target.getStatus() != Status.ENABLE) {
            throw new GenericError("La fiche conservée doit être active", "fusionCibleInactive");
        }
        return target;
    }

    private List<Customer> sources(Customer target, List<Integer> sourceIds, Map<Integer, String> rejets) {
        List<Customer> sources = new ArrayList<>();
        Set<Integer> organismes = tiersPayants(target.getId())
            .stream()
            .map(c -> c.getTiersPayant().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Integer id : new LinkedHashSet<>(sourceIds == null ? List.<Integer>of() : sourceIds)) {
            if (id == null || id.equals(target.getId())) {
                continue;
            }
            Customer source = em.find(Customer.class, id);
            if (source == null) {
                rejets.put(id, "Client " + id + " introuvable");
            } else if (Hibernate.getClass(source) != Hibernate.getClass(target)) {
                rejets.put(id, libelle(source) + " : un assuré ne se fusionne qu'avec un assuré");
            } else if (source.getTypeAssure() != target.getTypeAssure()) {
                rejets.put(id, libelle(source) + " : un assuré principal ne se fusionne pas avec un ayant droit");
            } else {
                // Les tiers payants d'un même organisme se fondent en un seul ; les autres s'ajoutent.
                Set<Integer> apres = new LinkedHashSet<>(organismes);
                tiersPayants(id).forEach(c -> apres.add(c.getTiersPayant().getId()));
                if (apres.size() > PrioriteTiersPayant.values().length) {
                    rejets.put(id, libelle(source) + " : la fiche conservée aurait plus de " + PrioriteTiersPayant.values().length + " tiers payants");
                } else {
                    organismes.addAll(apres);
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    private static String libelle(Customer c) {
        return c.getFirstName() + " " + c.getLastName() + " (" + c.getCode() + ")";
    }

    private void completerIdentite(Customer target, Customer source) {
        if (!StringUtils.hasText(target.getPhone())) {
            target.setPhone(source.getPhone());
        }
        if (!StringUtils.hasText(target.getEmail())) {
            target.setEmail(source.getEmail());
        }
        if (target.getRemiseClient() == null) {
            target.setRemiseClient(source.getRemiseClient());
        }
        if (target instanceof AssuredCustomer t && source instanceof AssuredCustomer s) {
            if (t.getDatNaiss() == null) {
                t.setDatNaiss(s.getDatNaiss());
            }
            if (!StringUtils.hasText(t.getSexe())) {
                t.setSexe(s.getSexe());
            }
            if (!StringUtils.hasText(t.getNumAyantDroit())) {
                t.setNumAyantDroit(s.getNumAyantDroit());
            }
        }
    }

    /** Allergies réunies ; dossier : pathologies et notes réunies, le reste pris au dossier le plus récent. */
    private void fusionnerDossierSante(Integer targetId, Integer sourceId, Map<String, Integer> counts) {
        List<CustomerAllergie> connues = allergies(targetId);
        for (CustomerAllergie allergie : allergies(sourceId)) {
            if (connues.stream().anyMatch(a -> memeAllergie(a, allergie))) {
                em.remove(allergie);
            } else {
                allergie.setCustomerId(targetId);
                connues.add(allergie);
                counts.merge("allergies", 1, Integer::sum);
            }
        }
        Optional<CustomerDossierSante> source = dossier(sourceId);
        if (source.isEmpty()) {
            return;
        }
        CustomerDossierSante s = source.get();
        CustomerDossierSante t = dossier(targetId).orElse(null);
        if (t == null) {
            t = new CustomerDossierSante().setCustomerId(targetId);
            copier(s, t);
            t.setPathologies(new ArrayList<>(s.getPathologies()));
            t.setNote(s.getNote());
            em.persist(t);
        } else {
            if (s.getUpdatedAt().isAfter(t.getUpdatedAt())) {
                copier(s, t);
            }
            List<String> pathologies = new ArrayList<>(new LinkedHashSet<>(t.getPathologies()));
            s.getPathologies().stream().filter(p -> !pathologies.contains(p)).forEach(pathologies::add);
            t.setPathologies(pathologies);
            t.setNote(
                Arrays.stream(new String[] { t.getNote(), s.getNote() }).filter(StringUtils::hasText).distinct().collect(Collectors.joining("\n"))
            );
        }
        t.setUpdatedAt(LocalDateTime.now());
        em.remove(s);
        counts.merge("dossierSante", 1, Integer::sum);
    }

    private static void copier(CustomerDossierSante from, CustomerDossierSante to) {
        to.setGrossesse(from.isGrossesse());
        to.setDateTerme(from.getDateTerme());
        to.setAllaitement(from.isAllaitement());
        if (from.getPoidsKg() != null) {
            to.setPoidsKg(from.getPoidsKg());
            to.setDatePesee(from.getDatePesee());
        }
        to.setUpdatedBy(from.getUpdatedBy());
    }

    private static boolean memeAllergie(CustomerAllergie a, CustomerAllergie b) {
        if (a.getDci() != null || b.getDci() != null) {
            return a.getDci() != null && b.getDci() != null && a.getDci().getId().equals(b.getDci().getId());
        }
        return normaliser(a.getLibelle()).equals(normaliser(b.getLibelle()));
    }

    /** Carnet : repris tel quel si la fiche conservée n'en a pas, sinon soldes additionnés et ventes rattachées. */
    private void fusionnerCompteCarnet(Customer target, Customer source, Map<String, Integer> counts) {
        CustomerAccount compteSource = compteCarnet(source.getId());
        if (compteSource == null) {
            return;
        }
        CustomerAccount compteCible = compteCarnet(target.getId());
        if (compteCible == null) {
            compteSource.setCustomer((UninsuredCustomer) target);
        } else {
            compteCible.setBalance(compteCible.balance() + compteSource.balance());
            compteSource.setBalance(0);
            compteSource.setEnabled(false);
            em.flush();
            deplacer("UPDATE CashSale s SET s.account = :to WHERE s.account = :from", compteSource, compteCible);
            deplacer("UPDATE AccountTransaction t SET t.account = :to WHERE t.account = :from", compteSource, compteCible);
        }
        counts.merge("compteCarnet", 1, Integer::sum);
    }

    /**
     * Un organisme déjà présent sur la fiche conservée : ses lignes de vente et la consommation y
     * passent, puis le doublon disparaît. Sinon, le tiers payant change de fiche, à la première
     * priorité libre si la sienne est prise.
     */
    private int fusionnerTiersPayants(Integer targetId, Integer sourceId) {
        List<ClientTiersPayant> cible = tiersPayants(targetId);
        Set<PrioriteTiersPayant> prises = cible.stream().map(ClientTiersPayant::getPriorite).collect(Collectors.toCollection(() -> EnumSet.noneOf(PrioriteTiersPayant.class)));
        int n = 0;
        for (ClientTiersPayant ctp : tiersPayants(sourceId)) {
            Optional<ClientTiersPayant> meme = cible.stream().filter(c -> c.getTiersPayant().getId().equals(ctp.getTiersPayant().getId())).findFirst();
            if (meme.isPresent()) {
                Integer garde = meme.get().getId();
                deplacer("UPDATE ThirdPartySaleLine l SET l.clientTiersPayant = :to WHERE l.clientTiersPayant = :from", ctp, meme.get());
                deplacer("UPDATE ReinitialisationConsommation r SET r.clientTiersPayantId = :to WHERE r.clientTiersPayantId = :from", ctp.getId(), garde);
                ClientTiersPayant conserve = meme.get();
                LocalDate fin = Stream.of(conserve.getDateFinValidite(), ctp.getDateFinValidite())
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
                em
                    .createQuery("UPDATE ClientTiersPayant c SET c.consoMensuelle = :conso, c.dateFinValidite = :fin WHERE c.id = :id")
                    .setParameter("conso", Objects.requireNonNullElse(conserve.getConsoMensuelle(), 0L) + Objects.requireNonNullElse(ctp.getConsoMensuelle(), 0L))
                    .setParameter("fin", fin)
                    .setParameter("id", garde)
                    .executeUpdate();
                em.createQuery("DELETE FROM ClientTiersPayant c WHERE c.id = :id").setParameter("id", ctp.getId()).executeUpdate();
            } else {
                PrioriteTiersPayant priorite = prises.contains(ctp.getPriorite())
                    ? Arrays.stream(PrioriteTiersPayant.values()).filter(p -> !prises.contains(p)).findFirst().orElseThrow()
                    : ctp.getPriorite();
                prises.add(priorite);
                em
                    .createQuery("UPDATE ClientTiersPayant c SET c.assuredCustomer = :to, c.priorite = :priorite WHERE c.id = :id")
                    .setParameter("to", em.getReference(AssuredCustomer.class, targetId))
                    .setParameter("priorite", priorite)
                    .setParameter("id", ctp.getId())
                    .executeUpdate();
            }
            n++;
        }
        return n;
    }

    private List<ClientTiersPayant> tiersPayants(Integer customerId) {
        return em
            .createQuery("SELECT c FROM ClientTiersPayant c JOIN FETCH c.tiersPayant WHERE c.assuredCustomer.id = :id ORDER BY c.priorite", ClientTiersPayant.class)
            .setParameter("id", customerId)
            .getResultList();
    }

    private List<CustomerAllergie> allergies(Integer customerId) {
        return new ArrayList<>(
            em.createQuery("SELECT a FROM CustomerAllergie a WHERE a.customerId = :id", CustomerAllergie.class).setParameter("id", customerId).getResultList()
        );
    }

    private Optional<CustomerDossierSante> dossier(Integer customerId) {
        return Optional.ofNullable(em.find(CustomerDossierSante.class, customerId));
    }

    private CustomerAccount compteCarnet(Integer customerId) {
        return em
            .createQuery("SELECT a FROM CustomerAccount a WHERE a.customer.id = :id AND a.enabled = true", CustomerAccount.class)
            .setParameter("id", customerId)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private int compter(String jpql, Integer id) {
        return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult().intValue();
    }

    private int deplacer(String jpql, Object from, Object to) {
        return em.createQuery(jpql).setParameter("from", from).setParameter("to", to).executeUpdate();
    }

    static String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        return Normalizer.normalize(valeur, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", " ")
            .trim();
    }

    /** Une fiche active, et les clés qui la rapprochent d'une autre. */
    record Fiche(Integer id, String code, String firstName, String lastName, String phone, LocalDate datNaiss, String typeAssure, String type) {
        /** Prénom et nom mêlés puis triés : « KOUASSI Awa » et « AWA Kouassi » se rejoignent. */
        String nom() {
            String nom = Arrays.stream(normaliser(firstName + " " + lastName).split(" ")).filter(s -> !s.isEmpty()).sorted().collect(Collectors.joining(" "));
            return nom.isEmpty() ? null : nom;
        }

        /** Les huit derniers chiffres : l'indicatif du pays est saisi ou non selon les postes. */
        String telephone() {
            String chiffres = phone == null ? "" : phone.replaceAll("\\D", "");
            return chiffres.length() < CHIFFRES_TELEPHONE ? null : chiffres.substring(chiffres.length() - CHIFFRES_TELEPHONE);
        }

        List<String> cles() {
            String prefixe = type + "|" + typeAssure + "|";
            List<String> cles = new ArrayList<>(2);
            if (nom() != null) {
                cles.add(prefixe + "N|" + nom());
            }
            if (telephone() != null && datNaiss != null) {
                cles.add(prefixe + "TD|" + telephone() + "|" + datNaiss);
            }
            return cles;
        }
    }

    /** Union-find : deux fiches partageant une clé, directement ou de proche en proche, forment un groupe. */
    private static final class Regroupement {

        private final int[] parent;

        Regroupement(int taille) {
            parent = new int[taille];
            Arrays.setAll(parent, i -> i);
        }

        int racine(int i) {
            while (parent[i] != i) {
                parent[i] = parent[parent[i]];
                i = parent[i];
            }
            return i;
        }

        void unir(int a, int b) {
            parent[racine(a)] = racine(b);
        }
    }
}
