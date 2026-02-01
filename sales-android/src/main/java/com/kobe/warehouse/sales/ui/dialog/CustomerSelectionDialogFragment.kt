package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.databinding.DialogCustomerSelectionBinding
import com.kobe.warehouse.sales.ui.adapter.CustomerSearchAdapter
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModel
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Customer Selection Dialog Fragment
 * Allows searching and selecting a customer for Assurance or Carnet sales
 */
class CustomerSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogCustomerSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerSearchAdapter: CustomerSearchAdapter
    private lateinit var customerRepository: CustomerRepository
    private lateinit var saleViewModel: UnifiedSaleViewModel

    private var searchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    companion object {
        fun newInstance(): CustomerSelectionDialogFragment {
            return CustomerSelectionDialogFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)

        // Get ViewModel from activity
        saleViewModel = ViewModelProvider(requireActivity())[UnifiedSaleViewModel::class.java]

        // Setup repository
        val tokenManager = TokenManager(requireContext())
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val customerApiService = retrofit.create(CustomerApiService::class.java)
        customerRepository = CustomerRepository(customerApiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomerSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchField()
        setupListeners()
    }

    private fun setupRecyclerView() {
        customerSearchAdapter = CustomerSearchAdapter { customer ->
            // Customer selected
            saleViewModel.selectCustomer(customer)
            dismiss()
        }

        binding.rvCustomers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = customerSearchAdapter
        }
    }

    private fun setupSearchField() {
        binding.etCustomerSearch.addTextChangedListener { text ->
            val query = text.toString().trim()

            // Cancel previous search job
            searchJob?.cancel()

            if (query.length >= 2) {
                // Debounce search - wait 300ms before searching
                searchJob = coroutineScope.launch {
                    delay(300)
                    searchCustomers(query)
                }
            } else {
                // Clear results
                customerSearchAdapter.submitList(emptyList())
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Entrez au moins 2 caractères pour rechercher"
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreateCustomer.setOnClickListener {
            // Open create customer dialog
            val createDialog = UninsuredCustomerCreateDialogFragment.newInstance { createdCustomer ->
                // Customer created successfully, select it and close
                saleViewModel.selectCustomer(createdCustomer)
                dismiss()
            }
            createDialog.show(parentFragmentManager, UninsuredCustomerCreateDialogFragment.TAG)
        }
    }

    private fun searchCustomers(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        coroutineScope.launch {
            val result = customerRepository.searchAssuredCustomers(query)

            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { customers ->
                    if (customers.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "Aucun client trouvé"
                        customerSearchAdapter.submitList(emptyList())
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        customerSearchAdapter.submitList(customers)
                    }
                },
                onFailure = { error ->
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "Erreur: ${error.message}"
                    Toast.makeText(
                        requireContext(),
                        "Erreur de recherche: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
