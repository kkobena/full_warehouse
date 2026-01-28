package com.kobe.warehouse.sales.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.ui.compose.components.EmptyState
import com.kobe.warehouse.sales.ui.compose.components.LoadingState
import com.kobe.warehouse.sales.ui.compose.components.SaleCard
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Full Sale Home Screen Composable
 * Main screen with tabs for "Ventes en cours" and "Préventes"
 *
 * This replaces FullSaleHomeActivity
 *
 * Features:
 * - Tab layout (Ventes en cours / Préventes)
 * - List of sales per tab
 * - Pull-to-refresh
 * - Loading and empty states
 * - FAB for new sale
 *
 * @param uiState Current UI state
 * @param onTabSelected Action when tab is selected
 * @param onSaleClick Action when sale is clicked
 * @param onNewSaleClick Action when "New Sale" FAB is clicked
 * @param onRefresh Action to refresh current tab
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullSaleHomeScreen(
    uiState: FullSaleHomeUiState,
    onTabSelected: (Int) -> Unit,
    onSaleClick: (Sale) -> Unit,
    onNewSaleClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pharma Smart - Ventes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                    contentDescription = "New sale",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Ventes en cours") }
                )

                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("Préventes") }
                )
            }

            // Content based on selected tab
            when {
                uiState.isLoading -> {
                    LoadingState(message = "Chargement...")
                }

                uiState.currentSales.isEmpty() -> {
                    EmptyState(
                        message = if (uiState.selectedTabIndex == 0) {
                            "Aucune vente en cours"
                        } else {
                            "Aucune prévente"
                        },
                        actionText = "Nouvelle vente",
                        onActionClick = onNewSaleClick
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.currentSales,
                            key = { it.id ?: 0 }
                        ) { sale ->
                            SaleCard(
                                sale = sale,
                                onClick = { onSaleClick(sale) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI State for Full Sale Home Screen
 */
data class FullSaleHomeUiState(
    val selectedTabIndex: Int = 0,
    val ongoingSales: List<Sale> = emptyList(),
    val preventes: List<Sale> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentSales: List<Sale>
        get() = if (selectedTabIndex == 0) ongoingSales else preventes
}

// ===== PREVIEWS =====

@Preview(showBackground = true)
@Composable
fun FullSaleHomeScreenPreview() {
    PharmaSmartTheme {
        val mockSales = List(3) { index ->
            Sale(
                id = index.toLong(),
                numberTransaction = "VNO-2024-${String.format("%03d", index + 1)}",
                salesAmount = (15000 + index * 5000),
                netAmount = (15000 + index * 5000)
            )
        }

        FullSaleHomeScreen(
            uiState = FullSaleHomeUiState(
                ongoingSales = mockSales,
                preventes = emptyList()
            ),
            onTabSelected = {},
            onSaleClick = {},
            onNewSaleClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FullSaleHomeScreenEmptyPreview() {
    PharmaSmartTheme {
        FullSaleHomeScreen(
            uiState = FullSaleHomeUiState(),
            onTabSelected = {},
            onSaleClick = {},
            onNewSaleClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FullSaleHomeScreenLoadingPreview() {
    PharmaSmartTheme(darkTheme = true) {
        FullSaleHomeScreen(
            uiState = FullSaleHomeUiState(isLoading = true),
            onTabSelected = {},
            onSaleClick = {},
            onNewSaleClick = {},
            onRefresh = {}
        )
    }
}
