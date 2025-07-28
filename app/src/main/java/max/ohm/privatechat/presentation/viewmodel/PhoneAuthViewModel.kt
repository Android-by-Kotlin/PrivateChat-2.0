package max.ohm.privatechat.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.browser.trusted.Token
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import max.ohm.privatechat.models.PhoneAuthUser
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
// import kotlin.io.encoding.Base64
import android.util.Base64
import com.google.firebase.database.DatabaseReference

// Hilt make object of class
@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private  val firebaseAuth: FirebaseAuth,
    private val database: FirebaseDatabase

): ViewModel() {

      // authentication state private (ideal - nothing)
    private val _authState= MutableStateFlow<AuthState>(AuthState.Ideal)
    // authentication state public
    val authState= _authState.asStateFlow()

  // create data base reference make nodes
    // save user data
    private val userRef = database.reference.child("users")

   // send otp to user
    fun sendVerificationCode(phoneNumber:String, activity:Activity){
        // ideal to loading state
     _authState.value= AuthState.Loading

        val option= object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(id, token)
                // log detect codesend or not
                Log.d("PhoneAuth", "onCodeSent triggered. verification ID: $id")
                _authState.value= AuthState.CodeSent(verificationId = id)

            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                   signWithCredential(credential, context= activity)

            }

            override fun onVerificationFailed(exception: FirebaseException) {
               Log.e("PhoneAuth", "Verification failed : ${exception.message}")
                _authState.value= AuthState.Error(exception.message ?: "Verification failed")

            }

        }


            // sent otp
       val phoneAuthOptions= PhoneAuthOptions.newBuilder(firebaseAuth)
           .setPhoneNumber(phoneNumber)
           .setTimeout(60L, TimeUnit.SECONDS)
           .setActivity(activity)
           .setCallbacks(option)
           .build()

       PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

    }

    private fun signWithCredential(credential: PhoneAuthCredential, context: Context){
        _authState.value= AuthState.Loading

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener{
            task ->
            if(task.isSuccessful){

                val user= firebaseAuth.currentUser
                val phoneAuthUser = PhoneAuthUser(
                    userId= user?.uid?: "",
                    phoneNumber = user?.phoneNumber?: ""
                )

                markUserAsSignedIn(context)
                _authState.value= AuthState.Success(phoneAuthUser)
                fetchUserProfile(user?.uid?: "")
             } else{

                 _authState.value= AuthState.Error(task.exception?.message?: "Sign-in failed")
            }
        }
    }

    // any data used by Context
   private fun markUserAsSignedIn(context: Context){

       // data saved in app_perfs in private
       val sharedPreference = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreference.edit().putBoolean("isSignedIn", true).apply()
   }

    private fun fetchUserProfile(userId: String){
        val userRef= userRef.child(userId)
        userRef.get().addOnSuccessListener{
            snapshot ->
            if(snapshot.exists()){
                val userProfile= snapshot.getValue(PhoneAuthUser::class.java)
                if(userProfile != null){
                    _authState.value= AuthState.Success(userProfile)

                }
            }
        }.addOnFailureListener{
            _authState.value= AuthState.Error("Failed to fetch user profile")

        }

    }

    fun verifyCode(otp : String, context: Context ){
        val currentAuthState= _authState.value
        if(currentAuthState !is AuthState.CodeSent || currentAuthState.verificationId.isEmpty()){
            Log.e("PhoneAuth", "Attempting to verify OTP without a valid verification ID")
            _authState.value= AuthState.Error("Verification not started or invalid ID")

            return

        }

        // context should be context //////////////
        val credential= PhoneAuthProvider.getCredential(currentAuthState.verificationId, otp)
        signWithCredential(credential, context)
    }

    // Ready from here

    // Bitmap - change image to string so that use can store image in firebase free

    fun savedUserProfile(userId: String, name:String, status:String, profileImage:Bitmap?){
 // photo special to change and store in database
      val database = FirebaseDatabase.getInstance().reference   // (child used for this only)

        val encodedImage= profileImage?.let{convertBitmapToBase64(it)}
        val userProfile= PhoneAuthUser(
            userId = userId,
            name = name,
            status =  status,
            phoneNumber =  Firebase.auth.currentUser?.phoneNumber?: "",
            profileImage= encodedImage,
        )
      database.child("users").child(userId).setValue(userProfile)

    }


    // JPG image convert to firebase and store in database
    private fun convertBitmapToBase64(bitmap: Bitmap): String{
        val btyeArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, btyeArrayOutputStream)
        // image data convert to byteArray(image is now raw data)
        val byteArray= btyeArrayOutputStream.toByteArray()
        // easy way to change binarydata to string
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun resetAuthState(){
        _authState.value= AuthState.Ideal
    }

    fun signOut(activity: Activity){
        firebaseAuth.signOut()
        val sharePreference = activity.getSharedPreferences("app_prefs", Activity.MODE_PRIVATE)
        sharePreference.edit().putBoolean("isSignedIn", false).apply()
    }
    
    // Public method to fetch user profile
    fun fetchUserProfile(userId: String, onResult: (PhoneAuthUser?) -> Unit) {
        val userRef = userRef.child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userProfile = snapshot.getValue(PhoneAuthUser::class.java)
                onResult(userProfile)
            } else {
                onResult(null)
            }
        }.addOnFailureListener {
            onResult(null)
        }
    }


}


//event are fixed thats why we use sealed class

sealed class AuthState{

    object Ideal: AuthState()   // do nothing
    object Loading: AuthState()  // data comes or goes
    data class CodeSent(val verificationId: String): AuthState() // phone number send
    data class Success(val user: PhoneAuthUser): AuthState() // successfully authentication complete
    data class Error(val message:String): AuthState()  // any error issue

}