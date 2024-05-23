package com.helge.safstorageavailabilitytest

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helge.safstorageavailabilitytest.ui.theme.SafStorageAvailabilityTestTheme
import java.io.File

fun Intent.canBeHandled(packageManager: PackageManager): Boolean {
    return resolveActivity(packageManager) != null
}

private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
    val manager = context.packageManager
    val info = manager.queryIntentActivities(intent, 0)
    return info.size > 0
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSaf()
        }
    }
}

@Composable
private fun AppSaf() {
    SafStorageAvailabilityTestTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val context = LocalContext.current

                var selectedFolderUri by rememberSaveable {
                    mutableStateOf<String?>(null)
                }

                var error by rememberSaveable {
                    mutableStateOf<String?>(null)
                }

                val safResultLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { activityResult ->
                    val treeUri = activityResult.data?.data
                    selectedFolderUri = treeUri?.toString() ?: "failed"
                }

                Text(
                    text = "${Build.VERSION.SDK_INT} API, ${Build.BRAND}, ${Build.MODEL}, ${Build.MANUFACTURER}, ${Build.DEVICE}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    fontSize = 12.sp
                )

                Text(
                    text = "SAF: ${isSafSupported(context)}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    fontSize = 12.sp
                )

                Text(
                    text = getStorageInfo(context).joinToString("\n"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onClick = {
                        selectFolderSaf(context, safResultLauncher) {
                            error = it
                        }
                    }
                ) {
                    Text(
                        text = "Select folder using SAF storage",
                        fontSize = 12.sp
                    )
                }

                error?.let {
                    Text(
                        text = "Device doesn't support this storage API.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "error: $it",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        fontSize = 12.sp
                    )
                }

                selectedFolderUri?.let {
                    Text(
                        text = "Selected folder:",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        fontSize = 12.sp
                    )
                    Text(
                        text = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppSaf()
}


private fun getStorageInfo(context: Context): List<String> {
    val storageManager =
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val storages = storageManager.storageVolumes
        .mapIndexedNotNull { index, volume ->
            buildString {
                appendLine("#${index + 1} Storage")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    appendLine("mediaStoreVolumeName: ${volume.mediaStoreVolumeName}")
                }
                appendLine("uuid: ${volume.uuid}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appendLine("storageUuid: ${volume.storageUuid}")
                }
                appendLine("path: ${getVolumeFileRoot(volume)}")
                appendLine("desc: ${volume.getDescription(context)}")
                appendLine("state: ${volume.state}")
                appendLine("isDir: ${getVolumeFileRoot(volume)?.isDirectory}")
                appendLine("isRemovable: ${volume.isRemovable}")
                appendLine("isEmulated: ${volume.isEmulated}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appendLine("isPrimary: ${volume.isPrimary}")
                    appendLine("allocatedBytes: ${
                        volume.storageUuid?.let {
                            storageManager.getAllocatableBytes(it)
                        }
                    }")
                }
            }
        }
    return storages
}

private fun getVolumeFileRoot(storageVolume: StorageVolume): File? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        return storageVolume.directory
    try {
        val storageVolumeClazz = StorageVolume::class.java
        val getPath = storageVolumeClazz.getMethod("getPath")
        return File(getPath.invoke(storageVolume) as String)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return null
}


private fun isSafSupported(context: Context): Boolean {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).canBeHandled(context.packageManager)
            || isIntentAvailable(context, Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
}

private fun selectFolderSaf(
    context: Context,
    activityResultLauncher: ActivityResultLauncher<Intent>,
    onError: (String) -> Unit
) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val storageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
            .takeIf { it.canBeHandled(context.packageManager) }
            ?: Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    } else {
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    }

    with(intent) {
        putExtra("android.content.extra.SHOW_ADVANCED", true)
        putExtra("android.content.extra.FANCY", true)
        putExtra("android.content.extra.SHOW_FILESIZE", true)
    }

    try {
        activityResultLauncher.launch(intent)
    } catch (e: Throwable) {
        try {
            activityResultLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            onError("${e.message ?: ""}, $e")
        }
        e.printStackTrace()
    }
}