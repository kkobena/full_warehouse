package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.WarehouseApp;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.repository.PaymentModeRepository;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;
/**
 * Integration tests for the {@link PaymentModeResource} REST controller.
 */
@SpringBootTest(classes = WarehouseApp.class)
@AutoConfigureMockMvc
@WithMockUser
public class PaymentModeResourceIT {

    private static final String DEFAULT_LIBELLE = "AAAAAAAAAA";
    private static final String UPDATED_LIBELLE = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_CODE = "BBBBBBBBBB";

    private static final PaymentGroup DEFAULT_GROUP = PaymentGroup.CASH;
    private static final PaymentGroup UPDATED_GROUP = PaymentGroup.CREDIT;

    @Autowired
    private PaymentModeRepository paymentModeRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restPaymentModeMockMvc;

    private PaymentMode paymentMode;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentMode createEntity(EntityManager em) {
        PaymentMode paymentMode = new PaymentMode()
            .libelle(DEFAULT_LIBELLE)
            .code(DEFAULT_CODE)
            .group(DEFAULT_GROUP);
        return paymentMode;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static PaymentMode createUpdatedEntity(EntityManager em) {
        PaymentMode paymentMode = new PaymentMode()
            .libelle(UPDATED_LIBELLE)
            .code(UPDATED_CODE)
            .group(UPDATED_GROUP);
        return paymentMode;
    }

    @BeforeEach
    public void initTest() {
        paymentMode = createEntity(em);
    }

    @Test
    @Transactional
    public void createPaymentMode() throws Exception {
        int databaseSizeBeforeCreate = paymentModeRepository.findAll().size();
        // Create the PaymentMode
        restPaymentModeMockMvc.perform(post("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isCreated());

        // Validate the PaymentMode in the database
        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeCreate + 1);
        PaymentMode testPaymentMode = paymentModeList.get(paymentModeList.size() - 1);
        assertThat(testPaymentMode.getLibelle()).isEqualTo(DEFAULT_LIBELLE);
        assertThat(testPaymentMode.getCode()).isEqualTo(DEFAULT_CODE);
        assertThat(testPaymentMode.getGroup()).isEqualTo(DEFAULT_GROUP);
    }

    @Test
    @Transactional
    public void createPaymentModeWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = paymentModeRepository.findAll().size();

        // Create the PaymentMode with an existing ID
        paymentMode.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restPaymentModeMockMvc.perform(post("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isBadRequest());

        // Validate the PaymentMode in the database
        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void checkLibelleIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentModeRepository.findAll().size();
        // set the field null
        paymentMode.setLibelle(null);

        // Create the PaymentMode, which fails.


        restPaymentModeMockMvc.perform(post("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isBadRequest());

        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCodeIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentModeRepository.findAll().size();
        // set the field null
        paymentMode.setCode(null);

        // Create the PaymentMode, which fails.


        restPaymentModeMockMvc.perform(post("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isBadRequest());

        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGroupIsRequired() throws Exception {
        int databaseSizeBeforeTest = paymentModeRepository.findAll().size();
        // set the field null
        paymentMode.setGroup(null);

        // Create the PaymentMode, which fails.


        restPaymentModeMockMvc.perform(post("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isBadRequest());

        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllPaymentModes() throws Exception {
        // Initialize the database
        paymentModeRepository.saveAndFlush(paymentMode);

        // Get all the paymentModeList
        restPaymentModeMockMvc.perform(get("/api/payment-modes?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(paymentMode.getId().intValue())))
            .andExpect(jsonPath("$.[*].libelle").value(hasItem(DEFAULT_LIBELLE)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].group").value(hasItem(DEFAULT_GROUP.toString())));
    }
    
    @Test
    @Transactional
    public void getPaymentMode() throws Exception {
        // Initialize the database
        paymentModeRepository.saveAndFlush(paymentMode);

        // Get the paymentMode
        restPaymentModeMockMvc.perform(get("/api/payment-modes/{id}", paymentMode.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(paymentMode.getId().intValue()))
            .andExpect(jsonPath("$.libelle").value(DEFAULT_LIBELLE))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.group").value(DEFAULT_GROUP.toString()));
    }
    @Test
    @Transactional
    public void getNonExistingPaymentMode() throws Exception {
        // Get the paymentMode
        restPaymentModeMockMvc.perform(get("/api/payment-modes/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePaymentMode() throws Exception {
        // Initialize the database
        paymentModeRepository.saveAndFlush(paymentMode);

        int databaseSizeBeforeUpdate = paymentModeRepository.findAll().size();

        // Update the paymentMode
        PaymentMode updatedPaymentMode = paymentModeRepository.findById(paymentMode.getId()).get();
        // Disconnect from session so that the updates on updatedPaymentMode are not directly saved in db
        em.detach(updatedPaymentMode);
        updatedPaymentMode
            .libelle(UPDATED_LIBELLE)
            .code(UPDATED_CODE)
            .group(UPDATED_GROUP);

        restPaymentModeMockMvc.perform(put("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedPaymentMode)))
            .andExpect(status().isOk());

        // Validate the PaymentMode in the database
        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeUpdate);
        PaymentMode testPaymentMode = paymentModeList.get(paymentModeList.size() - 1);
        assertThat(testPaymentMode.getLibelle()).isEqualTo(UPDATED_LIBELLE);
        assertThat(testPaymentMode.getCode()).isEqualTo(UPDATED_CODE);
        assertThat(testPaymentMode.getGroup()).isEqualTo(UPDATED_GROUP);
    }

    @Test
    @Transactional
    public void updateNonExistingPaymentMode() throws Exception {
        int databaseSizeBeforeUpdate = paymentModeRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restPaymentModeMockMvc.perform(put("/api/payment-modes").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(paymentMode)))
            .andExpect(status().isBadRequest());

        // Validate the PaymentMode in the database
        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deletePaymentMode() throws Exception {
        // Initialize the database
        paymentModeRepository.saveAndFlush(paymentMode);

        int databaseSizeBeforeDelete = paymentModeRepository.findAll().size();

        // Delete the paymentMode
        restPaymentModeMockMvc.perform(delete("/api/payment-modes/{id}", paymentMode.getId()).with(csrf())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<PaymentMode> paymentModeList = paymentModeRepository.findAll();
        assertThat(paymentModeList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
