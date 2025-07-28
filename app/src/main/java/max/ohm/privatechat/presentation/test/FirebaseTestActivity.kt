package max.ohm.privatechat.presentation.test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import max.ohm.privatechat.models.PhoneAuthUser

class FirebaseTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseTestScreen()
        }
    }
}

@Composable
fun FirebaseTestScreen() {
    var users by remember { mutableStateOf<List<PhoneAuthUser>>(emptyList()) }
    var currentUser by remember { mutableStateOf<String>("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        // Get current user
        val auth = FirebaseAuth.getInstance()
        currentUser = "Current User: ${auth.currentUser?.phoneNumber ?: "Not logged in"} (${auth.currentUser?.uid ?: "No UID"})"
        
        // Fetch all users
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<PhoneAuthUser>()
                
                Log.d("FirebaseTest", "Total users: ${snapshot.childrenCount}")
                
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(PhoneAuthUser::class.java)
                    if (user != null) {
                        userList.add(user)
                        Log.d("FirebaseTest", "User: ${user.name}, Phone: ${user.phoneNumber}, UID: ${user.userId}")
                    }
                }
                
                users = userList
                loading = false
            }
            
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseTest", "Error: ${databaseError.message}")
                error = databaseError.message
                loading = false
            }
        })
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Firebase Test",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = currentUser)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(text = "Total users in database: ${users.size}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(users) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("Name: ${user.name}")
                            Text("Phone: ${user.phoneNumber}")
                            Text("Status: ${user.status}")
                            Text("UID: ${user.userId}")
                        }
                    }
                }
            }
        }
    }
}
