package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.repository.PaymentFournisseurRepository;

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

/**
 * Integration tests for the {@link PaymentFournisseurResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class PaymentFournisseurResourceIT {

    private static final Integer DEFAULT_NET_AMOUNT = 1;
    private static final Integer UPDATED_NET_AMOUNT = 2;

    private static final Integer DEFAULT_PAID_AMOUNT = 1;
    private static final Integer UPDATED_PAID_AMOUNT = 2;

    private static final Integer DEFAULT_REST_TO_PAY = 1;
    private static final Integer UPDATED_REST_TO_PAY = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private PaymentFournisseurRepository paymentFournisseurRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPaymentFournisseurMockMvc;

    private PaymentFournisseur paymentFournisseur;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentFournisseur createEntity(EntityManager em) {
        PaymentFournisseur paymentFournisseur = new PaymentFournisseur()
            .netAmount(DEFAULT_NET_AMOUNT)
            .paidAmount(DEFAULT_PAID_AMOUNT)
            .restToPay(DEFAULT_REST_TO_PAY)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        return paymentFournisseur;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentFournisseur createUpdatedEntity(EntityManager em) {
        PaymentFournisseur paymentFournisseur = new PaymentFournisseur()
            .netAmount(UPDATED_NET_AMOUNT)
            .paidAmount(UPDATED_PAID_AMOUNT)
            .restToPay(UPDATED_REST_TO_PAY)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        return paymentFournisseur;
    }

    @BeforeEach
    public void initTest() {
        paymentFournisseur = createEntity(em);
    }

    @Test
    @Transactional
    public void createPaymentFournisseur() throws Exception {
        int databaseSizeBeforeCreate = paymentFournisseurRepository.findAll().size();
        // Create the PaymentFournisseur
        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isCreated());

        // Validate the PaymentFournisseur in the database
        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeCreate + 1);
        PaymentFournisseur testPaymentFournisseur = paymentFournisseurList.get(paymentFournisseurList.size() - 1);
        assertThat(testPaymentFournisseur.getNetAmount()).isEqualTo(DEFAULT_NET_AMOUNT);
        assertThat(testPaymentFournisseur.getPaidAmount()).isEqualTo(DEFAULT_PAID_AMOUNT);
        assertThat(testPaymentFournisseur.getRestToPay()).isEqualTo(DEFAULT_REST_TO_PAY);
        assertThat(testPaymentFournisseur.getCreatedAt()).isEqualTo(DEFAULT_CREATED_AT);
        assertThat(testPaymentFournisseur.getUpdatedAt()).isEqualTo(DEFAULT_UPDATED_AT);
    }

    @Test
    @Transactional
    public void createPaymentFournisseurWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = paymentFournisseurRepository.findAll().size();

        // Create the PaymentFournisseur with an existing ID
        paymentFournisseur.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        // Validate the PaymentFournisseur in the database
        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkNetAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentFournisseurRepository.findAll().size();
        // set the field null
        paymentFournisseur.setNetAmount(null);

        // Create the PaymentFournisseur, which fails.


        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkPaidAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentFournisseurRepository.findAll().size();
        // set the field null
        paymentFournisseur.setPaidAmount(null);

        // Create the PaymentFournisseur, which fails.


        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkRestToPayIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentFournisseurRepository.findAll().size();
        // set the field null
        paymentFournisseur.setRestToPay(null);

        // Create the PaymentFournisseur, which fails.


        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentFournisseurRepository.findAll().size();
        // set the field null
        paymentFournisseur.setCreatedAt(null);

        // Create the PaymentFournisseur, which fails.


        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdatedAtIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentFournisseurRepository.findAll().size();
        // set the field null
        paymentFournisseur.setUpdatedAt(null);

        // Create the PaymentFournisseur, which fails.


        restPaymentFournisseurMockMvc.perform(post("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllPaymentFournisseurs() throws Exception {
        // Initialize the database
        paymentFournisseurRepository.saveAndFlush(paymentFournisseur);

        // Get all the paymentFournisseurList
        restPaymentFournisseurMockMvc.perform(get("/api/payment-fournisseurs?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(paymentFournisseur.getId().intValue())))
            .andExpect(jsonPath("$.[*].netAmount").value(hasItem(DEFAULT_NET_AMOUNT)))
            .andExpect(jsonPath("$.[*].paidAmount").value(hasItem(DEFAULT_PAID_AMOUNT)))
            .andExpect(jsonPath("$.[*].restToPay").value(hasItem(DEFAULT_REST_TO_PAY)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }
    
    @Test
    @Transactional
    public void getPaymentFournisseur() throws Exception {
        // Initialize the database
        paymentFournisseurRepository.saveAndFlush(paymentFournisseur);

        // Get the paymentFournisseur
        restPaymentFournisseurMockMvc.perform(get("/api/payment-fournisseurs/{id}", paymentFournisseur.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(paymentFournisseur.getId().intValue()))
            .andExpect(jsonPath("$.netAmount").value(DEFAULT_NET_AMOUNT))
            .andExpect(jsonPath("$.paidAmount").value(DEFAULT_PAID_AMOUNT))
            .andExpect(jsonPath("$.restToPay").value(DEFAULT_REST_TO_PAY))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingPaymentFournisseur() throws Exception {
        // Get the paymentFournisseur
        restPaymentFournisseurMockMvc.perform(get("/api/payment-fournisseurs/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePaymentFournisseur() throws Exception {
        // Initialize the database
        paymentFournisseurRepository.saveAndFlush(paymentFournisseur);

        int databaseSizeBeforeUpdate = paymentFournisseurRepository.findAll().size();

        // Update the paymentFournisseur
        PaymentFournisseur updatedPaymentFournisseur = paymentFournisseurRepository.findById(paymentFournisseur.getId()).get();
        // Disconnect from session so that the updates on updatedPaymentFournisseur are not directly saved in db
        em.detach(updatedPaymentFournisseur);
        updatedPaymentFournisseur
            .netAmount(UPDATED_NET_AMOUNT)
            .paidAmount(UPDATED_PAID_AMOUNT)
            .restToPay(UPDATED_REST_TO_PAY)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restPaymentFournisseurMockMvc.perform(put("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedPaymentFournisseur)))
            .andExpect(status().isOk());

        // Validate the PaymentFournisseur in the database
        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeUpdate);
        PaymentFournisseur testPaymentFournisseur = paymentFournisseurList.get(paymentFournisseurList.size() - 1);
        assertThat(testPaymentFournisseur.getNetAmount()).isEqualTo(UPDATED_NET_AMOUNT);
        assertThat(testPaymentFournisseur.getPaidAmount()).isEqualTo(UPDATED_PAID_AMOUNT);
        assertThat(testPaymentFournisseur.getRestToPay()).isEqualTo(UPDATED_REST_TO_PAY);
        assertThat(testPaymentFournisseur.getCreatedAt()).isEqualTo(UPDATED_CREATED_AT);
        assertThat(testPaymentFournisseur.getUpdatedAt()).isEqualTo(UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    public void updateNonExistingPaymentFournisseur() throws Exception {
        int databaseSizeBeforeUpdate = paymentFournisseurRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPaymentFournisseurMockMvc.perform(put("/api/payment-fournisseurs").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentFournisseur)))
            .andExpect(status().isBadRequest());

        // Validate the PaymentFournisseur in the database
        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deletePaymentFournisseur() throws Exception {
        // Initialize the database
        paymentFournisseurRepository.saveAndFlush(paymentFournisseur);

        int databaseSizeBeforeDelete = paymentFournisseurRepository.findAll().size();

        // Delete the paymentFournisseur
        restPaymentFournisseurMockMvc.perform(delete("/api/payment-fournisseurs/{id}", paymentFournisseur.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<PaymentFournisseur> paymentFournisseurList = paymentFournisseurRepository.findAll();
        assertThat(paymentFournisseurList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
