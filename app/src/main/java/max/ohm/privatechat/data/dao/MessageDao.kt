package max.ohm.privatechat.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import max.ohm.privatechat.models.Message

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(message: Message): Long

    @Insert
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT * FROM messages WHERE chatPhoneNumber = :phoneNumber ORDER BY timeStamp ASC")
    fun getMessagesForChat(phoneNumber: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?

    @Query("UPDATE messages SET messageStatus = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Long, status: String)

    @Query("UPDATE messages SET isRead = 1 WHERE chatPhoneNumber = :phoneNumber AND isRead = 0")
    suspend fun markMessagesAsRead(phoneNumber: String)

    @Query("DELETE FROM messages WHERE chatPhoneNumber = :phoneNumber")
    suspend fun deleteMessagesForChat(phoneNumber: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT * FROM messages WHERE chatPhoneNumber = :phoneNumber ORDER BY timeStamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(phoneNumber: String): Message?
    
    @Query("SELECT * FROM messages WHERE chatPhoneNumber = :phoneNumber AND message = :message AND timeStamp = :timestamp AND senderPhoneNumber = :senderPhoneNumber LIMIT 1")
    suspend fun getMessageByContent(phoneNumber: String, message: String, timestamp: Long, senderPhoneNumber: String): Message?
}
