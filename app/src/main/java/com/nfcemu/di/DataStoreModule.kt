package com.nfcemu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProfileStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LibraryStore

/**
 * Provides the raw [DataStore] instances as separate bindings (rather than each
 * repository deriving its own from [Context] via the `preferencesDataStore` property
 * delegate) so `ProfileDataStore`/`LibraryDataStore` can be constructed directly in
 * plain JUnit tests against a temp-file-backed DataStore, no Android Context needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @ProfileStore
    fun provideProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("profiles") })

    @Provides
    @Singleton
    @LibraryStore
    fun provideLibraryDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("library") })
}
