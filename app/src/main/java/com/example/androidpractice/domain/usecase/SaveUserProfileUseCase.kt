package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.UserProfile
import com.example.androidpractice.domain.repository.UserProfileRepository

class SaveUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(profile: UserProfile) {
        repository.saveProfile(profile)
    }
}
