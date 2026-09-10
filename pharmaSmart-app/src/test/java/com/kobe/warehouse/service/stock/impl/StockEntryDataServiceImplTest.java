package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.DeliveryTotalsDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import com.kobe.warehouse.service.stock.DeliveryReceiptReportReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockEntryDataServiceImpl")
class StockEntryDataServiceImplTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 18);
    private static final CommandeId COMMANDE_ID = new CommandeId(12, ORDER_DATE);

    @Mock
    private EntityManager em;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private DeliveryReceiptReportReportService receiptReportService;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private EtiquetteExportReportServiceImpl etiquetteExportService;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    @SuppressWarnings("rawtypes")
    private CriteriaQuery criteriaQuery;

    @Mock
    @SuppressWarnings("rawtypes")
    private Root root;

    @Mock
    @SuppressWarnings("rawtypes")
    private Path path;

    @Mock
    private Predicate predicate;

    @Mock
    @SuppressWarnings("rawtypes")
    private TypedQuery typedQuery;

    @Mock
    private Query jpqlQuery;

    @Mock
    private Tuple tuple;

    @Mock
    @SuppressWarnings("rawtypes")
    private jakarta.persistence.criteria.CompoundSelection tupleSelection;

    private StockEntryDataServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new StockEntryDataServiceImpl(
            em,
            commandeRepository,
            receiptReportService,
            orderLineRepository,
            etiquetteExportService
        );

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(any(Class.class))).thenReturn(criteriaQuery);
        when(cb.createTupleQuery()).thenReturn(criteriaQuery);
        when(criteriaQuery.from(any(Class.class))).thenReturn(root);
        when(criteriaQuery.select(any(Selection.class))).thenReturn(criteriaQuery);
        when(criteriaQuery.distinct(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(criteriaQuery);
        when(criteriaQuery.orderBy(any(jakarta.persistence.criteria.Order[].class))).thenReturn(criteriaQuery);
        when(criteriaQuery.where(nullable(Predicate.class))).thenReturn(criteriaQuery);
        when(root.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(path.get(nullable(SingularAttribute.class))).thenReturn(path);
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.equal(nullable(Expression.class), any(Object.class))).thenReturn(predicate);
        when(cb.like(nullable(Expression.class), anyString())).thenReturn(predicate);
        when(cb.upper(nullable(Expression.class))).thenReturn(path);
        when(cb.between(nullable(Expression.class), any(Comparable.class), any(Comparable.class))).thenReturn(predicate);
        when(cb.countDistinct(nullable(Expression.class))).thenReturn(path);
        when(cb.count(nullable(Expression.class))).thenReturn(path);
        when(cb.sum(nullable(Expression.class))).thenReturn(path);
        when(cb.coalesce(nullable(Expression.class), any(Object.class))).thenReturn(path);
        when(cb.desc(nullable(Expression.class))).thenReturn(org.mockito.Mockito.mock(jakarta.persistence.criteria.Order.class));
        when(cb.tuple(any(Selection[].class))).thenReturn(tupleSelection);
        when(cb.conjunction()).thenReturn(predicate);
        when(em.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(em.createQuery(anyString())).thenReturn(jpqlQuery);
        when(jpqlQuery.setParameter(anyString(), any())).thenReturn(jpqlQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(typedQuery.getSingleResult()).thenReturn(0L);

        // Les specifications par defaut du depot sont renvoyees non nulles : Specification.and refuse null.
        Specification<Commande> spec = (r, q, builder) -> null;
        when(commandeRepository.byStatut(any())).thenReturn(spec);
        when(commandeRepository.hasOrderStatut(any())).thenReturn(spec);
        when(commandeRepository.between(any(LocalDate.class), any(LocalDate.class))).thenReturn(spec);
        when(commandeRepository.bySearchRef(anyString())).thenReturn(spec);
        when(commandeRepository.bySearchTerm(anyString())).thenReturn(spec);
        when(commandeRepository.byFournisseur(anyInt())).thenReturn(spec);
        when(commandeRepository.byUser(anyInt())).thenReturn(spec);
    }

    private static Commande commande(int id) {
        Commande commande = new Commande();
        commande.setId(id);
        commande.setOrderDate(ORDER_DATE);
        commande.setOrderReference("CMD-000" + id);
        commande.setReceiptReference("BL-000" + id);
        commande.setOrderStatus(OrderStatut.CLOSED);
        commande.setGrossAmount(1000);
        commande.setHtAmount(900);
        commande.setTaxAmount(100);
        commande.setDiscountAmount(0);
        commande.setUpdatedAt(LocalDateTime.now());
        AppUser user = new AppUser();
        user.setFirstName("Awa");
        user.setLastName("KOUAME");
        commande.setUser(user);
        commande.setOrderLines(new ArrayList<>());
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(3);
        fournisseur.setLibelle("LABOREX");
        commande.setFournisseur(fournisseur);
        return commande;
    }

    private static DeliveryReceiptFilterDTO filtre() {
        return new DeliveryReceiptFilterDTO()
            .setFromDate(LocalDate.of(2026, 4, 1))
            .setToDate(LocalDate.of(2026, 4, 30));
    }

    @Nested
    @DisplayName("fetchAllReceipts par filtre")
    class FetchAllReceipts {

        @Test
        @DisplayName("renvoie une page vide quand rien ne correspond")
        void aucunResultat() {
            when(typedQuery.getSingleResult()).thenReturn(0L);

            Page<DeliveryReceiptDTO> page = service.fetchAllReceipts(filtre(), PageRequest.of(0, 20));

            assertThat(page).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("un compte nul de la base est traite comme zero")
        void compteNul() {
            when(typedQuery.getSingleResult()).thenReturn(null);

            assertThat(service.fetchAllReceipts(filtre(), PageRequest.of(0, 20)).getTotalElements()).isZero();
        }

        @Test
        @DisplayName("mappe les bons trouves et reporte le total")
        void avecResultats() {
            when(typedQuery.getSingleResult()).thenReturn(3L);
            when(typedQuery.getResultList()).thenReturn(List.of(commande(12)));

            // page pleine : PageImpl conserve le total remonte par le comptage
            Page<DeliveryReceiptDTO> page = service.fetchAllReceipts(filtre(), PageRequest.of(0, 1));

            assertThat(page.getTotalElements()).isEqualTo(3L);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().getReceiptReference()).isEqualTo("BL-00012");
            assertThat(page.getContent().getFirst().getFournisseurLibelle()).isEqualTo("LABOREX");
        }

        @Test
        @DisplayName("pagine la requete quand le filtre n est pas exhaustif")
        void pagination() {
            service.fetchAllDeliveryReceipts(filtre(), PageRequest.of(2, 15));

            verify(typedQuery).setFirstResult(30);
            verify(typedQuery).setMaxResults(15);
        }

        @Test
        @DisplayName("ne pagine pas quand le filtre demande tout")
        void sansPagination() {
            service.fetchAllDeliveryReceipts(filtre().setAll(true), PageRequest.of(2, 15));

            verify(typedQuery, never()).setFirstResult(anyInt());
            verify(typedQuery, never()).setMaxResults(anyInt());
        }
    }

    @Nested
    @DisplayName("predicats de filtrage")
    class PredicatsDeFiltrage {

        @Test
        @DisplayName("filtre sur la liste de statuts quand elle est renseignee")
        void listeDeStatuts() {
            DeliveryReceiptFilterDTO filtre = filtre();
            filtre.setStatuts(Set.of(OrderStatut.CLOSED, OrderStatut.RECEIVED));

            service.fetchAllDeliveryReceipts(filtre, PageRequest.of(0, 20));

            verify(path, atLeastOnce()).in(any(java.util.Collection.class));
        }

        @Test
        @DisplayName("retombe sur le statut unique quand la liste est vide")
        void statutUnique() {
            DeliveryReceiptFilterDTO filtre = filtre();
            filtre.setStatuts(Set.of());
            filtre.setStatut(OrderStatut.CLOSED);

            service.fetchAllDeliveryReceipts(filtre, PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(OrderStatut.CLOSED));
        }

        @Test
        @DisplayName("filtre sur la reference du bon")
        void rechercheParReference() {
            service.fetchAllDeliveryReceipts(filtre().setSearchByRef("BL-2026"), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).like(nullable(Expression.class), org.mockito.ArgumentMatchers.eq("BL-2026%"));
        }

        @Test
        @DisplayName("filtre en recherche libre sur les references, codes et libelles")
        void rechercheLibre() {
            service.fetchAllDeliveryReceipts(filtre().setSearch("dolipra"), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).like(nullable(Expression.class), org.mockito.ArgumentMatchers.eq("DOLIPRA%"));
        }

        @ParameterizedTest(name = "une recherche [{0}] n ajoute aucun predicat de texte")
        @ValueSource(strings = { "" })
        void rechercheVideIgnoree(String search) {
            service.fetchAllDeliveryReceipts(filtre().setSearch(search).setSearchByRef(search), PageRequest.of(0, 20));

            verify(cb, never()).like(nullable(Expression.class), anyString());
        }

        @Test
        @DisplayName("filtre sur le fournisseur")
        void filtreSurFournisseur() {
            service.fetchAllDeliveryReceipts(filtre().setFournisseurId(3L), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(3L));
        }

        @Test
        @DisplayName("filtre sur l utilisateur")
        void filtreSurUtilisateur() {
            service.fetchAllDeliveryReceipts(filtre().setUserId(7L), PageRequest.of(0, 20));

            verify(cb, atLeastOnce()).equal(nullable(Expression.class), org.mockito.ArgumentMatchers.eq(7L));
        }

        @Test
        @DisplayName("borne toujours la periode de commande")
        void borneLaPeriode() {
            service.fetchAllDeliveryReceipts(filtre(), PageRequest.of(0, 20));

            verify(cb).between(
                nullable(Expression.class),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 4, 30))
            );
        }
    }

    @Nested
    @DisplayName("acces unitaires")
    class AccesUnitaires {

        @Test
        @DisplayName("findOneById renvoie vide quand le bon est introuvable")
        void bonIntrouvable() {
            when(commandeRepository.findById(COMMANDE_ID)).thenReturn(Optional.empty());

            assertThat(service.findOneById(COMMANDE_ID)).isEmpty();
        }

        @Test
        @DisplayName("findOneById mappe le bon trouve")
        void bonTrouve() {
            when(commandeRepository.findById(COMMANDE_ID)).thenReturn(Optional.of(commande(12)));

            assertThat(service.findOneById(COMMANDE_ID)).get().extracting(DeliveryReceiptDTO::getReceiptReference).isEqualTo("BL-00012");
        }

        @Test
        @DisplayName("exportToPdf delegue au service de rapport")
        void exportPdf() {
            Commande commande = commande(12);
            when(commandeRepository.getReferenceById(COMMANDE_ID)).thenReturn(commande);
            when(receiptReportService.export(commande)).thenReturn(new byte[] { 1, 2, 3 });

            assertThat(service.exportToPdf(COMMANDE_ID)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("printEtiquette passe les lignes du bon au service d etiquettes")
        void printEtiquette() {
            List<OrderLine> lignes = List.of(new OrderLine());
            when(orderLineRepository.findAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE)).thenReturn(lignes);
            when(etiquetteExportService.export(lignes, 4)).thenReturn(new byte[] { 9 });

            assertThat(service.printEtiquette(COMMANDE_ID, 4)).containsExactly(9);
        }

        @Test
        @DisplayName("fetchAllReceipts par terme cherche sur les douze derniers mois de bons clotures")
        void rechercheParTerme() {
            Slice<DeliveryReceiptProjection> slice = new SliceImpl<>(List.of());
            when(commandeRepository.fetchAllReceipts(anyString(), any(LocalDate.class), any(OrderStatut.class), any(Pageable.class)))
                .thenReturn(slice);

            assertThat(service.fetchAllReceipts("BL-2026")).isSameAs(slice);

            verify(commandeRepository).fetchAllReceipts(
                org.mockito.ArgumentMatchers.eq("BL-2026"),
                org.mockito.ArgumentMatchers.eq(LocalDate.now().minusMonths(12)),
                org.mockito.ArgumentMatchers.eq(OrderStatut.CLOSED),
                any(Pageable.class)
            );
        }

        @Test
        @DisplayName("findAllByCommandeIdAndCommandeOrderDate delegue au depot des lignes")
        void detailDesLignes() {
            List<DeliveryReceiptItemProjection> items = List.of();
            when(orderLineRepository.findDetailAllByCommandeIdAndCommandeOrderDate(12, ORDER_DATE)).thenReturn(items);

            assertThat(service.findAllByCommandeIdAndCommandeOrderDate(COMMANDE_ID)).isSameAs(items);
        }

        @Test
        @DisplayName("countByOrderStatusAndType ne compte que les commandes")
        void compteLesCommandes() {
            when(commandeRepository.countByOrderStatusAndType(OrderStatut.REQUESTED, TypeDeliveryReceipt.ORDER)).thenReturn(6);

            assertThat(service.countByOrderStatusAndType(OrderStatut.REQUESTED)).isEqualTo(6L);
        }
    }

    @Nested
    @DisplayName("fetchAllWithoutDetail")
    class FetchAllWithoutDetail {

        @Test
        @DisplayName("renvoie une page vide quand rien ne correspond")
        @SuppressWarnings("unchecked")
        void aucunResultat() {
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            assertThat(service.fetchAllWithoutDetail(filtre(), PageRequest.of(0, 20))).isEmpty();
        }

        @Test
        @DisplayName("trie par date de mise a jour descendante")
        @SuppressWarnings("unchecked")
        void triParDateDeMiseAJour() {
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            service.fetchAllWithoutDetail(filtre(), PageRequest.of(1, 25, Sort.by("orderReference")));

            org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(commandeRepository).findAll(any(Specification.class), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(captor.getValue().getPageSize()).isEqualTo(25);
            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "updatedAt"));
        }

        @Test
        @DisplayName("renseigne le nombre de lignes de chaque bon sans charger le detail")
        @SuppressWarnings("unchecked")
        void compteLesLignes() {
            Commande commande = commande(12);
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(commande)));
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[] { 12, ORDER_DATE, 4L });
            when(jpqlQuery.getResultList()).thenReturn(rows);

            Page<DeliveryReceiptDTO> page = service.fetchAllWithoutDetail(filtre(), PageRequest.of(0, 20));

            assertThat(page.getContent().getFirst().getItemSize()).isEqualTo(4);
            assertThat(page.getContent().getFirst().getOrderLines()).isEmpty();
        }

        @Test
        @DisplayName("un bon absent du comptage affiche zero ligne")
        @SuppressWarnings("unchecked")
        void bonSansComptage() {
            when(commandeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(commande(12))));
            when(jpqlQuery.getResultList()).thenReturn(new ArrayList<Object[]>());

            assertThat(service.fetchAllWithoutDetail(filtre(), PageRequest.of(0, 20)).getContent().getFirst().getItemSize()).isZero();
        }
    }

    @Nested
    @DisplayName("computeTotals")
    class ComputeTotals {

        @BeforeEach
        void stubTuple() {
            when(typedQuery.getSingleResult()).thenReturn(tuple);
            when(tuple.get(0)).thenReturn(3L);
            when(tuple.get(1)).thenReturn(30000L);
            when(tuple.get(2)).thenReturn(27000L);
            when(tuple.get(3)).thenReturn(3000L);
        }

        @Test
        @DisplayName("agrege le nombre de bons et les trois montants")
        void agregeLesTotaux() {
            DeliveryTotalsDTO totals = service.computeTotals(filtre());

            assertThat(totals.count()).isEqualTo(3L);
            assertThat(totals.totalGrossAmount()).isEqualTo(30000L);
            assertThat(totals.totalNetAmount()).isEqualTo(27000L);
            assertThat(totals.totalTaxAmount()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("filtre sur la liste de statuts quand elle est renseignee")
        void filtreSurListeDeStatuts() {
            DeliveryReceiptFilterDTO filtre = filtre();
            filtre.setStatuts(Set.of(OrderStatut.CLOSED));

            service.computeTotals(filtre);

            verify(commandeRepository).byStatut(any());
            verify(commandeRepository, never()).hasOrderStatut(any());
        }

        @Test
        @DisplayName("retombe sur le statut unique quand la liste est vide")
        void filtreSurStatutUnique() {
            DeliveryReceiptFilterDTO filtre = filtre();
            filtre.setStatuts(Set.of());
            filtre.setStatut(OrderStatut.RECEIVED);

            service.computeTotals(filtre);

            verify(commandeRepository).hasOrderStatut(OrderStatut.RECEIVED);
        }

        @Test
        @DisplayName("n ajoute aucun filtre de statut quand ni liste ni statut")
        void sansFiltreDeStatut() {
            DeliveryReceiptFilterDTO filtre = filtre();
            filtre.setStatuts(Set.of());

            service.computeTotals(filtre);

            verify(commandeRepository, never()).byStatut(any());
            verify(commandeRepository, never()).hasOrderStatut(any());
        }

        @Test
        @DisplayName("borne la periode sur les dates du filtre")
        void borneSurLesDatesDuFiltre() {
            service.computeTotals(filtre());

            verify(commandeRepository).between(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        }

        @Test
        @DisplayName("retombe sur les cinq derniers mois quand la periode est incomplete")
        void periodeParDefaut() {
            service.computeTotals(filtre().setToDate(null));

            verify(commandeRepository).between(LocalDate.now().minusMonths(5), LocalDate.now());
        }

        @Test
        @DisplayName("retombe sur les cinq derniers mois quand la date de debut manque")
        void periodeParDefautSansDebut() {
            service.computeTotals(filtre().setFromDate(null));

            verify(commandeRepository).between(LocalDate.now().minusMonths(5), LocalDate.now());
        }

        @Test
        @DisplayName("filtre sur la reference du bon")
        void filtreSurReference() {
            service.computeTotals(filtre().setSearchByRef("BL-2026"));

            verify(commandeRepository).bySearchRef("BL-2026");
        }

        @Test
        @DisplayName("filtre en recherche libre")
        void filtreEnRechercheLibre() {
            service.computeTotals(filtre().setSearch("dolipra"));

            verify(commandeRepository).bySearchTerm("dolipra");
        }

        @Test
        @DisplayName("une recherche vide n ajoute aucun filtre de texte")
        void rechercheVideIgnoree() {
            service.computeTotals(filtre().setSearch("").setSearchByRef(""));

            verify(commandeRepository, never()).bySearchRef(anyString());
            verify(commandeRepository, never()).bySearchTerm(anyString());
        }

        @Test
        @DisplayName("filtre sur le fournisseur")
        void filtreSurFournisseur() {
            service.computeTotals(filtre().setFournisseurId(3L));

            verify(commandeRepository).byFournisseur(3);
        }

        @Test
        @DisplayName("filtre sur l utilisateur")
        void filtreSurUtilisateur() {
            service.computeTotals(filtre().setUserId(7L));

            verify(commandeRepository).byUser(7);
        }

        @Test
        @DisplayName("aucun filtre facultatif quand rien n est transmis")
        void sansFiltreFacultatif() {
            service.computeTotals(filtre());

            verify(commandeRepository, never()).byFournisseur(anyInt());
            verify(commandeRepository, never()).byUser(anyInt());
        }
    }
}
