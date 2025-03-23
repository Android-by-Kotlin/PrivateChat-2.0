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
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class BaseViewModel: ViewModel() {


    fun seachUserByPhoneNumber(phoneNumber: String, callback : (ChatListModel?) -> Unit) {
          val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser == null){

            Log.e("BaseViewModel", "User is not authenticated")
            callback(null)
            return
        }

        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.orderByChild("phoneNumber").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        val user = snapshot.children.first().getValue(ChatListModel::class.java)
                        callback(user)
                    } else{
                        callback(null)

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BaseViewModel", "Error fething User: ${error.message}, Details : ${error.details}")
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

        if(currentUserId != null){

            val newChatRef= FirebaseDatabase.getInstance().getReference("chats").push()
            val chatWithUser= newChat.copy(currentUserId)
            newChatRef.setValue(chatWithUser).addOnSuccessListener{
                Log.d("BaseViewModel", "Chat added successfully")
            }.addOnFailureListener{
                exception ->

                Log.e("BaseViewModel", "Failed to add chat: ${exception.message}")

            }
        }else{
            Log.e("BaseViewModel", "User not authenticated")

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
        val messageRef = databaseReference.child("message")
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
           .child("message")
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

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64toBitmap(base64Image: String):Bitmap?{

        return try {
            val decodeByte = Base64.decode(base64Image,android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodeByte, 0, decodeByte.size)
        }catch (e: IOException){
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun base64ToBitmap(base64String: String): Bitmap?{

        return try {
            val decodeByte = Base64.decode(base64String,android.util.Base64.DEFAULT)
            val inputStream: InputStream = ByteArrayInputStream(decodeByte)
            BitmapFactory.decodeStream(inputStream)

        }catch (e : IOException){
            null
        }
    }


}