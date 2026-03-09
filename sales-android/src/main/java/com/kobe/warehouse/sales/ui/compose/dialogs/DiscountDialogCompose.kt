package com.kobe.warehouse.sales.ui.compose.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kobe.warehouse.sales.data.model.Discount
import com.kobe.warehouse.sales.data.model.DiscountType
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Discount Dialog Composable
 * Jetpack Compose version of DiscountDialog
 *
 * Features:
 * - Percentage or fixed amount discount
 * - Quick amount buttons for common fixed amounts
 * - Real-time discount preview
 * - Input validation
 *
 * @param originalAmount Original amount before discount
 * @param onDismiss Action when dialog is dismissed
 * @param onApply Action when discount is applied
 */
@Composable
fun DiscountDialog(
    originalAmount: Int,
    onDismiss: () -> Unit,
    onApply: (Discount) -> Unit
) {
    var discountType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var discountValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Calculate discount amount
    val discountAmount = remember(discountValue, discountType) {
        val value = discountValue.toIntOrNull() ?: 0
        when (discountType) {
            DiscountType.PERCENTAGE -> if (value in 1..100) {
                (originalAmount * value) / 100
            } else 0
            DiscountType.FIXED -> minOf(value, originalAmount)
        }
    }

    val netAmount = originalAmount - discountAmount

    // Validation
    val isValid = remember(discountValue, discountType) {
        val value = discountValue.toIntOrNull()
        when {
            value == null || value == 0 -> {
                errorMessage = "Veuillez saisir une valeur"
                false
            }
            discountType == DiscountType.PERCENTAGE && value !in 1..100 -> {
                errorMessage = "Le pourcentage doit être entre 1 et 100"
                false
            }
            discountType == DiscountType.FIXED && value > originalAmount -> {
                errorMessage = "Le montant ne peut pas dépasser le montant initial"
                false
            }
            else -> {
                errorMessage = null
                true
            }
        }
    }

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
                // Title with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Appliquer une remise",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Original amount display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Montant initial",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatAmount(originalAmount)} FCFA",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Discount type selection
                Text(
                    text = "Type de remise",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = discountType == DiscountType.PERCENTAGE,
                        onClick = { discountType = DiscountType.PERCENTAGE },
                        label = { Text("Pourcentage") },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = discountType == DiscountType.FIXED,
                        onClick = { discountType = DiscountType.FIXED },
                        label = { Text("Montant fixe") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Discount value input
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { discountValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Valeur de la remise") },
                    suffix = {
                        Text(if (discountType == DiscountType.PERCENTAGE) "%" else "FCFA")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick amount buttons (for fixed discount)
                if (discountType == DiscountType.FIXED) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Montants rapides",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 500, 1000, 5000).forEach { amount ->
                            FilterChip(
                                selected = discountValue == amount.toString(),
                                onClick = { discountValue = amount.toString() },
                                label = { Text("$amount") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Discount preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remise",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${formatAmount(discountAmount)} FCFA",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Montant net",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${formatAmount(netAmount)} FCFA",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isValid) {
                                val discount = Discount(
                                    value = discountValue.toInt(),
                                    type = discountType
                                )
                                onApply(discount)
                            }
                        },
                        enabled = isValid
                    ) {
                        Text("Appliquer")
                    }
                }
            }
        }
    }
}

// Helper function to format amount
private fun formatAmount(amount: Int): String {
    return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
}

// ===== PREVIEW =====

@Preview
@Composable
fun DiscountDialogPreview() {
    PharmaSmartTheme {
        DiscountDialog(
            originalAmount = 50000,
            onDismiss = {},
            onApply = {}
        )
    }
}
