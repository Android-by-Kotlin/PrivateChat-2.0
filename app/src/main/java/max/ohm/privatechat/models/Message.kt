package max.ohm.privatechat.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "messages")
@TypeConverters(MessageStatusConverter::class)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val chatPhoneNumber: String = "",
    val senderPhoneNumber: String = "",
    val message: String = "",
    val timeStamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageStatus: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

class MessageStatusConverter {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String {
        return status.name
    }

    @TypeConverter
    fun toMessageStatus(status: String): MessageStatus {
        return MessageStatus.valueOf(status)
    }
}
