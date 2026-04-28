package com.example.androidpractice.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpractice.domain.model.UserProfile
import com.example.androidpractice.domain.notifications.FavoriteLessonReminderScheduler
import com.example.androidpractice.domain.usecase.ObserveUserProfileUseCase
import com.example.androidpractice.domain.usecase.SaveUserProfileUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class EditProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val favoriteLessonReminderScheduler: FavoriteLessonReminderScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var currentProfile: UserProfile = UserProfile()

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeUserProfileUseCase().collect { profile ->
                currentProfile = profile
            }
        }
    }

    fun startEditing() {
        val favoriteLessonTime = currentProfile.favoriteLessonTime
        val favoriteLessonTimeError = validateFavoriteLessonTime(favoriteLessonTime)
        _uiState.value = EditProfileUiState(
            fullName = currentProfile.fullName,
            position = currentProfile.position,
            resumeUrl = currentProfile.resumeUrl,
            favoriteLessonTime = favoriteLessonTime,
            favoriteLessonTimeError = favoriteLessonTimeError,
            isSaveEnabled = favoriteLessonTimeError == null,
            avatarUri = currentProfile.avatarUri,
            errorMessage = null
        )
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onPositionChange(value: String) {
        _uiState.update { it.copy(position = value, errorMessage = null) }
    }

    fun onResumeUrlChange(value: String) {
        _uiState.update { it.copy(resumeUrl = value, errorMessage = null) }
    }

    fun onAvatarUriChange(uri: String?) {
        _uiState.update { it.copy(avatarUri = uri, errorMessage = null) }
    }

    fun onFavoriteLessonTimeChange(value: String) {
        val favoriteLessonTimeError = validateFavoriteLessonTime(value)
        _uiState.update {
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
        val state = _uiState.value
        val normalizedFavoriteTime = state.favoriteLessonTime.trim()
        val favoriteLessonTimeError = validateFavoriteLessonTime(normalizedFavoriteTime)
        if (favoriteLessonTimeError != null) {
            _uiState.update {
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

    companion object {
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

        private fun formatFavoriteLessonTime(hour: Int, minute: Int): String {
            return String.format(Locale.US, "%02d:%02d", hour, minute)
        }

        private val FAVORITE_LESSON_TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        private const val DEFAULT_OWNER_NAME = "Profile owner"
    }
}
