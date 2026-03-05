package com.kobe.warehouse.sales.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.ui.compose.components.EmptyState
import com.kobe.warehouse.sales.ui.compose.components.ErrorState
import com.kobe.warehouse.sales.ui.compose.components.LoadingState
import com.kobe.warehouse.sales.ui.compose.components.SaleCard
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Sales List Screen Composable
 * Displays a list of sales (ongoing or preventes)
 *
 * This replaces VenteEnCoursFragment and PreventeFragment
 *
 * Features:
 * - Display list of sales
 * - Pull-to-refresh
 * - Loading, empty, and error states
 * - Search functionality
 * - Swipe-to-delete (optional)
 *
 * @param uiState Current UI state
 * @param onRefresh Action to refresh the list
 * @param onSaleClick Action when a sale is clicked
 * @param onDeleteSale Action when a sale is deleted (optional)
 * @param onNewSaleClick Action when "New Sale" button is clicked
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesListScreen(
    uiState: SalesListUiState,
    onRefresh: () -> Unit,
    onSaleClick: (Sale) -> Unit,
    onDeleteSale: ((Sale) -> Unit)? = null,
    onNewSaleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewSaleClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New sale"
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    message = "Chargement des ventes...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    message = uiState.error,
                    onRetryClick = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.sales.isEmpty() -> {
                EmptyState(
                    message = uiState.emptyMessage,
                    actionText = "Nouvelle vente",
                    onActionClick = onNewSaleClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.sales,
                            key = { it.id ?: 0 }
                        ) { sale ->
                            SaleCard(
                                sale = sale,
                                onClick = { onSaleClick(sale) },
                                onDeleteClick = onDeleteSale?.let { { it(sale) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI State for Sales List Screen
 */
data class SalesListUiState(
    val title: String = "Ventes en cours",
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val emptyMessage: String = "Aucune vente en cours"
)

// ===== PREVIEWS =====

@Preview(showBackground = true)
@Composable
fun SalesListScreenLoadingPreview() {
    PharmaSmartTheme {
        SalesListScreen(
            uiState = SalesListUiState(isLoading = true),
            onRefresh = {},
            onSaleClick = {},
            onNewSaleClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SalesListScreenEmptyPreview() {
    PharmaSmartTheme {
        SalesListScreen(
            uiState = SalesListUiState(),
            onRefresh = {},
            onSaleClick = {},
            onNewSaleClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SalesListScreenWithDataPreview() {
    PharmaSmartTheme {
        val mockSales = List(5) { index ->
            Sale(
                id = index.toLong(),
                numberTransaction = "VNO-2024-${String.format("%03d", index + 1)}",
                salesAmount = (15000 + index * 5000),
                netAmount = (15000 + index * 5000),
                updatedAt = "2024-01-15T${10 + index}:30:00"
            )
        }

        SalesListScreen(
            uiState = SalesListUiState(sales = mockSales),
            onRefresh = {},
            onSaleClick = {},
            onDeleteSale = {},
            onNewSaleClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SalesListScreenErrorPreview() {
    PharmaSmartTheme(darkTheme = true) {
        SalesListScreen(
            uiState = SalesListUiState(
                error = "Impossible de charger les ventes. Vérifiez votre connexion internet."
            ),
            onRefresh = {},
            onSaleClick = {},
            onNewSaleClick = {}
        )
    }
}
