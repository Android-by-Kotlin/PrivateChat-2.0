package max.ohm.privatechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import max.ohm.privatechat.presentation.navigation.WhatsAppNavigationSystem
import max.ohm.privatechat.presentation.splashscreen.SplashScreen
import max.ohm.privatechat.ui.theme.PrivateChatTheme
import max.ohm.privatechat.utils.OnlineStatusManager
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var onlineStatusManager: OnlineStatusManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            PrivateChatTheme {
                WhatsAppNavigationSystem()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        onlineStatusManager.startOnlineTracking()
    }
    
    override fun onPause() {
        super.onPause()
        onlineStatusManager.updateLastSeen()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        onlineStatusManager.stopOnlineTracking()
    }
}
