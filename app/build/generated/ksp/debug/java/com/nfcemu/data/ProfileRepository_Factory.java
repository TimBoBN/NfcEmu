package com.nfcemu.data;

import com.nfcemu.data.local.ProfileDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nfcemu.di.ApplicationScope")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<ProfileDataStore> dataStoreProvider;

  private final Provider<CoroutineScope> scopeProvider;

  public ProfileRepository_Factory(Provider<ProfileDataStore> dataStoreProvider,
      Provider<CoroutineScope> scopeProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.scopeProvider = scopeProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(dataStoreProvider.get(), scopeProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<ProfileDataStore> dataStoreProvider,
      Provider<CoroutineScope> scopeProvider) {
    return new ProfileRepository_Factory(dataStoreProvider, scopeProvider);
  }

  public static ProfileRepository newInstance(ProfileDataStore dataStore, CoroutineScope scope) {
    return new ProfileRepository(dataStore, scope);
  }
}
