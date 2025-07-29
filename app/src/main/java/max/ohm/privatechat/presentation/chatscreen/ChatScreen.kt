package max.ohm.privatechat.presentation.chatscreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import max.ohm.privatechat.R
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.models.MessageStatus
import max.ohm.privatechat.presentation.viewmodel.ChatViewModel
import max.ohm.privatechat.ui.theme.WhatsAppColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    receiverPhoneNumber: String,
    receiverName: String = "Contact",
receiverProfileImage: String? = null,
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserPhone = currentUser?.phoneNumber ?: ""
    
    var messageText by remember { mutableStateOf("") }
val messages by chatViewModel.messages.collectAsState()
    val messageIds = remember { mutableSetOf<String>() }
    
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    // Load messages when screen opens with a small delay to ensure proper initialization
    LaunchedEffect(receiverPhoneNumber) {
        kotlinx.coroutines.delay(100) // Small delay to ensure proper initialization
        chatViewModel.loadMessagesForChat(receiverPhoneNumber)
        chatViewModel.markChatAsRead(receiverPhoneNumber)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppColors.DarkBackground)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorResource(R.color.light_green))
                    ) {
                        if (receiverProfileImage != null) {
                            val decodedBytes = try {
                                Base64.decode(receiverProfileImage, Base64.DEFAULT)
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
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.user_profile),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(R.drawable.user_profile),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = receiverName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Online",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = WhatsAppColors.DarkSurface,
                titleContentColor = WhatsAppColors.DarkTextPrimary,
                navigationIconContentColor = WhatsAppColors.DarkTextPrimary
            ),
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_videocam_24),
                        contentDescription = "Video call",
                        tint = WhatsAppColors.DarkTextSecondary
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_call_24),
                        contentDescription = "Voice call",
                        tint = WhatsAppColors.DarkTextSecondary
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.more),
                        contentDescription = "More options",
                        tint = WhatsAppColors.DarkTextSecondary
                    )
                }
            }
        )
        
        // Messages List with WhatsApp-like background
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(WhatsAppColors.DarkBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                state = scrollState,
                reverseLayout = false
            ) {
                // Group messages by date
                val groupedMessages = messages.groupBy { message ->
                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(message.timeStamp))
                }
                
                groupedMessages.forEach { (date, messagesForDate) ->
                    // Date separator
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = WhatsAppColors.DarkCard)
                            ) {
                                Text(
                                    text = date,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = WhatsAppColors.DarkTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    
                    // Messages for this date
                    items(messagesForDate) { message ->
                        MessageItem(
                            message = message,
                            isCurrentUser = message.senderPhoneNumber == currentUserPhone
                        )
                    }
                }
            }
        }
        
        // Message Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppColors.DarkSurface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = WhatsAppColors.DarkCard)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_emoji_emotions_24),
                        contentDescription = "Emoji",
                        tint = WhatsAppColors.DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { 
                            Text(
                                "Message", 
                                color = WhatsAppColors.DarkTextSecondary
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = WhatsAppColors.LightGreen,
                            focusedTextColor = WhatsAppColors.DarkTextPrimary,
                            unfocusedTextColor = WhatsAppColors.DarkTextPrimary
                        ),
                        singleLine = true
                    )
                    
                    Icon(
                        painter = painterResource(R.drawable.baseline_attach_file_24),
                        contentDescription = "Attach",
                        tint = WhatsAppColors.DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    if (messageText.isEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.camera),
                            contentDescription = "Camera",
                            tint = WhatsAppColors.DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotEmpty()) {
chatViewModel.sendMessage(
    receiverPhoneNumber = receiverPhoneNumber,
    messageText = messageText
)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = WhatsAppColors.LightGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = WhatsAppColors.DarkBackground
                )
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) 
        Color(0xFFDCF8C6) // WhatsApp green for sent messages
    else 
        Color.White // White for received messages
    
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
    }
    
    // Format timestamp
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(message.timeStamp))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 40.dp else 8.dp,
                end = if (isCurrentUser) 8.dp else 40.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = bubbleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.message,
                    color = Color.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Row(
                    modifier = Modifier.align(Alignment.Bottom),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    
                    // Show message status for current user's messages
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(
                                when (message.messageStatus) {
                                    MessageStatus.SENDING -> R.drawable.ic_check
                                    MessageStatus.SENT -> R.drawable.ic_check
                                    MessageStatus.DELIVERED -> R.drawable.ic_double_check
                                    MessageStatus.READ -> R.drawable.ic_double_check_blue
                                }
                            ),
                            contentDescription = "Message status",
                            modifier = Modifier.size(16.dp),
                            tint = if (message.messageStatus == MessageStatus.READ) 
                                Color(0xFF53BDEB) // WhatsApp blue for read receipts
                            else 
                                Color.Gray
                        )
                    }
                }
            }
        }
    }
}
