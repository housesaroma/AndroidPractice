package com.example.androidpractice.ui.viewmodel

import com.example.androidpractice.domain.model.UserProfile

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
