package com.nfcemu.data;

import android.content.Context;
import com.nfcemu.data.library.LibraryDataStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class FileRepository_Factory implements Factory<FileRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<LibraryDataStore> libraryDataStoreProvider;

  public FileRepository_Factory(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<LibraryDataStore> libraryDataStoreProvider) {
    this.contextProvider = contextProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.libraryDataStoreProvider = libraryDataStoreProvider;
  }

  @Override
  public FileRepository get() {
    return newInstance(contextProvider.get(), profileRepositoryProvider.get(), libraryDataStoreProvider.get());
  }

  public static FileRepository_Factory create(Provider<Context> contextProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<LibraryDataStore> libraryDataStoreProvider) {
    return new FileRepository_Factory(contextProvider, profileRepositoryProvider, libraryDataStoreProvider);
  }

  public static FileRepository newInstance(Context context, ProfileRepository profileRepository,
      LibraryDataStore libraryDataStore) {
    return new FileRepository(context, profileRepository, libraryDataStore);
  }
}
