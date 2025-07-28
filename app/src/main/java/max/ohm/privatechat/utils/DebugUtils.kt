package max.ohm.privatechat.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import max.ohm.privatechat.models.PhoneAuthUser

object DebugUtils {
    fun checkAllUsersInDatabase() {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        
        Log.d("DebugUtils", "Checking all users in database...")
        
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DebugUtils", "Total users in database: ${snapshot.childrenCount}")
                
                if (snapshot.childrenCount == 0L) {
                    Log.d("DebugUtils", "No users found in database!")
                    return
                }
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key
                    val user = userSnapshot.getValue(PhoneAuthUser::class.java)
                    
                    Log.d("DebugUtils", "User ID: $userId")
                    Log.d("DebugUtils", "  Name: ${user?.name}")
                    Log.d("DebugUtils", "  Phone: ${user?.phoneNumber}")
                    Log.d("DebugUtils", "  Status: ${user?.status}")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DebugUtils", "Error checking database: ${error.message}")
            }
        })
    }
}
