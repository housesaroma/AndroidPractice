package com.example.androidpractice.domain.usecase

import com.example.androidpractice.domain.model.UserProfile
import com.example.androidpractice.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile> = repository.observeProfile()
}
