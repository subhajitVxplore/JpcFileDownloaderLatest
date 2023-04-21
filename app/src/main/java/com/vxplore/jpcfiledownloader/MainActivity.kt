package com.vxplore.jpcfiledownloader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.vxplore.jpcfiledownloader.ui.theme.JpcFileDownloaderTheme

import androidx.work.*

class MainActivity : ComponentActivity() {

    private lateinit var requestMultiplePermission: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMultiplePermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            var isGranted = false
            it.forEach { s, b ->
                isGranted = b
            }

            if (!isGranted) {
                Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            val context = LocalContext.current
            JpcFileDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    requestMultiplePermission.launch(
                        arrayOf(

                           android.Manifest.permission.READ_EXTERNAL_STORAGE,
                           android.Manifest.permission.WRITE_EXTERNAL_STORAGE

//                            Manifest.permission.READ_EXTERNAL_STORAGE,
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                    ShowItemFileLayout(context)
                }
            }
        }
    }
//////////////////////////methods//////////////////////////////////////////////////////////////////////////////////////
    fun startDownloadingFile(
    file: MyFileModel,
    success:(String) -> Unit,
    failed:(String) -> Unit,
    running:() -> Unit
    ) {
        val data = Data.Builder()
        val workManager = WorkManager.getInstance(this)

        data.apply {
            putString(FileDownloadWorker.FileParams.KEY_FILE_NAME, file.name)
            putString(FileDownloadWorker.FileParams.KEY_FILE_URL, file.url)
            putString(FileDownloadWorker.FileParams.KEY_FILE_TYPE, file.type)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .build()

        val fileDownloadWorker = OneTimeWorkRequestBuilder<FileDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(data.build())
            .build()


        workManager.enqueueUniqueWork(
            "oneFileDownloadWork_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            fileDownloadWorker
        )

        workManager.getWorkInfoByIdLiveData(fileDownloadWorker.id)
            .observe(this){ info->
                info?.let {
                    when (it.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            success(it.outputData.getString(FileDownloadWorker.FileParams.KEY_FILE_URI) ?: "")
                        }
                        WorkInfo.State.FAILED -> {
                            failed("Downloading failed!")
                        }
                        WorkInfo.State.RUNNING -> {
                            running()
                        }
                        else -> {
                            failed("Something went wrong")
                        }
                    }
                }
            }
    }
    //////////////////////////composable///////////////////
    @Composable
    fun ShowItemFileLayout(context: Context) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val data = remember {
                mutableStateOf(
                    MyFileModel(
                        id = "10",
                        name = "Pdf File 10 MB",
                        type = "PDF",
                        url = "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-download-10-mb.pdf",
                        downloadedUri = null
                    )
                )
            }

            ItemFile(
                file = data.value,
                startDownload = {
                    startDownloadingFile(
                        file = data.value,
                        success = {
                            data.value = data.value.copy().apply {
                                isDownloading = false
                                downloadedUri = it
                            }
                        },
                        failed = {
                            data.value = data.value.copy().apply {
                                isDownloading = false
                                downloadedUri = null
                            }
                        },
                        running = {
                            data.value = data.value.copy().apply {
                                isDownloading = true
                            }
                        }
                    )
                },

                openFile = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(it.downloadedUri?.toUri(), "application/pdf")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        ContextCompat.startActivity(context, intent, null)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "Can't open Pdf", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}//end of class






