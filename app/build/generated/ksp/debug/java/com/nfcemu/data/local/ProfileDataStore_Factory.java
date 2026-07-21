package com.nfcemu.data.local;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nfcemu.di.ProfileStore")
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
public final class ProfileDataStore_Factory implements Factory<ProfileDataStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public ProfileDataStore_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ProfileDataStore get() {
    return newInstance(dataStoreProvider.get());
  }

  public static ProfileDataStore_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new ProfileDataStore_Factory(dataStoreProvider);
  }

  public static ProfileDataStore newInstance(DataStore<Preferences> dataStore) {
    return new ProfileDataStore(dataStore);
  }
}
