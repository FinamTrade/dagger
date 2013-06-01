package dagger.internal.codegen;

import com.google.gwt.core.ext.GeneratorContext;
import dagger.Module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: ylevin
 * Date: 30.05.13
 */
public class Utils {

  private static final String MODULE_PROPERTY = "dagger.module";

  private final static ClassLoader loader = Utils.class.getClassLoader();

  public static Class<?> loadModuleType(GeneratorContext context)
      throws Exception {
    List<String> modulePropValues = context.getPropertyOracle()
        .getConfigurationProperty(MODULE_PROPERTY).getValues();

    if (modulePropValues == null
        || modulePropValues.isEmpty()) {
      throw new RuntimeException("Not found \""
          + MODULE_PROPERTY + "\" property in gwt module.");
    }

    String moduleClassname = modulePropValues.get(0);

    Class<?> moduleClass;
    try {
      moduleClass = loader.loadClass(moduleClassname);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Invalid value in \""
        + MODULE_PROPERTY + "\" property. Not found class "
        + moduleClassname, e);
    }

    if (moduleClass.getAnnotation(Module.class) == null) {
      throw new RuntimeException("Specified in \""
          + MODULE_PROPERTY + "\" property class " + moduleClassname
          + " has no @Module annotation.");
    }

    return moduleClass;
  }

  public static Set<Class<?>> findAllModules(Class<?> rootModuleClass)
      throws Exception {

    Set<Class<?>> allModules = new HashSet<Class<?>>();
    Set<Class<?>> processed = new HashSet<Class<?>>();
    allModules.add(rootModuleClass);

    while (allModules.size() > processed.size()) {
      for (Class<?> moduleClass : allModules) {
        if (processed.contains(moduleClass)) {
          continue;
        }

        Module annotation = moduleClass.getAnnotation(Module.class);

        if (annotation == null) {
          throw new RuntimeException("Class " + moduleClass.getCanonicalName()
              + " has no @Module annotation.");
        }

        allModules.addAll(Arrays.asList(annotation.includes()));
        processed.add(moduleClass);
      }
    }

    return allModules;
  }
}
