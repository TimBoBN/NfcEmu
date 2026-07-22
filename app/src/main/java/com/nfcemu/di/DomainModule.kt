package com.nfcemu.di

import com.nfcemu.data.ProfileRepository
import com.nfcemu.domain.ActiveNdefSource
import com.nfcemu.nfc.NfcAdapterTagReaderSource
import com.nfcemu.nfc.NfcStateObserver
import com.nfcemu.nfc.NfcStateSource
import com.nfcemu.nfc.TagReaderSource
import com.nfcemu.util.InstalledAppsSource
import com.nfcemu.util.PackageManagerInstalledAppsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindActiveNdefSource(repository: ProfileRepository): ActiveNdefSource

    @Binds
    abstract fun bindNfcStateSource(observer: NfcStateObserver): NfcStateSource

    @Binds
    abstract fun bindInstalledAppsSource(source: PackageManagerInstalledAppsSource): InstalledAppsSource

    @Binds
    abstract fun bindTagReaderSource(source: NfcAdapterTagReaderSource): TagReaderSource
}
