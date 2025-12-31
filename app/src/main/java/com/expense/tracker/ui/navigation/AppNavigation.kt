package com.expense.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import com.expense.tracker.ui.theme.PurplePrimary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expense.tracker.ui.screens.calendar.CalendarScreen
import com.expense.tracker.ui.screens.home.HomeScreen
import com.expense.tracker.ui.screens.insights.InsightsScreen
import com.expense.tracker.ui.screens.settings.SettingsScreen
import com.expense.tracker.ui.screens.timeline.TimelineScreen

/**
 * Main navigation host for the app with bottom navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        bottomBar = {
            // Hide Bottom Navigation on Settings Screen
            if (currentDestination?.route != Screen.Settings.route) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimary.copy(alpha = 0.2f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Timeline.route) {
                TimelineScreen()
            }
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.Insights.route) {
                InsightsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
