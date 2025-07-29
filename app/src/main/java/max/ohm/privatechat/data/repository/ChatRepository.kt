package max.ohm.privatechat.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import max.ohm.privatechat.data.dao.ChatDao
import max.ohm.privatechat.data.dao.MessageDao
import max.ohm.privatechat.data.entities.ChatEntity
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    fun getAllChats(): Flow<List<ChatListModel>> {
        return chatDao.getAllChats().map { chatEntities ->
            chatEntities.mapNotNull { entity ->
                try {
                    ChatListModel(
                        name = entity.name,
                        phoneNumber = entity.phoneNumber,
                        lastMessage = entity.lastMessage,
                        lastMessageTime = entity.lastMessageTime,
                        profilePicture = entity.profilePicture,
                        profileImage = entity.profilePicture, // Map to both fields for compatibility
                        isOnline = entity.isOnline,
                        status = entity.status,
                        unreadCount = entity.unreadCount
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "Error mapping chat entity: ${e.message}")
                    null
                }
            }
        }
    }

    suspend fun insertChat(chatModel: ChatListModel) {
        val chatEntity = ChatEntity(
            phoneNumber = chatModel.phoneNumber ?: "",
            name = chatModel.name ?: "Unknown",
            lastMessage = chatModel.lastMessage,
            lastMessageTime = chatModel.lastMessageTime,
            profilePicture = limitProfilePictureSize(chatModel.profilePicture),
            isOnline = chatModel.isOnline,
            status = chatModel.status,
            unreadCount = chatModel.unreadCount
        )
        chatDao.insertChat(chatEntity)
    }
    
    private fun limitProfilePictureSize(profilePicture: String?): String? {
        return if (profilePicture != null && profilePicture.length > 10000) {
            // If profile picture is too large, store null instead
            null
        } else {
            profilePicture
        }
    }

    suspend fun updateChat(chatModel: ChatListModel) {
        val chatEntity = ChatEntity(
            phoneNumber = chatModel.phoneNumber ?: "",
            name = chatModel.name ?: "Unknown",
            lastMessage = chatModel.lastMessage,
            lastMessageTime = chatModel.lastMessageTime,
            profilePicture = chatModel.profilePicture,
            isOnline = chatModel.isOnline,
            status = chatModel.status,
            unreadCount = chatModel.unreadCount
        )
        chatDao.updateChat(chatEntity)
    }

    suspend fun getChatByPhoneNumber(phoneNumber: String): ChatListModel? {
        return try {
            val entity = chatDao.getChatByPhoneNumberSafe(phoneNumber)
            entity?.let {
                ChatListModel(
                    name = it.name,
                    phoneNumber = it.phoneNumber,
                    lastMessage = it.lastMessage,
                    lastMessageTime = it.lastMessageTime,
                    profilePicture = it.profilePicture,
                    profileImage = it.profilePicture, // Map to both fields for compatibility
                    isOnline = it.isOnline,
                    status = it.status,
                    unreadCount = it.unreadCount
                )
            }
        } catch (e: Exception) {
            // Log error and return null instead of crashing
            android.util.Log.e("ChatRepository", "Error getting chat by phone number: ${e.message}")
            null
        }
    }

    suspend fun updateLastMessage(phoneNumber: String, message: String, timestamp: Long) {
        chatDao.updateLastMessage(phoneNumber, message, timestamp)
    }

    suspend fun markChatAsRead(phoneNumber: String) {
        chatDao.markAsRead(phoneNumber)
        messageDao.markMessagesAsRead(phoneNumber)
    }

    suspend fun incrementUnreadCount(phoneNumber: String) {
        chatDao.incrementUnreadCount(phoneNumber)
    }

    // Message related operations
    fun getMessagesForChat(phoneNumber: String): Flow<List<Message>> {
        return messageDao.getMessagesForChat(phoneNumber)
    }

    suspend fun insertMessage(message: Message): Long {
        val messageId = messageDao.insertMessage(message)
        // Update the last message in chat
        updateLastMessage(message.chatPhoneNumber, message.message, message.timeStamp)
        return messageId
    }

    suspend fun updateMessageStatus(messageId: Long, status: String) {
        messageDao.updateMessageStatus(messageId, status)
    }

    suspend fun deleteChat(phoneNumber: String) {
        val chat = chatDao.getChatByPhoneNumber(phoneNumber)
        chat?.let {
            chatDao.deleteChat(it)
            messageDao.deleteMessagesForChat(phoneNumber)
        }
    }
    
    suspend fun getMessageByContent(
        phoneNumber: String,
        message: String,
        timestamp: Long,
        senderPhoneNumber: String
    ): Message? {
        return messageDao.getMessageByContent(phoneNumber, message, timestamp, senderPhoneNumber)
    }
}
