package com.example.androidpractice.domain.notifications

interface FavoriteLessonReminderScheduler {
    fun schedule(ownerName: String, favoriteLessonTime: String)
}
