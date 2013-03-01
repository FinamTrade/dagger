package dagger;

import com.google.gwt.core.client.GWT;
import dagger.internal.Plugin;

public class GwtObjectGraph {

  public ObjectGraph create(Object... modules) {
    return ObjectGraph.makeGraph(null, GWT.<Plugin>create(Plugin.class), modules);
  }
}
