package max.ohm.privatechat.presentation.chat_box

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import max.ohm.privatechat.presentation.viewmodel.BaseViewModel
import max.ohm.privatechat.R
import max.ohm.privatechat.ui.theme.WhatsAppColors
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import max.ohm.privatechat.utils.ImageCache


@Composable
fun ChatListBox(
    chatListModel: ChatListModel,
    onClick: () -> Unit,   // Higher order function
    baseViewModel : BaseViewModel
) {
    // Add click debouncing to prevent multiple rapid clicks
    var lastClickTime by remember { mutableStateOf(0L) }
    val currentTime = System.currentTimeMillis()
    
    // Use persistent data from chatListModel
    val lastMessage = chatListModel.lastMessage ?: chatListModel.message ?: "Tap to start chatting"
    val lastMessageTime = if (chatListModel.lastMessageTime > 0) {
        formatMessageTime(chatListModel.lastMessageTime.toString())
    } else {
        chatListModel.time ?: ""
    }
    val unreadCount = chatListModel.unreadCount

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { 
            // Debounce clicks - prevent multiple clicks within 500ms
            if (currentTime - lastClickTime > 500) {
                lastClickTime = currentTime
                onClick()
            }
        }
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        
        // Load profile image with caching
        LaunchedEffect(chatListModel.phoneNumber) {
            chatListModel.phoneNumber?.let { phoneNumber ->
                Log.d("ChatListBox", "Loading profile image for $phoneNumber, profileImage exists: ${!chatListModel.profileImage.isNullOrEmpty()}, length: ${chatListModel.profileImage?.length ?: 0}")
                
                // First check memory cache
                ImageCache.getBitmapFromCache(phoneNumber)?.let {
                    Log.d("ChatListBox", "Found image in memory cache for $phoneNumber")
                    bitmap = it
                    return@LaunchedEffect
                }
                
                // Then check disk cache
                ImageCache.loadBitmapFromDisk(context, phoneNumber)?.let {
                    Log.d("ChatListBox", "Found image in disk cache for $phoneNumber")
                    bitmap = it
                    return@LaunchedEffect
                }
                
                // Finally decode from base64 if available
                chatListModel.profileImage?.let { profileImage ->
                    if (profileImage.isNotEmpty() && profileImage.length < 100000) {
                        Log.d("ChatListBox", "Decoding base64 image for $phoneNumber, size: ${profileImage.length}")
                        scope.launch {
                            try {
                                baseViewModel.base64ToBitmap(profileImage)?.let { decodedBitmap ->
                                    bitmap = decodedBitmap
                                    ImageCache.addBitmapToCache(phoneNumber, decodedBitmap)
                                    ImageCache.saveBitmapToDisk(context, phoneNumber, decodedBitmap)
                                    Log.d("ChatListBox", "Successfully decoded and cached image for $phoneNumber")
                                }
                            } catch (e: Exception) {
                                Log.e("ChatListBox", "Error decoding profile image: ${e.message}")
                            }
                        }
                    } else if (profileImage.isNotEmpty()) {
                        Log.w("ChatListBox", "Profile image too large for $phoneNumber: ${profileImage.length} bytes")
                    }
                } ?: run {
                    Log.d("ChatListBox", "No profile image available for $phoneNumber")
                }
            }
        }

        // Profile Image
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
        ) {
            if (bitmap != null) {
                Image(
                    painter = rememberAsyncImagePainter(bitmap),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Show default avatar with first letter of name
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF19AB60)),
                    contentAlignment = Alignment.Center
                ) {
                    val displayName = chatListModel.name ?: "?"
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = chatListModel.name ?: "Unknown",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = WhatsAppColors.DarkTextPrimary
                )
                Text(
                    text = lastMessageTime,
                    color = if (lastMessageTime == "Yesterday") WhatsAppColors.LightGreen else WhatsAppColors.DarkTextSecondary,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lastMessage,
                    color = WhatsAppColors.DarkTextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Unread message count badge
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(WhatsAppColors.LightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = WhatsAppColors.DarkBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format time
fun formatMessageTime(timestamp: String): String {
    return try {
        val time = timestamp.toLongOrNull() ?: return ""
        val messageDate = Date(time)
        val now = Date()
        val dateFormat = when {
            isSameDay(messageDate, now) -> SimpleDateFormat("HH:mm", Locale.getDefault())
            isYesterday(messageDate, now) -> return "Yesterday"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        }
        dateFormat.format(messageDate)
    } catch (e: Exception) {
        ""
    }
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    cal2.add(Calendar.DAY_OF_YEAR, -1)
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
