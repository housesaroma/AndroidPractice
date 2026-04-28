package com.example.androidpractice.ui.screens.profile.edit

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.androidpractice.ui.viewmodel.EditProfileUiState
import java.io.File
import java.util.Calendar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onBackClick: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onResumeUrlChange: (String) -> Unit,
    onFavoriteLessonTimeChange: (String) -> Unit,
    onFavoriteLessonTimeSelected: (Int, Int) -> Unit,
    onAvatarUriChange: (String?) -> Unit,
    onDoneClick: () -> Unit,
    onStoragePermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    var showAvatarDialog by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val storagePermissions = remember { resolveStoragePermissions() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onAvatarUriChange(pendingCameraUri?.toString())
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onAvatarUriChange(uri.toString())
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = storagePermissions.any { permissions[it] == true }
        if (!granted) {
            onStoragePermissionDenied()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera(context) { uri ->
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(storagePermissions) {
        if (storagePermissions.isNotEmpty() && !context.hasAnyPermission(storagePermissions)) {
            storagePermissionLauncher.launch(storagePermissions.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Edit profile") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AvatarPicker(
                avatarUri = uiState.avatarUri,
                onClick = { showAvatarDialog = true }
            )

            OutlinedTextField(
                value = uiState.fullName,
                onValueChange = onFullNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Full name") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.position,
                onValueChange = onPositionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Position") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.resumeUrl,
                onValueChange = onResumeUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Resume URL") },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.favoriteLessonTime,
                onValueChange = onFavoriteLessonTimeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Favorite lesson time") },
                placeholder = { Text("HH:mm") },
                singleLine = true,
                isError = uiState.favoriteLessonTimeError != null,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            showTimePicker(
                                context = context,
                                currentTime = uiState.favoriteLessonTime,
                                onTimeSelected = onFavoriteLessonTimeSelected
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "Pick time"
                        )
                    }
                },
                supportingText = {
                    uiState.favoriteLessonTimeError?.let { errorText ->
                        Text(text = errorText, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = onDoneClick,
                    enabled = uiState.isSaveEnabled
                ) {
                    Text("Done")
                }
            }
        }
    }

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Choose avatar source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
                            if (storagePermissions.isEmpty() || context.hasAnyPermission(storagePermissions)) {
                                galleryLauncher.launch(arrayOf("image/*"))
                            } else {
                                storagePermissionLauncher.launch(storagePermissions.toTypedArray())
                            }
                        }
                    ) {
                        Text("Gallery")
                    }
                    TextButton(
                        onClick = {
                            showAvatarDialog = false
                            if (context.hasPermission(Manifest.permission.CAMERA)) {
                                launchCamera(context) { uri ->
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Text("Camera")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AvatarPicker(
    avatarUri: String?,
    onClick: () -> Unit
) {
    val avatarModifier = Modifier
        .size(132.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .clickable(onClick = onClick)

    if (avatarUri.isNullOrBlank()) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Avatar",
            modifier = avatarModifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        AsyncImage(
            model = avatarUri,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    }
}

private fun resolveStoragePermissions(): List<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> emptyList()
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasAnyPermission(permissions: List<String>): Boolean {
    return permissions.any(::hasPermission)
}

private fun launchCamera(context: Context, onUriReady: (Uri) -> Unit) {
    val uri = createTemporaryImageUri(context)
    if (uri != null) {
        onUriReady(uri)
    }
}

private fun createTemporaryImageUri(context: Context): Uri? {
    return runCatching {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "avatar_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun showTimePicker(
    context: Context,
    currentTime: String,
    onTimeSelected: (Int, Int) -> Unit
) {
    val now = Calendar.getInstance()
    val parsedTime = parseTimeOrNull(currentTime)
    val initialHour = parsedTime?.first ?: now.get(Calendar.HOUR_OF_DAY)
    val initialMinute = parsedTime?.second ?: now.get(Calendar.MINUTE)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(selectedHour, selectedMinute)
        },
        initialHour,
        initialMinute,
        true
    ).show()
}

private fun parseTimeOrNull(value: String): Pair<Int, Int>? {
    val parts = value.split(":")
    if (parts.size != 2) {
        return null
    }

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) {
        return null
    }

    return hour to minute
}
