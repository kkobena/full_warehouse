package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.repository.ProduitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.kobe.warehouse.domain.enumeration.TypeProduit;
/**
 * Integration tests for the {@link ProduitResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class ProduitResourceIT {

    private static final String DEFAULT_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_LIBELLE = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_IMAGE_URL = "AAAAAAAAAA";
    private static final String UPDATED_IMAGE_URL = "BBBBBBBBBB";

    private static final TypeProduit DEFAULT_TYPE_PRODUIT = TypeProduit.DETAIL;
    private static final TypeProduit UPDATED_TYPE_PRODUIT = TypeProduit.PACKAGE;

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;

    private static final Integer DEFAULT_COST_AMOUNT = 1;
    private static final Integer UPDATED_COST_AMOUNT = 2;

    private static final Integer DEFAULT_REGULAR_UNIT_PRICE = 1;
    private static final Integer UPDATED_REGULAR_UNIT_PRICE = 2;

    private static final Integer DEFAULT_NET_UNIT_PRICE = 1;
    private static final Integer UPDATED_NET_UNIT_PRICE = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_ITEM_QTY = 0;
    private static final Integer UPDATED_ITEM_QTY = 1;

    private static final Integer DEFAULT_ITEM_COST_AMOUNT = 0;
    private static final Integer UPDATED_ITEM_COST_AMOUNT = 1;

    private static final Integer DEFAULT_ITEM_REGULAR_UNIT_PRICE = 0;
    private static final Integer UPDATED_ITEM_REGULAR_UNIT_PRICE = 1;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProduitMockMvc;

    private Produit produit;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Produit createEntity(EntityManager em) {
        Produit produit = new Produit()
            .libelle(DEFAULT_LIBELLE)
            .code(DEFAULT_CODE)
            .imageUrl(DEFAULT_IMAGE_URL)
            .typeProduit(DEFAULT_TYPE_PRODUIT)
            .quantity(DEFAULT_QUANTITY)
            .costAmount(DEFAULT_COST_AMOUNT)
            .regularUnitPrice(DEFAULT_REGULAR_UNIT_PRICE)
            .netUnitPrice(DEFAULT_NET_UNIT_PRICE)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .itemQty(DEFAULT_ITEM_QTY)
            .itemCostAmount(DEFAULT_ITEM_COST_AMOUNT)
            .itemRegularUnitPrice(DEFAULT_ITEM_REGULAR_UNIT_PRICE);
        return produit;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Produit createUpdatedEntity(EntityManager em) {
        Produit produit = new Produit()
            .libelle(UPDATED_LIBELLE)
            .code(UPDATED_CODE)
            .imageUrl(UPDATED_IMAGE_URL)
            .typeProduit(UPDATED_TYPE_PRODUIT)
            .quantity(UPDATED_QUANTITY)
            .costAmount(UPDATED_COST_AMOUNT)
            .regularUnitPrice(UPDATED_REGULAR_UNIT_PRICE)
            .netUnitPrice(UPDATED_NET_UNIT_PRICE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .itemQty(UPDATED_ITEM_QTY)
            .itemCostAmount(UPDATED_ITEM_COST_AMOUNT)
            .itemRegularUnitPrice(UPDATED_ITEM_REGULAR_UNIT_PRICE);
        return produit;
    }

    @BeforeEach
    public void initTest() {
        produit = createEntity(em);
    }

    @Test
    @Transactional
    public void createProduit() throws Exception {
        int databaseSizeBeforeCreate = produitRepository.findAll().size();
        // Create the Produit
        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isCreated());

        // Validate the Produit in the database
        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeCreate + 1);
        Produit testProduit = produitList.get(produitList.size() - 1);
        assertThat(testProduit.getLibelle()).isEqualTo(DEFAULT_LIBELLE);
        assertThat(testProduit.getCode()).isEqualTo(DEFAULT_CODE);
        assertThat(testProduit.getImageUrl()).isEqualTo(DEFAULT_IMAGE_URL);
        assertThat(testProduit.getTypeProduit()).isEqualTo(DEFAULT_TYPE_PRODUIT);
        assertThat(testProduit.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testProduit.getCostAmount()).isEqualTo(DEFAULT_COST_AMOUNT);
        assertThat(testProduit.getRegularUnitPrice()).isEqualTo(DEFAULT_REGULAR_UNIT_PRICE);
        assertThat(testProduit.getNetUnitPrice()).isEqualTo(DEFAULT_NET_UNIT_PRICE);
        assertThat(testProduit.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testProduit.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
        assertThat(testProduit.getItemQty()).isEqualTo(DEFAULT_ITEM_QTY);
        assertThat(testProduit.getItemCostAmount()).isEqualTo(DEFAULT_ITEM_COST_AMOUNT);
        assertThat(testProduit.getItemRegularUnitPrice()).isEqualTo(DEFAULT_ITEM_REGULAR_UNIT_PRICE);
    }

    @Test
    @Transactional
    public void createProduitWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = produitRepository.findAll().size();

        // Create the Produit with an existing ID
        produit.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        // Validate the Produit in the database
        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkLibelleIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setLibelle(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCodeIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setCode(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeProduitIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setTypeProduit(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setQuantity(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCostAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setCostAmount(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkRegularUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setRegularUnitPrice(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setNetUnitPrice(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setCreatedAt(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setUpdatedAt(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkItemQtyIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setItemQty(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkItemCostAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setItemCostAmount(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkItemRegularUnitPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = produitRepository.findAll().size();
        // set the field null
        produit.setItemRegularUnitPrice(null);

        // Create the Produit, which fails.


        restProduitMockMvc.perform(post("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllProduits() throws Exception {
        // Initialize the database
        produitRepository.saveAndFlush(produit);

        // Get all the produitList
        restProduitMockMvc.perform(get("/api/produits?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(produit.getId().intValue())))
            .andExpect(jsonPath("$.[*].libelle").value(hasItem(DEFAULT_LIBELLE)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].typeProduit").value(hasItem(DEFAULT_TYPE_PRODUIT.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].costAmount").value(hasItem(DEFAULT_COST_AMOUNT)))
            .andExpect(jsonPath("$.[*].regularUnitPrice").value(hasItem(DEFAULT_REGULAR_UNIT_PRICE)))
            .andExpect(jsonPath("$.[*].netUnitPrice").value(hasItem(DEFAULT_NET_UNIT_PRICE)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].itemQty").value(hasItem(DEFAULT_ITEM_QTY)))
            .andExpect(jsonPath("$.[*].itemCostAmount").value(hasItem(DEFAULT_ITEM_COST_AMOUNT)))
            .andExpect(jsonPath("$.[*].itemRegularUnitPrice").value(hasItem(DEFAULT_ITEM_REGULAR_UNIT_PRICE)));
    }
    
    @Test
    @Transactional
    public void getProduit() throws Exception {
        // Initialize the database
        produitRepository.saveAndFlush(produit);

        // Get the produit
        restProduitMockMvc.perform(get("/api/produits/{id}", produit.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(produit.getId().intValue()))
            .andExpect(jsonPath("$.libelle").value(DEFAULT_LIBELLE))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGE_URL))
            .andExpect(jsonPath("$.typeProduit").value(DEFAULT_TYPE_PRODUIT.toString()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.costAmount").value(DEFAULT_COST_AMOUNT))
            .andExpect(jsonPath("$.regularUnitPrice").value(DEFAULT_REGULAR_UNIT_PRICE))
            .andExpect(jsonPath("$.netUnitPrice").value(DEFAULT_NET_UNIT_PRICE))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.itemQty").value(DEFAULT_ITEM_QTY))
            .andExpect(jsonPath("$.itemCostAmount").value(DEFAULT_ITEM_COST_AMOUNT))
            .andExpect(jsonPath("$.itemRegularUnitPrice").value(DEFAULT_ITEM_REGULAR_UNIT_PRICE));
    }
    @Test
    @Transactional
    public void getNonExistingProduit() throws Exception {
        // Get the produit
        restProduitMockMvc.perform(get("/api/produits/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateProduit() throws Exception {
        // Initialize the database
        produitRepository.saveAndFlush(produit);

        int databaseSizeBeforeUpdate = produitRepository.findAll().size();

        // Update the produit
        Produit updatedProduit = produitRepository.findById(produit.getId()).get();
        // Disconnect from session so that the updates on updatedProduit are not directly saved in db
        em.detach(updatedProduit);
        updatedProduit
            .libelle(UPDATED_LIBELLE)
            .code(UPDATED_CODE)
            .imageUrl(UPDATED_IMAGE_URL)
            .typeProduit(UPDATED_TYPE_PRODUIT)
            .quantity(UPDATED_QUANTITY)
            .costAmount(UPDATED_COST_AMOUNT)
            .regularUnitPrice(UPDATED_REGULAR_UNIT_PRICE)
            .netUnitPrice(UPDATED_NET_UNIT_PRICE)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .itemQty(UPDATED_ITEM_QTY)
            .itemCostAmount(UPDATED_ITEM_COST_AMOUNT)
            .itemRegularUnitPrice(UPDATED_ITEM_REGULAR_UNIT_PRICE);

        restProduitMockMvc.perform(put("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedProduit)))
            .andExpect(status().isOk());

        // Validate the Produit in the database
        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeUpdate);
        Produit testProduit = produitList.get(produitList.size() - 1);
        assertThat(testProduit.getLibelle()).isEqualTo(UPDATED_LIBELLE);
        assertThat(testProduit.getCode()).isEqualTo(UPDATED_CODE);
        assertThat(testProduit.getImageUrl()).isEqualTo(UPDATED_IMAGE_URL);
        assertThat(testProduit.getTypeProduit()).isEqualTo(UPDATED_TYPE_PRODUIT);
        assertThat(testProduit.getQuantity()).isEqualTo(UPDATED_QUANTITY);
        assertThat(testProduit.getCostAmount()).isEqualTo(UPDATED_COST_AMOUNT);
        assertThat(testProduit.getRegularUnitPrice()).isEqualTo(UPDATED_REGULAR_UNIT_PRICE);
        assertThat(testProduit.getNetUnitPrice()).isEqualTo(UPDATED_NET_UNIT_PRICE);
        assertThat(testProduit.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testProduit.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
        assertThat(testProduit.getItemQty()).isEqualTo(UPDATED_ITEM_QTY);
        assertThat(testProduit.getItemCostAmount()).isEqualTo(UPDATED_ITEM_COST_AMOUNT);
        assertThat(testProduit.getItemRegularUnitPrice()).isEqualTo(UPDATED_ITEM_REGULAR_UNIT_PRICE);
    }

    @Test
    @Transactional
    public void updateNonExistingProduit() throws Exception {
        int databaseSizeBeforeUpdate = produitRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProduitMockMvc.perform(put("/api/produits").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(produit)))
            .andExpect(status().isBadRequest());

        // Validate the Produit in the database
        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteProduit() throws Exception {
        // Initialize the database
        produitRepository.saveAndFlush(produit);

        int databaseSizeBeforeDelete = produitRepository.findAll().size();

        // Delete the produit
        restProduitMockMvc.perform(delete("/api/produits/{id}", produit.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Produit> produitList = produitRepository.findAll();
        assertThat(produitList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
