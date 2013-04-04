package dagger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import dagger.internal.gwt.ModuleProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DaggerEntryPoint implements EntryPoint {

  protected abstract void onLoad();

  protected Object[] getCustomModules() {
    return new Object[] {};
  }

  @Override
  public final void onModuleLoad() {
    Object[] modules = GWT.<ModuleProvider>create(ModuleProvider.class).getModules(this);
    Object[] customModules = getCustomModules();

    Map<String, Object> modulesByClass = new LinkedHashMap<String, Object>();
    for (Object module : modules) {
      modulesByClass.put(module.getClass().getName(), module);
    }
    for (Object module : customModules) {
      modulesByClass.put(module.getClass().getName(), module);
    }
    Collection<Object> collection = modulesByClass.values();
    modules = collection.toArray(new Object[collection.size()]);

    ObjectGraph objectGraph = ObjectGraph.create(modules);
    objectGraph.injectStatics();
    objectGraph.inject(this);

    this.onLoad();
  }
}
