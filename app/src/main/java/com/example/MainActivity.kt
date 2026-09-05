package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.AuditViewModel
import com.example.ui.screens.DiffViewScreen
import com.example.ui.screens.PrivacyVaultScreen
import com.example.ui.screens.RectifyLetterScreen
import com.example.ui.screens.ScanInputScreen
import com.example.ui.theme.DeepNavyPrimary
import com.example.ui.theme.DiscrepancyRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VerifiedGreen
import com.example.util.VaultSecurity

class MainActivity : ComponentActivity() {
    private val viewModel: AuditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CertiCheckApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertiCheckApp(viewModel: AuditViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val vaultState by viewModel.vaultState.collectAsState()
    val activeAudit by viewModel.activeAudit.collectAsState()

    val discrepancyCount = activeAudit?.discrepancies?.count { !it.isMatch } ?: 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DeepNavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "NDcertiCheck",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Certificate Discrepancy Auditor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Vault Lock Status Quick Indicator
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.PRIVACY_VAULT) },
                        modifier = Modifier.testTag("appbar_vault_icon")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (vaultState == VaultSecurity.VaultState.UNLOCKED)
                                VerifiedGreen.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (vaultState == VaultSecurity.VaultState.UNLOCKED)
                                        Icons.Default.LockOpen
                                    else Icons.Default.Lock,
                                    contentDescription = "Vault Status",
                                    tint = if (vaultState == VaultSecurity.VaultState.UNLOCKED)
                                        VerifiedGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (vaultState == VaultSecurity.VaultState.UNLOCKED) "Vault" else "PIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (vaultState == VaultSecurity.VaultState.UNLOCKED)
                                        VerifiedGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SCAN_INPUT,
                    onClick = { viewModel.navigateTo(AppScreen.SCAN_INPUT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Audit & OCR"
                        )
                    },
                    label = { Text("Audit & OCR", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepNavyPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_scan")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.DIFF_VIEW,
                    onClick = { viewModel.navigateTo(AppScreen.DIFF_VIEW) },
                    icon = {
                        if (discrepancyCount > 0) {
                            BadgedBox(badge = {
                                Badge(
                                    containerColor = DiscrepancyRed,
                                    contentColor = Color.White
                                ) {
                                    Text("$discrepancyCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = "Diff View"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Diff View"
                            )
                        }
                    },
                    label = { Text("Diff View", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepNavyPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_diff")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.RECTIFY_LETTER,
                    onClick = { viewModel.navigateTo(AppScreen.RECTIFY_LETTER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Rectify Letter"
                        )
                    },
                    label = { Text("Rectify Letter", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepNavyPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_letter")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.PRIVACY_VAULT,
                    onClick = { viewModel.navigateTo(AppScreen.PRIVACY_VAULT) },
                    icon = {
                        Icon(
                            imageVector = if (vaultState == VaultSecurity.VaultState.UNLOCKED)
                                Icons.Default.LockOpen
                            else Icons.Default.Lock,
                            contentDescription = "Privacy Vault"
                        )
                    },
                    label = { Text("Privacy Vault", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepNavyPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_vault")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding)
        ) { screen ->
            when (screen) {
                AppScreen.SCAN_INPUT -> ScanInputScreen(viewModel = viewModel)
                AppScreen.DIFF_VIEW -> DiffViewScreen(viewModel = viewModel)
                AppScreen.RECTIFY_LETTER -> RectifyLetterScreen(viewModel = viewModel)
                AppScreen.PRIVACY_VAULT -> PrivacyVaultScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
