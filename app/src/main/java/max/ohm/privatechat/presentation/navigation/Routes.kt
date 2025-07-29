package max.ohm.privatechat.presentation.navigation

import kotlinx.serialization.Serializable

sealed class Routes {


    // Routes - connect to nav to nav

    @Serializable
    data object SplashScreen : Routes()


    @Serializable
    data object WelcomeScreen : Routes()

    @Serializable
    data object UserRegistrationScreen : Routes()

    @Serializable
    data object HomeScreen : Routes()

    @Serializable
    data object UpdateScreen : Routes()

    @Serializable
    data object CommunitiesScreen: Routes()

    @Serializable
    data object CallScreen: Routes()

    @Serializable
    data object UserProfileSetScreen: Routes()

    @Serializable
    data object SettingScreen: Routes()

    @Serializable
    data object ChatScreen: Routes(){

        const val route = "chat_screen/{phoneNumber}"
        fun createRoute(phoneNumber: String)= "chat_screen/$phoneNumber"

    }
    
    @Serializable
    data object UserProfileScreen: Routes()

}
