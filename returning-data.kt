// This file demonstrates how to return a simple result (String)
// from a secondary screen back to the source screen using SavedStateHandle.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// --- Constants for Routes and Result Key ---
const val COLOR_RESULT_KEY = "selected_color_key"

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ColorPicker : Screen("color_picker")
}

// --- Main App Setup ---
@Composable
fun ResultNavigationApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) { backStackEntry ->
            val stateBackStackEntry = rememberUpdatedState(backStackEntry)
            HomeScreen(navController, stateBackStackEntry)
        }
        composable(Screen.ColorPicker.route) {
            ColorPickerScreen(navController)
        }
    }
}

// --- Screen A (Source): Receives the Result ---

@Composable
fun HomeScreen(navController: NavController, backStackEntry: State<NavBackStackEntry?>) {
    // 1. Access the current NavBackStackEntry's SavedStateHandle
    val savedStateHandle = backStackEntry.value?.savedStateHandle

    // 2. Observe the LiveData for the result key
    // The observed value will be the Color String, or null if no result has been set yet.
    val colorResult = savedStateHandle?.getLiveData<String>(COLOR_RESULT_KEY)?.observeAsState()

    // State to hold the applied color for the UI
    var appliedColor by remember { mutableStateOf(Color.LightGray) }
    var resultText by remember { mutableStateOf("No color selected yet.") }

    // 3. Crucial Cleanup and Handling: Use LaunchedEffect to handle the result once
    LaunchedEffect(colorResult?.value) {
        val result = colorResult?.value
        if (result != null) {
            // Processing the result
            appliedColor = Color(android.graphics.Color.parseColor(result))
            resultText = "Successfully applied color: $result"

            // 4. IMPORTANT: Remove the key immediately to prevent re-triggering
            // (e.g., if we navigate away and come back to this screen)
            savedStateHandle?.remove<String>(COLOR_RESULT_KEY)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Home Screen",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(appliedColor, RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(16.dp))

        Text(resultText, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate(Screen.ColorPicker.route) }
        ) {
            Text("Go Pick a Color")
        }
    }
}
// --- Screen B (Destination): Sends the Result ---

@Composable
fun ColorPickerScreen(navController: NavController) {
    val colorOptions = mapOf(
        "Blue" to "#2196F3",
        "Green" to "#4CAF50",
        "Red" to "#F44336"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select a Color",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        colorOptions.forEach { (name, hex) ->
            Button(
                onClick = {
                    // 1. Get the previous back stack entry
                    val previousEntry = navController.previousBackStackEntry

                    // 2. Set the result on the previous screen's SavedStateHandle
                    // Use the key defined in the companion object
                    previousEntry?.savedStateHandle?.set(COLOR_RESULT_KEY, hex)

                    // 3. Pop the current screen off the stack to return to the previous one
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(hex)))
            ) {
                Text("Select $name", color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.outlinedButtonColors()) {
            Text("Cancel")
        }
    }
}
