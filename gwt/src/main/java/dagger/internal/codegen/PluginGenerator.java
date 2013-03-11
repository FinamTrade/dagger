package dagger.internal.codegen;


import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import dagger.internal.Binding;
import dagger.internal.ModuleAdapter;
import dagger.internal.Plugin;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class PluginGenerator extends IncrementalGenerator {

  private static final String PACKAGE_NAME = "dagger.internal.gwt";
  private static final String CLASS_SIMPLE_NAME = "GwtPlugin";
  private static final String INJECT_ADAPTER_NAME = "InjectAdapter";
  private static final String MODULE_ADAPTER_NAME = "ModuleAdapter";
  private static final String STATIC_INJECTION_NAME = "StaticInjection";

  @Override
  public RebindResult generateIncrementally(
      TreeLogger logger,
      GeneratorContext context, String typeName) throws UnableToCompleteException {

    PrintWriter writer = context.tryCreate(logger, PACKAGE_NAME, CLASS_SIMPLE_NAME);

    if (writer == null) {
      new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING,
          PACKAGE_NAME + "." + CLASS_SIMPLE_NAME);
    }

    try {
      TypeOracle typeOracle = context.getTypeOracle();
      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(PACKAGE_NAME, CLASS_SIMPLE_NAME);
      factory.addImplementedInterface(Plugin.class.getName());

      SourceWriter sw = factory.createSourceWriter(context, writer);

      List<String> injectAdapterNames = new ArrayList<String>();
      List<String> moduleAdapterNames = new ArrayList<String>();
      List<String> staticInjectionNames = new ArrayList<String>();

      for (JClassType type : typeOracle.getTypes()) {
        String className = type.getQualifiedSourceName();
        int dotIndex = className.lastIndexOf('.');
        String simpleName = className;
        String prefixName = "";
        if (dotIndex >= 0) {
          simpleName = className.substring(dotIndex + 1);
          prefixName = className.substring(0, dotIndex);
        }

        if (simpleName.equals(INJECT_ADAPTER_NAME)
            && typeOracle.findType(prefixName) != null) {
          injectAdapterNames.add(prefixName + "$" + INJECT_ADAPTER_NAME);
        } else if (simpleName.equals(MODULE_ADAPTER_NAME)
            && typeOracle.findType(prefixName) != null) {
          moduleAdapterNames.add(prefixName + "$" + MODULE_ADAPTER_NAME);
        } else if (simpleName.equals(STATIC_INJECTION_NAME)
            && typeOracle.findType(prefixName) != null) {
          staticInjectionNames.add(prefixName + "$" + STATIC_INJECTION_NAME);
        }
      }

      sw.println("@Override");
      sw.println("public %s<?> getAtInjectBinding"
          + "(String key, String className, boolean mustBeInjectable) {",
          Binding.class.getCanonicalName());
      sw.indent();
      printInstantiations(injectAdapterNames, sw);
      sw.outdent();
      sw.println("}");

      sw.println("@Override");
      sw.println("@SuppressWarnings(\"unchecked\")");
      sw.println("public <T> %s<T> getModuleAdapter(Class<? extends T> moduleClass, T module) {",
          ModuleAdapter.class.getCanonicalName());
      sw.indent();
      sw.print("String className = moduleClass.getName();");
      printInstantiations(moduleAdapterNames, sw);
      sw.outdent();
      sw.println("}");

      sw.println("@Override");
      sw.println("public StaticInjection getStaticInjection(Class<?> injectedClass) {");
      sw.indent();
      sw.println("String className = injectedClass.getName();");
      printInstantiations(staticInjectionNames, sw);
      sw.outdent();
      sw.println("}");

      sw.commit(logger);
    } catch (Exception e) {
      logger.log(TreeLogger.Type.ERROR, e.toString());
      throw new UnableToCompleteException();
    }

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING,
        PACKAGE_NAME + "." + CLASS_SIMPLE_NAME);
  }

  private void printInstantiations(List<String> classNames, SourceWriter sw) {
    boolean first = true;
    for (String className : classNames) {
      if (first) {
        first = false;
      } else {
        sw.print(" else ");
      }
      sw.println("if (className.equals(\"%s\")) {", className);
      sw.indent();
      sw.println("return new %s();", className);
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
}
