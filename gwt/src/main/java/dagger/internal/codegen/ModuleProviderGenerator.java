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
import dagger.DaggerEntryPoint;
import dagger.Module;
import dagger.internal.gwt.ModuleProvider;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleProviderGenerator extends IncrementalGenerator {

  private static final String PACKAGE_NAME = "dagger.internal.gwt";
  private static final String CLASS_SIMPLE_NAME = "ModuleProviderImpl";

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
      TypeOracle typeOracle = context.getTypeOracle();
      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(PACKAGE_NAME, CLASS_SIMPLE_NAME);
      factory.addImplementedInterface(ModuleProvider.class.getName());

      SourceWriter sw = factory.createSourceWriter(context, writer);

      Map<String, List<String>> modulesByEntryPoint = new HashMap<String, List<String>>();

      for (JClassType type : typeOracle.getTypes()) {
        Module moduleAnnotation = type.getAnnotation(Module.class);

        if (moduleAnnotation == null) {
          continue;
        }

        Class<?>[] entryPoints = moduleAnnotation.entryPoints();

        for (Class<?> entryPoint : entryPoints) {
          if (DaggerEntryPoint.class.isAssignableFrom(entryPoint)) {
            List<String> modules = modulesByEntryPoint.get(entryPoint.getName());

            if (modules == null) {
              modulesByEntryPoint.put(entryPoint.getName(), modules = new ArrayList<String>());
            }

            modules.add(type.getQualifiedSourceName());
          }
        }
      }

      sw.println("@Override");
      sw.println("public Object[] getModules(%s entryPoint) {", DaggerEntryPoint.class.getName());
      sw.indent();

      boolean first = true;
      for (Map.Entry<String, List<String>> entry : modulesByEntryPoint.entrySet()) {
        if (first) {
          first = false;
        } else {
          sw.print(" else ");
        }
        sw.println("if (entryPoint.getClass() == %s.class) {", entry.getKey());
        sw.indent();
        sw.print("return new Object[] { ");
        boolean firstName = true;
        for (String moduleName : entry.getValue()) {
          if (firstName) {
            firstName = false;
          } else {
            sw.print(", ");
          }
          sw.print("new %s()", moduleName);
        }
        sw.println(" };");
        sw.outdent();
        sw.print("}");
      }
      if (!first) {
        sw.println();
      }

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

  @Override
  public long getVersionId() {
    return 0;
  }
}
