package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.TransactionViewModel
import com.example.ui.screens.AddScreen
import com.example.ui.screens.ListScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

enum class MainScreen {
    LIST, STATS, ADD
}

class MainActivity : ComponentActivity() {
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModel.Factory(application)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkThemeOverride by remember { mutableStateOf<Boolean?>(null) }
            val useDarkTheme = darkThemeOverride ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = useDarkTheme) {
                var currentScreen by remember { mutableStateOf(MainScreen.LIST) }

                // System Back Handler
                BackHandler(enabled = currentScreen != MainScreen.LIST) {
                    currentScreen = MainScreen.LIST
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // Show Top Bar with app title and theme toggle only on main screens (LIST, STATS)
                        if (currentScreen != MainScreen.ADD) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "Личные расходы",
                                        fontWeight = FontWeight.Black
                                    )
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            darkThemeOverride = !useDarkTheme
                                        },
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (useDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Переключить тему"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    },
                    bottomBar = {
                        // Display bottom bar only on main tabs
                        if (currentScreen != MainScreen.ADD) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen == MainScreen.LIST,
                                    onClick = { currentScreen = MainScreen.LIST },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Операции") },
                                    label = { Text("Операции") },
                                    modifier = Modifier.testTag("nav_item_operations")
                                )
                                NavigationBarItem(
                                    selected = currentScreen == MainScreen.STATS,
                                    onClick = { currentScreen = MainScreen.STATS },
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Статистика") },
                                    label = { Text("Статистика") },
                                    modifier = Modifier.testTag("nav_item_stats")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    
                    when (currentScreen) {
                        MainScreen.LIST -> {
                            ListScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { currentScreen = MainScreen.ADD },
                                modifier = contentModifier
                            )
                        }
                        MainScreen.STATS -> {
                            StatsScreen(
                                viewModel = viewModel,
                                modifier = contentModifier
                            )
                        }
                        MainScreen.ADD -> {
                            AddScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = MainScreen.LIST }
                            )
                        }
                    }
                }
            }
        }
    }
}
