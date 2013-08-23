/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger;

import com.google.gwt.core.client.GWT;
import dagger.internal.Binding;
import dagger.internal.Keys;
import dagger.internal.Linker;
import dagger.internal.ModuleAdapter;
import dagger.internal.Plugin;
import dagger.internal.ProblemDetector;
import dagger.internal.RuntimeAggregatingPlugin;
import dagger.internal.StaticInjection;
import dagger.internal.ThrowingErrorHandler;
import dagger.internal.UniqueMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A graph of objects linked by their dependencies.
 *
 * <p>The following injection features are supported:
 * <ul>
 *   <li>Field injection. A class may have any number of field injections, and
 *       fields may be of any visibility. Static fields will be injected each
 *       time an instance is injected.
 *   <li>Constructor injection. A class may have a single
 *       {@code @Inject}-annotated constructor. Classes that have fields
 *       injected may omit the {@code @Inject} annotation if they have a public
 *       no-arguments constructor.
 *   <li>Injection of {@code @Provides} method parameters.
 *   <li>{@code @Provides} methods annotated {@code @Singleton}.
 *   <li>Constructor-injected classes annotated {@code @Singleton}.
 *   <li>Injection of {@code Provider}s.
 *   <li>Injection of {@code MembersInjector}s.
 *   <li>Qualifier annotations on injected parameters and fields.
 *   <li>JSR 330 annotations.
 * </ul>
 *
 * <p>The following injection features are not currently supported:
 * <ul>
 *   <li>Method injection.</li>
 *   <li>Circular dependencies.</li>
 * </ul>
 */
public abstract class ObjectGraph {
  ObjectGraph() {
  }

  /**
   * Returns an instance of {@code type}.
   *
   * @throws IllegalArgumentException if {@code type} is not one of this object
   *     graph's {@link dagger.Module#injects injectable types}.
   */
  public abstract <T> T get(Class<T> type);

  /**
   * Injects the members of {@code instance}, including injectable members
   * inherited from its supertypes.
   *
   * @throws IllegalArgumentException if the runtime type of {@code instance} is
   *     not one of this object graph's {@link dagger.Module#injects injectable types}.
   */
  public abstract <T> T inject(T instance);

  /**
   * Returns a new object graph that includes all of the objects in this graph,
   * plus additional objects in the {@literal @}{@link dagger.Module}-annotated
   * modules. This graph is a subgraph of the returned graph.
   *
   * <p>The current graph is not modified by this operation: its objects and the
   * dependency links between them are unchanged. But this graph's objects may
   * be shared by both graphs. For example, the singletons of this graph may be
   * injected and used by the returned graph.
   *
   * <p>This <strong>does not</strong> inject any members or validate the graph.
   * See {@link #create} for guidance on injection and validation.
   */
  public abstract ObjectGraph plus(Object... modules);

  /**
   * Do runtime graph problem detection. For fastest graph creation, rely on
   * build time tools for graph validation.
   *
   * @throws IllegalStateException if this graph has problems.
   */
  public abstract void validate();

  /**
   * Injects the static fields of the classes listed in the object graph's
   * {@code staticInjections} property.
   */
  public abstract void injectStatics();

  /**
   * Returns a new dependency graph using the {@literal @}{@link
   * dagger.Module}-annotated modules.
   *
   * <p>This <strong>does not</strong> inject any members. Most applications
   * should call {@link #injectStatics} to inject static members and {@link
   * #inject} or get {@link #get(Class)} to inject instance members when this
   * method has returned.
   *
   * <p>This <strong>does not</strong> validate the graph. Rely on build time
   * tools for graph validation, or call {@link #validate} to find problems in
   * the graph at runtime.
   */
  public static ObjectGraph create(Object... modules) {
    Plugin plugin = new RuntimeAggregatingPlugin(GWT.<Plugin>create(Plugin.class));
    return DaggerObjectGraph.makeGraph(null, plugin, modules);
  }

  static class DaggerObjectGraph extends ObjectGraph {

    private final DaggerObjectGraph base;
    private final Linker linker;
    private final Map<Class<?>, StaticInjection> staticInjections;
    private final Map<String, Class<?>> injectableTypes;
    private final Plugin plugin;

    DaggerObjectGraph(DaggerObjectGraph base,
        Linker linker,
        Plugin plugin,
        Map<Class<?>, StaticInjection> staticInjections,
        Map<String, Class<?>> injectableTypes) {
      if (linker == null) throw new NullPointerException("linker");
      if (plugin == null) throw new NullPointerException("plugin");
      if (staticInjections == null) throw new NullPointerException("staticInjections");
      if (injectableTypes == null) throw new NullPointerException("injectableTypes");

      this.base = base;
      this.linker = linker;
      this.plugin = plugin;
      this.staticInjections = staticInjections;
      this.injectableTypes = injectableTypes;
    }

    static ObjectGraph makeGraph(DaggerObjectGraph base, Plugin plugin, Object... modules) {
      Map<String, Class<?>> injectableTypes = new LinkedHashMap<String, Class<?>>();
      Map<Class<?>, StaticInjection> staticInjections
          = new LinkedHashMap<Class<?>, StaticInjection>();

      // Extract bindings in the 'base' and 'overrides' set. Within each set no
      // duplicates are permitted.
      Map<String, Binding<?>> baseBindings = new UniqueMap<String, Binding<?>>();
      Map<String, Binding<?>> overrideBindings = new UniqueMap<String, Binding<?>>();
      for (ModuleAdapter<?> moduleAdapter : getAllModuleAdapters(plugin, modules).values()) {
        for (String key : moduleAdapter.injectableTypes) {
          injectableTypes.put(key, moduleAdapter.getModule().getClass());
        }
        for (Class<?> c : moduleAdapter.staticInjections) {
          staticInjections.put(c, null);
        }
        Map<String, Binding<?>> addTo = moduleAdapter.overrides ? overrideBindings : baseBindings;
        moduleAdapter.getBindings(addTo);
      }

      // Create a linker and install all of the user's bindings
      Linker linker = new Linker((base != null) ? base.linker : null, plugin,
          new ThrowingErrorHandler());
      linker.installBindings(baseBindings);
      linker.installBindings(overrideBindings);

      return new DaggerObjectGraph(base, linker, plugin, staticInjections, injectableTypes);
    }


    @Override public ObjectGraph plus(Object... modules) {
      linkEverything();
      return makeGraph(this, plugin, modules);
    }

    private void linkStaticInjections() {
      for (Map.Entry<Class<?>, StaticInjection> entry : staticInjections.entrySet()) {
        StaticInjection staticInjection = entry.getValue();
        if (staticInjection == null) {
          staticInjection = plugin.getStaticInjection(entry.getKey());
          entry.setValue(staticInjection);
        }
        staticInjection.attach(linker);
      }
    }

    private void linkInjectableTypes() {
      for (Map.Entry<String, Class<?>> entry : injectableTypes.entrySet()) {
        linker.requestBinding(entry.getKey(), entry.getValue(), false, true);
      }
    }

    @Override public void validate() {
      Map<String, Binding<?>> allBindings = linkEverything();
      new ProblemDetector().detectProblems(allBindings.values());
    }

    /**
     * Links all bindings, injectable types and static injections.
     */
    private Map<String, Binding<?>> linkEverything() {
      synchronized (linker) {
        linkStaticInjections();
        linkInjectableTypes();
        return linker.linkAll();
      }
    }

    @Override public void injectStatics() {
      // We call linkStaticInjections() twice on purpose. The first time through
      // we request all of the bindings we need. The linker returns null for
      // bindings it doesn't have. Then we ask the linker to link all of those
      // requested bindings. Finally we call linkStaticInjections() again: this
      // time the linker won't return null because everything has been linked.
      synchronized (linker) {
        linkStaticInjections();
        linker.linkRequested();
        linkStaticInjections();
      }

      for (Map.Entry<Class<?>, StaticInjection> entry : staticInjections.entrySet()) {
        entry.getValue().inject();
      }
    }

    @Override public <T> T get(Class<T> type) {
      String key = Keys.get(type);
      String injectableTypeKey = type.isInterface() ? key : Keys.getMembersKey(type);
      Class<?> moduleClass = getModuleOfInjectableType(injectableTypeKey);
      if (moduleClass == null) {
        throw new IllegalArgumentException("No inject registered for " + injectableTypeKey
            + ". You must explicitly add it to the 'injects' option in one of your modules.");
      }
      @SuppressWarnings("unchecked") // The linker matches keys to bindings by their type.
          Binding<T> binding = (Binding<T>) getLinkedBinding(moduleClass, key);
      return binding.get();
    }

    @Override public <T> T inject(T instance) {
      Class<?> type = instance.getClass();

      Class<?> moduleClass;
      String membersKey;
      do {
        membersKey = Keys.getMembersKey(type.getClass());
        moduleClass = getModuleOfInjectableType(membersKey);
        type = type.getSuperclass();
      } while (moduleClass  != null && type != Object.class);

      if (moduleClass == null) {
        throw new IllegalArgumentException("Not found injects for " + type
            + ". You must explicitly add it to the 'injects' option in one of your modules.");
      }

      @SuppressWarnings("unchecked") // The linker matches keys to bindings by their type.
        Binding<T> binding = (Binding<T>) getLinkedBinding(moduleClass, membersKey);
      binding.injectMembers(instance);
      return instance;
    }

    private Binding<?> getLinkedBinding(Class<?> moduleClass, String key) {
      Binding<?> binding = linker.requestBinding(key, moduleClass, false, true);
      if (binding == null || !binding.isLinked()) {
        linker.linkRequested();
        binding = linker.requestBinding(key, moduleClass, false, true);
      }
      return binding;
    }

    private Class<?> getModuleOfInjectableType(String key) {
      Class<?> moduleClass = null;
      for (DaggerObjectGraph graph = this; graph != null; graph = graph.base) {
        moduleClass = graph.injectableTypes.get(key);
        if (moduleClass != null) break;
      }
      return moduleClass;
    }
  }

  public static Map<Class<?>, ModuleAdapter<?>> getAllModuleAdapters(Plugin plugin,
      Object[] seedModules) {
    // Create a module adapter for each seed module.
    ModuleAdapter<?>[] seedAdapters = new ModuleAdapter<?>[seedModules.length];
    int s = 0;
    for (Object module : seedModules) {
      if (module instanceof Class) {
        seedAdapters[s++] = plugin.getModuleAdapter((Class<?>) module, null); // Plugin constructs.
      } else {
        seedAdapters[s++] = plugin.getModuleAdapter(module.getClass(), module);
      }
    }
    Map<Class<?>, ModuleAdapter<?>> adaptersByModuleType
        = new LinkedHashMap<Class<?>, ModuleAdapter<?>>();

    // Add the adapters that we have module instances for. This way we won't
    // construct module objects when we have a user-supplied instance.
    for (ModuleAdapter<?> adapter : seedAdapters) {
      adaptersByModuleType.put(adapter.getModule().getClass(), adapter);
    }

    // Next add adapters for the modules that we need to construct. This creates
    // instances of modules as necessary.
    for (ModuleAdapter<?> adapter : seedAdapters) {
      collectIncludedModulesRecursively(plugin, adapter, adaptersByModuleType);
    }

    return adaptersByModuleType;
  }

  private static void collectIncludedModulesRecursively(Plugin plugin, ModuleAdapter<?> adapter,
      Map<Class<?>, ModuleAdapter<?>> result) {
    for (Class<?> include : adapter.includes) {
      if (!result.containsKey(include)) {
        ModuleAdapter<Object> includedModuleAdapter = plugin.getModuleAdapter(include, null);
        result.put(include, includedModuleAdapter);
        collectIncludedModulesRecursively(plugin, includedModuleAdapter, result);
      }
    }
  }
}
