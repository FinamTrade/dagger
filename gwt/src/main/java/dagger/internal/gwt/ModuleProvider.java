package dagger.internal.gwt;

import dagger.DaggerEntryPoint;

public interface ModuleProvider {
  Object[] getModules(DaggerEntryPoint entryPoint);
}
