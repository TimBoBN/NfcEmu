package com.nfcemu.hce;

import com.nfcemu.domain.ActiveNdefSource;
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
public final class NfcEmuHostApduService_MembersInjector implements MembersInjector<NfcEmuHostApduService> {
  private final Provider<ActiveNdefSource> activeNdefSourceProvider;

  public NfcEmuHostApduService_MembersInjector(
      Provider<ActiveNdefSource> activeNdefSourceProvider) {
    this.activeNdefSourceProvider = activeNdefSourceProvider;
  }

  public static MembersInjector<NfcEmuHostApduService> create(
      Provider<ActiveNdefSource> activeNdefSourceProvider) {
    return new NfcEmuHostApduService_MembersInjector(activeNdefSourceProvider);
  }

  @Override
  public void injectMembers(NfcEmuHostApduService instance) {
    injectActiveNdefSource(instance, activeNdefSourceProvider.get());
  }

  @InjectedFieldSignature("com.nfcemu.hce.NfcEmuHostApduService.activeNdefSource")
  public static void injectActiveNdefSource(NfcEmuHostApduService instance,
      ActiveNdefSource activeNdefSource) {
    instance.activeNdefSource = activeNdefSource;
  }
}
