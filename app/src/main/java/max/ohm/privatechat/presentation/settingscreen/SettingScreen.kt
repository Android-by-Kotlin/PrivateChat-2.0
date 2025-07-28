package max.ohm.privatechat.presentation.settingscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import max.ohm.privatechat.R
import max.ohm.privatechat.presentation.navigation.Routes
import max.ohm.privatechat.presentation.viewmodel.PhoneAuthViewModel
import androidx.compose.foundation.Image
import android.util.Base64
import android.graphics.BitmapFactory
import max.ohm.privatechat.models.PhoneAuthUser
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navHostController: NavHostController,
    phoneAuthViewModel: PhoneAuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var userProfile by remember { mutableStateOf<PhoneAuthUser?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Fetch user profile
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            phoneAuthViewModel.fetchUserProfile(userId) { profile ->
                userProfile = profile
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.light_green),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color.White)
        ) {
            // Profile Section
            ProfileSection(
                userProfile = userProfile,
                onProfileClick = {
                    navHostController.navigate(Routes.UserProfileSetScreen)
                }
            )

            HorizontalDivider(thickness = 8.dp, color = Color(0xFFF0F0F0))

            // Settings Options
            SettingsItem(
                icon = R.drawable.baseline_key_24,
                title = "Account",
                subtitle = "Security notifications, change number"
            )

            SettingsItem(
                icon = R.drawable.baseline_lock_24,
                title = "Privacy",
                subtitle = "Block contacts, disappearing messages"
            )

            SettingsItem(
                icon = R.drawable.baseline_chat_24,
                title = "Chats",
                subtitle = "Theme, wallpapers, chat history"
            )

            SettingsItem(
                icon = R.drawable.baseline_notifications_24,
                title = "Notifications",
                subtitle = "Message, group & call tones"
            )

            SettingsItem(
                icon = R.drawable.baseline_data_usage_24,
                title = "Storage and data",
                subtitle = "Network usage, auto-download"
            )

            SettingsItem(
                icon = R.drawable.baseline_help_24,
                title = "Help",
                subtitle = "Help centre, contact us, privacy policy"
            )

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))

            // Logout Option
            SettingsItem(
                icon = R.drawable.baseline_logout_24,
                title = "Logout",
                subtitle = "Sign out from your account",
                onClick = {
                    showLogoutDialog = true
                }
            )
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        phoneAuthViewModel.signOut(context as ComponentActivity)
                        navHostController.navigate(Routes.WelcomeScreen) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = colorResource(R.color.light_green))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileSection(
    userProfile: PhoneAuthUser?,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        ) {
            userProfile?.profileImage?.let { encodedImage ->
                val decodedBytes = try {
                    Base64.decode(encodedImage, Base64.DEFAULT)
                } catch (e: Exception) {
                    null
                }
                
                decodedBytes?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: Icon(
                    painter = painterResource(R.drawable.baseline_person_24),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    tint = Color.White
                )
            } ?: Icon(
                painter = painterResource(R.drawable.baseline_person_24),
                contentDescription = "Profile",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Profile Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = userProfile?.name ?: "Your Name",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = userProfile?.status ?: "Hey there! I am using WhatsApp",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // QR Code Icon
        IconButton(onClick = { /* TODO: Implement QR code */ }) {
            Icon(
                painter = painterResource(R.drawable.baseline_qr_code_24),
                contentDescription = "QR Code",
                tint = colorResource(R.color.light_green)
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.Black
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
