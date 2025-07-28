package max.ohm.privatechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import max.ohm.privatechat.presentation.callscreen.CallItemDesign
import max.ohm.privatechat.presentation.callscreen.CallScreen
import max.ohm.privatechat.presentation.chatscreen.ChatScreen
import max.ohm.privatechat.presentation.communitiesscreen.CommunitiesScreen
import max.ohm.privatechat.presentation.homescreen.HomeScreen
import max.ohm.privatechat.presentation.profile.UserProfileSetScreen
import max.ohm.privatechat.presentation.splashscreen.SplashScreen
import max.ohm.privatechat.presentation.updatescreen.UpdateScreen
import max.ohm.privatechat.presentation.userregistrationscreen.AuthScreen
import max.ohm.privatechat.presentation.viewmodel.BaseViewModel
import max.ohm.privatechat.presentation.viewmodel.PhoneAuthViewModel
import max.ohm.privatechat.presentation.welcomescreen.WelcomeScreen


@Composable

fun WhatsAppNavigationSystem() {


    val navController= rememberNavController()

    NavHost(startDestination = Routes.SplashScreen, navController= navController){

        // navGraph- which routes screen go


        composable<Routes.SplashScreen>{
            SplashScreen(navController)
        }

        composable<Routes.WelcomeScreen>{
            WelcomeScreen(navController)
        }

        composable<Routes.UserRegistrationScreen>{
            AuthScreen(navController)
        }
        composable<Routes.HomeScreen>{
            val baseViewModel: BaseViewModel = hiltViewModel()
            HomeScreen(navController, baseViewModel)
        }
        composable<Routes.UpdateScreen>{
            UpdateScreen(navController)
        }
        composable<Routes.CommunitiesScreen>{
            CommunitiesScreen(navController)
        }
        composable<Routes.CallScreen>{
            CallScreen(navController)
        }

        composable<Routes.UserProfileSetScreen>{

            UserProfileSetScreen(navHostController = navController)
        }
        
        composable<Routes.SettingScreen>{
            max.ohm.privatechat.presentation.settingscreen.SettingScreen(navHostController = navController)
        }
        
        composable(Routes.ChatScreen.route) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val baseViewModel: BaseViewModel = hiltViewModel()
            val phoneAuthViewModel: PhoneAuthViewModel = hiltViewModel()
            
            // Fetch user details for the phone number
            var receiverName by remember { mutableStateOf("Loading...") }
            var receiverProfileImage by remember { mutableStateOf<String?>(null) }
            
            LaunchedEffect(phoneNumber) {
                baseViewModel.searchUserByPhoneNumber(phoneNumber) { user ->
                    if (user != null) {
                        receiverName = user.name ?: "Contact"
                        receiverProfileImage = user.profileImage
                    }
                }
            }
            
            ChatScreen(
                navController = navController,
                receiverPhoneNumber = phoneNumber,
                receiverName = receiverName,
                receiverProfileImage = receiverProfileImage,
                baseViewModel = baseViewModel
            )
        }

    }
}
