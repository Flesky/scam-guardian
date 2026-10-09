package ph.scamguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ph.scamguardian.theme.ScamGuardianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Replace the launch theme before drawing Compose; the system owns the splash window.
        setTheme(R.style.Theme_ScamGuardian)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ScamGuardianTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation()
                }
            }
        }
    }
}
