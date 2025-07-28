package max.ohm.privatechat.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import max.ohm.privatechat.models.PhoneAuthUser
import max.ohm.privatechat.presentation.chat_box.ChatListModel
import javax.inject.Inject

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val database: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    fun searchUserByPhoneNumber(phoneNumber: String, callback: (ChatListModel?) -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e("UserSearchViewModel", "User is not authenticated")
            callback(null)
            return
        }

        // Clean the phone number (remove spaces, dashes, etc)
        val cleanPhoneNumber = phoneNumber.trim().replace(" ", "").replace("-", "")
        
        Log.d("UserSearchViewModel", "Searching for phone number: $cleanPhoneNumber")

        val usersRef = database.getReference("users")
        
        // First, let's check all users to debug
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserSearchViewModel", "Total users in database: ${snapshot.childrenCount}")
                
                var userFound = false
                
                for (userSnapshot in snapshot.children) {
                    val phoneAuthUser = userSnapshot.getValue(PhoneAuthUser::class.java)
                    val userId = userSnapshot.key
                    
                    Log.d("UserSearchViewModel", "Checking user: ${phoneAuthUser?.phoneNumber} against $cleanPhoneNumber")
                    
                    if (phoneAuthUser != null && phoneAuthUser.phoneNumber == cleanPhoneNumber) {
                        // Don't return the current user
                        if (userId != currentUser.uid) {
                            userFound = true
                            val chatListModel = ChatListModel(
                                name = phoneAuthUser.name,
                                phoneNumber = phoneAuthUser.phoneNumber,
                                userId = userId,
                                profileImage = phoneAuthUser.profileImage,
                                image = null,
                                time = null,
                                message = null
                            )
                            Log.d("UserSearchViewModel", "User found: ${phoneAuthUser.name}")
                            callback(chatListModel)
                            break
                        }
                    }
                }
                
                if (!userFound) {
                    Log.d("UserSearchViewModel", "No user found with phone number: $cleanPhoneNumber")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserSearchViewModel", "Error searching user: ${error.message}, Details: ${error.details}")
                callback(null)
            }
        })
    }
}
