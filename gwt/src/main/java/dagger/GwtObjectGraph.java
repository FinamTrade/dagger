package dagger;

import com.google.gwt.core.client.GWT;
import dagger.internal.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static dagger.internal.RuntimeAggregatingPlugin.getAllModuleAdapters;

public class GwtObjectGraph {

  public ObjectGraph create(Object... modules) {
    return ObjectGraph.makeGraph(null, GWT.<Plugin>create(Plugin.class), modules);
  }
}
