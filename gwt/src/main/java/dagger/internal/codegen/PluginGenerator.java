package dagger.internal.codegen;


import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.collect.Iterators;
import com.google.gwt.thirdparty.guava.common.io.Resources;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import dagger.internal.Binding;
import dagger.internal.ModuleAdapter;
import dagger.internal.Plugin;
import dagger.internal.StaticInjection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

      List<String> injectedClasses = retrieveResource("dagger/injectedClasses.txt");
      List<String> moduleClasses = retrieveResource("dagger/moduleClasses.txt");

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

  private boolean foundAdapter(String adapterName) {
    try {
      Class.forName(adapterName, false, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  public static List<String> retrieveResource(String filePath) throws IOException {
    List<String> result = new ArrayList<String>();
    try {
      ClassLoader loader = PluginGenerator.class.getClassLoader();
      Iterator<URL> urls = Iterators.forEnumeration(loader.getResources(filePath));
      while (urls.hasNext()) {
        URL url = urls.next();
        result.addAll(Resources.readLines(url, Charsets.UTF_8));
      }
    } catch (FileNotFoundException e) {
      //ignore exception silently and return empty list
    }
    return result;
  }
}
