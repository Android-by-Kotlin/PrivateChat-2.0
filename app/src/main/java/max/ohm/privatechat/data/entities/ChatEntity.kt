package max.ohm.privatechat.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val phoneNumber: String,
    val name: String,
    val profilePicture: String? = null, // Store path or small identifier instead of full base64
    val lastMessage: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val status: String? = null
)
