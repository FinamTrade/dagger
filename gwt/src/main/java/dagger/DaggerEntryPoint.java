package dagger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import dagger.internal.gwt.ModuleProvider;

public abstract class DaggerEntryPoint implements EntryPoint {

  private ObjectGraph objectGraph;

  protected abstract void onLoad();

  @Override
  public final void onModuleLoad() {
    Object[] modules = GWT.<ModuleProvider>create(ModuleProvider.class).getModules(this);
    objectGraph = ObjectGraph.create(modules);
    objectGraph.injectStatics();
    objectGraph.inject(this);

    this.onLoad();
  }

  public ObjectGraph getObjectGraph() {
    return objectGraph;
  }
}
