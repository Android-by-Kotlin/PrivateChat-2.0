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

        const val route = "chat_screen/{phoneNumber}?profileImage={profileImage}&name={name}"
        fun createRoute(phoneNumber: String, profileImage: String? = null, name: String? = null): String {
            val encodedImage = profileImage?.let { 
                java.net.URLEncoder.encode(it, "UTF-8")
            } ?: ""
            val encodedName = name?.let {
                java.net.URLEncoder.encode(it, "UTF-8")
            } ?: "Contact"
            return "chat_screen/$phoneNumber?profileImage=$encodedImage&name=$encodedName"
        }

    }
    
    @Serializable
    data object UserProfileScreen: Routes()

}
