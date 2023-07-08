package com.muhammadali.sharemedia

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.muhammadali.sharemedia.ui.theme.ShareMediaTheme

class MainActivity : ComponentActivity() {

    private val Tag = "MainActivityT"
    private var uri: MutableState<Uri?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onIntentReceived(intent = intent)

        setContent {
            ShareMediaTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        
                        DrawImage(uri.value)
                    }
                }
            }
        }
    }

    private fun Intent.isTheWaitedIntent(action: String, typePrefix: String): Boolean {
        return if (this.type != null) {
            this.action.equals(action) && type!!.startsWith(typePrefix)
        } else
            false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null)
            onIntentReceived(intent)
    }

    private fun onIntentReceived(intent: Intent) {
        val action = Intent.ACTION_SEND

        if (intent.isTheWaitedIntent(action, "image/"))
        {
            uri.value = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
            else
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)

            Log.d(Tag, "image uri: $uri")
        }
    }

}

@Composable
fun MainScreen(uri: Uri?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DrawImage(uri = uri)
    }
}

@Composable
fun DrawImage(uri: Uri?) {
    
    val painter = if (uri !=null)
        rememberAsyncImagePainter(uri)
    else
        painterResource(id = R.drawable.ic_launcher_foreground)
    
    Image(
        modifier= Modifier.fillMaxWidth(),
        painter = painter,
        contentDescription = "Image"
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShareMediaTheme {
       MainScreen(uri = null)
    }
}