package max.ohm.privatechat.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import max.ohm.privatechat.data.entities.ChatEntity

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("SELECT phoneNumber, name, lastMessage, lastMessageTime, unreadCount, isOnline, status FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE phoneNumber = :phoneNumber")
    suspend fun getChatByPhoneNumber(phoneNumber: String): ChatEntity?
    
    @Query("SELECT phoneNumber, name, lastMessage, lastMessageTime, unreadCount, isOnline, status FROM chats WHERE phoneNumber = :phoneNumber")
    suspend fun getChatByPhoneNumberSafe(phoneNumber: String): ChatEntity?

    @Query("UPDATE chats SET lastMessage = :message, lastMessageTime = :time WHERE phoneNumber = :phoneNumber")
    suspend fun updateLastMessage(phoneNumber: String, message: String, time: Long)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE phoneNumber = :phoneNumber")
    suspend fun incrementUnreadCount(phoneNumber: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE phoneNumber = :phoneNumber")
    suspend fun markAsRead(phoneNumber: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
}
