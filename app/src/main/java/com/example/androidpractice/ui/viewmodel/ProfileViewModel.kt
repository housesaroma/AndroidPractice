package com.example.androidpractice.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidpractice.data.di.AppContainer
import com.example.androidpractice.domain.usecase.DownloadResumeUseCase
import com.example.androidpractice.domain.usecase.ObserveUserProfileUseCase
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

class ProfileViewModel(
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val downloadResumeUseCase: DownloadResumeUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

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
                    downloadResumeUseCase = DownloadResumeUseCase(downloader)
                )
            }
        }

        private fun CreationExtras.requireApplication(): Application {
            return checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                "Application is required to build ProfileViewModel"
            }
        }
    }
}
