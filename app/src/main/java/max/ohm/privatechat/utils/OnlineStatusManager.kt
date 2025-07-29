package max.ohm.privatechat.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineStatusManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statusRef: DatabaseReference? = null
    private var lastSeenRef: DatabaseReference? = null
    
    fun startOnlineTracking() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        
        scope.launch {
            try {
                // Reference to user's online status
                statusRef = firebaseDatabase.reference
                    .child("users")
                    .child(userId)
                    .child("online_status")
                
                lastSeenRef = firebaseDatabase.reference
                    .child("users")
                    .child(userId)
                    .child("last_seen")
                
                // Set online status
                statusRef?.setValue(true)
                
                // Set offline on disconnect
                statusRef?.onDisconnect()?.setValue(false)
                
                // Update last seen on disconnect
                lastSeenRef?.onDisconnect()?.setValue(ServerValue.TIMESTAMP)
                
            } catch (e: Exception) {
                android.util.Log.e("OnlineStatusManager", "Error setting online status: ${e.message}")
            }
        }
    }
    
    fun stopOnlineTracking() {
        scope.launch {
            try {
                statusRef?.setValue(false)
                lastSeenRef?.setValue(ServerValue.TIMESTAMP)
                statusRef?.onDisconnect()?.cancel()
                lastSeenRef?.onDisconnect()?.cancel()
            } catch (e: Exception) {
                android.util.Log.e("OnlineStatusManager", "Error setting offline status: ${e.message}")
            }
        }
    }
    
    fun updateLastSeen() {
        scope.launch {
            try {
                lastSeenRef?.setValue(ServerValue.TIMESTAMP)
            } catch (e: Exception) {
                android.util.Log.e("OnlineStatusManager", "Error updating last seen: ${e.message}")
            }
        }
    }
}
