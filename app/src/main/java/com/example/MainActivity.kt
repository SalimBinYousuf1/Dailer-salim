package com.example

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.telephony.CallManager
import com.example.ui.blocked.BlockedNumbersScreen
import com.example.ui.call.CallActivity
import com.example.ui.components.SalimBottomNavigation
import com.example.ui.components.SalimTab
import com.example.ui.components.SquirclePillShape
import com.example.ui.contacts.ContactsScreen
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.contacts.FavoritesScreen
import com.example.ui.dialpad.DialpadScreen
import com.example.ui.dialpad.DialpadViewModel
import com.example.ui.recents.RecentsScreen
import com.example.ui.recents.RecentsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.SalimPhoneTheme

class MainActivity : ComponentActivity() {

    private val dialpadViewModel: DialpadViewModel by viewModels()
    private val recentsViewModel: RecentsViewModel by viewModels()
    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SalimPhoneTheme {
                val context = LocalContext.current
                val app = application as SalimApplication
                val telephonyRepo = app.telephonyRepository

                var currentTab by remember { mutableStateOf(SalimTab.KEYPAD) }
                var currentSubScreen by remember { mutableStateOf<String?>(null) } // "settings", "blocked"

                val recentsUiState by recentsViewModel.uiState.collectAsStateWithLifecycle()
                val activeCallState by CallManager.callState.collectAsStateWithLifecycle()

                // Default Dialer Role Request Launcher
                val roleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    dialpadViewModel.refreshState()
                    recentsViewModel.loadRecents()
                    contactsViewModel.loadContacts()
                }

                fun requestDefaultDialer() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val roleManager = getSystemService(Context.ROLE_SERVICE) as? RoleManager
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                roleLauncher.launch(intent)
                                return
                            }
                        }
                    }
                    Toast.makeText(context, "Salim is already default dialer", Toast.LENGTH_SHORT).show()
                }

                // Permissions Launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    recentsViewModel.loadRecents()
                    contactsViewModel.loadContacts()
                    dialpadViewModel.refreshState()
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        android.Manifest.permission.CALL_PHONE,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.WRITE_CONTACTS,
                        android.Manifest.permission.READ_CALL_LOG,
                        android.Manifest.permission.WRITE_CALL_LOG
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsLauncher.launch(permissions.toTypedArray())

                    // Trigger default dialer role check on launch if not held
                    if (!telephonyRepo.isDefaultDialer()) {
                        requestDefaultDialer()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentSubScreen == null) {
                            SalimBottomNavigation(
                                selectedTab = currentTab,
                                onTabSelected = { currentTab = it },
                                missedCallsCount = recentsUiState.missedCallsCount
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        // Top ongoing call banner if active call is underway
                        AnimatedVisibility(
                            visible = activeCallState.hasCall,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            ActiveCallBanner(
                                callerText = activeCallState.displayName ?: activeCallState.number,
                                onClick = {
                                    val intent = Intent(context, CallActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Top settings action button on relevant screens
                        if (currentSubScreen == null && currentTab != SalimTab.KEYPAD) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { currentSubScreen = "settings" },
                                    modifier = Modifier.testTag("open_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Screen routing
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentSubScreen) {
                                "settings" -> {
                                    SettingsScreen(
                                        onBack = { currentSubScreen = null },
                                        onNavigateToBlocked = { currentSubScreen = "blocked" }
                                    )
                                }
                                "blocked" -> {
                                    BlockedNumbersScreen(
                                        onBack = { currentSubScreen = "settings" }
                                    )
                                }
                                else -> {
                                    when (currentTab) {
                                        SalimTab.FAVORITES -> {
                                            FavoritesScreen(
                                                viewModel = contactsViewModel,
                                                onNavigateToContacts = { currentTab = SalimTab.CONTACTS }
                                            )
                                        }
                                        SalimTab.RECENTS -> {
                                            RecentsScreen(
                                                viewModel = recentsViewModel,
                                                onAddContactClicked = { number ->
                                                    contactsViewModel.openAddContact(number)
                                                    currentTab = SalimTab.CONTACTS
                                                }
                                            )
                                        }
                                        SalimTab.CONTACTS -> {
                                            ContactsScreen(viewModel = contactsViewModel)
                                        }
                                        SalimTab.KEYPAD -> {
                                            DialpadScreen(
                                                viewModel = dialpadViewModel,
                                                onRequestDefaultDialer = { requestDefaultDialer() },
                                                onAddContactClicked = { number ->
                                                    contactsViewModel.openAddContact(number)
                                                    currentTab = SalimTab.CONTACTS
                                                }
                                            )
                                        }
                                        SalimTab.VOICEMAIL -> {
                                            val vmNum = telephonyRepo.getVoicemailNumber()
                                            com.example.ui.voicemail.VoicemailScreen(
                                                onCallVoicemail = { dialpadViewModel.onVoicemailTriggered() },
                                                voicemailNumber = vmNum
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialpadViewModel.refreshState()
        recentsViewModel.loadRecents()
    }
}

@Composable
private fun ActiveCallBanner(
    callerText: String,
    onClick: () -> Unit
) {
    Surface(
        color = AccentGreen,
        shape = SquirclePillShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(SquirclePillShape)
            .clickable { onClick() }
            .testTag("active_call_banner")
            .semantics { contentDescription = "Active call with $callerText. Tap to return to call." }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Touch to return to call • $callerText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
