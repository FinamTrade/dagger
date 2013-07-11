package dagger.internal.codegen;


import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import dagger.Factory;
import dagger.Module;
import dagger.Provides;
import dagger.internal.Binding;
import dagger.internal.Keys;
import dagger.internal.Linker;
import dagger.internal.ModuleAdapter;
import dagger.internal.Plugin;
import dagger.internal.SetBinding;
import dagger.internal.StaticInjection;

import javax.inject.Singleton;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dagger.internal.codegen.Utils.findAllModules;
import static dagger.internal.codegen.Utils.loadModuleType;

public class PluginGenerator extends IncrementalGenerator {

  private static final String PACKAGE_NAME = "dagger.internal.gwt";
  private static final String CLASS_SIMPLE_NAME = "GwtPlugin";
  private static final String INJECT_ADAPTER_SUFFIX = "$$InjectAdapter";
  private static final String MODULE_ADAPTER_SUFFIX = "$$ModuleAdapter";
  private static final String STATIC_INJECTION_SUFFIX = "$$StaticInjection";

  @Override
  public RebindResult generateIncrementally(
      TreeLogger logger,
      GeneratorContext context, String typeName) throws UnableToCompleteException {

    PrintWriter writer = context.tryCreate(logger, PACKAGE_NAME, CLASS_SIMPLE_NAME);

    if (writer == null) {
      return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING,
          PACKAGE_NAME + "." + CLASS_SIMPLE_NAME);
    }

    try {
      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(PACKAGE_NAME, CLASS_SIMPLE_NAME);
      factory.addImplementedInterface(Plugin.class.getName());

      SourceWriter sw = factory.createSourceWriter(context, writer);

      Class<?> rootModule = loadModuleType(context);
      Set<Class<?>> modules = findAllModules(rootModule);
      List<String> injectedClasses = findAllInjects(modules);
      List<String> moduleClasses = new ArrayList<String>();
      for (Class<?> moduleClass : modules) {
        moduleClasses.add(moduleClass.getName());
      }

      sw.println("@Override");
      sw.println("public %s<?> getAtInjectBinding"
          + "(String key, String className, boolean mustBeInjectable) {",
          Binding.class.getCanonicalName());
      sw.indent();
      printInstantiations(injectedClasses, INJECT_ADAPTER_SUFFIX, null, sw);
      sw.outdent();
      sw.println("}");

      sw.println("@Override");
      sw.println("@SuppressWarnings(\"unchecked\")");
      sw.println("public <T> %s<T> getModuleAdapter(Class<? extends T> moduleClass, T module) {",
          ModuleAdapter.class.getCanonicalName());
      sw.indent();
      sw.println("String className = moduleClass.getName();");
      printInstantiations(moduleClasses, MODULE_ADAPTER_SUFFIX,
          ModuleAdapter.class.getName() + "<T>", sw);
      sw.outdent();
      sw.println("}");

      sw.println("@Override");
      sw.println("public %s getStaticInjection(Class<?> injectedClass) {",
          StaticInjection.class.getName());
      sw.indent();
      sw.println("String className = injectedClass.getName();");
      // TODO: static injections
      // printInstantiations(staticInjectionNames, STATIC_INJECTION_SUFFIX, null, sw);
      sw.println("return null;");
      sw.outdent();
      sw.println("}");

      sw.commit(logger);
    } catch (Exception e) {
      logger.log(TreeLogger.Type.ERROR, e.toString());
      e.printStackTrace();
      throw new UnableToCompleteException();
    }

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING,
        PACKAGE_NAME + "." + CLASS_SIMPLE_NAME);
  }

  private void printInstantiations(
      List<String> classNames, String suffix, String castClassName, SourceWriter sw) {

    boolean first = true;
    for (String className : classNames) {
      if (first) {
        first = false;
      } else {
        sw.print(" else ");
      }
      sw.println("if (className.equals(\"%s\")) {", className);
      sw.indent();
      if (castClassName != null) {
        sw.println("return (%s) new %s();", castClassName, className + suffix);
      } else {
        sw.println("return new %s();", className + suffix);
      }
      sw.outdent();
      sw.print("}");
    }
    if (!first) {
      sw.println();
    }
    sw.println("return null;");
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  public static List<String> findAllInjects(Collection<Class<?>> allModules) {
    final List<String> handledErrors = new ArrayList<String>();
    final List<String> injectedClasses = new ArrayList<String>();

    final Linker linker = new Linker(null, new GwtCompilePlugin(injectedClasses),
        new Linker.ErrorHandler() {
          @Override
          public void handleErrors(List<String> errors) {
            handledErrors.addAll(errors);
          }
        }
    );

    Map<String, Binding<?>> overrideBindings = new HashMap<String, Binding<?>>();
    Map<String, Binding<?>> baseBindings = new HashMap<String, Binding<?>>();
    synchronized (linker) {
      for (Class<?> module : allModules) {
        Module annotation = module.getAnnotation(Module.class);
        Class<?>[] injects = annotation.injects();
        boolean overrides = annotation.overrides();
        Map<String, Binding<?>> addTo = overrides ? overrideBindings : baseBindings;
        for (Class<?> inject : injects) {
          String key = inject.isInterface()
              ? Keys.get(inject) : Keys.getMembersKey(inject);
          linker.requestBinding(key, module.getCanonicalName(), false, true);
        }

        for (Method method : module.getDeclaredMethods()) {
          Provides provides = method.getAnnotation(Provides.class);
          Factory factory = method.getAnnotation(Factory.class);
          if (provides == null) {
            continue;
          }

          Type type = method.getGenericReturnType();
          String key;
          if (factory == null) {
            key = Keys.get(type, method.getAnnotations(), method);
          } else {
            Class<?> factoryType = factory.value();
            key = Keys.get(factoryType, method.getAnnotations(), method);
          }
          Binding binding = new ProviderMethodBinding(key, method);

          switch(provides.type()) {
            case UNIQUE:
              addTo.put(key, binding);
              break;
            case SET:
              key = Keys.getSetKey(type, method.getAnnotations(), method);
              SetBinding.add(addTo, key, binding);
              break;
            default:
              throw new RuntimeException("Unsupported @Provides type.");
          }
        }
      }

      linker.installBindings(baseBindings);
      linker.installBindings(overrideBindings);

      linker.linkAll();
    }

    if (!handledErrors.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (String error : handledErrors) {
        sb.append(error);
        sb.append("\n");
      }
      throw new RuntimeException(sb.toString());
    }

    return injectedClasses;
  }


  private static class ProviderMethodBinding extends Binding<Object> {
    private final Method method;
    private final Binding<?>[] parameters;

    protected ProviderMethodBinding(String provideKey, Method method) {
      super(provideKey, null, null, method.getAnnotation(Singleton.class) != null,
          method.getName());
      this.method = method;
      this.parameters = new Binding[method.getParameterTypes().length];
    }

    @Override public void attach(Linker linker) {
      Type[] types = method.getGenericParameterTypes();
      Annotation[][] annotations = method.getParameterAnnotations();
      for (int i = 0; i < types.length; i++) {
        Type parameterType = types[i];
        String parameterKey = Keys.get(types[i], annotations[i], method + " parameter " + i);
        if (parameterType.equals(method.getReturnType())) {
          parameterKey = "adapter/" + parameterKey;
        }
        parameters[i] = linker.requestBinding(parameterKey, method.toString());
      }
    }

    @Override public Object get() {
      throw new UnsupportedOperationException();
    }

    @Override public void injectMembers(Object t) {
      throw new UnsupportedOperationException();
    }

    @Override public void getDependencies(Set<Binding<?>> get, Set<Binding<?>> injectMembers) {
      Collections.addAll(get, parameters);
    }
  }


  private static class GwtCompilePlugin implements Plugin {
    private static final String INJECT_ADAPTER_SUFFIX = "$$InjectAdapter";

    private final List<String> injectedClasses;

    private static final ClassLoader loader = GwtCompilePlugin.class.getClassLoader();

    public GwtCompilePlugin(List<String> injectedClasses) {
      this.injectedClasses = injectedClasses;
    }

    @Override
    public Binding<?> getAtInjectBinding(String key, String className, boolean mustHaveInjections) {
      try {
        Class<?> clazz = loader.loadClass(className);
        if (clazz.isInterface()) {
          return null;
        }
        Class<Binding<?>> bindingClass = getBindingClass(className);
        injectedClasses.add(className);
        return bindingClass.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("unchecked")
    private Class<Binding<?>> getBindingClass(String className)
        throws ClassNotFoundException {
      return (Class<Binding<?>>) loader.loadClass(className + INJECT_ADAPTER_SUFFIX);
    }

    @Override
    public <T> ModuleAdapter<T> getModuleAdapter(Class<? extends T> moduleClass, T module) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StaticInjection getStaticInjection(Class<?> injectedClass) {
      throw new UnsupportedOperationException();
    }
  }
}
