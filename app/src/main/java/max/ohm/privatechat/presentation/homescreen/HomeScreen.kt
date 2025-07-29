package max.ohm.privatechat.presentation.homescreen

import android.graphics.Paint
import android.graphics.drawable.shapes.RoundRectShape
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults

import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import max.ohm.privatechat.R
import max.ohm.privatechat.presentation.bottomnavigation.BottomNavigation
import max.ohm.privatechat.presentation.chat_box.ChatListBox
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import max.ohm.privatechat.presentation.navigation.Routes
import max.ohm.privatechat.presentation.viewmodel.BaseViewModel
import max.ohm.privatechat.presentation.viewmodel.ChatViewModel
import max.ohm.privatechat.ui.theme.WhatsAppColors
import max.ohm.privatechat.utils.DebugUtils
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    navHostController: NavHostController, 
    homeBaseViewModel: BaseViewModel,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    var shownPopup by remember { mutableStateOf(false) }
    // Use ChatViewModel for persistent chat data
    val chatData by chatViewModel.chatList.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (userId != null) {
        LaunchedEffect(userId) {
            // Debug: Check all users in database
            DebugUtils.checkAllUsersInDatabase()
            homeBaseViewModel.getChatForUser(userId) { chats -> }
        }
    }

    Scaffold(
        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    shownPopup= true
                },
                containerColor = colorResource(R.color.light_green),
                contentColor = Color.White,
                modifier = Modifier.size(65.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_chat_icon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        },
        bottomBar = {

            BottomNavigation(navHostController, selectedItem = 0, onClick = {index ->
                when(index){

                    0 -> {navHostController.navigate(Routes.HomeScreen)}
                    1 -> {navHostController.navigate(Routes.UpdateScreen)}
                    2 -> {navHostController.navigate(Routes.CommunitiesScreen)}
                    3 -> {navHostController.navigate(Routes.CallScreen)}
                }

            })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhatsAppColors.DarkBackground)
                .padding(it)
        ) {
            // Top Bar with Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "WhatsApp",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppColors.DarkTextPrimary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(R.drawable.camera),
                            contentDescription = "Camera",
                            tint = WhatsAppColors.DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search",
                            tint = WhatsAppColors.DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { navHostController.navigate(Routes.UserProfileScreen) }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_person_24),
                            contentDescription = "Profile",
                            tint = WhatsAppColors.DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.more),
                            contentDescription = "More",
                            tint = WhatsAppColors.DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Search Bar (if searching)
            if (isSearching) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WhatsAppColors.DarkCard)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text(
                                "Ask Meta AI or Search", 
                                color = WhatsAppColors.DarkTextSecondary
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = WhatsAppColors.DarkTextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { 
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.cross),
                                    contentDescription = "Close",
                                    tint = WhatsAppColors.DarkTextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = WhatsAppColors.LightGreen
                        )
                    )
                }
            }
            
            // Filter Tabs
            if (!isSearching) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("All", "Unread", "Favourites", "Groups")) { tab ->
                        Card(
                            modifier = Modifier,
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tab == "All") WhatsAppColors.LightGreen else WhatsAppColors.DarkCard
                            )
                        ) {
                            Text(
                                text = tab,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = if (tab == "All") WhatsAppColors.DarkBackground else WhatsAppColors.DarkTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            // Chat List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WhatsAppColors.DarkBackground)
            ) {
                items(chatData) { chat ->
                    ChatListBox(
                        chatListModel = chat,
                        onClick = {
                            navHostController.navigate(
                                Routes.ChatScreen.createRoute(
                                    phoneNumber = chat.phoneNumber ?: ""
                                )
                            )
                        },
                        baseViewModel = homeBaseViewModel
                    )
                }
            }
            
            if (shownPopup) {
                AddUserPopup(
                    onDismiss = { shownPopup = false },
                    onUserAdd = { newUser ->
                        chatViewModel.addChat(newUser)
                    },
                    chatViewModel = chatViewModel
                )
            }
        }
    }
}

@Composable
fun AddUserPopup(
    onDismiss: () -> Unit,
    onUserAdd: (ChatListModel) -> Unit,
    chatViewModel: ChatViewModel
) {
    var phoneNumber by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var userFound by remember { mutableStateOf<ChatListModel?>(null) }
    var searchAttempted by remember { mutableStateOf(false) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WhatsAppColors.DarkSurface,
        title = {
            Text(
                text = "Add New Contact",
                color = WhatsAppColors.DarkTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { 
                        Text(
                            "Phone Number",
                            color = WhatsAppColors.DarkTextSecondary
                        ) 
                    },
                    placeholder = { 
                        Text(
                            "+919876543210",
                            color = WhatsAppColors.DarkTextSecondary.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = WhatsAppColors.DarkCard,
                        unfocusedContainerColor = WhatsAppColors.DarkCard,
                        focusedIndicatorColor = WhatsAppColors.LightGreen,
                        unfocusedIndicatorColor = WhatsAppColors.DarkTextSecondary,
                        cursorColor = WhatsAppColors.LightGreen,
                        focusedLabelColor = WhatsAppColors.LightGreen,
                        unfocusedLabelColor = WhatsAppColors.DarkTextSecondary,
                        focusedTextColor = WhatsAppColors.DarkTextPrimary,
                        unfocusedTextColor = WhatsAppColors.DarkTextPrimary
                    )
                )
                
                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = WhatsAppColors.LightGreen
                        )
                    }
                }
                
                userFound?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = WhatsAppColors.DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "User Found",
                                color = WhatsAppColors.LightGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.name ?: "Unknown",
                                color = WhatsAppColors.DarkTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.phoneNumber ?: "",
                                color = WhatsAppColors.DarkTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } ?: run {
                    if (!isSearching && searchAttempted) {
                        Text(
                            text = "No user found with this phone number",
                            color = Color(0xFFE91E63),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (userFound != null) {
                    Button(
                        onClick = {
                            userFound?.let { user ->
                                android.util.Log.d("AddUserPopup", "Add button clicked for user: ${user.name}")
                                onUserAdd(user)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppColors.LightGreen
                        )
                    ) {
                        Text(
                            "Add to Chat",
                            color = WhatsAppColors.DarkBackground
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (phoneNumber.isNotEmpty() && !isSearching) {
                                isSearching = true
                                searchAttempted = true
                                userFound = null
                                chatViewModel.searchUserByPhoneNumber(phoneNumber) { user ->
                                    isSearching = false
                                    userFound = user
                                }
                            }
                        },
                        enabled = phoneNumber.isNotEmpty() && !isSearching,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppColors.LightGreen,
                            disabledContainerColor = WhatsAppColors.DarkCard
                        )
                    ) {
                        Text(
                            "Search",
                            color = if (phoneNumber.isNotEmpty() && !isSearching) 
                                WhatsAppColors.DarkBackground 
                            else 
                                WhatsAppColors.DarkTextSecondary
                        )
                    }
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text(
                    "Cancel",
                    color = WhatsAppColors.DarkTextSecondary
                )
            }
        }
    )
}
