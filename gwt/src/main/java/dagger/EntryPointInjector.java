package dagger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import dagger.internal.gwt.ModuleProvider;

/**
 * Created by ylevin on 28.04.14.
 */
public class EntryPointInjector {
  static ObjectGraph objectGraph;

  public static void inject(EntryPoint entryPoint) {
    Object[] modules = GWT.<ModuleProvider>create(ModuleProvider.class).getModules(entryPoint);
    objectGraph = ObjectGraph.create(modules);
    objectGraph.injectStatics();
    objectGraph.inject(entryPoint);
  }

  public static ObjectGraph getObjectGraph() {
    return objectGraph;
  }
}
