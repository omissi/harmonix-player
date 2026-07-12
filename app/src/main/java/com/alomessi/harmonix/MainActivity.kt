package com.alomessi.harmonix

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.alomessi.harmonix.ui.HarmonixApp
import com.alomessi.harmonix.ui.HarmonixViewModel
import com.alomessi.harmonix.widget.MusicWidgetProvider

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<HarmonixViewModel>()
    private var pendingArtworkTrackId: Long? = null
    private var pendingLyricsTrackId: Long? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onPermissionsChanged()
        if (it) requestNotificationPermissionIfNeeded()
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(viewModel.exportBackup())
                }
            }.onSuccess {
                toast("تم حفظ النسخة الاحتياطية", "Backup saved")
            }.onFailure {
                toast("تعذر حفظ النسخة الاحتياطية", "Could not save backup")
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val success = runCatching {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                viewModel.importBackup(text)
            }.getOrDefault(false)
            if (success) toast("تمت استعادة النسخة الاحتياطية", "Backup restored")
            else toast("ملف النسخة الاحتياطية غير صالح", "Invalid backup")
        }
    }

    private val artworkPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val trackId = pendingArtworkTrackId
        pendingArtworkTrackId = null
        if (uri != null && trackId != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.setCustomArtwork(trackId, uri.toString())
        }
    }

    private val lyricsPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val trackId = pendingLyricsTrackId
        pendingLyricsTrackId = null
        if (uri != null && trackId != null) {
            val text = runCatching {
                contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")
            if (text.isNotBlank()) viewModel.saveLyrics(trackId, text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HarmonixApp(
                viewModel = viewModel,
                onRequestAudioPermission = ::requestAudioPermission,
                onExportBackup = { exportBackupLauncher.launch("harmonix-backup.json") },
                onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                onPickArtwork = { track ->
                    pendingArtworkTrackId = track.id
                    artworkPickerLauncher.launch(arrayOf("image/*"))
                },
                onImportLyrics = { track ->
                    pendingLyricsTrackId = track.id
                    lyricsPickerLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                },
                onAddWidget = ::requestMusicWidget,
            )
        }
        if (viewModel.permissionGranted.value) requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onPermissionsChanged()
        viewModel.refreshLibrary()
    }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else Manifest.permission.READ_EXTERNAL_STORAGE
        audioPermissionLauncher.launch(permission)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestMusicWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = AppWidgetManager.getInstance(this)
            val provider = ComponentName(this, MusicWidgetProvider::class.java)
            if (!manager.requestPinAppWidget(provider, null, null)) {
                toast("أضف Widget Harmonix من قائمة أدوات الشاشة الرئيسية", "Add the Harmonix widget from your home screen widgets menu", Toast.LENGTH_LONG)
            }
        } else {
            toast("أضف Widget Harmonix من قائمة أدوات الشاشة الرئيسية", "Add the Harmonix widget from your home screen widgets menu", Toast.LENGTH_LONG)
        }
    }

    private fun toast(arabic: String, english: String, duration: Int = Toast.LENGTH_SHORT) {
        val message = if (viewModel.settings.value.language == "ar") arabic else english
        Toast.makeText(this, message, duration).show()
    }
}
