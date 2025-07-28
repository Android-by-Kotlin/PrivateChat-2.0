package max.ohm.privatechat.presentation.chatscreen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.presentation.viewmodel.BaseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    receiverPhoneNumber: String,
    receiverName: String = "Contact",
    receiverProfileImage: String? = null,
    baseViewModel: BaseViewModel = hiltViewModel()
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserPhone = currentUser?.phoneNumber ?: ""
    
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    
    val context = LocalContext.current
    
    // Load messages when screen opens
    LaunchedEffect(receiverPhoneNumber) {
        baseViewModel.getMessage(currentUserPhone, receiverPhoneNumber) { newMessage ->
            messages = messages + newMessage
        }
        
        // Also load messages from receiver to sender
        baseViewModel.getMessage(receiverPhoneNumber, currentUserPhone) { newMessage ->
            messages = messages + newMessage
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                containerColor = Color.White
            )
        )
        
        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            reverseLayout = false
        ) {
            items(messages.sortedBy { it.timeStamp }) { message ->
                MessageItem(
                    message = message,
                    isCurrentUser = message.senderPhoneNumber == currentUserPhone
                )
            }
        }
        
        // Message Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.LightGray.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.LightGray.copy(alpha = 0.2f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotEmpty()) {
                        baseViewModel.sendMessage(
                            currentUserPhone,
                            receiverPhoneNumber,
                            messageText
                        )
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = colorResource(R.color.light_green)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
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
        colorResource(R.color.light_green).copy(alpha = 0.8f) 
    else 
        Color.LightGray.copy(alpha = 0.3f)
    
    val textColor = if (isCurrentUser) Color.White else Color.Black
    
    // Format timestamp
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(message.timeStamp))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 100.dp, max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    color = textColor,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formattedTime,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
