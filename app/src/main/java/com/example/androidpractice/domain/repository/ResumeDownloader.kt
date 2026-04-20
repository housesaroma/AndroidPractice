package com.example.androidpractice.domain.repository

interface ResumeDownloader {
    suspend fun downloadResume(url: String): String
}
