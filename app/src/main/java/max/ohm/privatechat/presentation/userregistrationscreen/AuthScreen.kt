package max.ohm.privatechat.presentation.userregistrationscreen

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import max.ohm.privatechat.R
import max.ohm.privatechat.presentation.navigation.Routes
import max.ohm.privatechat.presentation.viewmodel.AuthState
import max.ohm.privatechat.presentation.viewmodel.PhoneAuthViewModel

@Composable


fun AuthScreen(navController: NavHostController, phoneAuthViewModel: PhoneAuthViewModel= hiltViewModel()) {

   val authState by phoneAuthViewModel.authState.collectAsState()
    val context = LocalContext.current

    // originally was ( val activity = LocalContext.current as Activity)
    val activity = context as Activity





   var otp by remember{mutableStateOf("")}

    // type string and value null
    var verificationId by remember { mutableStateOf<String?>(null) }



    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedCountry by remember{
        mutableStateOf("India")
    }

    var countryCode by remember{
        mutableStateOf("+91")
    }
    
    // Country code mapping
    val countryCodeMap = mapOf(
        "India" to "+91",
        "USA" to "+1",
        "China" to "+86",
        "Canada" to "+1",
        "UK" to "+44",
        "Australia" to "+61",
        "Germany" to "+49",
        "France" to "+33",
        "Japan" to "+81",
        "Brazil" to "+55"
    )

    var phone by remember {
        mutableStateOf("")
    }



        Column(
            modifier = Modifier.fillMaxSize()
                .background(color= Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {
            Text(
                text = "Enter your phone number", fontSize = 20.sp,
                color = colorResource(id = R.color.dark_green),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {

                Text(text = "whatsApp will need to verify your phone number")

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = "what's", color = colorResource(id = R.color.dark_green))
            }

            Text(text = "my number?", color = colorResource(id = R.color.dark_green))

            Spacer(modifier= Modifier.height(16.dp))

            TextButton(onClick = {expanded= true}, modifier = Modifier.fillMaxWidth()) {
            
                Box(modifier= Modifier.width(230.dp)){

                    Text(text= selectedCountry,
                        modifier= Modifier.align(Alignment.Center),
                        fontSize = 16.sp, color= Color.Black
                    )

                    Icon(imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier= Modifier.align(Alignment.CenterEnd),
                        tint = colorResource(id= R.color.light_green)
                        )


                }
            }

            HorizontalDivider(
                modifier= Modifier.padding(horizontal = 66.dp), thickness = 2.dp, color= colorResource(
                    id= R.color.light_green
                )
            )

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded= false}, modifier= Modifier.fillMaxWidth()) {

                countryCodeMap.keys.forEach{country->

                DropdownMenuItem(text= { Text(text= "$country (${countryCodeMap[country]})")},
                    onClick =  { 
                        selectedCountry= country
                        countryCode = countryCodeMap[country] ?: "+91"
                        expanded= false
                    })
                }
            }



           when(authState){
               is AuthState.Ideal, is AuthState.Loading, is AuthState.CodeSent ->{

                   if(authState is AuthState.CodeSent){
                       verificationId = (authState as AuthState.CodeSent).verificationId
                   }

                   if( verificationId == null){
                       Spacer( Modifier.height(16.dp))

                       Row(verticalAlignment = Alignment.CenterVertically,
                           modifier= Modifier.fillMaxWidth()
                       ){

                           TextField(
                               value= countryCode,
                               onValueChange = {countryCode= it},
                               modifier= Modifier.width(70.dp),
                               singleLine = true,
                               colors= TextFieldDefaults.colors(
                                   unfocusedContainerColor = Color.Transparent,
                                   focusedContainerColor = Color.Transparent,
                                   focusedIndicatorColor = colorResource(R.color.light_green)
                               )
                           )

                           Spacer(modifier= Modifier.width(8.dp))

                           TextField(
                               value= phone,
                               onValueChange = {phone= it},
                               placeholder = { Text("Phone Number")},
                               singleLine = true,
                               colors= TextFieldDefaults.colors(
                                   unfocusedContainerColor = Color.Transparent,
                                   focusedContainerColor = Color.Transparent,
                                   focusedIndicatorColor = Color.Transparent

                               )

                           )

                       }

                       Spacer(modifier = Modifier.height(16.dp))

                       Button(onClick = {

                           if(phone.isNotEmpty() && phone.length >= 7){
                               // Remove any spaces or special characters from phone number
                               val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
                               val fullPhoneNumber = "$countryCode$cleanPhone"
                               
                               // Basic validation
                               if (cleanPhone.length >= 7 && cleanPhone.length <= 15) {
                                   phoneAuthViewModel.sendVerificationCode(fullPhoneNumber, activity)
                               } else {
                                   Toast.makeText(context, "Please enter a valid phone number (7-15 digits)", Toast.LENGTH_LONG).show()
                               }

                           }else{
                               Toast.makeText(context, "Please enter a valid Phone Number", Toast.LENGTH_SHORT).show()
                           }



                       },
                           shape= RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(
                               colorResource(R.color.light_green)
                           )

                           ){

                           Text("Send OTP")
                       }

                       if (authState is AuthState.Loading){
                           Spacer(modifier= Modifier.height(16.dp))
                           CircularProgressIndicator()
                       }
                   }else{
                       // otp input UI

                       Spacer(modifier= Modifier.height(40.dp))

                       Text(

                           "Enter OTP",
                           fontSize = 20.sp,
                           fontWeight = FontWeight.Bold,
                           color= colorResource(R.color.dark_green)

                       )
                       Spacer(modifier= Modifier.height(8.dp))

                       TextField(

                           value= otp,
                           onValueChange = { otp = it},
                           placeholder = { Text("OTP")},
                           modifier= Modifier.fillMaxWidth(),
                           singleLine = true,
                           colors = TextFieldDefaults.colors(
                               unfocusedContainerColor = Color.Transparent,
                               focusedContainerColor = Color.Transparent,
                               focusedIndicatorColor = Color.Transparent


                           )

                       )

                       Spacer(modifier= Modifier.height(32.dp))

                       Button(onClick = {
                           if(otp.isNotEmpty() && verificationId!= null){

                               phoneAuthViewModel.verifyCode(otp, context)
                           }else{
                               Toast.makeText(context, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                           }
                       },
                           shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(
                               colorResource(R.color.dark_green)
                           )

                           ){
                           Text("verify OTP")


                       }
                       if(authState is AuthState.Loading){
                           Spacer(modifier= Modifier.height(16.dp))
                           CircularProgressIndicator()
                       }

                   }
               }

               is AuthState.Success ->{

                   Log.d("PhoneAuth", "LoginSuccessful")
                   phoneAuthViewModel.resetAuthState()
                   navController.navigate(Routes.UserProfileSetScreen){
                       popUpTo<Routes.UserRegistrationScreen>{
                           inclusive= true
                       }
                   }

               }

               is AuthState.Error ->{

                   Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
               }

           }
        }



}