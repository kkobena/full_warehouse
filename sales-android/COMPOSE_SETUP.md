# Jetpack Compose Setup Guide

## Overview

Jetpack Compose is now configured in this project and ready to use alongside existing XML layouts. This allows for gradual migration from XML to Compose.

## Configuration

### Dependencies Added

- **Compose BOM**: `androidx.compose:compose-bom:2024.02.00`
- **Compose UI**: Material 3, tooling, preview support
- **Integration**: Activity Compose, ViewModel Compose, Navigation Compose
- **Testing**: UI testing with Compose

### Build Configuration

```gradle
// Compose plugin enabled
id 'org.jetbrains.kotlin.plugin.compose' version '2.0.21'

// Compose feature enabled
buildFeatures {
    compose = true
}

// Compose options
composeOptions {
    kotlinCompilerExtensionVersion = '1.5.15'
}
```

## Theme Setup

The app uses **Material Design 3** with custom theme:

### Theme Files

- `ui/theme/Color.kt` - Color palette (primary, secondary, tertiary, etc.)
- `ui/theme/Type.kt` - Typography (Material 3 text styles)
- `ui/theme/Theme.kt` - Main theme composable with light/dark mode support

### Using the Theme

Wrap your composables with `PharmaSmartTheme`:

```kotlin
@Composable
fun MyScreen() {
    PharmaSmartTheme {
        // Your composable content here
        Text("Hello Compose!")
    }
}
```

### Theme Features

- ✅ Light and dark mode support
- ✅ Dynamic color (Android 12+)
- ✅ Material Design 3 components
- ✅ Custom Pharma Smart color palette

## Sample Composables

See `ui/compose/SampleComposables.kt` for examples:

- `WelcomeScreen` - Full screen example with buttons
- `SaleCard` - Card component example
- `LoadingScreen` - Loading indicator example

These can be used as templates or deleted once you create real composables.

## Preview Support

All sample composables have `@Preview` annotations for Android Studio preview:

```kotlin
@Preview(name = "My Composable", showBackground = true)
@Composable
fun MyComposablePreview() {
    PharmaSmartTheme {
        MyComposable()
    }
}
```

## Integrating Compose with Existing XML Layouts

### Option 1: ComposeView in XML

Add a `ComposeView` to your existing XML layout:

```xml
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Then set content in your Activity/Fragment:

```kotlin
binding.composeView.setContent {
    PharmaSmartTheme {
        MyComposable()
    }
}
```

### Option 2: ComponentActivity.setContent()

Convert an entire Activity to Compose:

```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PharmaSmartTheme {
                MyScreen()
            }
        }
    }
}
```

## Navigation with Compose

Use Navigation Compose for screen navigation:

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("details/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            DetailsScreen(id, navController)
        }
    }
}
```

## State Management

### Using ViewModels with Compose

```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    fun updateData() {
        _uiState.value = _uiState.value.copy(data = "new data")
    }
}

@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Text(text = uiState.data)
}
```

### Using LiveData with Compose

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
    val data by viewModel.liveData.observeAsState(initial = "")

    Text(text = data)
}
```

## Material 3 Components

### Common Components

```kotlin
// Button
Button(onClick = { /* action */ }) {
    Text("Click Me")
}

// Card
Card(onClick = { /* action */ }) {
    Text("Card content")
}

// Text Field
var text by remember { mutableStateOf("") }
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Label") }
)

// List
LazyColumn {
    items(listOfItems) { item ->
        Text(item.name)
    }
}
```

## Testing Compose UI

### UI Tests

```kotlin
@Test
fun myComposableTest() {
    composeTestRule.setContent {
        PharmaSmartTheme {
            MyComposable()
        }
    }

    composeTestRule.onNodeWithText("Button").performClick()
    composeTestRule.onNodeWithText("Result").assertIsDisplayed()
}
```

## Migration Strategy

### Phase 1 (Current): Setup ✅
- Compose dependencies added
- Theme configured
- Sample composables created

### Phase 2: Simple Components
- Migrate `EmptyStateView` → Composable
- Migrate `LoadingStateView` → Composable
- Migrate `ErrorStateView` → Composable

### Phase 3: Dialogs
- Migrate dialogs to Compose
- Convert `DiscountDialog`, `ForceStockDialog`, etc.

### Phase 4: Screens
- Migrate fragments to Composable screens
- Migrate activities to use `setContent`

## Best Practices

1. **State Hoisting**: Keep state in parent composables
2. **Reusability**: Create small, reusable composables
3. **Preview**: Add `@Preview` for all composables
4. **Naming**: Use clear, descriptive names (e.g., `SaleCard`, not `Card1`)
5. **Modifiers**: Always accept a `Modifier` parameter
6. **Performance**: Use `remember`, `derivedStateOf` for expensive calculations
7. **Side Effects**: Use `LaunchedEffect`, `DisposableEffect` appropriately

## Resources

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Compose Samples](https://github.com/android/compose-samples)
- [Compose Pathway](https://developer.android.com/courses/pathways/compose)

## Troubleshooting

### Common Issues

**Issue**: "Compose compiler version mismatch"
**Solution**: Ensure `kotlinCompilerExtensionVersion` matches your Kotlin version

**Issue**: "Preview not showing"
**Solution**: Rebuild project, invalidate caches, ensure `@Preview` annotation is present

**Issue**: "Runtime crash with Compose"
**Solution**: Check that `PharmaSmartTheme` is wrapping your content

## Next Steps

1. Familiarize yourself with sample composables
2. Try creating a simple composable for your feature
3. Follow the migration plan to gradually convert XML to Compose
4. Refer to official documentation for advanced topics

---

**Note**: Compose is fully interoperable with existing View-based code. You can use both XML and Compose in the same app during migration.
