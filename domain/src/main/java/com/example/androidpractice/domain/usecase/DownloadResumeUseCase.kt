package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.repository.ResumeDownloader

class DownloadResumeUseCase(
    private val downloader: ResumeDownloader
) {
    suspend operator fun invoke(url: String): String {
        return downloader.downloadResume(url)
    }
}
