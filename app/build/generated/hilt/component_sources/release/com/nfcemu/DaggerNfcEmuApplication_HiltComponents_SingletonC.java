package com.nfcemu;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.nfcemu.data.FileRepository;
import com.nfcemu.data.ProfileRepository;
import com.nfcemu.data.library.LibraryDataStore;
import com.nfcemu.data.local.ProfileDataStore;
import com.nfcemu.data.local.SettingsDataStore;
import com.nfcemu.di.CoroutineModule_ProvideApplicationScopeFactory;
import com.nfcemu.di.DataStoreModule_ProvideLibraryDataStoreFactory;
import com.nfcemu.di.DataStoreModule_ProvideProfileDataStoreFactory;
import com.nfcemu.di.DataStoreModule_ProvideSettingsDataStoreFactory;
import com.nfcemu.hce.NfcEmuHostApduService;
import com.nfcemu.hce.NfcEmuHostApduService_MembersInjector;
import com.nfcemu.nfc.NfcStateObserver;
import com.nfcemu.ui.MainActivity;
import com.nfcemu.ui.home.HomeViewModel;
import com.nfcemu.ui.home.HomeViewModel_HiltModules;
import com.nfcemu.ui.library.LibraryViewModel;
import com.nfcemu.ui.library.LibraryViewModel_HiltModules;
import com.nfcemu.ui.onboarding.OnboardingViewModel;
import com.nfcemu.ui.onboarding.OnboardingViewModel_HiltModules;
import com.nfcemu.ui.profileform.ProfileFormViewModel;
import com.nfcemu.ui.profileform.ProfileFormViewModel_HiltModules;
import com.nfcemu.ui.profilelist.ProfileListViewModel;
import com.nfcemu.ui.profilelist.ProfileListViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

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
public final class DaggerNfcEmuApplication_HiltComponents_SingletonC {
  private DaggerNfcEmuApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public NfcEmuApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements NfcEmuApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements NfcEmuApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements NfcEmuApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements NfcEmuApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements NfcEmuApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements NfcEmuApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements NfcEmuApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public NfcEmuApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends NfcEmuApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends NfcEmuApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends NfcEmuApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends NfcEmuApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(5).put(LazyClassKeyProvider.com_nfcemu_ui_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nfcemu_ui_library_LibraryViewModel, LibraryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nfcemu_ui_onboarding_OnboardingViewModel, OnboardingViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nfcemu_ui_profileform_ProfileFormViewModel, ProfileFormViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_nfcemu_ui_profilelist_ProfileListViewModel, ProfileListViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_nfcemu_ui_onboarding_OnboardingViewModel = "com.nfcemu.ui.onboarding.OnboardingViewModel";

      static String com_nfcemu_ui_profilelist_ProfileListViewModel = "com.nfcemu.ui.profilelist.ProfileListViewModel";

      static String com_nfcemu_ui_home_HomeViewModel = "com.nfcemu.ui.home.HomeViewModel";

      static String com_nfcemu_ui_library_LibraryViewModel = "com.nfcemu.ui.library.LibraryViewModel";

      static String com_nfcemu_ui_profileform_ProfileFormViewModel = "com.nfcemu.ui.profileform.ProfileFormViewModel";

      @KeepFieldType
      OnboardingViewModel com_nfcemu_ui_onboarding_OnboardingViewModel2;

      @KeepFieldType
      ProfileListViewModel com_nfcemu_ui_profilelist_ProfileListViewModel2;

      @KeepFieldType
      HomeViewModel com_nfcemu_ui_home_HomeViewModel2;

      @KeepFieldType
      LibraryViewModel com_nfcemu_ui_library_LibraryViewModel2;

      @KeepFieldType
      ProfileFormViewModel com_nfcemu_ui_profileform_ProfileFormViewModel2;
    }
  }

  private static final class ViewModelCImpl extends NfcEmuApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LibraryViewModel> libraryViewModelProvider;

    private Provider<OnboardingViewModel> onboardingViewModelProvider;

    private Provider<ProfileFormViewModel> profileFormViewModelProvider;

    private Provider<ProfileListViewModel> profileListViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.libraryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.onboardingViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.profileFormViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.profileListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(5).put(LazyClassKeyProvider.com_nfcemu_ui_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_nfcemu_ui_library_LibraryViewModel, ((Provider) libraryViewModelProvider)).put(LazyClassKeyProvider.com_nfcemu_ui_onboarding_OnboardingViewModel, ((Provider) onboardingViewModelProvider)).put(LazyClassKeyProvider.com_nfcemu_ui_profileform_ProfileFormViewModel, ((Provider) profileFormViewModelProvider)).put(LazyClassKeyProvider.com_nfcemu_ui_profilelist_ProfileListViewModel, ((Provider) profileListViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_nfcemu_ui_profilelist_ProfileListViewModel = "com.nfcemu.ui.profilelist.ProfileListViewModel";

      static String com_nfcemu_ui_library_LibraryViewModel = "com.nfcemu.ui.library.LibraryViewModel";

      static String com_nfcemu_ui_home_HomeViewModel = "com.nfcemu.ui.home.HomeViewModel";

      static String com_nfcemu_ui_onboarding_OnboardingViewModel = "com.nfcemu.ui.onboarding.OnboardingViewModel";

      static String com_nfcemu_ui_profileform_ProfileFormViewModel = "com.nfcemu.ui.profileform.ProfileFormViewModel";

      @KeepFieldType
      ProfileListViewModel com_nfcemu_ui_profilelist_ProfileListViewModel2;

      @KeepFieldType
      LibraryViewModel com_nfcemu_ui_library_LibraryViewModel2;

      @KeepFieldType
      HomeViewModel com_nfcemu_ui_home_HomeViewModel2;

      @KeepFieldType
      OnboardingViewModel com_nfcemu_ui_onboarding_OnboardingViewModel2;

      @KeepFieldType
      ProfileFormViewModel com_nfcemu_ui_profileform_ProfileFormViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nfcemu.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.nfcStateObserverProvider.get());

          case 1: // com.nfcemu.ui.library.LibraryViewModel 
          return (T) new LibraryViewModel(singletonCImpl.fileRepositoryProvider.get());

          case 2: // com.nfcemu.ui.onboarding.OnboardingViewModel 
          return (T) new OnboardingViewModel(singletonCImpl.settingsDataStoreProvider.get());

          case 3: // com.nfcemu.ui.profileform.ProfileFormViewModel 
          return (T) new ProfileFormViewModel(singletonCImpl.profileRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 4: // com.nfcemu.ui.profilelist.ProfileListViewModel 
          return (T) new ProfileListViewModel(singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.fileRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends NfcEmuApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends NfcEmuApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectNfcEmuHostApduService(NfcEmuHostApduService arg0) {
      injectNfcEmuHostApduService2(arg0);
    }

    private NfcEmuHostApduService injectNfcEmuHostApduService2(NfcEmuHostApduService instance) {
      NfcEmuHostApduService_MembersInjector.injectActiveNdefSource(instance, singletonCImpl.profileRepositoryProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends NfcEmuApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<DataStore<Preferences>> provideProfileDataStoreProvider;

    private Provider<ProfileDataStore> profileDataStoreProvider;

    private Provider<CoroutineScope> provideApplicationScopeProvider;

    private Provider<ProfileRepository> profileRepositoryProvider;

    private Provider<NfcStateObserver> nfcStateObserverProvider;

    private Provider<DataStore<Preferences>> provideLibraryDataStoreProvider;

    private Provider<LibraryDataStore> libraryDataStoreProvider;

    private Provider<FileRepository> fileRepositoryProvider;

    private Provider<DataStore<Preferences>> provideSettingsDataStoreProvider;

    private Provider<SettingsDataStore> settingsDataStoreProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideProfileDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 2));
      this.profileDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<ProfileDataStore>(singletonCImpl, 1));
      this.provideApplicationScopeProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineScope>(singletonCImpl, 3));
      this.profileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 0));
      this.nfcStateObserverProvider = DoubleCheck.provider(new SwitchingProvider<NfcStateObserver>(singletonCImpl, 4));
      this.provideLibraryDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 7));
      this.libraryDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<LibraryDataStore>(singletonCImpl, 6));
      this.fileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<FileRepository>(singletonCImpl, 5));
      this.provideSettingsDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 9));
      this.settingsDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<SettingsDataStore>(singletonCImpl, 8));
    }

    @Override
    public void injectNfcEmuApplication(NfcEmuApplication nfcEmuApplication) {
      injectNfcEmuApplication2(nfcEmuApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private NfcEmuApplication injectNfcEmuApplication2(NfcEmuApplication instance) {
      NfcEmuApplication_MembersInjector.injectProfileRepository(instance, profileRepositoryProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nfcemu.data.ProfileRepository 
          return (T) new ProfileRepository(singletonCImpl.profileDataStoreProvider.get(), singletonCImpl.provideApplicationScopeProvider.get());

          case 1: // com.nfcemu.data.local.ProfileDataStore 
          return (T) new ProfileDataStore(singletonCImpl.provideProfileDataStoreProvider.get());

          case 2: // @com.nfcemu.di.ProfileStore androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) DataStoreModule_ProvideProfileDataStoreFactory.provideProfileDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // @com.nfcemu.di.ApplicationScope kotlinx.coroutines.CoroutineScope 
          return (T) CoroutineModule_ProvideApplicationScopeFactory.provideApplicationScope();

          case 4: // com.nfcemu.nfc.NfcStateObserver 
          return (T) new NfcStateObserver(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.nfcemu.data.FileRepository 
          return (T) new FileRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.libraryDataStoreProvider.get());

          case 6: // com.nfcemu.data.library.LibraryDataStore 
          return (T) new LibraryDataStore(singletonCImpl.provideLibraryDataStoreProvider.get());

          case 7: // @com.nfcemu.di.LibraryStore androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) DataStoreModule_ProvideLibraryDataStoreFactory.provideLibraryDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.nfcemu.data.local.SettingsDataStore 
          return (T) new SettingsDataStore(singletonCImpl.provideSettingsDataStoreProvider.get());

          case 9: // @com.nfcemu.di.SettingsStore androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) DataStoreModule_ProvideSettingsDataStoreFactory.provideSettingsDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
