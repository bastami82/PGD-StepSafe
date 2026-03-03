package uk.appyapp.stepsafe

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import dagger.hilt.android.AndroidEntryPoint
import uk.appyapp.stepsafe.data.local.entities.CaregiverContact
import uk.appyapp.stepsafe.data.local.entities.HomeLocation
import uk.appyapp.stepsafe.ui.MainViewModel
import uk.appyapp.stepsafe.ui.components.PermissionProvider
import uk.appyapp.stepsafe.ui.theme.StepSafeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepSafeTheme {
                PermissionProvider {
                    MainScreenContent(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(activity: MainActivity, viewModel: MainViewModel = hiltViewModel()) {
    val homeLocation by viewModel.homeLocation.collectAsState()
    val caregiverContact by viewModel.caregiverContact.collectAsState()
    val safeZoneRadius by viewModel.safeZoneRadius.collectAsState()
    val monitoringActive by viewModel.monitoringActive.collectAsState()
    val isGeocoding by viewModel.isGeocoding
    val geocodingError by viewModel.geocodingError
    val isRegisteringGeofence by viewModel.isRegisteringGeofence
    val geofenceResult by viewModel.geofenceResult
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(geocodingError) {
        geocodingError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearGeocodingError()
        }
    }

    LaunchedEffect(geofenceResult) {
        geofenceResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearGeofenceResult()
            // Automate app backgrounding when monitoring is successfully started
            if (it.contains("Monitoring started successfully")) {
                activity.moveTaskToBack(true)
            }
        }
    }

    var showContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("StepSafe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            HomeLocationCard(
                homeLocation = homeLocation,
                radius = safeZoneRadius,
                isGeocoding = isGeocoding,
                isRegisteringGeofence = isRegisteringGeofence,
                geofenceResult = geofenceResult,
                onRadiusChange = { viewModel.updateSafeZoneRadius(it) },
                onSetGeofence = { viewModel.updateGeofenceSettings() },
                onPostcodeSearch = { viewModel.searchPostcode(it) },
                onMapClick = { viewModel.updateLocationFromMap(it.latitude, it.longitude) }
            )

            CaregiverCard(
                contact = caregiverContact,
                onClick = { showContactDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Monitoring Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (monitoringActive) {
                    OutlinedButton(
                        onClick = { viewModel.stopGeofencing() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Stop Monitoring", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Button(
                        onClick = { activity.moveTaskToBack(true) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Run In Background", style = MaterialTheme.typography.titleMedium)
                    }
                    // Debug-only: Insert sample ExitEvent for smoke-testing DB export (runtime check)
                    val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    if (isDebuggable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                // Insert sample exit event and show a toast/snackbar
                                viewModel.insertSampleExitEvent()
                                Toast.makeText(context, "Inserted debug ExitEvent", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Insert Debug ExitEvent")
                        }
                    }
                } else {
                    Button(
                        onClick = { 
                            viewModel.startMonitoring()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = homeLocation != null && caregiverContact != null
                    ) {
                        Text("Start Monitoring", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showContactDialog) {
            CaregiverContactDialogComponent(
                currentContact = caregiverContact,
                onDismiss = { showContactDialog = false },
                onSave = { name: String, phone: String ->
                    viewModel.updateCaregiverContact(name, phone)
                    showContactDialog = false
                }
            )
        }
    }
}

@Composable
fun HomeLocationCard(
    homeLocation: HomeLocation?,
    radius: Double,
    isGeocoding: Boolean,
    isRegisteringGeofence: Boolean,
    geofenceResult: String?,
    onRadiusChange: (Double) -> Unit,
    onSetGeofence: () -> Unit,
    onPostcodeSearch: (String) -> Unit,
    onMapClick: (LatLng) -> Unit
) {
    var postcodeInput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val showSaved = remember(geofenceResult) { geofenceResult != null && geofenceResult.contains("Settings saved") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F0FE)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Home location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // 1. Postcode Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = postcodeInput,
                    onValueChange = { postcodeInput = it },
                    label = { Text("Postcode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPostcodeSearch(postcodeInput) 
                    },
                    enabled = !isGeocoding && postcodeInput.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF0047AB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    if (isGeocoding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search Postcode")
                    }
                }
            }

            // 2. Google Map preview
            val location = remember(homeLocation) {
                homeLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(51.5074, -0.1278)
            }
            
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(location, 15f)
            }

            LaunchedEffect(location) {
                cameraPositionState.animate(
                    update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(location, 15f),
                    durationMs = 1000
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = MaterialTheme.shapes.small
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false,
                        compassEnabled = false,
                        mapToolbarEnabled = false
                    ),
                    onMapClick = onMapClick
                ) {
                    if (homeLocation != null) {
                        Marker(
                            state = MarkerState(position = location),
                            title = "Home"
                        )
                        Circle(
                            center = location,
                            radius = radius,
                            fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // 3. Radius Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Radius: ${radius.toInt()} m",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Slider(
                    value = radius.toFloat(),
                    onValueChange = { onRadiusChange(it.toDouble()) },
                    valueRange = 50f..500f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0047AB),
                        activeTrackColor = Color(0xFF0047AB),
                        inactiveTrackColor = Color(0xFFFFDAD6)
                    )
                )
            }

            // 4. Home Location Label (Address)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                val addressText = homeLocation?.address ?: "Not set"
                Text(
                    text = "${radius.toInt()} m radius from $addressText",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            // 5. "Set Geofence" Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSetGeofence()
                },
                enabled = !isRegisteringGeofence,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0047AB)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRegisteringGeofence) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text("Saving...", fontWeight = FontWeight.Bold)
                    } else if (showSaved) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Saved", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Set Geofence", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CaregiverCard(contact: CaregiverContact?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContactPhone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text("Caregiver Contact", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = contact?.let { "${it.name} (${it.phoneNumber})" } ?: "Tap to set contact",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (contact == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CaregiverContactDialogComponent(
    currentContact: CaregiverContact?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentContact?.name ?: "") }
    var phone by remember { mutableStateOf(currentContact?.phoneNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Caregiver Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, 
                    onValueChange = { phone = it }, 
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, phone) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
