package max.ohm.privatechat.presentation.viewmodel


import android.R.id.message
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.snap
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import max.ohm.privatechat.models.Message
import max.ohm.privatechat.models.PhoneAuthUser
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


class BaseViewModel: ViewModel() {


    fun searchUserByPhoneNumber(phoneNumber: String, callback : (ChatListModel?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser == null){
            Log.e("BaseViewModel", "User is not authenticated")
            callback(null)
            return
        }

        // Clean the phone number
        var cleanPhoneNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
        
        // If the phone number doesn't start with +, try to add a default country code
        if (!cleanPhoneNumber.startsWith("+")) {
            // Try with +91 (India) as default - you can make this configurable
            if (cleanPhoneNumber.length == 10) {
                cleanPhoneNumber = "+91$cleanPhoneNumber"
            } else if (cleanPhoneNumber.length == 11 && cleanPhoneNumber.startsWith("1")) {
                cleanPhoneNumber = "+$cleanPhoneNumber" // US numbers
            }
        }
        
        Log.d("BaseViewModel", "Searching for phone number: $cleanPhoneNumber")

        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.orderByChild("phoneNumber").equalTo(cleanPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if(snapshot.exists()){
                            val userSnapshot = snapshot.children.firstOrNull()
                            val userId = userSnapshot?.key
                            
                            // Don't return the current user
                            if (userId != null && userId != currentUser.uid) {
                                val phoneAuthUser = userSnapshot.getValue(PhoneAuthUser::class.java)
                                if (phoneAuthUser != null) {
                                    val chatListModel = ChatListModel(
                                        name = phoneAuthUser.name ?: "Unknown",
                                        phoneNumber = phoneAuthUser.phoneNumber,
                                        userId = userId,
                                        profileImage = phoneAuthUser.profileImage,
                                        image = null,
                                        time = null,
                                        message = null
                                    )
                                    Log.d("BaseViewModel", "User found: ${phoneAuthUser.name}")
                                    callback(chatListModel)
                                } else {
                                    Log.e("BaseViewModel", "Failed to parse PhoneAuthUser")
                                    callback(null)
                                }
                            } else {
                                Log.d("BaseViewModel", "User is current user or null userId")
                                callback(null)
                            }
                        } else{
                            Log.d("BaseViewModel", "No user found with phone number: $cleanPhoneNumber")
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("BaseViewModel", "Error processing user search: ${e.message}")
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BaseViewModel", "Error fetching User: ${error.message}, Details : ${error.details}")
                    callback(null)
                }
            }
            )



    }

    // higher order funnction
    fun getChatForUser(userId: String, callback: (List<ChatListModel>) -> Unit ){

        // path of fethching chats
        val chatref= FirebaseDatabase.getInstance().getReference("users/$userId/chats")
        chatref.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList= mutableListOf<ChatListModel>()

                    for(childSnapshot in snapshot.children){

                        val chat = childSnapshot.getValue(ChatListModel::class.java)

                        if(chat != null){
                            chatList.add(chat)
                        }

                    }
                    callback(chatList)

                }

                override fun onCancelled(error: DatabaseError) {

                    Log.e("BaseViewModel", "Error fetching user chats: ${error.message}")
                    callback(emptyList())
                }


            }

            )
    }


    private val _chatList = MutableStateFlow<List<ChatListModel>>(emptyList())
    val chatList = _chatList.asStateFlow()

    init {
        loadChatData()
    }

    private fun loadChatData(){

        val currentuserId = FirebaseAuth.getInstance().currentUser?.uid

        if(currentuserId != null){

            val chatRef = FirebaseDatabase.getInstance().getReference("chats")

            chatRef.orderByChild("userId").equalTo(currentuserId)
                .addValueEventListener(object : ValueEventListener{

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chatList = mutableListOf<ChatListModel>()
                        for(childSnapshot in snapshot.children){

                            val chat = childSnapshot.getValue(ChatListModel::class.java)

                            if(chat !=null){
                                chatList.add(chat)

                            }
                        }
                        _chatList.value = chatList

                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("BaseViewModel", "Error fetching user chats: ${error.message}")

                    }


                })
        }
    }

    fun addChat(newChat: ChatListModel){
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val currentUserPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber

        Log.d("BaseViewModel", "Adding chat for user: ${newChat.name}, userId: ${newChat.userId}")
        
        if(currentUserId != null && currentUserPhone != null && newChat.userId != null){
            try {
                // Create a unique chat ID for both users
                val chatId = if (currentUserId < newChat.userId!!) {
                    "${currentUserId}_${newChat.userId}"
                } else {
                    "${newChat.userId}_${currentUserId}"
                }

            // Add chat reference for current user
            val currentUserChatRef = FirebaseDatabase.getInstance()
                .getReference("users/$currentUserId/chats/$chatId")
            currentUserChatRef.setValue(mapOf(
                "userId" to newChat.userId,
                "phoneNumber" to newChat.phoneNumber,
                "name" to newChat.name,
                "profileImage" to newChat.profileImage
            )).addOnSuccessListener{
                Log.d("BaseViewModel", "Chat added for current user")
            }.addOnFailureListener{ exception ->
                Log.e("BaseViewModel", "Failed to add chat for current user: ${exception.message}")
            }

            // Add chat reference for the other user
            val otherUserChatRef = FirebaseDatabase.getInstance()
                .getReference("users/${newChat.userId}/chats/$chatId")
            
            // Get current user's name for the other user's chat list
            FirebaseDatabase.getInstance().getReference("users/$currentUserId")
                .get().addOnSuccessListener { snapshot ->
                    val currentUserData = snapshot.getValue(PhoneAuthUser::class.java)
                    if (currentUserData != null) {
                        otherUserChatRef.setValue(mapOf(
                            "userId" to currentUserId,
                            "phoneNumber" to currentUserPhone,
                            "name" to currentUserData.name,
                            "profileImage" to currentUserData.profileImage
                        )).addOnSuccessListener{
                            Log.d("BaseViewModel", "Chat added for other user")
                        }.addOnFailureListener{ exception ->
                            Log.e("BaseViewModel", "Failed to add chat for other user: ${exception.message}")
                        }
                    }
                }

            // Also add to the main chats node for easy access
            val chatRef = FirebaseDatabase.getInstance().getReference("chats").push()
            val chatData = newChat.copy(userId = currentUserId)
            chatRef.setValue(chatData).addOnSuccessListener {
                Log.d("BaseViewModel", "Chat added to main chats node")
            }.addOnFailureListener { exception ->
                Log.e("BaseViewModel", "Failed to add chat to main chats: ${exception.message}")
            }
            
            } catch (e: Exception) {
                Log.e("BaseViewModel", "Error in addChat: ${e.message}")
            }
        }else{
            Log.e("BaseViewModel", "User not authenticated or missing data: userId=$currentUserId, phone=$currentUserPhone, newChat.userId=${newChat.userId}")
        }
    }

    private val databaseReference= FirebaseDatabase.getInstance().reference

    fun sendMessage(senderPhoneNumber: String, receiverPhoneNumber: String, messageText: String){
         val messageId = databaseReference.push().key?: return
         val message = Message(
             senderPhoneNumber,
             messageText,
             System.currentTimeMillis()

         )

        databaseReference.child("messages")
            .child(senderPhoneNumber)
            .child(receiverPhoneNumber)
            .child(messageId)
            .setValue(message)


        databaseReference.child("messages")

            .child(receiverPhoneNumber)
            .child(senderPhoneNumber)
            .child(messageId)
            .setValue(message)
    }

    fun getMessage(
        senderPhoneNumber: String,
        receiverPhoneNumber: String,
        onNewMessage: (Message) -> Unit

    ){
        val messageRef = databaseReference.child("messages")
            .child(senderPhoneNumber)
            .child(receiverPhoneNumber)


        messageRef.addChildEventListener(object : ChildEventListener{

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message:: class.java)

                if(message != null){
                    onNewMessage(message)
                }


            }

            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })


    }

    fun fetchLastMessageForChat(
        senderPhoneNumber: String,
        receiverPhoneNumber: String,
        onLastMessageFetched: (String, String) -> Unit


    ){

       val chatRef= FirebaseDatabase.getInstance().reference
           .child("messages")
           .child(senderPhoneNumber)
           .child(receiverPhoneNumber)


        chatRef.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        val lastMessage= snapshot.children.firstOrNull()?.child("message")?.value as? String
                        val timestamp= snapshot.children.firstOrNull()?.child("timestamp")?.value as? String

                        // higher order funnction
                        onLastMessageFetched(lastMessage?: "No message", timestamp?: "--:--")

                    }else{
                        onLastMessageFetched("No message", "--:--")
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    onLastMessageFetched("No message", "--:--")
                }


            })



    }

      fun loadChatList(
          currentUserPhoneNumber: String,
          onChatListLoaded: (List<ChatListModel>) -> Unit


      ){

          val chatList = mutableListOf<ChatListModel>()
          val chatRef = FirebaseDatabase.getInstance().reference
              .child("chats")
              .child(currentUserPhoneNumber)


          chatRef.addListenerForSingleValueEvent(object : ValueEventListener{
              override fun onDataChange(snapshot: DataSnapshot) {

                  if(snapshot.exists()){
                      snapshot.children.forEach{ child ->

                          val phoneNumber =child.key?: return@forEach
                          val name= child.child("name").value as? String ?: "unknown"
                          val image = child.child("image").value as? String

                          val profileImageBitmap = image?.let {decodeBase64toBitmap(it)}

                          fetchLastMessageForChat(currentUserPhoneNumber, phoneNumber){ lastMessage, time ->

                              chatList.add(
                                  ChatListModel(
                                  name= name,
                                      // originally-  image= profileImageBitmap,
                                  image= profileImageBitmap as Int?,
                                  message = lastMessage,
                                  time= time

                              )

                              )

                              if (chatList.size == snapshot.childrenCount.toInt()){
                                  onChatListLoaded(chatList)
                              }

                          }

                      }
                  }else{
                      onChatListLoaded(emptyList())
                  }
              }

              override fun onCancelled(error: DatabaseError) {

                  onChatListLoaded(emptyList())

              }


          })
      }

    private fun decodeBase64toBitmap(base64Image: String):Bitmap?{
        return try {
            val decodeByte = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodeByte, 0, decodeByte.size)
        }catch (e: Exception){
            Log.e("BaseViewModel", "Error decoding base64: ${e.message}")
            null
        }
    }

    fun base64ToBitmap(base64String: String): Bitmap?{
        return try {
            val decodeByte = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            val inputStream: InputStream = ByteArrayInputStream(decodeByte)
            BitmapFactory.decodeStream(inputStream)
        }catch (e : Exception){
            Log.e("BaseViewModel", "Error converting base64 to bitmap: ${e.message}")
            null
        }
    }


}