package com.nfcemu.data.library;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.nfcemu.di.LibraryStore")
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
public final class LibraryDataStore_Factory implements Factory<LibraryDataStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public LibraryDataStore_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public LibraryDataStore get() {
    return newInstance(dataStoreProvider.get());
  }

  public static LibraryDataStore_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new LibraryDataStore_Factory(dataStoreProvider);
  }

  public static LibraryDataStore newInstance(DataStore<Preferences> dataStore) {
    return new LibraryDataStore(dataStore);
  }
}
