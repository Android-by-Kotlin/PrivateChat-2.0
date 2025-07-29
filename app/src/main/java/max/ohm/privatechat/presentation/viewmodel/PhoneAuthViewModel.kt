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
                _authState.value= AuthState.CodeSent(verificationId = id, resendToken = token)

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
           .setTimeout(120L, TimeUnit.SECONDS) // Increased timeout to 2 minutes
           .setActivity(activity)
           .setCallbacks(option)
           .build()

       PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)

    }

    private fun signWithCredential(credential: PhoneAuthCredential, context: Context){
        _authState.value= AuthState.Loading
        Log.d("PhoneAuth", "Attempting to sign in with credential")

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    Log.d("PhoneAuth", "Sign-in successful")
                    val user= firebaseAuth.currentUser
                    val phoneAuthUser = PhoneAuthUser(
                        userId= user?.uid?: "",
                        phoneNumber = user?.phoneNumber?: ""
                    )

                    markUserAsSignedIn(context)
                    _authState.value= AuthState.Success(phoneAuthUser)
                    fetchUserProfile(user?.uid?: "")
                } else{
                    val errorMessage = when {
                        task.exception?.message?.contains("invalid", ignoreCase = true) == true -> 
                            "Invalid OTP. Please check and try again."
                        task.exception?.message?.contains("expired", ignoreCase = true) == true -> 
                            "OTP has expired. Please request a new one."
                        task.exception?.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your connection and try again."
                        else -> task.exception?.message ?: "Sign-in failed. Please try again."
                    }
                    Log.e("PhoneAuth", "Sign-in failed: $errorMessage")
                    _authState.value= AuthState.Error(errorMessage)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PhoneAuth", "Sign-in failure: ${exception.message}")
                _authState.value= AuthState.Error("Authentication failed: ${exception.message}")
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

        // Validate OTP format
        if(otp.length != 6 || !otp.all { it.isDigit() }) {
            Log.e("PhoneAuth", "Invalid OTP format: $otp")
            _authState.value= AuthState.Error("Please enter a valid 6-digit OTP")
            return
        }

        Log.d("PhoneAuth", "Verifying OTP: $otp with verificationId: ${currentAuthState.verificationId}")
        
        try {
            val credential= PhoneAuthProvider.getCredential(currentAuthState.verificationId, otp)
            signWithCredential(credential, context)
        } catch (e: Exception) {
            Log.e("PhoneAuth", "Error creating credential: ${e.message}")
            _authState.value= AuthState.Error("Failed to verify OTP: ${e.message}")
        }
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
        // Scale down the bitmap to reduce size
        val maxWidth = 300
        val maxHeight = 300
        
        val width = bitmap.width
        val height = bitmap.height
        
        val scaledBitmap = if (width > maxWidth || height > maxHeight) {
            val aspectRatio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            
            if (width > height) {
                newWidth = maxWidth
                newHeight = (maxWidth / aspectRatio).toInt()
            } else {
                newHeight = maxHeight
                newWidth = (maxHeight * aspectRatio).toInt()
            }
            
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress with 70% quality to reduce size
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun resetAuthState(){
        _authState.value= AuthState.Ideal
    }
    
    // Add resend OTP functionality
    fun resendOTP(phoneNumber: String, activity: Activity, token: PhoneAuthProvider.ForceResendingToken?) {
        Log.d("PhoneAuth", "Resending OTP to: $phoneNumber")
        _authState.value = AuthState.Loading
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, newToken: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, newToken)
                Log.d("PhoneAuth", "OTP resent successfully. New verification ID: $verificationId")
                _authState.value = AuthState.CodeSent(verificationId = verificationId)
            }
            
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signWithCredential(credential, activity)
            }
            
            override fun onVerificationFailed(exception: FirebaseException) {
                Log.e("PhoneAuth", "OTP resend failed: ${exception.message}")
                _authState.value = AuthState.Error("Failed to resend OTP: ${exception.message}")
            }
        }
        
        val options = if (token != null) {
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(120L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .setForceResendingToken(token)
                .build()
        } else {
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(120L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        }
        
        PhoneAuthProvider.verifyPhoneNumber(options)
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
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ): AuthState() // phone number send
    data class Success(val user: PhoneAuthUser): AuthState() // successfully authentication complete
    data class Error(val message:String): AuthState()  // any error issue

}
