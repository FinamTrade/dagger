package dagger;

import com.google.gwt.core.client.EntryPoint;

/**
 * Created by ylevin on 28.04.14.
 */
public class EntryPointInjector {
  static ObjectGraph objectGraph;

  public static void inject(EntryPoint entryPoint, Object... modules) {
    objectGraph = ObjectGraph.create(modules);
    objectGraph.injectStatics();
    objectGraph.inject(entryPoint);
  }

  public static ObjectGraph getObjectGraph() {
    return objectGraph;
  }
}
