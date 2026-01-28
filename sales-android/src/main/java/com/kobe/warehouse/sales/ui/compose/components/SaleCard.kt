package com.kobe.warehouse.sales.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Sale Card composable
 * Displays a sale item in a card format
 *
 * Replaces the XML-based item_sale.xml layout
 *
 * @param sale Sale data to display
 * @param onClick Action when card is clicked
 * @param onDeleteClick Action when delete button is clicked (optional)
 * @param modifier Modifier for customization
 */
@Composable
fun SaleCard(
    sale: Sale,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Sale information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Transaction number
                Text(
                    text = sale.numberTransaction,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Customer name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sale.getCustomerName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Amount and status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Amount
                    Text(
                        text = sale.getFormattedSalesAmount(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Status badge
                    if (sale.isPending()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "EN COURS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Updated date
                if (sale.updatedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sale.getFormattedUpdatedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Delete button (if provided)
            if (onDeleteClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete sale"
                    )
                }
            }
        }
    }
}

/**
 * Compact Sale Card composable
 * Smaller version for dense lists
 *
 * @param sale Sale data to display
 * @param onClick Action when card is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun CompactSaleCard(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sale.numberTransaction,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = sale.getCustomerName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = sale.getFormattedSalesAmount(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ===== PREVIEWS =====

@Preview(name = "Sale Card", showBackground = true)
@Composable
fun SaleCardPreview() {
    PharmaSmartTheme {
        SaleCard(
            sale = Sale(
                id = 1,
                numberTransaction = "VNO-2024-001",
                salesAmount = 25000,
                netAmount = 25000,
                updatedAt = "2024-01-15T10:30:00"
            ),
            onClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Sale Card - Dark", showBackground = true)
@Composable
fun SaleCardDarkPreview() {
    PharmaSmartTheme(darkTheme = true) {
        SaleCard(
            sale = Sale(
                id = 1,
                numberTransaction = "VNO-2024-001",
                salesAmount = 25000,
                netAmount = 25000,
                updatedAt = "2024-01-15T10:30:00"
            ),
            onClick = {},
            onDeleteClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Compact Sale Card", showBackground = true)
@Composable
fun CompactSaleCardPreview() {
    PharmaSmartTheme {
        CompactSaleCard(
            sale = Sale(
                id = 1,
                numberTransaction = "VNO-2024-002",
                salesAmount = 15000,
                netAmount = 15000
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Sale Card List", showBackground = true, heightDp = 500)
@Composable
fun SaleCardListPreview() {
    PharmaSmartTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) { index ->
                SaleCard(
                    sale = Sale(
                        id = index.toLong(),
                        numberTransaction = "VNO-2024-${String.format("%03d", index + 1)}",
                        salesAmount = (15000 + index * 5000),
                        netAmount = (15000 + index * 5000),
                        updatedAt = "2024-01-15T${10 + index}:30:00"
                    ),
                    onClick = {}
                )
            }
        }
    }
}
