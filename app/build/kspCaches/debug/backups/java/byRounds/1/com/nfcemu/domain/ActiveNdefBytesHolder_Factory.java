package com.nfcemu.domain;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ActiveNdefBytesHolder_Factory implements Factory<ActiveNdefBytesHolder> {
  @Override
  public ActiveNdefBytesHolder get() {
    return newInstance();
  }

  public static ActiveNdefBytesHolder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ActiveNdefBytesHolder newInstance() {
    return new ActiveNdefBytesHolder();
  }

  private static final class InstanceHolder {
    private static final ActiveNdefBytesHolder_Factory INSTANCE = new ActiveNdefBytesHolder_Factory();
  }
}
