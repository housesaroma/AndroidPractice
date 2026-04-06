package com.example.androidpractice.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.androidpractice.domain.model.UserProfile
import com.example.androidpractice.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

class UserProfileDataStoreRepository(
    private val context: Context
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile> {
        return context.userProfileDataStore.data.map { preferences ->
            preferences.toUserProfile()
        }
    }

    override suspend fun saveProfile(profile: UserProfile) {
        context.userProfileDataStore.edit { preferences ->
            preferences[FULL_NAME_KEY] = profile.fullName
            preferences[RESUME_URL_KEY] = profile.resumeUrl
            preferences[POSITION_KEY] = profile.position

            if (profile.avatarUri.isNullOrBlank()) {
                preferences.remove(AVATAR_URI_KEY)
            } else {
                preferences[AVATAR_URI_KEY] = profile.avatarUri
            }
        }
    }

    private fun Preferences.toUserProfile(): UserProfile {
        return UserProfile(
            fullName = this[FULL_NAME_KEY].orEmpty(),
            avatarUri = this[AVATAR_URI_KEY],
            resumeUrl = this[RESUME_URL_KEY].orEmpty(),
            position = this[POSITION_KEY].orEmpty()
        )
    }

    private companion object {
        val FULL_NAME_KEY = stringPreferencesKey("full_name")
        val AVATAR_URI_KEY = stringPreferencesKey("avatar_uri")
        val RESUME_URL_KEY = stringPreferencesKey("resume_url")
        val POSITION_KEY = stringPreferencesKey("position")
    }
}
