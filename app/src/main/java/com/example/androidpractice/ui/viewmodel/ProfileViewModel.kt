package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.model.UserProfile
import com.example.androidpractice.domain.usecase.DownloadResumeUseCase
import com.example.androidpractice.domain.usecase.ObserveUserProfileUseCase
import com.example.androidpractice.domain.usecase.SaveUserProfileUseCase
import com.example.androidpractice.notifications.FavoriteLessonReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = true,
    val isDownloadingResume: Boolean = false
)

data class EditProfileUiState(
    val fullName: String = "",
    val position: String = "",
    val resumeUrl: String = "",
    val favoriteLessonTime: String = "",
    val favoriteLessonTimeError: String? = null,
    val isSaveEnabled: Boolean = false,
    val avatarUri: String? = null,
    val errorMessage: String? = null
)

sealed class ProfileEvent {
    data class OpenDownloadedFile(val uri: String) : ProfileEvent()
    data class ShowMessage(val message: String) : ProfileEvent()
}

class ProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val downloadResumeUseCase: DownloadResumeUseCase,
    private val favoriteLessonReminderScheduler: FavoriteLessonReminderScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _editUiState = MutableStateFlow(EditProfileUiState())
    val editUiState: StateFlow<EditProfileUiState> = _editUiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeUserProfileUseCase().collect { profile ->
                _profileUiState.update {
                    it.copy(
                        profile = profile,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun startEditing() {
        val profile = _profileUiState.value.profile
        val favoriteLessonTime = profile.favoriteLessonTime
        val favoriteLessonTimeError = validateFavoriteLessonTime(favoriteLessonTime)
        _editUiState.value = EditProfileUiState(
            fullName = profile.fullName,
            position = profile.position,
            resumeUrl = profile.resumeUrl,
            favoriteLessonTime = favoriteLessonTime,
            favoriteLessonTimeError = favoriteLessonTimeError,
            isSaveEnabled = favoriteLessonTimeError == null,
            avatarUri = profile.avatarUri,
            errorMessage = null
        )
    }

    fun onFullNameChange(value: String) {
        _editUiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onPositionChange(value: String) {
        _editUiState.update { it.copy(position = value, errorMessage = null) }
    }

    fun onResumeUrlChange(value: String) {
        _editUiState.update { it.copy(resumeUrl = value, errorMessage = null) }
    }

    fun onAvatarUriChange(uri: String?) {
        _editUiState.update { it.copy(avatarUri = uri, errorMessage = null) }
    }

    fun onFavoriteLessonTimeChange(value: String) {
        val favoriteLessonTimeError = validateFavoriteLessonTime(value)
        _editUiState.update {
            it.copy(
                favoriteLessonTime = value,
                favoriteLessonTimeError = favoriteLessonTimeError,
                isSaveEnabled = favoriteLessonTimeError == null,
                errorMessage = null
            )
        }
    }

    fun onFavoriteLessonTimeSelected(hour: Int, minute: Int) {
        onFavoriteLessonTimeChange(formatFavoriteLessonTime(hour, minute))
    }

    fun saveProfile(): Boolean {
        val state = _editUiState.value
        val normalizedFavoriteTime = state.favoriteLessonTime.trim()
        val favoriteLessonTimeError = validateFavoriteLessonTime(normalizedFavoriteTime)
        if (favoriteLessonTimeError != null) {
            _editUiState.update {
                it.copy(
                    favoriteLessonTime = normalizedFavoriteTime,
                    favoriteLessonTimeError = favoriteLessonTimeError,
                    isSaveEnabled = false
                )
            }
            return false
        }

        val profile = UserProfile(
            fullName = state.fullName.trim(),
            position = state.position.trim(),
            resumeUrl = state.resumeUrl.trim(),
            avatarUri = state.avatarUri,
            favoriteLessonTime = normalizedFavoriteTime
        )

        viewModelScope.launch(ioDispatcher) {
            saveUserProfileUseCase(profile)
            favoriteLessonReminderScheduler.schedule(
                ownerName = profile.fullName.ifBlank { DEFAULT_OWNER_NAME },
                favoriteLessonTime = profile.favoriteLessonTime
            )
        }
        return true
    }

    fun downloadAndOpenResume() {
        val resumeUrl = _profileUiState.value.profile.resumeUrl.trim()
        if (resumeUrl.isBlank()) {
            viewModelScope.launch {
                _events.emit(ProfileEvent.ShowMessage("Resume URL is empty"))
            }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _profileUiState.update { it.copy(isDownloadingResume = true) }
            runCatching {
                downloadResumeUseCase(resumeUrl)
            }.onSuccess { uri ->
                _events.emit(ProfileEvent.OpenDownloadedFile(uri))
            }.onFailure {
                _events.emit(ProfileEvent.ShowMessage("Failed to download resume"))
            }
            _profileUiState.update { it.copy(isDownloadingResume = false) }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this.requireApplication()
                val repository = AppContainer.provideUserProfileRepository(application)
                val downloader = AppContainer.provideResumeDownloader(application)

                ProfileViewModel(
                    observeUserProfileUseCase = ObserveUserProfileUseCase(repository),
                    saveUserProfileUseCase = SaveUserProfileUseCase(repository),
                    downloadResumeUseCase = DownloadResumeUseCase(downloader),
                    favoriteLessonReminderScheduler = AppContainer.provideFavoriteLessonReminderScheduler(application)
                )
            }
        }

        private fun validateFavoriteLessonTime(value: String): String? {
            val normalized = value.trim()
            if (normalized.isBlank()) {
                return "Time is required"
            }
            if (!FAVORITE_LESSON_TIME_REGEX.matches(normalized)) {
                return "Use HH:mm format"
            }
            return null
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build ProfileViewModel"
            }
        }

        private val FAVORITE_LESSON_TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        private const val DEFAULT_OWNER_NAME = "Profile owner"

        private fun formatFavoriteLessonTime(hour: Int, minute: Int): String {
            return String.format(Locale.US, "%02d:%02d", hour, minute)
        }
    }
}
