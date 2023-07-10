package com.muhammadali.sharemedia

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.muhammadali.sharemedia.ui.theme.ShareMediaTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    companion object {
        val PERMISSION_NOT_GRANTED = Exception("Permission is not granted ")
        val EXTERNAL_STORAGE_UNMOUNTED = Exception("External storage is unmounted")
    }

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

                    val storagePermissionRegister = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = {
                            Log.d(Tag, "permission granted")
                        })


                    MainScreen(
                        uri = uri.value,
                        onImageSave = { uri ->
                            if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                storagePermissionRegister.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                            saveImageToExStorage(uri) { fileName ->
                                Toast.makeText(
                                    this,
                                    "saved file: $fileName",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        },
                        onNoImage = {
                            Toast.makeText(this, "No image to save", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(this, permission)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun canWriteToExStorage(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    private fun saveImageToExStorage(uri: Uri, onFinishWriting: (String) -> Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            throw PERMISSION_NOT_GRANTED

        if (!canWriteToExStorage())
            throw EXTERNAL_STORAGE_UNMOUNTED

        writeToExStorageFromUri(uri, onFinishWriting)


    }

    private fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String {
        var fileName = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName = it.getString(it.getColumnIndexOrThrow("_display_name"))
            }
        }
        return fileName
    }

    private fun getFileExtensionFromUri(contentResolver: ContentResolver, uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.substringAfterLast('/') ?: ""
    }


    private fun writeToExStorageFromUri(uri: Uri, onFinishWriting: (String) -> Unit) {
        val contentResolver = this.contentResolver
        val fileName = getFileNameFromUri(contentResolver, uri)
        val fileExtension = getFileExtensionFromUri(contentResolver, uri)
        val dir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(dir, "$fileName.$fileExtension")


        try {
            val inputSteam = contentResolver.openInputStream(uri)
            val outputSteam = FileOutputStream(file)
            val bufferSize = 4 * 1024
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int

            if (inputSteam != null)
                while (inputSteam.read(buffer).also { bytesRead = it } >= 0)
                    outputSteam.write(buffer, 0, bytesRead)

            inputSteam?.close()
            outputSteam.close()

            onFinishWriting("$fileName.$fileExtension")
        }
        catch (e: Exception) {
            Log.e(Tag, "Exc-> writeToExStorageFromUri| message: ${e.message}" +
                    "| cause: ${e.cause}")
            e.printStackTrace()
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
fun MainScreen(
    uri: Uri?,
    modifier: Modifier = Modifier,
    onImageSave: (Uri) -> Unit = {},
    onNoImage: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DrawImage(uri = uri)
        
        Button(onClick = {

            if (uri != null)
                onImageSave(uri)
            else
                onNoImage()
        }) {
            Text(
                text = "Save To Private Storage",
                fontSize = 15.sp
            )
        }
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