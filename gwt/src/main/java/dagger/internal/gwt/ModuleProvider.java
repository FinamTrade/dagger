package dagger.internal.gwt;

import com.google.gwt.core.client.EntryPoint;

public interface ModuleProvider {
  Object[] getModules(EntryPoint entryPoint);
}
