package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.AppPreferences
import com.example.ui.navigation.AccountDetailRoute
import com.example.ui.navigation.AccountsRoute
import com.example.ui.navigation.ActivityRoute
import com.example.ui.navigation.BudgetsRoute
import com.example.ui.navigation.BuyGoldRoute
import com.example.ui.navigation.CategoriesRoute
import com.example.ui.navigation.DebtDetailRoute
import com.example.ui.navigation.DebtsRoute
import com.example.ui.navigation.GoalDetailRoute
import com.example.ui.navigation.GoalsRoute
import com.example.ui.navigation.GoldAnalyticsRoute
import com.example.ui.navigation.GoldDashboardRoute
import com.example.ui.navigation.GoldInventoryRoute
import com.example.ui.navigation.GoldItemDetailRoute
import com.example.ui.navigation.GoldObligationsRoute
import com.example.ui.navigation.GoldRatesRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.InsightsRoute
import com.example.ui.navigation.MoreRoute
import com.example.ui.navigation.PlanningRoute
import com.example.ui.navigation.RecurringRoute
import com.example.ui.navigation.SellGoldRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.StockDetailRoute
import com.example.ui.navigation.StocksRoute
import com.example.ui.navigation.TransactionDetailRoute
import com.example.ui.navigation.TransactionEntryRoute
import com.example.ui.screens.activity.ActivityScreen
import com.example.ui.screens.activity.ActivityViewModel
import com.example.ui.screens.gold.BuyGoldScreen
import com.example.ui.screens.gold.GoldAnalyticsScreen
import com.example.ui.screens.gold.GoldDashboardScreen
import com.example.ui.screens.gold.GoldDashboardViewModel
import com.example.ui.screens.gold.GoldInventoryScreen
import com.example.ui.screens.gold.GoldInventoryViewModel
import com.example.ui.screens.gold.GoldItemDetailScreen
import com.example.ui.screens.gold.GoldObligationsScreen
import com.example.ui.screens.gold.GoldObligationsViewModel
import com.example.ui.screens.gold.GoldRatesScreen
import com.example.ui.screens.gold.GoldRatesViewModel
import com.example.ui.screens.gold.GoldTradeViewModel
import com.example.ui.screens.gold.SellGoldScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.insights.InsightsViewModel
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.planning.AccountDetailScreen
import com.example.ui.screens.planning.AccountsScreen
import com.example.ui.screens.planning.AccountsViewModel
import com.example.ui.screens.planning.BudgetsScreen
import com.example.ui.screens.planning.BudgetsViewModel
import com.example.ui.screens.planning.CategoriesScreen
import com.example.ui.screens.planning.CategoriesViewModel
import com.example.ui.screens.planning.DebtDetailScreen
import com.example.ui.screens.planning.DebtsScreen
import com.example.ui.screens.planning.DebtsViewModel
import com.example.ui.screens.planning.GoalDetailScreen
import com.example.ui.screens.planning.GoalsScreen
import com.example.ui.screens.planning.GoalsViewModel
import com.example.ui.screens.planning.PlanningScreen
import com.example.ui.screens.planning.PlanningViewModel
import com.example.ui.screens.planning.RecurringScreen
import com.example.ui.screens.planning.RecurringViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.stocks.StockDetailScreen
import com.example.ui.screens.stocks.StocksScreen
import com.example.ui.screens.stocks.StocksViewModel
import com.example.ui.screens.transaction.TransactionDetailScreen
import com.example.ui.screens.transaction.TransactionEntryScreen
import com.example.ui.screens.transaction.TransactionViewModel
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.Charcoal850
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.RupeeLedgerTheme

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RupeeLedgerApp
        val container = app.container

        setContent {
            val prefs by container.preferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = AppPreferences())
            val windowSizeClass = calculateWindowSizeClass(this)
            var isUnlocked by rememberSaveable { mutableStateOf(false) }

            RupeeLedgerTheme(themeMode = prefs.themeMode) {
                if (prefs.appLockEnabled && !isUnlocked) {
                    AppLockGate(
                        onUnlock = { isUnlocked = true },
                        launchPrompt = { onResult -> launchBiometricUnlock(onResult) },
                    )
                } else {
                    RupeeLedgerRoot(
                        container = container,
                        widthSizeClass = windowSizeClass.widthSizeClass,
                    )
                }
            }
        }
    }

    private fun launchBiometricUnlock(onSuccess: () -> Unit) {
        val authFlags = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authFlags) != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess()
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_unlock_title))
            .setSubtitle(getString(R.string.biometric_unlock_subtitle))
            .setAllowedAuthenticators(authFlags)
            .build()
        prompt.authenticate(info)
    }
}

@Composable
private fun AppLockGate(
    onUnlock: () -> Unit,
    launchPrompt: (() -> Unit) -> Unit,
) {
    LaunchedEffect(Unit) {
        launchPrompt(onUnlock)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("RupeeLedger Locked", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Authenticate with your device biometric or PIN to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launchPrompt(onUnlock) },
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Unlock")
            }
        }
    }
}

private enum class TopTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val testTag: String,
) {
    HOME(R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, HomeRoute, "nav_home"),
    ACTIVITY(R.string.nav_activity, Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong, ActivityRoute, "nav_activity"),
    GOLD(R.string.nav_gold, Icons.Rounded.Diamond, Icons.Outlined.Diamond, GoldDashboardRoute, "nav_gold"),
    MORE(R.string.nav_more, Icons.Rounded.GridView, Icons.Outlined.GridView, MoreRoute, "nav_more"),
}

@Composable
fun RupeeLedgerRoot(
    container: AppContainer,
    widthSizeClass: WindowWidthSizeClass,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedGold = destination?.hasRoute<GoldDashboardRoute>() == true
    val isTopLevel = destination?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<ActivityRoute>() ||
            dest.hasRoute<GoldDashboardRoute>() ||
            dest.hasRoute<MoreRoute>()
    } ?: true

    val useRail = widthSizeClass != WindowWidthSizeClass.Compact

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (useRail) {
        Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isTopLevel) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = if (selectedGold) Charcoal850 else MaterialTheme.colorScheme.surface,
                    header = {
                        FloatingActionButton(
                            onClick = { navController.navigate(TransactionEntryRoute()) },
                            containerColor = if (selectedGold) ChampagneGold else MaterialTheme.colorScheme.primary,
                            contentColor = if (selectedGold) Charcoal950 else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(top = 12.dp).testTag("nav_add_transaction"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.nav_add))
                        }
                    },
                ) {
                    Spacer(Modifier.height(16.dp))
                    TopTab.entries.forEach { tab ->
                        val selected = when (tab) {
                            TopTab.HOME -> destination?.hasRoute<HomeRoute>() == true
                            TopTab.ACTIVITY -> destination?.hasRoute<ActivityRoute>() == true
                            TopTab.GOLD -> selectedGold
                            TopTab.MORE -> destination?.hasRoute<MoreRoute>() == true
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes),
                                    tint = if (tab == TopTab.GOLD && selected) ChampagneGold else Color.Unspecified,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag(tab.testTag),
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                RupeeLedgerNavHost(navController = navController, container = container)
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel) {
                    CompactFinanceBottomBar(
                        selectedHome = destination?.hasRoute<HomeRoute>() == true,
                        selectedActivity = destination?.hasRoute<ActivityRoute>() == true,
                        selectedGold = selectedGold,
                        selectedMore = destination?.hasRoute<MoreRoute>() == true,
                        onSelectTab = { tab -> navigateTopTab(tab.route) },
                        onAddTransaction = { navController.navigate(TransactionEntryRoute()) },
                    )
                }
            },
        ) { _ ->
            RupeeLedgerNavHost(navController = navController, container = container)
        }
    }
}

@Composable
private fun CompactFinanceBottomBar(
    selectedHome: Boolean,
    selectedActivity: Boolean,
    selectedGold: Boolean,
    selectedMore: Boolean,
    onSelectTab: (TopTab) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val barBg = if (selectedGold) Charcoal850 else MaterialTheme.colorScheme.surface
    val fabContainer = if (selectedGold) ChampagneGold else MaterialTheme.colorScheme.primary
    val fabContent = if (selectedGold) Charcoal950 else MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = barBg,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            BottomBarTabItem(
                tab = TopTab.HOME,
                selected = selectedHome,
                isGoldSurface = selectedGold,
                onClick = { onSelectTab(TopTab.HOME) },
                modifier = Modifier.weight(1f),
            )
            BottomBarTabItem(
                tab = TopTab.ACTIVITY,
                selected = selectedActivity,
                isGoldSurface = selectedGold,
                onClick = { onSelectTab(TopTab.ACTIVITY) },
                modifier = Modifier.weight(1f),
            )

            // Prominent central Add Transaction action
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                FloatingActionButton(
                    onClick = onAddTransaction,
                    shape = CircleShape,
                    containerColor = fabContainer,
                    contentColor = fabContent,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(4.dp, CircleShape)
                        .testTag("nav_add_transaction"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.nav_add),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            BottomBarTabItem(
                tab = TopTab.GOLD,
                selected = selectedGold,
                isGoldSurface = selectedGold,
                onClick = { onSelectTab(TopTab.GOLD) },
                modifier = Modifier.weight(1f),
            )
            BottomBarTabItem(
                tab = TopTab.MORE,
                selected = selectedMore,
                isGoldSurface = selectedGold,
                onClick = { onSelectTab(TopTab.MORE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomBarTabItem(
    tab: TopTab,
    selected: Boolean,
    isGoldSurface: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = when {
        tab == TopTab.GOLD && selected -> ChampagneGold
        selected -> MaterialTheme.colorScheme.primary
        isGoldSurface -> Color(0xFFA39E93)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(tab.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(tab.labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RupeeLedgerNavHost(
    navController: NavHostController,
    container: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<HomeRoute> {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            HomeScreen(
                viewModel = vm,
                onAddTransaction = { navController.navigate(TransactionEntryRoute()) },
                onOpenTransactionDetail = { id -> navController.navigate(TransactionDetailRoute(id)) },
                onOpenActivity = { navController.navigate(ActivityRoute) },
                onOpenAccounts = { navController.navigate(AccountsRoute) },
                onOpenBudgets = { navController.navigate(BudgetsRoute) },
                onOpenGoals = { navController.navigate(GoalsRoute) },
                onOpenRecurring = { navController.navigate(RecurringRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<ActivityRoute> {
            val vm: ActivityViewModel = viewModel(
                factory = ActivityViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            ActivityScreen(
                viewModel = vm,
                onOpenTransactionDetail = { id -> navController.navigate(TransactionDetailRoute(id)) },
                onAddTransaction = { navController.navigate(TransactionEntryRoute()) },
            )
        }

        composable<GoldDashboardRoute> {
            val vm: GoldDashboardViewModel = viewModel(
                factory = GoldDashboardViewModel.Factory(container.goldRepository, container.preferencesRepository),
            )
            GoldDashboardScreen(
                viewModel = vm,
                onBuyGold = { navController.navigate(BuyGoldRoute) },
                onSellGold = { navController.navigate(SellGoldRoute()) },
                onOpenInventory = { navController.navigate(GoldInventoryRoute) },
                onOpenObligations = { navController.navigate(GoldObligationsRoute) },
                onOpenRates = { navController.navigate(GoldRatesRoute) },
                onOpenAnalytics = { navController.navigate(GoldAnalyticsRoute) },
            )
        }

        composable<MoreRoute> {
            MoreScreen(
                onOpenInsights = { navController.navigate(InsightsRoute) },
                onOpenPlanning = { navController.navigate(PlanningRoute) },
                onOpenStocks = { navController.navigate(StocksRoute) },
                onOpenGold = { navController.navigate(GoldDashboardRoute) },
                onOpenAccounts = { navController.navigate(AccountsRoute) },
                onOpenBudgets = { navController.navigate(BudgetsRoute) },
                onOpenGoals = { navController.navigate(GoalsRoute) },
                onOpenRecurring = { navController.navigate(RecurringRoute) },
                onOpenDebts = { navController.navigate(DebtsRoute) },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        // Gold Treasury Subscreens
        composable<GoldInventoryRoute> {
            val vm: GoldInventoryViewModel = viewModel(
                factory = GoldInventoryViewModel.Factory(container.goldRepository, container.preferencesRepository),
            )
            GoldInventoryScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenItemDetail = { id -> navController.navigate(GoldItemDetailRoute(id)) },
                onOpenBuyGold = { navController.navigate(BuyGoldRoute) },
            )
        }

        composable<GoldItemDetailRoute> { entry ->
            val args = entry.toRoute<GoldItemDetailRoute>()
            val vm: GoldInventoryViewModel = viewModel(
                factory = GoldInventoryViewModel.Factory(
                    container.goldRepository,
                    container.preferencesRepository,
                    detailItemId = args.itemId,
                ),
            )
            GoldItemDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onSellItem = { id -> navController.navigate(SellGoldRoute(preselectedItemId = id)) },
            )
        }

        composable<BuyGoldRoute> {
            val vm: GoldTradeViewModel = viewModel(
                factory = GoldTradeViewModel.Factory(
                    container.goldRepository,
                    container.financeRepository,
                    container.preferencesRepository,
                ),
            )
            BuyGoldScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<SellGoldRoute> { entry ->
            val args = entry.toRoute<SellGoldRoute>()
            val vm: GoldTradeViewModel = viewModel(
                factory = GoldTradeViewModel.Factory(
                    container.goldRepository,
                    container.financeRepository,
                    container.preferencesRepository,
                ),
            )
            SellGoldScreen(
                viewModel = vm,
                preselectedItemId = args.preselectedItemId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<GoldObligationsRoute> {
            val vm: GoldObligationsViewModel = viewModel(
                factory = GoldObligationsViewModel.Factory(container.goldRepository, container.preferencesRepository),
            )
            GoldObligationsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<GoldRatesRoute> {
            val vm: GoldRatesViewModel = viewModel(
                factory = GoldRatesViewModel.Factory(container.goldRepository, container.preferencesRepository),
            )
            GoldRatesScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<GoldAnalyticsRoute> {
            val dashVm: GoldDashboardViewModel = viewModel(
                factory = GoldDashboardViewModel.Factory(container.goldRepository, container.preferencesRepository),
            )
            GoldAnalyticsScreen(
                dashboardViewModel = dashVm,
                goldRepository = container.goldRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Stock Investments
        composable<StocksRoute> {
            val vm: StocksViewModel = viewModel(
                factory = StocksViewModel.Factory(
                    container.stockRepository,
                    container.financeRepository,
                    container.preferencesRepository,
                ),
            )
            StocksScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenStockDetail = { id -> navController.navigate(StockDetailRoute(id)) },
            )
        }

        composable<StockDetailRoute> { entry ->
            val args = entry.toRoute<StockDetailRoute>()
            val vm: StocksViewModel = viewModel(
                factory = StocksViewModel.Factory(
                    container.stockRepository,
                    container.financeRepository,
                    container.preferencesRepository,
                    detailStockId = args.stockId,
                ),
            )
            StockDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // Existing Personal Finance Subscreens
        composable<InsightsRoute> {
            val vm: InsightsViewModel = viewModel(
                factory = InsightsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            InsightsScreen(
                viewModel = vm,
                onOpenTransactionDetail = { id -> navController.navigate(TransactionDetailRoute(id)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<PlanningRoute> {
            val vm: PlanningViewModel = viewModel(
                factory = PlanningViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            PlanningScreen(
                viewModel = vm,
                onOpenAccounts = { navController.navigate(AccountsRoute) },
                onOpenBudgets = { navController.navigate(BudgetsRoute) },
                onOpenGoals = { navController.navigate(GoalsRoute) },
                onOpenRecurring = { navController.navigate(RecurringRoute) },
                onOpenDebts = { navController.navigate(DebtsRoute) },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<TransactionEntryRoute> { entry ->
            val args = entry.toRoute<TransactionEntryRoute>()
            val vm: TransactionViewModel = viewModel(
                factory = TransactionViewModel.Factory(
                    financeRepository = container.financeRepository,
                    preferencesRepository = container.preferencesRepository,
                    transactionId = args.transactionId,
                    duplicateFromId = args.duplicateFromId,
                    initialType = args.initialType,
                    initialAccountId = args.initialAccountId,
                ),
            )
            TransactionEntryScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<TransactionDetailRoute> { entry ->
            val args = entry.toRoute<TransactionDetailRoute>()
            val vm: TransactionViewModel = viewModel(
                factory = TransactionViewModel.Factory(
                    financeRepository = container.financeRepository,
                    preferencesRepository = container.preferencesRepository,
                    transactionId = args.transactionId,
                ),
            )
            TransactionDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(TransactionEntryRoute(transactionId = id)) },
                onDuplicate = { id -> navController.navigate(TransactionEntryRoute(duplicateFromId = id)) },
            )
        }

        composable<AccountsRoute> {
            val vm: AccountsViewModel = viewModel(
                factory = AccountsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            AccountsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenAccountDetail = { id -> navController.navigate(AccountDetailRoute(id)) },
                onTransferMoney = { navController.navigate(TransactionEntryRoute(initialType = "TRANSFER")) },
            )
        }

        composable<AccountDetailRoute> { entry ->
            val args = entry.toRoute<AccountDetailRoute>()
            val vm: AccountsViewModel = viewModel(
                factory = AccountsViewModel.Factory(
                    container.financeRepository,
                    container.preferencesRepository,
                    detailAccountId = args.accountId,
                ),
            )
            AccountDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenTransactionDetail = { id -> navController.navigate(TransactionDetailRoute(id)) },
                onAddTransactionForAccount = { accId ->
                    navController.navigate(TransactionEntryRoute(initialAccountId = accId))
                },
            )
        }

        composable<CategoriesRoute> {
            val vm: CategoriesViewModel = viewModel(
                factory = CategoriesViewModel.Factory(container.financeRepository),
            )
            CategoriesScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<BudgetsRoute> {
            val vm: BudgetsViewModel = viewModel(
                factory = BudgetsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            BudgetsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<GoalsRoute> {
            val vm: GoalsViewModel = viewModel(
                factory = GoalsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            GoalsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenGoalDetail = { id -> navController.navigate(GoalDetailRoute(id)) },
            )
        }

        composable<GoalDetailRoute> { entry ->
            val args = entry.toRoute<GoalDetailRoute>()
            val vm: GoalsViewModel = viewModel(
                factory = GoalsViewModel.Factory(
                    container.financeRepository,
                    container.preferencesRepository,
                    detailGoalId = args.goalId,
                ),
            )
            GoalDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<RecurringRoute> {
            val vm: RecurringViewModel = viewModel(
                factory = RecurringViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            RecurringScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenAccounts = { navController.navigate(AccountsRoute) },
            )
        }

        composable<DebtsRoute> {
            val vm: DebtsViewModel = viewModel(
                factory = DebtsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            DebtsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenDebtDetail = { id -> navController.navigate(DebtDetailRoute(id)) },
            )
        }

        composable<DebtDetailRoute> { entry ->
            val args = entry.toRoute<DebtDetailRoute>()
            val vm: DebtsViewModel = viewModel(
                factory = DebtsViewModel.Factory(
                    container.financeRepository,
                    container.preferencesRepository,
                    detailDebtId = args.debtId,
                ),
            )
            DebtDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(container.financeRepository, container.preferencesRepository),
            )
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onOpenCategories = { navController.navigate(CategoriesRoute) },
            )
        }
    }
}
