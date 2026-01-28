package com.kobe.warehouse.sales.ui.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Déconditionnement Dialog Composable
 * Jetpack Compose version of DeconditionnementDialog
 *
 * Features:
 * - Display product information (box stock, detail stock, item qty)
 * - Allow user to specify quantity to décondition
 * - Preview result before confirmation
 * - Input validation
 *
 * @param product Product to décondition
 * @param onDismiss Action when dialog is dismissed
 * @param onConfirm Action when déconditionnement is confirmed with quantity
 */
@Composable
fun DeconditionnementDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    val quantity = quantityText.toIntOrNull() ?: 1

    val itemQty = product.itemQty ?: 1
    val boxStock = product.totalQuantity / itemQty
    val detailStock = product.totalQuantity % itemQty
    val totalUnits = quantity * itemQty

    val isValid = quantity > 0 && quantity <= boxStock

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Warning icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Déconditionnement",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = product.libelle ?: product.code ?: "Produit",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stock information
                        InfoRow(label = "Stock boîte", value = boxStock.toString())
                        InfoRow(label = "Stock détail", value = detailStock.toString())
                        InfoRow(
                            label = "Unités par boîte",
                            value = itemQty.toString(),
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Explanation card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Text(
                            text = "Le déconditionnement permet de casser une boîte pour obtenir des unités individuelles. Cette opération est irréversible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity input
                Text(
                    text = "Nombre de boîtes à déconditionner",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantité") },
                    suffix = { Text("boîte(s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid,
                    supportingText = {
                        if (!isValid && quantity > boxStock) {
                            Text("Stock insuffisant (max: $boxStock)")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Result preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Résultat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$quantity boîte(s) → $totalUnits unité(s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(quantity) },
                        enabled = isValid
                    ) {
                        Text("Déconditionner")
                    }
                }
            }
        }
    }
}

/**
 * Info Row composable
 * Helper for displaying label-value pairs
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

// ===== PREVIEW =====

@Preview
@Composable
fun DeconditionnementDialogPreview() {
    PharmaSmartTheme {
        DeconditionnementDialog(
            product = Product(
                id = 1,
                code = "PARA500",
                libelle = "Paracétamol 500mg - Boîte de 10 comprimés",
                totalQuantity = 55, // 5 boxes + 5 units
                itemQty = 10
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
