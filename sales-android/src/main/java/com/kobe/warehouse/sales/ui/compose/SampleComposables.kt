package com.kobe.warehouse.sales.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * Sample Composables for testing Jetpack Compose setup
 *
 * These composables serve as examples and can be used to verify
 * that Jetpack Compose is properly configured in the project.
 *
 * Delete or modify these once you start creating real composables.
 */

/**
 * Simple welcome screen composable
 * Demonstrates basic Material 3 components
 */
@Composable
fun WelcomeScreen(
    userName: String = "Vendeur",
    onNewSaleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Sales icon",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Bienvenue, $userName",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Pharma Smart Mobile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Button
            Button(
                onClick = onNewSaleClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nouvelle Vente",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Outlined button
            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Voir les ventes en cours",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Simple card composable example
 * Demonstrates Material 3 Card component
 */
@Composable
fun SaleCard(
    saleNumber: String,
    amount: String,
    customerName: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sale number
            Text(
                text = saleNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Customer name
            Text(
                text = customerName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Amount
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Loading composable example
 * Demonstrates circular progress indicator
 */
@Composable
fun LoadingScreen(
    message: String = "Chargement...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== PREVIEWS =====

/**
 * Preview for WelcomeScreen (Light theme)
 */
@Preview(name = "Welcome Screen - Light", showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    PharmaSmartTheme(darkTheme = false) {
        WelcomeScreen(userName = "Jean Dupont")
    }
}

/**
 * Preview for WelcomeScreen (Dark theme)
 */
@Preview(name = "Welcome Screen - Dark", showBackground = true)
@Composable
fun WelcomeScreenDarkPreview() {
    PharmaSmartTheme(darkTheme = true) {
        WelcomeScreen(userName = "Jean Dupont")
    }
}

/**
 * Preview for SaleCard
 */
@Preview(name = "Sale Card", showBackground = true)
@Composable
fun SaleCardPreview() {
    PharmaSmartTheme {
        SaleCard(
            saleNumber = "VNO-2024-001",
            amount = "25 000 FCFA",
            customerName = "Client Comptant",
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Preview for LoadingScreen
 */
@Preview(name = "Loading Screen", showBackground = true)
@Composable
fun LoadingScreenPreview() {
    PharmaSmartTheme {
        LoadingScreen(message = "Chargement des ventes...")
    }
}
