package com.example.notes.di

import com.example.notes.data.NotesRepository
import com.example.notes.data.local.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRepository(noteDao: NoteDao): NotesRepository {
        return NotesRepository(noteDao)
    }
}