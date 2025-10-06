// This file demonstrates the ID-Only pattern by passing a simple ID
// and fetching the complex object inside the destination ViewModel.

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// --- Data Model and Repository (Simulating Business Logic) ---

data class User(val id: Int, val name: String, val email: String)

/**
 * Simulates fetching complex data from a database or API.
 * This ensures the screen always gets the freshest version of the data.
 */
class UserRepository {
    private val users = mapOf(
        101 to User(101, "Anya Smith", "anya@example.com"),
        202 to User(202, "Ben Miller", "ben@example.com"),
        303 to User(303, "Cathy Lee", "cathy@example.com")
    )

    // Simulate a network/database call
    suspend fun loadUser(id: Int): User? {
        delay(500) // Simulate loading time
        return users[id]
    }
}

// --- ViewModel (Receives ID, Fetches Data) ---

const val USER_ID_ARG = "itemId"

class UserDetailViewModel(
    savedStateHandle: SavedStateHandle, // Provided by Navigation internally
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    // Get the ID that was passed in the navigation route
    private val userId: Int? = savedStateHandle[USER_ID_ARG]

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Start fetching data immediately when the ViewModel is created
        if (userId != null) {
            fetchUser(userId)
        } else {
            _isLoading.value = false
            // Handle error: ID was missing
        }
    }

    private fun fetchUser(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _user.value = repository.loadUser(id)
            _isLoading.value = false
        }
    }
}

// --- Navigation Setup (Routes and NavHost) ---

sealed class Screen(val route: String) {
    object List : Screen("user_list")

    // Placeholder for the ID argument
    object Detail : Screen("user_detail/{$USER_ID_ARG}") {
        fun createRoute(userId: Int) = "user_detail/$userId"
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.List.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.List.route) { UserListScreen(navController) }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument(USER_ID_ARG) { type = NavType.IntType }
            )
        ) {
            // No argument retrieval here! The ViewModel handles it.
            UserDetailScreen(
                // Use the standard ViewModel factory (provided by compose-lifecycle library)
                viewModel = viewModel(factory = createViewModelFactory())
            )
        }
    }
}

// Simple factory needed for demonstration outside of an Activity context
private fun createViewModelFactory() = object : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserDetailViewModel(
                // SavedStateHandle is usually provided by the framework, mocking here
                SavedStateHandle(mapOf(USER_ID_ARG to 101)), // Fallback for simple demo
                UserRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Screens (UI) ---

@Composable
fun UserListScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("User List", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        // Example: Navigating to User 202
        Button(
            onClick = { navController.navigate(Screen.Detail.createRoute(202)) }
        ) {
            Text("View Ben Miller (ID 202)")
        }
    }
}

@Composable
fun UserDetailScreen(viewModel: UserDetailViewModel) {
    // Observe the StateFlows from the ViewModel
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading user data...", style = MaterialTheme.typography.titleMedium)
        } else if (user != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Text("User Details", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("ID: ${user!!.id}", style = MaterialTheme.typography.bodyLarge)
                    Text("Name: ${user!!.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Email: ${user!!.email}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Text("Error: User not found.", color = MaterialTheme.colorScheme.error)
        }
    }
}
