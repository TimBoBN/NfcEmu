package com.nfcemu.di

import com.nfcemu.data.ProfileRepository
import com.nfcemu.domain.ActiveNdefSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindActiveNdefSource(repository: ProfileRepository): ActiveNdefSource
}
