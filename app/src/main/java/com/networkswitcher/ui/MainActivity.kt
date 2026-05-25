package com.networkswitcher.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.networkswitcher.NetworkSwitchApplication
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.PermissionState
import com.networkswitcher.ui.viewmodel.MainViewModel
import com.networkswitcher.ui.viewmodel.MainViewModelFactory
import com.networkswitcher.util.Resource
import kotlinx.coroutines.flow.collectLatest
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        viewModel.refreshState()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        viewModel.refreshState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        viewModel.refreshState()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as NetworkSwitchApplication
        val viewModelFactory = MainViewModelFactory(
            app,
            app.networkRepository,
            app.settingsRepository,
            app.permissionManager,
            app.networkModeManager
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        // Request READ_PHONE_STATE permission at startup for multi-SIM queries
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }

        setContent {
            NetworkSwitcherTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}

@Composable
fun NetworkSwitcherTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF00E5FF),      // Neon Teal
        onPrimary = Color(0xFF002025),
        secondary = Color(0xFF00E676),    // Emerald green
        background = Color(0xFF121214),   // Charcoal background
        surface = Color(0xFF1E1E24),      // Card background
        onBackground = Color(0xFFE3E3E6),
        onSurface = Color(0xFFE3E3E6),
        error = Color(0xFFFF5252)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = true) {
        viewModel.actionResult.collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, resource.data ?: "Applied successfully", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message ?: "Failed to apply", Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {
                    // Visual loading feedback
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Network Switcher",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshState() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val selectedSim = if (uiState.activeSims.isNotEmpty() && uiState.selectedSimIndex in uiState.activeSims.indices) {
                uiState.activeSims[uiState.selectedSimIndex]
            } else {
                null
            }

            // SIM Selection Tabs (only visible for multi-SIM setups)
            if (uiState.activeSims.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.activeSims.forEachIndexed { index, sim ->
                        val isSimSelected = uiState.selectedSimIndex == index
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectSim(index) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSimSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSimSelected) 2.dp else 1.dp,
                                color = if (isSimSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = sim.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSimSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sim.carrierName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sim.networkTypeName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // State Info Card
            if (selectedSim != null) {
                StatusCard(
                    carrierName = selectedSim.carrierName,
                    networkType = selectedSim.networkTypeName,
                    displayName = selectedSim.displayName,
                    slotIndex = selectedSim.slotIndex
                )
            }

            // Permissions Banner
            PermissionBanner(
                permissionState = uiState.permissionState,
                onCheckStatus = { viewModel.refreshState() },
                onAuthorizeShizuku = {
                    try {
                        if (Shizuku.pingBinder()) {
                            Shizuku.requestPermission(100)
                        } else {
                            Toast.makeText(context, "Shizuku is not running", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to request Shizuku: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Mode Selection Header
            Text(
                text = "Switch Network Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Dynamic Option Cards
            if (selectedSim != null) {
                val currentSimMode = selectedSim.resolvedMode
                    ?: viewModel.getSavedNetworkModeForSlot(selectedSim.slotIndex)

                NetworkMode.values().forEach { mode ->
                    ModeSelectionCard(
                        mode = mode,
                        isSelected = currentSimMode == mode,
                        isEnabled = uiState.permissionState == PermissionState.GRANTED,
                        onSelected = { viewModel.applyNetworkMode(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatusCard(carrierName: String, networkType: String, displayName: String, slotIndex: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = carrierName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$displayName (Slot ${slotIndex + 1})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current State: ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = networkType,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun PermissionBanner(
    permissionState: PermissionState,
    onCheckStatus: () -> Unit,
    onAuthorizeShizuku: () -> Unit
) {
    val context = LocalContext.current
    AnimatedVisibility(visible = permissionState != PermissionState.GRANTED) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Privileges Required",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                val descText = when (permissionState) {
                    PermissionState.SHIZUKU_NOT_RUNNING -> 
                        "Shizuku manager is not running. Please start the Shizuku service first."
                    PermissionState.SHIZUKU_DENIED -> 
                        "Shizuku access has not been authorized. Please approve the prompt or check Shizuku manager."
                    else -> 
                        "The application requires WRITE_SECURE_SETTINGS permissions to toggle hardware bands."
                }

                Text(
                    text = descText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))

                // Option 1: Shizuku steps
                Text(
                    text = "Option 1: Setup Shizuku Manager",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "1. Install the Shizuku app.\n2. Open Shizuku and start the service (using Wireless Debugging or Root access).\n3. Return here and tap \"Request Shizuku\" below to link.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Option 2: ADB command
                Text(
                    text = "Option 2: ADB Secure Setting Grant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Connect phone to PC with USB Debugging enabled, then execute:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                val adbCmd = "adb shell pm grant com.networkswitcher android.permission.WRITE_SECURE_SETTINGS"
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCmd))
                            Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = adbCmd,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Start
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAuthorizeShizuku,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Request Shizuku", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = onCheckStatus) {
                        Text("Recheck Status", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionCard(
    mode: NetworkMode,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelected: () -> Unit
) {
    val alpha = if (isEnabled) 1.0f else 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onSelected() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                val subtitle = when (mode) {
                    NetworkMode.FIVE_G_ONLY -> "Locks modem to NR network only (highest speeds)"
                    NetworkMode.FIVE_G_PREFERRED -> "Allows NR/LTE/GSM/WCDMA (recommended default)"
                    NetworkMode.FOUR_G_ONLY -> "Locks modem to LTE network (saves power, reliable)"
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

