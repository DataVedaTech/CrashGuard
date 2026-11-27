package com.dataveda.crashguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dataveda.crashguard.core.sensors.SimulationLevel
import com.dataveda.crashguard.ui.CrashGuardViewModel
import com.dataveda.crashguard.ui.contacts.ContactsScreen
import com.dataveda.crashguard.ui.history.HistoryScreen
import com.dataveda.crashguard.ui.history.IncidentDetailScreen
import com.dataveda.crashguard.ui.home.HomeScreen
import com.dataveda.crashguard.ui.onboarding.OnboardingScreen
import com.dataveda.crashguard.ui.profile.ProfileScreen
import com.dataveda.crashguard.ui.settings.PrivacyScreen
import com.dataveda.crashguard.ui.test.TestSosScreen
import com.dataveda.crashguard.ui.theme.CrashGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val vm: CrashGuardViewModel by viewModels()
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            CrashGuardTheme {
                CrashGuardNavApp(vm, requestedRoute) { requestedRoute = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("complete_onboarding", false) == true) {
            vm.completeOnboarding(enableProtection = true)
        }
        intent?.getStringExtra("route")?.let { requestedRoute = it }
        intent?.getStringExtra("simulate")?.let { sim ->
            when (sim) {
                "high" -> vm.simulate(SimulationLevel.HIGH)
                "need_help" -> vm. respondToCrash(com.dataveda.crashguard.domain.models.UserResponse.NEED_HELP)
                "sos" -> vm.triggerManualSos()
                "close" -> vm.closeIncident(markSafe = true)
            }
        }
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CONTACTS = "contacts"
    const val PROFILE = "profile"
    const val TEST_SOS = "test_sos"
    const val HISTORY = "history"
    const val INCIDENT = "incident/{id}"
    const val PRIVACY = "privacy"
}

@Composable
private fun CrashGuardNavApp(
    vm: CrashGuardViewModel,
    requestedRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val navController = rememberNavController()
    val startDestination = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING

    LaunchedEffect(requestedRoute) {
        if (!requestedRoute.isNullOrBlank()) {
            navController.navigate(requestedRoute) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    vm.completeOnboarding(enableProtection = true)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenTestSos = { navController.navigate(Routes.TEST_SOS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Routes.TEST_SOS) {
            TestSosScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenIncident = { id -> navController.navigate("incident/$id") },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onSelectIncident = { id -> navController.navigate("incident/$id") },
            )
        }
        composable(
            route = Routes.INCIDENT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            IncidentDetailScreen(
                incidentId = id,
                vm = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
