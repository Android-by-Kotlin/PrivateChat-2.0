package max.ohm.privatechat.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import max.ohm.privatechat.data.repository.ChatRepository
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.models.MessageStatus
import max.ohm.privatechat.models.PhoneAuthUser
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _chatList = MutableStateFlow<List<ChatListModel>>(emptyList())
    val chatList: StateFlow<List<ChatListModel>> = _chatList.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // Track current chat to prevent message mixing
    private var currentChatPhoneNumber: String? = null
    private var currentMessageListeners = mutableListOf<ChildEventListener>()
    private var currentSentRef: com.google.firebase.database.DatabaseReference? = null
    private var currentReceivedRef: com.google.firebase.database.DatabaseReference? = null

    init {
        loadChatsFromDatabase()
        syncWithFirebase()
    }

    private fun loadChatsFromDatabase() {
        viewModelScope.launch {
            chatRepository.getAllChats().collect { chats ->
                _chatList.value = chats
            }
        }
    }

    private fun syncWithFirebase() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        
        // Sync chats from Firebase
        val chatRef = firebaseDatabase.getReference("users/$currentUserId/chats")
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    for (childSnapshot in snapshot.children) {
                        val phoneNumber = childSnapshot.child("phoneNumber").getValue(String::class.java) ?: continue
                        val name = childSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val profileImage = childSnapshot.child("profileImage").getValue(String::class.java)
                        
                        Log.d("ChatViewModel", "Syncing chat - Phone: $phoneNumber, Name: $name, Has Profile Image: ${!profileImage.isNullOrEmpty()}")
                        
                        // Check if chat exists in local database
                        val existingChat = chatRepository.getChatByPhoneNumber(phoneNumber)
                        if (existingChat == null) {
                            // Add new chat to local database
                            val newChat = ChatListModel(
                                name = name,
                                phoneNumber = phoneNumber,
                                profilePicture = profileImage,
                                profileImage = profileImage, // Set both fields for compatibility
                                lastMessage = null,
                                lastMessageTime = System.currentTimeMillis()
                            )
                            Log.d("ChatViewModel", "Creating new chat with profile image size: ${profileImage?.length ?: 0}")
                            chatRepository.insertChat(newChat)
                        } else if (existingChat.profileImage.isNullOrEmpty() && !profileImage.isNullOrEmpty()) {
                            // Update existing chat with profile image if it doesn't have one
                            val updatedChat = existingChat.copy(
                                profilePicture = profileImage,
                                profileImage = profileImage
                            )
                            Log.d("ChatViewModel", "Updating existing chat with profile image size: ${profileImage.length}")
                            chatRepository.updateChat(updatedChat)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error syncing chats: ${error.message}")
            }
        })
    }

    fun loadMessagesForChat(phoneNumber: String) {
        // Clear messages and listeners if switching to a different chat
        if (currentChatPhoneNumber != phoneNumber) {
            _messages.value = emptyList()
            cleanupMessageListeners()
            currentChatPhoneNumber = phoneNumber
        }
        
        viewModelScope.launch {
            // Small delay to ensure proper initialization
            kotlinx.coroutines.delay(100)
            
            // First, load messages from local database
            chatRepository.getMessagesForChat(phoneNumber).collect { messages ->
                // Only update if this is still the current chat
                if (currentChatPhoneNumber == phoneNumber) {
                    val sortedMessages = messages.sortedBy { it.timeStamp }
                    // Remove duplicates based on content and timestamp
                    val uniqueMessages = sortedMessages.distinctBy { msg ->
                        "${msg.message}_${msg.timeStamp}_${msg.senderPhoneNumber}"
                    }
                    _messages.value = uniqueMessages
                }
            }
        }
        
        // Sync with Firebase after a delay to avoid race conditions
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            if (currentChatPhoneNumber == phoneNumber) {
                syncMessagesFromFirebase(phoneNumber)
            }
        }
    }

    private fun syncMessagesFromFirebase(phoneNumber: String) {
        val currentUserPhone = firebaseAuth.currentUser?.phoneNumber ?: return
        
        // Clean up previous listeners
        cleanupMessageListeners()
        
        // Track processed message IDs to prevent duplicates
        val processedMessageIds = mutableSetOf<String>()
        
        // Listen for sent messages
        currentSentRef = firebaseDatabase.getReference("messages")
            .child(currentUserPhone)
            .child(phoneNumber)
            
        val sentListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageKey = snapshot.key ?: return
                if (processedMessageIds.contains(messageKey)) return
                
                val message = snapshot.getValue(Message::class.java)
                if (message != null && currentChatPhoneNumber == phoneNumber) {
                    processedMessageIds.add(messageKey)
                    viewModelScope.launch {
                        // Check if message already exists in local database
                        val existingMessage = chatRepository.getMessageByContent(
                            phoneNumber,
                            message.message,
                            message.timeStamp,
                            message.senderPhoneNumber
                        )
                        
                        if (existingMessage == null) {
                            val updatedMessage = message.copy(chatPhoneNumber = phoneNumber)
                            chatRepository.insertMessage(updatedMessage)
                        }
                    }
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error syncing sent messages: ${error.message}")
            }
        }
        
        currentSentRef?.addChildEventListener(sentListener)
        currentMessageListeners.add(sentListener)
        
        // Listen for received messages
        currentReceivedRef = firebaseDatabase.getReference("messages")
            .child(phoneNumber)
            .child(currentUserPhone)
            
        val receivedListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val messageKey = snapshot.key ?: return
                if (processedMessageIds.contains(messageKey)) return
                
                val message = snapshot.getValue(Message::class.java)
                if (message != null && currentChatPhoneNumber == phoneNumber) {
                    processedMessageIds.add(messageKey)
                    viewModelScope.launch {
                        // Check if message already exists in local database
                        val existingMessage = chatRepository.getMessageByContent(
                            phoneNumber,
                            message.message,
                            message.timeStamp,
                            message.senderPhoneNumber
                        )
                        
                        if (existingMessage == null) {
                            val updatedMessage = message.copy(chatPhoneNumber = phoneNumber)
                            chatRepository.insertMessage(updatedMessage)
                            // Increment unread count for received messages
                            if (!message.isRead) {
                                chatRepository.incrementUnreadCount(phoneNumber)
                            }
                        }
                    }
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error syncing received messages: ${error.message}")
            }
        }
        
        currentReceivedRef?.addChildEventListener(receivedListener)
        currentMessageListeners.add(receivedListener)
    }
    
    private fun cleanupMessageListeners() {
        // Remove listeners from their specific references
        if (currentSentRef != null && currentMessageListeners.isNotEmpty()) {
            currentSentRef?.removeEventListener(currentMessageListeners[0])
        }
        if (currentReceivedRef != null && currentMessageListeners.size > 1) {
            currentReceivedRef?.removeEventListener(currentMessageListeners[1])
        }
        
        currentMessageListeners.clear()
        currentSentRef = null
        currentReceivedRef = null
    }

    fun sendMessage(receiverPhoneNumber: String, messageText: String) {
        val senderPhoneNumber = firebaseAuth.currentUser?.phoneNumber ?: return
        val messageId = firebaseDatabase.reference.push().key ?: return
        
        val message = Message(
            chatPhoneNumber = receiverPhoneNumber,
            senderPhoneNumber = senderPhoneNumber,
            message = messageText,
            timeStamp = System.currentTimeMillis(),
            isRead = false,
            messageStatus = MessageStatus.SENDING
        )
        
        viewModelScope.launch {
            // Save to local database first
            val localMessageId = chatRepository.insertMessage(message)
            
            // Update status to SENT
            chatRepository.updateMessageStatus(localMessageId, MessageStatus.SENT.name)
            
            // Send to Firebase
            firebaseDatabase.reference.child("messages")
                .child(senderPhoneNumber)
                .child(receiverPhoneNumber)
                .child(messageId)
                .setValue(message.copy(id = 0L, messageStatus = MessageStatus.SENT))
                .addOnSuccessListener {
                    viewModelScope.launch {
                        chatRepository.updateMessageStatus(localMessageId, MessageStatus.DELIVERED.name)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ChatViewModel", "Failed to send message: ${e.message}")
                }
                
            // Also add to receiver's node
            firebaseDatabase.reference.child("messages")
                .child(receiverPhoneNumber)
                .child(senderPhoneNumber)
                .child(messageId)
                .setValue(message.copy(id = 0L, messageStatus = MessageStatus.SENT))
        }
    }

    fun deleteChat(phoneNumber: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(phoneNumber)
        }
    }
    
    suspend fun getProfilePicture(phoneNumber: String): String? {
        return chatRepository.getProfilePicture(phoneNumber)
    }
    
    fun markChatAsRead(phoneNumber: String) {
        viewModelScope.launch {
            // Mark messages as read in local database
            chatRepository.markChatAsRead(phoneNumber)
            
            // Also update Firebase
            val currentUserPhone = firebaseAuth.currentUser?.phoneNumber ?: return@launch
            markMessagesAsReadInFirebase(currentUserPhone, phoneNumber)
        }
    }

    private fun markMessagesAsReadInFirebase(currentUserPhone: String, otherUserPhone: String) {
        val receivedRef = firebaseDatabase.reference.child("messages")
            .child(otherUserPhone)
            .child(currentUserPhone)
            
        receivedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { messageSnapshot ->
                    messageSnapshot.ref.child("isRead").setValue(true)
                    messageSnapshot.ref.child("messageStatus").setValue(MessageStatus.READ.name)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Error marking messages as read: ${error.message}")
            }
        })
    }

    fun searchUserByPhoneNumber(phoneNumber: String, callback: (ChatListModel?) -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e("ChatViewModel", "User is not authenticated")
            callback(null)
            return
        }

        var cleanPhoneNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
        
        if (!cleanPhoneNumber.startsWith("+")) {
            if (cleanPhoneNumber.length == 10) {
                cleanPhoneNumber = "+91$cleanPhoneNumber"
            } else if (cleanPhoneNumber.length == 11 && cleanPhoneNumber.startsWith("1")) {
                cleanPhoneNumber = "+$cleanPhoneNumber"
            }
        }
        
        Log.d("ChatViewModel", "Searching for phone number: $cleanPhoneNumber")

        val databaseReference = firebaseDatabase.getReference("users")
        databaseReference.orderByChild("phoneNumber").equalTo(cleanPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val userId = userSnapshot?.key
                            
                            if (userId != null && userId != currentUser.uid) {
                                val phoneAuthUser = userSnapshot.getValue(PhoneAuthUser::class.java)
                                if (phoneAuthUser != null) {
                                    val chatListModel = ChatListModel(
                                        name = phoneAuthUser.name ?: "Unknown",
                                        phoneNumber = phoneAuthUser.phoneNumber,
                                        userId = userId,
                                        profilePicture = phoneAuthUser.profileImage,
                                        profileImage = phoneAuthUser.profileImage, // Set both fields
                                        lastMessage = null,
                                        lastMessageTime = System.currentTimeMillis()
                                    )
                                    Log.d("ChatViewModel", "User found: ${phoneAuthUser.name}")
                                    callback(chatListModel)
                                } else {
                                    Log.e("ChatViewModel", "Failed to parse PhoneAuthUser")
                                    callback(null)
                                }
                            } else {
                                Log.d("ChatViewModel", "User is current user or null userId")
                                callback(null)
                            }
                        } else {
                            Log.d("ChatViewModel", "No user found with phone number: $cleanPhoneNumber")
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error processing user search: ${e.message}")
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error fetching User: ${error.message}")
                    callback(null)
                }
            })
    }

    fun addChat(newChat: ChatListModel) {
        viewModelScope.launch {
            // Save to local database
            chatRepository.insertChat(newChat)
            
            // Also add to Firebase
            addChatToFirebase(newChat)
        }
    }

    private fun addChatToFirebase(newChat: ChatListModel) {
        val currentUserId = firebaseAuth.currentUser?.uid
        val currentUserPhone = firebaseAuth.currentUser?.phoneNumber
        
        if (currentUserId != null && currentUserPhone != null && newChat.userId != null) {
            try {
                val chatId = if (currentUserId < newChat.userId!!) {
                    "${currentUserId}_${newChat.userId}"
                } else {
                    "${newChat.userId}_${currentUserId}"
                }

                // Add chat reference for current user
                val currentUserChatRef = firebaseDatabase
                    .getReference("users/$currentUserId/chats/$chatId")
                currentUserChatRef.setValue(mapOf(
                    "userId" to newChat.userId,
                    "phoneNumber" to newChat.phoneNumber,
                    "name" to newChat.name,
                    "profileImage" to newChat.profilePicture
                ))

                // Add chat reference for the other user
                val otherUserChatRef = firebaseDatabase
                    .getReference("users/${newChat.userId}/chats/$chatId")
                
                firebaseDatabase.getReference("users/$currentUserId")
                    .get().addOnSuccessListener { snapshot ->
                        val currentUserData = snapshot.getValue(PhoneAuthUser::class.java)
                        if (currentUserData != null) {
                            otherUserChatRef.setValue(mapOf(
                                "userId" to currentUserId,
                                "phoneNumber" to currentUserPhone,
                                "name" to currentUserData.name,
                                "profileImage" to currentUserData.profileImage
                            ))
                        }
                    }
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error in addChat: ${e.message}")
            }
        }
    }
    
    fun clearMessages() {
        _messages.value = emptyList()
        currentChatPhoneNumber = null
        cleanupMessageListeners()
    }
    
    override fun onCleared() {
        super.onCleared()
        cleanupMessageListeners()
    }
}
