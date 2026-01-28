package com.kobe.warehouse.sales.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kobe.warehouse.sales.ui.theme.PharmaSmartTheme

/**
 * State Components
 * Reusable composables for displaying empty, loading, and error states
 *
 * These replace the XML-based EmptyStateView, LoadingStateView, and ErrorStateView
 */

/**
 * Empty State composable
 * Displays when there is no data to show
 *
 * @param message Main message to display
 * @param icon Icon to display (optional)
 * @param actionText Text for action button (optional)
 * @param onActionClick Action to perform when button is clicked (optional)
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.ShoppingCart,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Action button (if provided)
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onActionClick,
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = actionText)
            }
        }
    }
}

/**
 * Loading State composable
 * Displays a loading indicator with optional message
 *
 * @param message Loading message to display
 * @param modifier Modifier for customization
 */
@Composable
fun LoadingState(
    message: String = "Chargement...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
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

/**
 * Error State composable
 * Displays an error message with retry button
 *
 * @param message Error message to display
 * @param onRetryClick Action to perform when retry button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Retry button
        Button(
            onClick = onRetryClick,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "Réessayer")
        }
    }
}

/**
 * Shimmer Loading State composable
 * Displays a shimmer effect for skeleton loading
 *
 * This is a placeholder for future shimmer implementation
 * Requires a shimmer library or custom implementation
 *
 * @param modifier Modifier for customization
 */
@Composable
fun ShimmerLoadingState(
    modifier: Modifier = Modifier
) {
    // TODO: Implement shimmer effect
    // For now, use regular loading state
    LoadingState(
        message = "Chargement...",
        modifier = modifier
    )
}

// ===== PREVIEWS =====

@Preview(name = "Empty State", showBackground = true, heightDp = 400)
@Composable
fun EmptyStatePreview() {
    PharmaSmartTheme {
        EmptyState(
            message = "Aucune vente en cours",
            actionText = "Nouvelle vente",
            onActionClick = {}
        )
    }
}

@Preview(name = "Empty State - Dark", showBackground = true, heightDp = 400)
@Composable
fun EmptyStateDarkPreview() {
    PharmaSmartTheme(darkTheme = true) {
        EmptyState(
            message = "Aucune vente en cours",
            actionText = "Nouvelle vente",
            onActionClick = {}
        )
    }
}

@Preview(name = "Loading State", showBackground = true, heightDp = 300)
@Composable
fun LoadingStatePreview() {
    PharmaSmartTheme {
        LoadingState(message = "Chargement des ventes...")
    }
}

@Preview(name = "Error State", showBackground = true, heightDp = 400)
@Composable
fun ErrorStatePreview() {
    PharmaSmartTheme {
        ErrorState(
            message = "Impossible de charger les données. Vérifiez votre connexion internet.",
            onRetryClick = {}
        )
    }
}

@Preview(name = "Error State - Dark", showBackground = true, heightDp = 400)
@Composable
fun ErrorStateDarkPreview() {
    PharmaSmartTheme(darkTheme = true) {
        ErrorState(
            message = "Impossible de charger les données. Vérifiez votre connexion internet.",
            onRetryClick = {}
        )
    }
}
