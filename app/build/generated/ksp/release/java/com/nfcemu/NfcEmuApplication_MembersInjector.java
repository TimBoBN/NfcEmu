package com.nfcemu;

import com.nfcemu.data.ProfileRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
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
public final class NfcEmuApplication_MembersInjector implements MembersInjector<NfcEmuApplication> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  public NfcEmuApplication_MembersInjector(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  public static MembersInjector<NfcEmuApplication> create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new NfcEmuApplication_MembersInjector(profileRepositoryProvider);
  }

  @Override
  public void injectMembers(NfcEmuApplication instance) {
    injectProfileRepository(instance, profileRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.nfcemu.NfcEmuApplication.profileRepository")
  public static void injectProfileRepository(NfcEmuApplication instance,
      ProfileRepository profileRepository) {
    instance.profileRepository = profileRepository;
  }
}
