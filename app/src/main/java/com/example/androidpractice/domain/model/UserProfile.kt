package com.example.androidpractice.domain.model

data class UserProfile(
    val fullName: String = "",
    val avatarUri: String? = null,
    val resumeUrl: String = "",
    val position: String = "",
    val favoriteLessonTime: String = ""
)
