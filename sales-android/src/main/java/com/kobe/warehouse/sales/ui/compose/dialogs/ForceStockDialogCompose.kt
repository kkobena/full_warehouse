package com.kobe.warehouse.sales.ui.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Force Stock Dialog Composable
 * Jetpack Compose version of ForceStockDialog
 *
 * Features:
 * - Display product information and stock shortage
 * - Require reason input (minimum 10 characters)
 * - Validation before confirmation
 * - Warning indicators
 *
 * @param product Product with insufficient stock
 * @param requestedQuantity Quantity requested by user
 * @param availableStock Available stock quantity
 * @param onDismiss Action when dialog is dismissed
 * @param onConfirm Action when force stock is confirmed with reason
 */
@Composable
fun ForceStockDialog(
    product: Product,
    requestedQuantity: Int,
    availableStock: Int,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val shortage = requestedQuantity - availableStock

    val isValid = reason.trim().length >= 10

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
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = "Stock insuffisant",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
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
                        InfoRow(label = "Stock disponible", value = availableStock.toString())
                        InfoRow(label = "Quantité demandée", value = requestedQuantity.toString())
                        InfoRow(
                            label = "Manque",
                            value = shortage.toString(),
                            valueColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Warning message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Vous êtes sur le point d'ajouter ce produit malgré un stock insuffisant. Cette action sera enregistrée et nécessite une autorisation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Autorisation requise: PR_FORCE_STOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reason input
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Raison du forçage de stock (obligatoire)") },
                    placeholder = { Text("Expliquez pourquoi vous forcez le stock...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    minLines = 3,
                    maxLines = 4,
                    isError = reason.isNotEmpty() && !isValid,
                    supportingText = {
                        Text("${reason.length}/200 caractères (minimum 10)")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                )

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
                        onClick = { onConfirm(reason.trim()) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Forcer l'ajout")
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
fun ForceStockDialogPreview() {
    PharmaSmartTheme {
        ForceStockDialog(
            product = Product(
                id = 1,
                code = "PARA500",
                libelle = "Paracétamol 500mg",
                totalQuantity = 5
            ),
            requestedQuantity = 20,
            availableStock = 5,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
