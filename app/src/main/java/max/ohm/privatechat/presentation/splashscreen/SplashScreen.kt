package max.ohm.privatechat.presentation.splashscreen

import android.window.SplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import max.ohm.privatechat.R  // should be manually import
import max.ohm.privatechat.presentation.navigation.Routes

@Composable


fun SplashScreen(navHostController: NavHostController) {


    // one time run (composable- run so many times)
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        delay(1000)
        
        // Check if user is already signed in
        val sharedPreferences = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        
        if (isSignedIn && currentUser != null) {
            // User is already authenticated, go directly to HomeScreen
            navHostController.navigate(Routes.HomeScreen){
                popUpTo<Routes.SplashScreen>{ inclusive=true }
            }
        } else {
            // User is not authenticated, go to WelcomeScreen
            navHostController.navigate(Routes.WelcomeScreen){
                popUpTo<Routes.SplashScreen>{ inclusive=true }
            }
        }
    }



    Box (modifier= Modifier.fillMaxSize()){
        Image(painter= painterResource(id = R.drawable.whatsapp_icon),
            contentDescription = null,
            modifier= Modifier.size(80.dp)
                .align(Alignment.Center)

        )

        Column(modifier= Modifier.align(Alignment.BottomCenter).padding(bottom = 19.dp),  horizontalAlignment = Alignment.CenterHorizontally)
        {
            Text(text = "From", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row {
                Icon(painter = painterResource(id = R.drawable.meta), contentDescription = null,
                    modifier= Modifier.size(24.dp),
                    tint = colorResource(id = R.color.light_green)
                )
                Spacer(modifier = Modifier.width(3.dp))

                Text(text = "Meta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    
}