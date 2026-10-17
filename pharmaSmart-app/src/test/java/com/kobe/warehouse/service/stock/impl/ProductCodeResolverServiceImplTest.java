package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitCodeAlias;
import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.ProduitCodeAliasRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.dto.ProductResolveResult;
import com.kobe.warehouse.service.dto.ProductResolveResult.MatchedBy;
import com.kobe.warehouse.service.dto.ProduitCodeAliasDTO;
import com.kobe.warehouse.service.stock.DataMatrixParserService.BarcodeType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductCodeResolverServiceImpl")
class ProductCodeResolverServiceImplTest {

    private static final String CIP = "3400930000000";
    private static final String EAN = "7353163006031";
    private static final String GTIN = "07353163006031";

    @Mock
    private FournisseurProduitRepository fournisseurProduitRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private ProduitCodeAliasRepository aliasRepository;

    @Mock
    private DataMatrixParserServiceImpl dataMatrixParserService;

    private ProductCodeResolverServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductCodeResolverServiceImpl(
            fournisseurProduitRepository,
            produitRepository,
            aliasRepository,
            dataMatrixParserService
        );
    }

    private Produit produit(Integer id, String libelle) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle(libelle);
        return produit;
    }

    private FournisseurProduit fournisseurProduit(Produit produit) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produit);
        return fournisseurProduit;
    }

    private ProduitCodeAlias alias(Produit produit, String code) {
        return new ProduitCodeAlias().setProduit(produit).setCode(code).setCodeType(CodeAliasType.GTIN_14);
    }

    @Nested
    @DisplayName("listerCodesCandidats")
    class ListerCodesCandidats {

        @Test
        @DisplayName("ordonne du plus sur au plus large : CIP, EAN, GTIN")
        void ordreDesCandidats() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().gtin(GTIN).ean13(EAN).cip13(CIP).build();

            assertThat(service.listerCodesCandidats(parsed)).containsExactly(CIP, EAN, GTIN);
        }

        @Test
        @DisplayName("un meme code present dans deux champs ne donne qu un candidat")
        void doublonsRetires() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().cip13(CIP).ean13(CIP).build();

            assertThat(service.listerCodesCandidats(parsed)).containsExactly(CIP);
        }

        @Test
        @DisplayName("les codes vides ou blancs sont ignores")
        void codesVidesIgnores() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().cip13("   ").ean13(EAN).build();

            assertThat(service.listerCodesCandidats(parsed)).containsExactly(EAN);
        }

        @Test
        @DisplayName("un scan sans code produit ne donne aucun candidat")
        void aucunCandidat() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().batchNumber("LOT1").build();

            assertThat(service.listerCodesCandidats(parsed)).isEmpty();
        }
    }

    @Nested
    @DisplayName("resoudreCode")
    class ResoudreCode {

        @Test
        @DisplayName("retrouve un produit par le CIP de la fiche fournisseur")
        void parCodeCip() {
            when(fournisseurProduitRepository.findAllByCodeCip(CIP)).thenReturn(List.of(fournisseurProduit(produit(7, "DOLIPRANE"))));

            List<ProductResolveResult> resultats = service.resoudreCode(CIP);

            assertThat(resultats).hasSize(1);
            assertThat(resultats.getFirst().produitId()).isEqualTo(7);
            assertThat(resultats.getFirst().libelle()).isEqualTo("DOLIPRANE");
            assertThat(resultats.getFirst().matchedCode()).isEqualTo(CIP);
            assertThat(resultats.getFirst().matchedBy()).isEqualTo(MatchedBy.CODE_CIP);
        }

        @Test
        @DisplayName("retrouve un produit par l EAN de la fiche fournisseur")
        void parCodeEan() {
            when(fournisseurProduitRepository.findAllByCodeEan(EAN)).thenReturn(List.of(fournisseurProduit(produit(8, "EFFERALGAN"))));

            assertThat(service.resoudreCode(EAN))
                .singleElement()
                .extracting(ProductResolveResult::produitId, ProductResolveResult::matchedBy)
                .containsExactly(8, MatchedBy.CODE_EAN);
        }

        @Test
        @DisplayName("retrouve un produit par l EAN laboratoire")
        void parCodeEanLabo() {
            when(produitRepository.findAllByCodeEanLaboratoire(EAN)).thenReturn(List.of(produit(9, "ASPEGIC")));

            assertThat(service.resoudreCode(EAN))
                .singleElement()
                .extracting(ProductResolveResult::produitId, ProductResolveResult::matchedBy)
                .containsExactly(9, MatchedBy.CODE_EAN_LABO);
        }

        @Test
        @DisplayName("retrouve un produit par un code alternatif memorise")
        void parAlias() {
            when(aliasRepository.findAllByCode(GTIN)).thenReturn(List.of(alias(produit(10, "SMECTA"), GTIN)));

            assertThat(service.resoudreCode(GTIN))
                .singleElement()
                .extracting(ProductResolveResult::produitId, ProductResolveResult::matchedBy)
                .containsExactly(10, MatchedBy.ALIAS);
        }

        @Test
        @DisplayName("un produit trouve par une source plus sure n est pas reinscrit par l alias")
        void pasDeDoublonEntreSources() {
            Produit produit = produit(11, "SPASFON");
            when(fournisseurProduitRepository.findAllByCodeCip(CIP)).thenReturn(List.of(fournisseurProduit(produit)));
            when(aliasRepository.findAllByCode(CIP)).thenReturn(List.of(alias(produit, CIP)));

            assertThat(service.resoudreCode(CIP))
                .singleElement()
                .extracting(ProductResolveResult::matchedBy)
                .isEqualTo(MatchedBy.CODE_CIP);
        }

        @Test
        @DisplayName("un code partage par deux produits les renvoie tous les deux")
        void codePartageParDeuxProduits() {
            when(fournisseurProduitRepository.findAllByCodeEan(EAN))
                .thenReturn(List.of(fournisseurProduit(produit(12, "BOITE DE 8")), fournisseurProduit(produit(13, "BOITE DE 16"))));

            assertThat(service.resoudreCode(EAN))
                .extracting(ProductResolveResult::produitId)
                .containsExactly(12, 13);
        }

        @Test
        @DisplayName("un code inconnu ne donne aucun resultat")
        void codeInconnu() {
            assertThat(service.resoudreCode(EAN)).isEmpty();
        }

        @Test
        @DisplayName("un code blanc n interroge aucun repository")
        void codeBlanc() {
            assertThat(service.resoudreCode("   ")).isEmpty();

            verify(fournisseurProduitRepository, never()).findAllByCodeCip(anyString());
            verify(aliasRepository, never()).findAllByCode(anyString());
        }
    }

    @Nested
    @DisplayName("resoudre")
    class Resoudre {

        @Test
        @DisplayName("s arrete au premier code candidat qui donne un resultat")
        void arretAuPremierTrouve() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().cip13(CIP).ean13(EAN).build();
            when(fournisseurProduitRepository.findAllByCodeCip(CIP)).thenReturn(List.of(fournisseurProduit(produit(14, "IMODIUM"))));

            assertThat(service.resoudre(parsed)).singleElement().extracting(ProductResolveResult::matchedCode).isEqualTo(CIP);

            verify(fournisseurProduitRepository, never()).findAllByCodeEan(EAN);
        }

        @Test
        @DisplayName("essaie le code suivant quand le premier ne donne rien")
        void replieSurLeCodeSuivant() {
            DataMatrixInfo parsed = DataMatrixInfo.builder().cip13(CIP).ean13(EAN).build();
            when(fournisseurProduitRepository.findAllByCodeEan(EAN)).thenReturn(List.of(fournisseurProduit(produit(15, "GAVISCON"))));

            assertThat(service.resoudre(parsed)).singleElement().extracting(ProductResolveResult::matchedCode).isEqualTo(EAN);
        }

        @Test
        @DisplayName("aucun code candidat : aucun resultat")
        void aucunCandidat() {
            assertThat(service.resoudre(DataMatrixInfo.builder().batchNumber("LOT1").build())).isEmpty();
        }
    }

    @Nested
    @DisplayName("enregistrerAlias")
    class EnregistrerAlias {

        @Test
        @DisplayName("cree l alias quand le produit ne le porte pas encore")
        void creation() {
            when(aliasRepository.findFirstByProduitIdAndCode(16, GTIN)).thenReturn(Optional.empty());
            when(produitRepository.getReferenceById(16)).thenReturn(produit(16, "DAFALGAN"));
            when(aliasRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ProduitCodeAlias.class)))
                .thenAnswer(invocation -> ((ProduitCodeAlias) invocation.getArgument(0)).setId(99));

            ProduitCodeAliasDTO alias = service.enregistrerAlias(16, GTIN, CodeAliasType.GTIN_14, CodeAliasSource.MANUAL);

            assertThat(alias.id()).isEqualTo(99);
            assertThat(alias.code()).isEqualTo(GTIN);
            assertThat(alias.codeType()).isEqualTo(CodeAliasType.GTIN_14);
            assertThat(alias.source()).isEqualTo(CodeAliasSource.MANUAL);
        }

        @Test
        @DisplayName("idempotent : un code deja associe est renvoye sans nouvelle ecriture")
        void idempotence() {
            ProduitCodeAlias existant = alias(produit(17, "VOGALENE"), GTIN).setId(42);
            when(aliasRepository.findFirstByProduitIdAndCode(17, GTIN)).thenReturn(Optional.of(existant));

            ProduitCodeAliasDTO alias = service.enregistrerAlias(17, GTIN, CodeAliasType.GTIN_14, CodeAliasSource.MANUAL);

            assertThat(alias.id()).isEqualTo(42);
            verify(aliasRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("les espaces autour du code sont retires avant memorisation")
        void codeNormalise() {
            when(aliasRepository.findFirstByProduitIdAndCode(18, GTIN)).thenReturn(Optional.of(alias(produit(18, "X"), GTIN).setId(1)));

            service.enregistrerAlias(18, "  " + GTIN + "  ", CodeAliasType.GTIN_14, CodeAliasSource.MANUAL);

            verify(aliasRepository).findFirstByProduitIdAndCode(18, GTIN);
        }
    }

    @Nested
    @DisplayName("deduireType")
    class DeduireType {

        @ParameterizedTest(name = "{0} donne le type {1}")
        @CsvSource({ "GTIN_14, GTIN_14", "EAN_13, EAN_13", "EAN_8, EAN_8", "CIP_13, CIP_13", "CIP_7, CIP_7" })
        void typesConnus(BarcodeType detecte, CodeAliasType attendu) {
            when(dataMatrixParserService.detectBarcodeType(anyString())).thenReturn(detecte);

            assertThat(service.deduireType("peu importe")).isEqualTo(attendu);
        }

        @ParameterizedTest(name = "{0} donne CUSTOM")
        @CsvSource({ "DATAMATRIX", "UNKNOWN" })
        void typesSansCorrespondance(BarcodeType detecte) {
            when(dataMatrixParserService.detectBarcodeType(anyString())).thenReturn(detecte);

            assertThat(service.deduireType("peu importe")).isEqualTo(CodeAliasType.CUSTOM);
        }
    }
}
