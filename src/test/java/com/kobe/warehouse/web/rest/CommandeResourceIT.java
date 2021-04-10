package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.repository.CommandeRepository;

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
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
/**
 * Integration tests for the {@link CommandeResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class CommandeResourceIT {

    private static final String DEFAULT_ORDER_REFERNCE = "AAAAAAAAAA";
    private static final String UPDATED_ORDER_REFERNCE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_RECEIPT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_RECEIPT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final Integer DEFAULT_DISCOUNT_AMOUNT = 1;
    private static final Integer UPDATED_DISCOUNT_AMOUNT = 2;

    private static final Integer DEFAULT_ORDER_AMOUNT = 1;
    private static final Integer UPDATED_ORDER_AMOUNT = 2;

    private static final Integer DEFAULT_GROSS_AMOUNT = 1;
    private static final Integer UPDATED_GROSS_AMOUNT = 2;

    private static final Integer DEFAULT_NET_AMOUNT = 1;
    private static final Integer UPDATED_NET_AMOUNT = 2;

    private static final Integer DEFAULT_TAX_AMOUNT = 1;
    private static final Integer UPDATED_TAX_AMOUNT = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final OrderStatut DEFAULT_ORDER_STATUS = OrderStatut.REQUESTED;
    private static final OrderStatut UPDATED_ORDER_STATUS = OrderStatut.RECEIVED;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCommandeMockMvc;

    private Commande commande;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Commande createEntity(EntityManager em) {
        Commande commande = new Commande()
            .orderRefernce(DEFAULT_ORDER_REFERNCE)
            .receiptDate(DEFAULT_RECEIPT_DATE)
            .discountAmount(DEFAULT_DISCOUNT_AMOUNT)
            .orderAmount(DEFAULT_ORDER_AMOUNT)
            .grossAmount(DEFAULT_GROSS_AMOUNT)
            .netAmount(DEFAULT_NET_AMOUNT)
            .taxAmount(DEFAULT_TAX_AMOUNT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .orderStatus(DEFAULT_ORDER_STATUS);
        return commande;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Commande createUpdatedEntity(EntityManager em) {
        Commande commande = new Commande()
            .orderRefernce(UPDATED_ORDER_REFERNCE)
            .receiptDate(UPDATED_RECEIPT_DATE)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .orderAmount(UPDATED_ORDER_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .orderStatus(UPDATED_ORDER_STATUS);
        return commande;
    }

    @BeforeEach
    public void initTest() {
        commande = createEntity(em);
    }

    @Test
    @Transactional
    public void createCommande() throws Exception {
        int databaseSizeBeforeCreate = commandeRepository.findAll().size();
        // Create the Commande
        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isCreated());

        // Validate the Commande in the database
        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeCreate + 1);
        Commande testCommande = commandeList.get(commandeList.size() - 1);
        assertThat(testCommande.getOrderRefernce()).isEqualTo(DEFAULT_ORDER_REFERNCE);
        assertThat(testCommande.getReceiptDate()).isEqualTo(DEFAULT_RECEIPT_DATE);
        assertThat(testCommande.getDiscountAmount()).isEqualTo(DEFAULT_DISCOUNT_AMOUNT);
        assertThat(testCommande.getOrderAmount()).isEqualTo(DEFAULT_ORDER_AMOUNT);
        assertThat(testCommande.getGrossAmount()).isEqualTo(DEFAULT_GROSS_AMOUNT);
        assertThat(testCommande.getNetAmount()).isEqualTo(DEFAULT_NET_AMOUNT);
        assertThat(testCommande.getTaxAmount()).isEqualTo(DEFAULT_TAX_AMOUNT);
        assertThat(testCommande.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testCommande.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
        assertThat(testCommande.getOrderStatus()).isEqualTo(DEFAULT_ORDER_STATUS);
    }

    @Test
    @Transactional
    public void createCommandeWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = commandeRepository.findAll().size();

        // Create the Commande with an existing ID
        commande.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        // Validate the Commande in the database
        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkReceiptDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setReceiptDate(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDiscountAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setDiscountAmount(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkOrderAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setOrderAmount(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGrossAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setGrossAmount(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNetAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setNetAmount(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaxAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setTaxAmount(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setCreatedAt(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = commandeRepository.findAll().size();
        // set the field null
        commande.setUpdatedAt(null);

        // Create the Commande, which fails.


        restCommandeMockMvc.perform(post("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCommandes() throws Exception {
        // Initialize the database
        commandeRepository.saveAndFlush(commande);

        // Get all the commandeList
        restCommandeMockMvc.perform(get("/api/commandes?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(commande.getId().intValue())))
            .andExpect(jsonPath("$.[*].orderRefernce").value(hasItem(DEFAULT_ORDER_REFERNCE)))
            .andExpect(jsonPath("$.[*].receiptDate").value(hasItem(DEFAULT_RECEIPT_DATE.toString())))
            .andExpect(jsonPath("$.[*].discountAmount").value(hasItem(DEFAULT_DISCOUNT_AMOUNT)))
            .andExpect(jsonPath("$.[*].orderAmount").value(hasItem(DEFAULT_ORDER_AMOUNT)))
            .andExpect(jsonPath("$.[*].grossAmount").value(hasItem(DEFAULT_GROSS_AMOUNT)))
            .andExpect(jsonPath("$.[*].netAmount").value(hasItem(DEFAULT_NET_AMOUNT)))
            .andExpect(jsonPath("$.[*].taxAmount").value(hasItem(DEFAULT_TAX_AMOUNT)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].orderStatus").value(hasItem(DEFAULT_ORDER_STATUS.toString())));
    }
    
    @Test
    @Transactional
    public void getCommande() throws Exception {
        // Initialize the database
        commandeRepository.saveAndFlush(commande);

        // Get the commande
        restCommandeMockMvc.perform(get("/api/commandes/{id}", commande.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(commande.getId().intValue()))
            .andExpect(jsonPath("$.orderRefernce").value(DEFAULT_ORDER_REFERNCE))
            .andExpect(jsonPath("$.receiptDate").value(DEFAULT_RECEIPT_DATE.toString()))
            .andExpect(jsonPath("$.discountAmount").value(DEFAULT_DISCOUNT_AMOUNT))
            .andExpect(jsonPath("$.orderAmount").value(DEFAULT_ORDER_AMOUNT))
            .andExpect(jsonPath("$.grossAmount").value(DEFAULT_GROSS_AMOUNT))
            .andExpect(jsonPath("$.netAmount").value(DEFAULT_NET_AMOUNT))
            .andExpect(jsonPath("$.taxAmount").value(DEFAULT_TAX_AMOUNT))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.orderStatus").value(DEFAULT_ORDER_STATUS.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingCommande() throws Exception {
        // Get the commande
        restCommandeMockMvc.perform(get("/api/commandes/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCommande() throws Exception {
        // Initialize the database
        commandeRepository.saveAndFlush(commande);

        int databaseSizeBeforeUpdate = commandeRepository.findAll().size();

        // Update the commande
        Commande updatedCommande = commandeRepository.findById(commande.getId()).get();
        // Disconnect from session so that the updates on updatedCommande are not directly saved in db
        em.detach(updatedCommande);
        updatedCommande
            .orderRefernce(UPDATED_ORDER_REFERNCE)
            .receiptDate(UPDATED_RECEIPT_DATE)
            .discountAmount(UPDATED_DISCOUNT_AMOUNT)
            .orderAmount(UPDATED_ORDER_AMOUNT)
            .grossAmount(UPDATED_GROSS_AMOUNT)
            .netAmount(UPDATED_NET_AMOUNT)
            .taxAmount(UPDATED_TAX_AMOUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .orderStatus(UPDATED_ORDER_STATUS);

        restCommandeMockMvc.perform(put("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedCommande)))
            .andExpect(status().isOk());

        // Validate the Commande in the database
        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeUpdate);
        Commande testCommande = commandeList.get(commandeList.size() - 1);
        assertThat(testCommande.getOrderRefernce()).isEqualTo(UPDATED_ORDER_REFERNCE);
        assertThat(testCommande.getReceiptDate()).isEqualTo(UPDATED_RECEIPT_DATE);
        assertThat(testCommande.getDiscountAmount()).isEqualTo(UPDATED_DISCOUNT_AMOUNT);
        assertThat(testCommande.getOrderAmount()).isEqualTo(UPDATED_ORDER_AMOUNT);
        assertThat(testCommande.getGrossAmount()).isEqualTo(UPDATED_GROSS_AMOUNT);
        assertThat(testCommande.getNetAmount()).isEqualTo(UPDATED_NET_AMOUNT);
        assertThat(testCommande.getTaxAmount()).isEqualTo(UPDATED_TAX_AMOUNT);
        assertThat(testCommande.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testCommande.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
        assertThat(testCommande.getOrderStatus()).isEqualTo(UPDATED_ORDER_STATUS);
    }

    @Test
    @Transactional
    public void updateNonExistingCommande() throws Exception {
        int databaseSizeBeforeUpdate = commandeRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCommandeMockMvc.perform(put("/api/commandes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(commande)))
            .andExpect(status().isBadRequest());

        // Validate the Commande in the database
        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteCommande() throws Exception {
        // Initialize the database
        commandeRepository.saveAndFlush(commande);

        int databaseSizeBeforeDelete = commandeRepository.findAll().size();

        // Delete the commande
        restCommandeMockMvc.perform(delete("/api/commandes/{id}", commande.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Commande> commandeList = commandeRepository.findAll();
        assertThat(commandeList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
