package dagger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import dagger.internal.gwt.ModuleProvider;

public abstract class DaggerEntryPoint implements EntryPoint {

  protected abstract void onLoad();

  @Override
  public final void onModuleLoad() {
    Object[] modules = GWT.<ModuleProvider>create(ModuleProvider.class).getModules(this);
    ObjectGraph objectGraph = ObjectGraph.create(modules);
    objectGraph.injectStatics();
    objectGraph.inject(this);

    this.onLoad();
  }
}
