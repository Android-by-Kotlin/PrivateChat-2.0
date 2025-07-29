package max.ohm.privatechat.presentation.profile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import max.ohm.privatechat.R
import max.ohm.privatechat.models.PhoneAuthUser
import max.ohm.privatechat.ui.theme.WhatsAppColors
import androidx.hilt.navigation.compose.hiltViewModel
import max.ohm.privatechat.presentation.viewmodel.PhoneAuthViewModel
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import max.ohm.privatechat.presentation.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavHostController
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val database = FirebaseDatabase.getInstance()
    val context = LocalContext.current
    val phoneAuthViewModel: PhoneAuthViewModel = hiltViewModel()
    
    var userProfile by remember { mutableStateOf<PhoneAuthUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load user profile from Firebase
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            database.getReference("users/$uid")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userProfile = snapshot.getValue(PhoneAuthUser::class.java)
                        isLoading = false
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                })
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = WhatsAppColors.DarkTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WhatsAppColors.DarkTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to edit profile */ }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = WhatsAppColors.DarkTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppColors.DarkSurface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WhatsAppColors.LightGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DarkBackground)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(WhatsAppColors.DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(WhatsAppColors.DarkCard)
                            .clickable { /* TODO: View/Change profile picture */ },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile?.profileImage != null) {
                            val decodedBytes = try {
                                Base64.decode(userProfile?.profileImage, Base64.DEFAULT)
                            } catch (e: Exception) {
                                null
                            }
                            
                            val bitmap = decodedBytes?.let { bytes ->
                                try {
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_person_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = WhatsAppColors.DarkTextSecondary
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.baseline_person_24),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = WhatsAppColors.DarkTextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // User Info Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WhatsAppColors.DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Name
                        ProfileInfoItem(
                            icon = painterResource(R.drawable.baseline_person_24),
                            label = "Name",
                            value = userProfile?.name ?: "Not set",
                            subtitle = "This is not your username or pin. This name will be visible to your WhatsApp contacts."
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = WhatsAppColors.DarkCard
                        )
                        
                        // About
                        ProfileInfoItem(
                            icon = painterResource(R.drawable.baseline_emoji_emotions_24),
                            label = "About",
                            value = userProfile?.about ?: "Hey there! I am using WhatsApp."
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = WhatsAppColors.DarkCard
                        )
                        
                        // Phone
                        ProfileInfoItem(
                            icon = painterResource(R.drawable.baseline_call_24),
                            label = "Phone",
                            value = currentUser?.phoneNumber ?: "Not available"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Additional Options
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WhatsAppColors.DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                .padding(8.dp)
            ) {
                ProfileOption(
                    icon = painterResource(R.drawable.baseline_notifications_24),
                    title = "Notifications",
                    onClick = { /* TODO: Navigate to notification settings */ }
                )

                ProfileOption(
                    icon = painterResource(R.drawable.baseline_lock_24),
                    title = "Privacy",
                    onClick = { /* TODO: Navigate to privacy settings */ }
                )

                ProfileOption(
                    icon = painterResource(R.drawable.baseline_data_usage_24),
                    title = "Storage and data",
                    onClick = { /* TODO: Navigate to storage settings */ }
                )

                ProfileOption(
                    icon = painterResource(R.drawable.baseline_logout_24),
                    title = "Logout",
                    onClick = {
                        phoneAuthViewModel.signOut(activity = context as Activity)
                        navController.navigate(Routes.WelcomeScreen) { 
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    value: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = WhatsAppColors.DarkTextSecondary
        )
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = WhatsAppColors.DarkTextSecondary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                fontSize = 16.sp,
                color = WhatsAppColors.DarkTextPrimary
            )
            
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = WhatsAppColors.DarkTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ProfileOption(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = WhatsAppColors.DarkTextSecondary
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Text(
            text = title,
            fontSize = 16.sp,
            color = WhatsAppColors.DarkTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

