package com.kobe.warehouse.sales.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Product Card composable
 * Displays a product item in a card format
 *
 * Replaces the XML-based item_product_list.xml and item_product_grid.xml layouts
 *
 * @param product Product data to display
 * @param onClick Action when card is clicked
 * @param onAddClick Action when add button is clicked (optional)
 * @param modifier Modifier for customization
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Product name and code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Product name
                    Text(
                        text = product.libelle ?: product.code ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Product code
                    if (product.code != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stock indicator
                StockBadge(stock = product.totalQuantity)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price and stock row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price
                Column {
                    Text(
                        text = "Prix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.getFormattedPrice(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Stock count
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${product.totalQuantity}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (product.isInStock()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                // Add button (if provided)
                if (onAddClick != null) {
                    IconButton(
                        onClick = onAddClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to cart"
                        )
                    }
                }
            }

            // Indicators (deconditionnable, force stock)
            if (product.deconditionnable || product.forceStock) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.deconditionnable) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Déconditionnable",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }

                    if (product.forceStock) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Force stock",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Product Card composable
 * Smaller version for dense lists
 */
@Composable
fun CompactProductCard(
    product: Product,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.libelle ?: product.code ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.getFormattedPrice(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Stock: ${product.totalQuantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.isInStock()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            StockBadge(stock = product.totalQuantity)
        }
    }
}

/**
 * Stock Badge composable
 * Displays stock status as a colored badge
 */
@Composable
private fun StockBadge(
    stock: Int,
    modifier: Modifier = Modifier
) {
    val (color, text) = when {
        stock == 0 -> MaterialTheme.colorScheme.errorContainer to "Rupture"
        stock < 10 -> MaterialTheme.colorScheme.tertiaryContainer to "Faible"
        else -> MaterialTheme.colorScheme.primaryContainer to "Disponible"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ===== PREVIEWS =====

@Preview(name = "Product Card", showBackground = true)
@Composable
fun ProductCardPreview() {
    PharmaSmartTheme {
        ProductCard(
            product = Product(
                id = 1,
                code = "PARA500",
                libelle = "Paracétamol 500mg - Boîte de 10 comprimés",
                regularUnitPrice = 500,
                totalQuantity = 150,
                deconditionnable = true
            ),
            onClick = {},
            onAddClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Product Card - Low Stock", showBackground = true)
@Composable
fun ProductCardLowStockPreview() {
    PharmaSmartTheme {
        ProductCard(
            product = Product(
                id = 2,
                code = "IBU400",
                libelle = "Ibuprofène 400mg",
                regularUnitPrice = 750,
                totalQuantity = 5,
                forceStock = true
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Product Card - Out of Stock", showBackground = true)
@Composable
fun ProductCardOutOfStockPreview() {
    PharmaSmartTheme(darkTheme = true) {
        ProductCard(
            product = Product(
                id = 3,
                code = "AMOX500",
                libelle = "Amoxicilline 500mg",
                regularUnitPrice = 1200,
                totalQuantity = 0
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Compact Product Card", showBackground = true)
@Composable
fun CompactProductCardPreview() {
    PharmaSmartTheme {
        CompactProductCard(
            product = Product(
                id = 4,
                code = "ASP100",
                libelle = "Aspirine 100mg",
                regularUnitPrice = 350,
                totalQuantity = 80
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
