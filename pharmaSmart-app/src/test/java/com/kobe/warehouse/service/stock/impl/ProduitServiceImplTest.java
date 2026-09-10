package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.domain.enumeration.ProduitFlag;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.dto.SubstitutDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProduitServiceImpl")
class ProduitServiceImplTest {

    private static final int PRODUIT_ID = 500;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private CustomizedProductService customizedProductService;

    @Mock
    private RayonRepository rayonRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private LogsService logsService;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private RayonProduitRepository rayonProduitRepository;

    @Mock
    private SuggestionReassortService suggestionReassortService;

    @Mock
    private SubstitutRepository substitutRepository;

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    @Mock
    private PrixRererenceService prixReferenceService;

    @Mock
    private DataMatrixParserService dataMatrixParserService;

    @Mock
    private SalesLineRepository salesLineRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private ProduitServiceImpl service;

    private Magasin magasin;
    private Storage principal;
    private Storage reserve;
    private Rayon rayon;

    @BeforeEach
    void setUp() {
        service = new ProduitServiceImpl(
            produitRepository,
            customizedProductService,
            rayonRepository,
            new ObjectMapper(),
            storageService,
            logsService,
            stockProduitRepository,
            rayonProduitRepository,
            suggestionReassortService,
            substitutRepository,
            fournisseurProduitRepository,
            prixReferenceService,
            dataMatrixParserService,
            salesLineRepository,
            orderLineRepository,
            inventoryTransactionService
        );
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        magasin = new Magasin();
        magasin.setId(1);
        principal = storage(2, StorageType.PRINCIPAL);
        reserve = storage(3, StorageType.SAFETY_STOCK);
        rayon = new Rayon();
        rayon.setId(4);
        rayon.setStorage(principal);

        when(storageService.getConnectedUserMagasin()).thenReturn(magasin);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(principal);
        when(rayonRepository.getReferenceById(4)).thenReturn(rayon);
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> {
            Produit p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(PRODUIT_ID);
            }
            return p;
        });
        when(stockProduitRepository.save(any(StockProduit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    private Storage storage(int id, StorageType type) {
        Storage s = new Storage();
        s.setId(id);
        s.setStorageType(type);
        s.setMagasin(magasin);
        return s;
    }

    /** DTO minimal accepte par ProduitBuilder (libelle, prix et drapeaux non nuls). */
    private ProduitDTO dto() {
        ProduitDTO dto = new ProduitDTO()
            .setLibelle(" doliprane 1000mg ")
            .setRegularUnitPrice(800)
            .setCostAmount(400)
            .setDeconditionnable(false)
            .setRayonId(4)
            .setCodeCip("1234567")
            .setFournisseurId(3)
            .setQtyAppro(10)
            .setQtySeuilMini(5);
        dto.setTvaId(5);
        return dto;
    }

    private Produit produit() {
        Produit p = new Produit();
        p.setId(PRODUIT_ID);
        p.setLibelle("DOLIPRANE 1000MG");
        p.setCostAmount(400);
        p.setRegularUnitPrice(800);
        p.setRayonProduits(new HashSet<>(Set.of(new RayonProduit().setRayon(rayon).setProduit(p))));
        p.setStockProduits(new HashSet<>());
        p.setProduits(new ArrayList<>());
        // champs requis par ProduitBuilder pour mapper un produit en DTO
        p.setCodeRemise(com.kobe.warehouse.domain.enumeration.CodeRemise.NONE);
        p.setTypeProduit(TypeProduit.PACKAGE);
        p.setStatus(Status.ENABLE);
        FournisseurProduit principalFp = fournisseurProduitPrincipal();
        principalFp.setProduit(p);
        p.setFournisseurProduits(new HashSet<>(Set.of(principalFp)));
        p.setFournisseurProduitPrincipal(principalFp);
        return p;
    }

    /** ProduitBuilder exige au moins un StockProduit pour mapper un produit en DTO. */
    private Produit produitMappable() {
        Produit p = produit();
        StockProduit sp = stock(principal, 10, 0);
        sp.setProduit(p);
        p.getStockProduits().add(sp);
        return p;
    }

    private StockProduit stock(Storage storage, int qtyStock, int qtyUg) {
        StockProduit sp = new StockProduit();
        sp.setStorage(storage);
        sp.setQtyStock(qtyStock);
        sp.setQtyUG(qtyUg);
        sp.setQtyVirtual(qtyStock);
        return sp;
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("cree un produit boite, ses stocks et journalise la creation")
        void creeUnProduitBoite() {
            Long id = service.save(dto());

            assertThat(id).isEqualTo(500L);
            ArgumentCaptor<Produit> captor = ArgumentCaptor.forClass(Produit.class);
            verify(produitRepository).save(captor.capture());
            assertThat(captor.getValue().getLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(captor.getValue().getTypeProduit()).isEqualTo(TypeProduit.PACKAGE);
            verify(stockProduitRepository).saveAll(any());
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.CREATE_PRODUCT),
                anyString(),
                org.mockito.ArgumentMatchers.eq("500")
            );
        }

        @Test
        @DisplayName("cree la reserve quand un seuil de reserve est fourni")
        void creeLaReserve() {
            service.save(dto().setSeuilMini(2));

            ArgumentCaptor<Produit> captor = ArgumentCaptor.forClass(Produit.class);
            verify(produitRepository).save(captor.capture());
            assertThat(captor.getValue().getStockProduits()).hasSize(2);
        }

        @Test
        @DisplayName("un produit DETAIL est reroute vers la creation de detail")
        void produitDetail() {
            Produit parent = produit();
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(parent);

            service.save(dto().setTypeProduit(TypeProduit.DETAIL).setProduitId(PRODUIT_ID));

            ArgumentCaptor<Produit> captor = ArgumentCaptor.forClass(Produit.class);
            verify(produitRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getValue().getTypeProduit()).isEqualTo(TypeProduit.DETAIL);
        }

        @Test
        @DisplayName("un type PACKAGE explicite reste une creation de boite")
        void typePackageExplicite() {
            service.save(dto().setTypeProduit(TypeProduit.PACKAGE));

            ArgumentCaptor<Produit> captor = ArgumentCaptor.forClass(Produit.class);
            verify(produitRepository).save(captor.capture());
            assertThat(captor.getValue().getTypeProduit()).isEqualTo(TypeProduit.PACKAGE);
        }

        @Test
        @DisplayName("un colisage et un minimum de commande absents retombent sur les defauts")
        void colisageAbsent() {
            com.kobe.warehouse.service.dto.FournisseurProduitDTO extra =
                new com.kobe.warehouse.service.dto.FournisseurProduitDTO();
            extra.setCodeCip("7654321");
            extra.setFournisseurId(4);

            service.save(dto().setFournisseurProduits(List.of(extra)));

            ArgumentCaptor<FournisseurProduit> captor = ArgumentCaptor.forClass(FournisseurProduit.class);
            verify(fournisseurProduitRepository).save(captor.capture());
            assertThat(captor.getValue().getQteColis()).isEqualTo(1);
            assertThat(captor.getValue().getQteMinimaleCommande()).isZero();
        }

        @Test
        @DisplayName("enregistre les fournisseurs supplementaires du produit")
        void fournisseursSupplementaires() {
            com.kobe.warehouse.service.dto.FournisseurProduitDTO extra =
                new com.kobe.warehouse.service.dto.FournisseurProduitDTO();
            extra.setCodeCip("7654321");
            extra.setPrixAchat(350);
            extra.setPrixUni(700);
            extra.setFournisseurId(4);
            extra.setQteColis(6);
            extra.setQteMinimaleCommande(12);

            service.save(dto().setFournisseurProduits(List.of(extra)));

            ArgumentCaptor<FournisseurProduit> captor = ArgumentCaptor.forClass(FournisseurProduit.class);
            verify(fournisseurProduitRepository).save(captor.capture());
            assertThat(captor.getValue().getPrixAchat()).isEqualTo(350);
            assertThat(captor.getValue().getQteColis()).isEqualTo(6);
            assertThat(captor.getValue().getQteMinimaleCommande()).isEqualTo(12);
        }

        @Test
        @DisplayName("normalise un colisage et un minimum de commande absents ou nuls")
        void colisageParDefaut() {
            com.kobe.warehouse.service.dto.FournisseurProduitDTO extra =
                new com.kobe.warehouse.service.dto.FournisseurProduitDTO();
            extra.setCodeCip("7654321");
            extra.setFournisseurId(4);
            extra.setQteColis(1);
            extra.setQteMinimaleCommande(0);

            service.save(dto().setFournisseurProduits(List.of(extra)));

            ArgumentCaptor<FournisseurProduit> captor = ArgumentCaptor.forClass(FournisseurProduit.class);
            verify(fournisseurProduitRepository).save(captor.capture());
            assertThat(captor.getValue().getQteColis()).isEqualTo(1);
            assertThat(captor.getValue().getQteMinimaleCommande()).isZero();
        }

        @Test
        @DisplayName("aucun fournisseur supplementaire quand la liste est vide")
        void sansFournisseurSupplementaire() {
            service.save(dto());

            verifyNoInteractions(fournisseurProduitRepository);
        }

        @Test
        @DisplayName("enregistre les prix de reference rattaches au produit cree")
        void prixDeReference() {
            com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO prix =
                new com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO();

            service.save(dto().setPrixReference(List.of(prix)));

            assertThat(prix.getProduitId()).isEqualTo(PRODUIT_ID);
            verify(prixReferenceService).add(prix);
        }

        @Test
        @DisplayName("aucun prix de reference quand la liste est vide")
        void sansPrixDeReference() {
            service.save(dto());

            verifyNoInteractions(prixReferenceService);
        }
    }

    private FournisseurProduit fournisseurProduitPrincipal() {
        com.kobe.warehouse.domain.Fournisseur fournisseur = new com.kobe.warehouse.domain.Fournisseur();
        fournisseur.setId(3);
        fournisseur.setLibelle("LABOREX");
        FournisseurProduit fp = new FournisseurProduit();
        fp.setId(77);
        fp.setCodeCip("1234567");
        fp.setCodeEan("3400931234567");
        fp.setFournisseur(fournisseur);
        return fp;
    }

    @Nested
    @DisplayName("saveDetail")
    class SaveDetail {

        @Test
        @DisplayName("reprend le colisage du parent quand il change")
        void metAJourLeColisageDuParent() {
            Produit parent = produit();
            parent.setItemQty(1);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(parent);

            service.saveDetail(dto().setProduitId(PRODUIT_ID).setItemQty(12));

            assertThat(parent.getItemQty()).isEqualTo(12);
            verify(produitRepository, times(2)).save(any(Produit.class));
        }

        @Test
        @DisplayName("laisse le colisage du parent intact quand il est identique")
        void colisageIdentique() {
            Produit parent = produit();
            parent.setItemQty(12);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(parent);

            service.saveDetail(dto().setProduitId(PRODUIT_ID).setItemQty(12));

            verify(produitRepository, times(1)).save(any(Produit.class));
        }

        @Test
        @DisplayName("ignore un colisage nul ou non positif")
        void colisageNonPositif() {
            Produit parent = produit();
            parent.setItemQty(1);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(parent);

            service.saveDetail(dto().setProduitId(PRODUIT_ID).setItemQty(0));

            assertThat(parent.getItemQty()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("lectures")
    class Lectures {

        @Test
        @DisplayName("findAll pagine le catalogue")
        void findAllPagine() {
            when(produitRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(produitMappable())));

            assertThat(service.findAll(PageRequest.of(0, 20))).hasSize(1);
        }

        @Test
        @DisplayName("findOne renvoie vide quand le produit est introuvable")
        void findOneIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            assertThat(service.findOne(PRODUIT_ID)).isEmpty();
        }

        @Test
        @DisplayName("findOne enrichit le produit de ses dernieres dates")
        void findOneEnrichi() {
            Produit p = produitMappable();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));
            LocalDateTime vente = LocalDateTime.of(2026, 3, 1, 10, 0);
            LocalDateTime commande = LocalDateTime.of(2026, 2, 1, 10, 0);
            LocalDateTime inventaire = LocalDateTime.of(2026, 1, 1, 10, 0);
            when(customizedProductService.lastSale(any())).thenReturn(vente);
            when(customizedProductService.lastOrder(any())).thenReturn(commande);
            when(inventoryTransactionService.fetchLastDateByTypeAndProduitId(MouvementProduit.INVENTAIRE, PRODUIT_ID))
                .thenReturn(inventaire);

            ProduitDTO dto = service.findOne(PRODUIT_ID).orElseThrow();

            assertThat(dto.getLastDateOfSale()).isEqualTo(vente);
            assertThat(dto.getLastOrderDate()).isEqualTo(commande);
            assertThat(dto.getLastInventoryDate()).isEqualTo(inventaire);
        }

        @Test
        @DisplayName("findOne tolere un produit sans stock sur l emplacement de l utilisateur")
        void findOneSansStockUtilisateur() {
            Produit p = produit();
            StockProduit sp = stock(reserve, 10, 0);
            sp.setProduit(p);
            p.getStockProduits().add(sp);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));

            // aucun stock sur l emplacement de vente : le DTO est construit sans stockProduit
            assertThat(service.findOne(PRODUIT_ID)).isPresent();
        }

        @Test
        @DisplayName("findAll par critere delegue au service de recherche")
        void findAllParCritere() throws Exception {
            ProduitCriteria criteria = new ProduitCriteria();
            Page<ProduitDTO> expected = Page.empty();
            when(customizedProductService.findAll(criteria, Pageable.unpaged())).thenReturn(expected);

            assertThat(service.findAll(criteria, Pageable.unpaged())).isSameAs(expected);
        }

        @Test
        @DisplayName("findAll par critere renvoie une page vide en cas d erreur")
        void findAllParCritereEnErreur() throws Exception {
            ProduitCriteria criteria = new ProduitCriteria();
            when(customizedProductService.findAll(criteria, Pageable.unpaged())).thenThrow(new IllegalStateException("boom"));

            assertThat(service.findAll(criteria, Pageable.unpaged())).isEmpty();
        }

        @Test
        @DisplayName("findOne par critere renvoie null quand le produit est introuvable")
        void findOneParCritereIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            assertThat(service.findOne(new ProduitCriteria().setId(PRODUIT_ID))).isNull();
        }

        @Test
        @DisplayName("findOne par critere enrichit des dernieres dates")
        void findOneParCritere() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produitMappable()));
            when(customizedProductService.lastSale(any())).thenReturn(LocalDateTime.of(2026, 3, 1, 10, 0));

            ProduitDTO dto = service.findOne(new ProduitCriteria().setId(PRODUIT_ID));

            assertThat(dto).isNotNull();
            assertThat(dto.getLastDateOfSale()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
        }

        @Test
        @DisplayName("findWithCriteria delegue au service de recherche")
        void findWithCriteria() throws Exception {
            ProduitCriteria criteria = new ProduitCriteria();
            List<ProduitDTO> expected = List.of(new ProduitDTO());
            when(customizedProductService.findAll(criteria)).thenReturn(expected);

            assertThat(service.findWithCriteria(criteria)).isSameAs(expected);
        }

        @Test
        @DisplayName("findWithCriteria renvoie une liste vide en cas d erreur")
        void findWithCriteriaEnErreur() throws Exception {
            ProduitCriteria criteria = new ProduitCriteria();
            when(customizedProductService.findAll(criteria)).thenThrow(new IllegalStateException("boom"));

            assertThat(service.findWithCriteria(criteria)).isEmpty();
        }

        @Test
        @DisplayName("lastSale et lastOrder deleguent au service de recherche")
        void dernieresDates() {
            ProduitCriteria criteria = new ProduitCriteria();
            when(customizedProductService.lastSale(criteria)).thenReturn(LocalDateTime.MIN);
            when(customizedProductService.lastOrder(criteria)).thenReturn(LocalDateTime.MAX);

            assertThat(service.lastSale(criteria)).isEqualTo(LocalDateTime.MIN);
            assertThat(service.lastOrder(criteria)).isEqualTo(LocalDateTime.MAX);
        }

        @Test
        @DisplayName("getFournisseurProduitByCriteria, find et productsLiteList deleguent")
        void delegationsSimples() {
            ProduitCriteria criteria = new ProduitCriteria();
            when(customizedProductService.getFournisseurProduitByCriteria("1234567", 3)).thenReturn(Optional.empty());
            when(customizedProductService.find(criteria)).thenReturn(List.of());
            when(customizedProductService.productsLiteList(criteria, Pageable.unpaged())).thenReturn(List.of());

            assertThat(service.getFournisseurProduitByCriteria("1234567", 3)).isEmpty();
            assertThat(service.find(criteria)).isEmpty();
            assertThat(service.productsLiteList(criteria, Pageable.unpaged())).isEmpty();
        }

        @Test
        @DisplayName("findByIds delegue au depot")
        void findByIds() {
            when(produitRepository.findAllById(Set.of(PRODUIT_ID))).thenReturn(List.of(produit()));

            assertThat(service.findByIds(Set.of(PRODUIT_ID))).hasSize(1);
        }

        @Test
        @DisplayName("findReferenceById delegue au depot")
        void findReferenceById() {
            Produit p = produit();
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(p);

            assertThat(service.findReferenceById(PRODUIT_ID)).isSameAs(p);
        }

        @Test
        @DisplayName("findProduitById delegue au depot")
        void findProduitById() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            assertThat(service.findProduitById(PRODUIT_ID)).isEmpty();
        }

        @Test
        @DisplayName("findGeneriques mappe les substituts du produit")
        void findGeneriques() {
            Substitut s = new Substitut();
            s.setId(1);
            s.setProduit(produitMappable());
            s.setSubstitut(produitMappable());
            s.setType(com.kobe.warehouse.domain.enumeration.TypeSubstitut.GENERIQUE);
            when(substitutRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(List.of(s));

            List<SubstitutDTO> generiques = service.findGeneriques(PRODUIT_ID);

            assertThat(generiques).hasSize(1);
        }
    }

    @Nested
    @DisplayName("stocks")
    class Stocks {

        @Test
        @DisplayName("produitTotalStock somme les quantites physiques")
        void produitTotalStock() {
            Produit p = produit();
            p.getStockProduits().add(stock(principal, 10, 3));
            p.getStockProduits().add(stock(reserve, 5, 0));

            assertThat(service.produitTotalStock(p)).isEqualTo(15);
        }

        @Test
        @DisplayName("getProductTotalStock somme les quantites totales UG comprises")
        void getProductTotalStock() {
            Produit p = produit();
            p.getStockProduits().add(stock(principal, 10, 3));
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(p);

            assertThat(service.getProductTotalStock(PRODUIT_ID)).isEqualTo(13);
        }

        @Test
        @DisplayName("save(StockProduitDTO) rattache le stock au produit")
        void saveStockProduit() {
            Produit p = produit();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));
            StockProduitDTO dto = new StockProduitDTO();
            dto.setProduitId(PRODUIT_ID);
            dto.setQtyStock(10);
            dto.setQtyVirtual(10);
            dto.setQtyUG(2);
            dto.setStorageId(2);

            service.save(dto);

            ArgumentCaptor<StockProduit> captor = ArgumentCaptor.forClass(StockProduit.class);
            verify(stockProduitRepository).save(captor.capture());
            assertThat(captor.getValue().getProduit()).isSameAs(p);
            assertThat(captor.getValue().getQtyStock()).isEqualTo(10);
            assertThat(captor.getValue().getStorage().getId()).isEqualTo(2);
        }

        @Test
        @DisplayName("save(StockProduitDTO) refuse un produit inconnu")
        void saveStockProduitInconnu() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());
            StockProduitDTO dto = new StockProduitDTO();
            dto.setProduitId(PRODUIT_ID);

            assertThatThrownBy(() -> service.save(dto)).isInstanceOf(GenericError.class).hasMessageContaining("Produit not found");
        }

        @Test
        @DisplayName("save(StockProduitDTO) tolere un emplacement absent")
        void saveStockProduitSansEmplacement() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit()));
            StockProduitDTO dto = new StockProduitDTO();
            dto.setProduitId(PRODUIT_ID);

            service.save(dto);

            ArgumentCaptor<StockProduit> captor = ArgumentCaptor.forClass(StockProduit.class);
            verify(stockProduitRepository).save(captor.capture());
            assertThat(captor.getValue().getStorage()).isNull();
        }
    }

    @Nested
    @DisplayName("updateTotalStock")
    class UpdateTotalStock {

        @Test
        @DisplayName("un stock unique est credite sans declencher de reassort")
        void stockUnique() {
            Produit p = produit();
            StockProduit sp = stock(principal, 10, 1);
            p.getStockProduits().add(sp);

            StockProduit result = service.updateTotalStock(p, 5, 2);

            assertThat(result.getQtyStock()).isEqualTo(15);
            assertThat(result.getQtyUG()).isEqualTo(3);
            assertThat(result.getQtyVirtual()).isEqualTo(15);
            assertThat(result.getUpdatedAt()).isNotNull();
            verifyNoInteractions(suggestionReassortService);
        }

        @Test
        @DisplayName("avec plusieurs emplacements, credite le principal du magasin et propose un reassort")
        void plusieursEmplacements() {
            Produit p = produit();
            StockProduit principalStock = stock(principal, 10, 0);
            StockProduit reserveStock = stock(reserve, 4, 0);
            p.getStockProduits().add(principalStock);
            p.getStockProduits().add(reserveStock);

            StockProduit result = service.updateTotalStock(p, 5, 0);

            assertThat(result).isSameAs(principalStock);
            assertThat(principalStock.getQtyStock()).isEqualTo(15);
            assertThat(reserveStock.getQtyStock()).isEqualTo(4);
            verify(suggestionReassortService).createRayonSuggestionReassort(principalStock);
            verify(suggestionReassortService).createReserveSuggestionReassort(principalStock);
        }

        @Test
        @DisplayName("refuse un produit dont aucun emplacement principal n appartient au magasin")
        void aucunEmplacementPrincipal() {
            Produit p = produit();
            Magasin autre = new Magasin();
            autre.setId(9);
            Storage ailleurs = new Storage();
            ailleurs.setId(8);
            ailleurs.setStorageType(StorageType.PRINCIPAL);
            ailleurs.setMagasin(autre);
            p.getStockProduits().add(stock(ailleurs, 10, 0));
            p.getStockProduits().add(stock(reserve, 4, 0));

            assertThatThrownBy(() -> service.updateTotalStock(p, 5, 0))
                .isInstanceOf(GenericError.class)
                .hasMessageContaining("Stock produit introuvable");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Produit produitAModifier(Set<RayonProduit> rayonProduits) {
            Produit p = produit();
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(p);
            when(rayonProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(rayonProduits);
            return p;
        }

        private RayonProduit rayonProduit(int rayonId, StorageType type) {
            Rayon r = new Rayon();
            r.setId(rayonId);
            r.setStorage(storage(rayonId + 10, type));
            return new RayonProduit().setRayon(r);
        }

        @Test
        @DisplayName("cree un rayon quand le produit n en a aucun")
        void sansRayonExistant() {
            Produit p = produitAModifier(new HashSet<>());

            service.update(dto().setId(PRODUIT_ID));

            assertThat(p.getRayonProduits()).hasSize(1);
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.UPDATE_PRODUCT),
                anyString(),
                org.mockito.ArgumentMatchers.eq("500")
            );
        }

        @Test
        @DisplayName("remplace le rayon principal quand il change")
        void changementDeRayon() {
            Produit p = produitAModifier(new HashSet<>(Set.of(rayonProduit(9, StorageType.PRINCIPAL))));
            p.setProduits(new ArrayList<>(List.of(produit())));

            service.update(dto().setId(PRODUIT_ID).setRayonId(4));

            assertThat(p.getRayonProduits()).hasSize(1);
            assertThat(p.getRayonProduits().iterator().next().getRayon().getId()).isEqualTo(4);
            // les produits detail suivent le rayon de la boite
            verify(produitRepository, atLeastOnce()).save(any(Produit.class));
        }

        @Test
        @DisplayName("conserve le rayon quand il est deja le bon")
        void rayonInchange() {
            produitAModifier(new HashSet<>(Set.of(rayonProduit(4, StorageType.PRINCIPAL))));

            service.update(dto().setId(PRODUIT_ID).setRayonId(4));

            verify(rayonProduitRepository, times(1)).findAllByProduitId(PRODUIT_ID);
        }

        @Test
        @DisplayName("un rayon hors emplacement principal n impose pas de changement")
        void rayonNonPrincipal() {
            Produit p = produitAModifier(new HashSet<>(Set.of(rayonProduit(9, StorageType.SAFETY_STOCK))));

            service.update(dto().setId(PRODUIT_ID).setRayonId(4));

            // le rayon de reserve est conserve tel quel
            assertThat(p.getRayonProduits()).hasSize(1);
            assertThat(p.getRayonProduits().iterator().next().getRayon().getId()).isEqualTo(9);
        }

        @Test
        @DisplayName("un rayon absent de la commande laisse la ligne sans rayon")
        void rayonIdAbsent() {
            Produit p = produitAModifier(new HashSet<>());

            service.update(dto().setId(PRODUIT_ID).setRayonId(null));

            assertThat(p.getRayonProduits()).hasSize(1);
            assertThat(p.getRayonProduits().iterator().next().getRayon()).isNull();
        }

        @Test
        @DisplayName("conserve les rayons hors emplacement de vente tels quels")
        void conserveLesRayonsDeReserve() {
            RayonProduit reserveRp = rayonProduit(9, StorageType.SAFETY_STOCK);
            Produit p = produitAModifier(new HashSet<>(Set.of(reserveRp)));

            service.update(dto().setId(PRODUIT_ID).setRayonId(4));

            assertThat(p.getRayonProduits()).containsExactly(reserveRp);
        }

        @Test
        @DisplayName("repercute le rayon sur les produits detail de la boite")
        void repercuteSurLesDetails() {
            Produit p = produitAModifier(new HashSet<>());
            Produit detail = produit();
            detail.setId(501);
            p.setProduits(new ArrayList<>(List.of(detail)));
            when(rayonProduitRepository.findAllByProduitId(501)).thenReturn(new HashSet<>());

            service.update(dto().setId(PRODUIT_ID).setRayonId(4));

            assertThat(detail.getRayonProduits()).hasSize(1);
            assertThat(detail.getRayonProduits().iterator().next().getRayon().getId()).isEqualTo(4);
            verify(produitRepository).save(detail);
        }

        @Test
        @DisplayName("ventile les seuils entre emplacement de vente et reserve")
        void ventileLesSeuils() {
            Produit p = produitAModifier(new HashSet<>());
            StockProduit principalStock = stock(principal, 10, 0);
            StockProduit reserveStock = stock(reserve, 4, 0);
            p.getStockProduits().add(principalStock);
            p.getStockProduits().add(reserveStock);

            ProduitDTO dto = dto().setId(PRODUIT_ID).setQtySeuilMini(8).setSeuilMini(3).setStockReassort(20);
            dto.setStockMaxi(50);

            service.update(dto);

            assertThat(principalStock.getSeuilMini()).isEqualTo(8);
            assertThat(principalStock.getStockReassort()).isEqualTo(20);
            assertThat(principalStock.getStockMaxi()).isEqualTo(50);
            assertThat(reserveStock.getSeuilMini()).isEqualTo(3);
            verify(stockProduitRepository, times(2)).save(any(StockProduit.class));
        }

        @Test
        @DisplayName("cree la reserve quand elle manque et qu un seuil de reserve est demande")
        void creeLaReserveManquante() {
            Produit p = produitAModifier(new HashSet<>());
            p.getStockProduits().add(stock(principal, 10, 0));

            service.update(dto().setId(PRODUIT_ID).setQtySeuilMini(8).setSeuilMini(3));

            assertThat(p.getStockProduits()).hasSize(2);
        }

        @Test
        @DisplayName("ne touche pas aux stocks quand aucun seuil n est transmis")
        void sansInfoDeStock() {
            Produit p = produitAModifier(new HashSet<>());
            p.getStockProduits().add(stock(principal, 10, 0));

            service.update(dto().setId(PRODUIT_ID).setQtySeuilMini(null).setSeuilMini(null).setStockReassort(null));

            verify(stockProduitRepository, never()).save(any(StockProduit.class));
        }

        @Test
        @DisplayName("update(Produit) enregistre le produit tel quel")
        void updateEntite() {
            Produit p = produit();

            service.update(p);

            verify(produitRepository).save(p);
        }

        @Test
        @DisplayName("updateProduit enregistre et renvoie le produit")
        void updateProduit() {
            Produit p = produit();

            assertThat(service.updateProduit(p)).isSameAs(p);
        }
    }

    @Nested
    @DisplayName("updateDetail")
    class UpdateDetail {

        @Test
        @DisplayName("met a jour le libelle et les prix du detail")
        void metAJourLeDetail() {
            Produit parent = produit();
            parent.setItemQty(1);
            Produit detail = produit();
            detail.setParent(parent);
            when(produitRepository.getReferenceById(PRODUIT_ID)).thenReturn(detail);

            service.updateDetail(dto().setId(PRODUIT_ID).setItemQty(12));

            assertThat(detail.getLibelle()).isEqualTo("DOLIPRANE 1000MG");
            assertThat(detail.getRegularUnitPrice()).isEqualTo(800);
            assertThat(detail.getCostAmount()).isEqualTo(400);
            assertThat(parent.getItemQty()).isEqualTo(12);
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.UPDATE_PRODUCT),
                anyString(),
                org.mockito.ArgumentMatchers.eq("500")
            );
        }
    }

    @Nested
    @DisplayName("updateFromCommande")
    class UpdateFromCommande {

        @Test
        @DisplayName("reprend TVA et code EAN laboratoire")
        void reprendTvaEtCodeEan() {
            Produit p = produit();
            when(rayonProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(new HashSet<>());

            service.updateFromCommande(dto().setCodeEanLaboratoire("3400931234567"), p);

            assertThat(p.getCodeEanLaboratoire()).isEqualTo("3400931234567");
            assertThat(p.getTva()).isNotNull();
            assertThat(p.getUpdatedAt()).isNotNull();
            verify(produitRepository).save(p);
        }

        @Test
        @DisplayName("laisse le code EAN laboratoire intact quand il est vide")
        void codeEanVide() {
            Produit p = produit();
            p.setCodeEanLaboratoire("ANCIEN");
            when(rayonProduitRepository.findAllByProduitId(PRODUIT_ID)).thenReturn(new HashSet<>());

            service.updateFromCommande(dto().setCodeEanLaboratoire(""), p);

            assertThat(p.getCodeEanLaboratoire()).isEqualTo("ANCIEN");
        }
    }

    @Nested
    @DisplayName("recherche produit")
    class RechercheProduit {

        @Test
        @DisplayName("cherche sur le code extrait du DataMatrix")
        void codeIssuDuDataMatrix() {
            com.kobe.warehouse.service.dto.DataMatrixInfo info = com.kobe.warehouse.service.dto.DataMatrixInfo
                .builder()
                .cip13("3400930000000")
                .build();
            when(dataMatrixParserService.parse("01034009300000005")).thenReturn(Optional.of(info));
            when(produitRepository.searchProduitsJson("3400930000000", 1, 20)).thenReturn("[]");

            assertThat(service.searchProducts("01034009300000005", null, PageRequest.of(0, 20))).isEmpty();

            verify(produitRepository).searchProduitsJson("3400930000000", 1, 20);
        }

        @Test
        @DisplayName("retombe sur le terme saisi quand le DataMatrix ne donne rien")
        void termeSaisi() {
            when(dataMatrixParserService.parse("dolipra")).thenReturn(Optional.empty());
            when(produitRepository.searchProduitsJson("dolipra", 7, 20)).thenReturn("[]");

            service.searchProducts("dolipra", 7, PageRequest.of(0, 20));

            verify(produitRepository).searchProduitsJson("dolipra", 7, 20);
        }

        @Test
        @DisplayName("retombe sur le terme saisi quand le DataMatrix ne porte pas de code produit")
        void dataMatrixSansCodeProduit() {
            com.kobe.warehouse.service.dto.DataMatrixInfo info = com.kobe.warehouse.service.dto.DataMatrixInfo
                .builder()
                .batchNumber("LOT1")
                .build();
            when(dataMatrixParserService.parse("10LOT1")).thenReturn(Optional.of(info));
            when(produitRepository.searchProduitsJson("10LOT1", 1, 20)).thenReturn("[]");

            service.searchProducts("10LOT1", null, PageRequest.of(0, 20));

            verify(produitRepository).searchProduitsJson("10LOT1", 1, 20);
        }

        @Test
        @DisplayName("une charge utile illisible renvoie une liste vide")
        void chargeUtileIllisible() {
            when(dataMatrixParserService.parse(anyString())).thenReturn(Optional.empty());
            when(produitRepository.searchProduitsJson(anyString(), anyInt(), anyInt())).thenReturn("pas du json");

            assertThat(service.searchProducts("dolipra", 1, PageRequest.of(0, 20))).isEmpty();
        }

        @Test
        @DisplayName("searchProductsByStorage renvoie vide sans emplacement")
        void parEmplacementSansEmplacement() {
            assertThat(service.searchProductsByStorage(null, "dolipra", PageRequest.of(0, 20))).isEmpty();

            verifyNoInteractions(produitRepository);
        }

        @Test
        @DisplayName("searchProductsByStorage interroge le depot")
        void parEmplacement() {
            when(produitRepository.searchProductsByStorage("dolipra", 2, 20)).thenReturn("[]");

            assertThat(service.searchProductsByStorage(2, "dolipra", PageRequest.of(0, 20))).isEmpty();
        }

        @Test
        @DisplayName("searchProductsByStorage renvoie vide sur charge utile illisible")
        void parEmplacementIllisible() {
            when(produitRepository.searchProductsByStorage(anyString(), anyInt(), anyInt())).thenReturn("pas du json");

            assertThat(service.searchProductsByStorage(2, "dolipra", PageRequest.of(0, 20))).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteProduit")
    class DeleteProduit {

        @Test
        @DisplayName("refuse un produit introuvable")
        void produitIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteProduit(PRODUIT_ID))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("Produit introuvable : 500");
        }

        @Test
        @DisplayName("refuse un produit portant des ventes")
        void produitAvecVentes() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit()));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of());
            when(salesLineRepository.countByProduitId(PRODUIT_ID)).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteProduit(PRODUIT_ID))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("des ventes sont enregistrées");
        }

        @Test
        @DisplayName("refuse un produit portant des commandes")
        void produitAvecCommandes() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit()));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of());
            when(salesLineRepository.countByProduitId(PRODUIT_ID)).thenReturn(0L);
            when(orderLineRepository.existsByFournisseurProduitProduitId(PRODUIT_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteProduit(PRODUIT_ID))
                .isInstanceOf(BadRequestAlertException.class)
                .hasMessageContaining("des commandes sont enregistrées");
        }

        @Test
        @DisplayName("supprime le produit, ses donnees liees et journalise")
        void supprime() {
            Produit p = produit();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of());

            service.deleteProduit(PRODUIT_ID);

            verify(entityManager).createNativeQuery(anyString());
            verify(entityManager, atLeastOnce()).createQuery(anyString());
            verify(entityManager).detach(p);
            verify(logsService).create(
                org.mockito.ArgumentMatchers.eq(TransactionType.DELETE_PRODUCT),
                anyString(),
                org.mockito.ArgumentMatchers.eq("500")
            );
        }

        @Test
        @DisplayName("supprime d abord les produits detail rattaches")
        void supprimeLesDetails() {
            Produit p = produit();
            Produit detail = produit();
            detail.setId(501);
            detail.setLibelle("DOLIPRANE DETAIL");
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of(detail));

            service.deleteProduit(PRODUIT_ID);

            verify(entityManager).detach(detail);
            verify(entityManager).detach(p);
            ArgumentCaptor<String> comments = ArgumentCaptor.forClass(String.class);
            verify(logsService).create(any(), comments.capture(), anyString());
            assertThat(comments.getValue()).contains("DOLIPRANE DETAIL");
        }

        @Test
        @DisplayName("un detail portant des ventes bloque la suppression de la boite")
        void detailAvecVentes() {
            Produit detail = produit();
            detail.setId(501);
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit()));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of(detail));
            when(salesLineRepository.countByProduitId(501)).thenReturn(2L);

            assertThatThrownBy(() -> service.deleteProduit(PRODUIT_ID)).isInstanceOf(BadRequestAlertException.class);
        }

        @Test
        @DisplayName("delete delegue a deleteProduit")
        void delete() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(produit()));
            when(produitRepository.findAllByParentId(PRODUIT_ID)).thenReturn(List.of());

            service.delete(PRODUIT_ID);

            verify(logsService).create(org.mockito.ArgumentMatchers.eq(TransactionType.DELETE_PRODUCT), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("drapeaux et statut")
    class DrapeauxEtStatut {

        @Test
        @DisplayName("changeStatus enregistre le nouveau statut")
        void changeStatus() {
            Produit p = produit();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));

            service.changeStatus(PRODUIT_ID, Status.DISABLE);

            assertThat(p.getStatus()).isEqualTo(Status.DISABLE);
            verify(produitRepository).save(p);
        }

        @Test
        @DisplayName("changeStatus ne fait rien quand le produit est introuvable")
        void changeStatusIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            service.changeStatus(PRODUIT_ID, Status.DISABLE);

            verify(produitRepository, never()).save(any(Produit.class));
        }

        @Test
        @DisplayName("toggleGestionLot active le suivi des lots")
        void toggleGestionLot() {
            Produit p = produit();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));

            service.toggleGestionLot(PRODUIT_ID, true);

            assertThat(p.getGestionLot()).isTrue();
            assertThat(p.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("toggleGestionLot ne fait rien quand le produit est introuvable")
        void toggleGestionLotIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            service.toggleGestionLot(PRODUIT_ID, true);

            verify(produitRepository, never()).save(any(Produit.class));
        }

        @ParameterizedTest(name = "toggleFlag positionne {0}")
        @EnumSource(ProduitFlag.class)
        void toggleFlag(ProduitFlag flag) {
            Produit p = produit();
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.of(p));

            service.toggleFlag(PRODUIT_ID, flag, true);

            switch (flag) {
                case THERMOSENSIBLE -> assertThat(p.getThermosensible()).isTrue();
                case MEDICAMENT_ESSENTIEL -> assertThat(p.getEstMedicamentEssentiel()).isTrue();
                case PRODUIT_GARDE -> assertThat(p.getEstProduitGarde()).isTrue();
                case CLASSIFICATION_OVERRIDDEN -> assertThat(p.getIsClassificationOverridden()).isTrue();
            }
            verify(produitRepository).save(p);
        }

        @Test
        @DisplayName("toggleFlag ne fait rien quand le produit est introuvable")
        void toggleFlagIntrouvable() {
            when(produitRepository.findById(PRODUIT_ID)).thenReturn(Optional.empty());

            service.toggleFlag(PRODUIT_ID, ProduitFlag.THERMOSENSIBLE, true);

            verify(produitRepository, never()).save(any(Produit.class));
        }
    }

    @Nested
    @DisplayName("calculPrixMoyenPondereReception")
    class CalculPrixMoyenPondere {

        @Test
        @DisplayName("pondere l ancien et le nouveau stock sur la quantite recue")
        void pondere() {
            assertThat(service.calculPrixMoyenPondereReception(10, 400, 5, 500)).isEqualTo((10 * 400 + 5 * 500) / 5);
        }
    }
}
