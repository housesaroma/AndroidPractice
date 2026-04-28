package com.example.androidpractice.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.usecase.DownloadResumeUseCase
import com.example.androidpractice.domain.usecase.ObserveUserProfileUseCase
import com.example.androidpractice.domain.usecase.SaveUserProfileUseCase

class ProfileViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val repository = AppContainer.provideUserProfileRepository(appContext)
            val downloader = AppContainer.provideResumeDownloader(appContext)
            return ProfileViewModel(
                observeUserProfileUseCase = ObserveUserProfileUseCase(repository),
                downloadResumeUseCase = DownloadResumeUseCase(downloader)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class EditProfileViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            val repository = AppContainer.provideUserProfileRepository(appContext)
            return EditProfileViewModel(
                observeUserProfileUseCase = ObserveUserProfileUseCase(repository),
                saveUserProfileUseCase = SaveUserProfileUseCase(repository),
                favoriteLessonReminderScheduler = AppContainer.provideFavoriteLessonReminderScheduler(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
