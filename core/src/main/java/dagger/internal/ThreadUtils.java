package dagger.internal;

public class ThreadUtils {
  public static boolean holdsLock(Object obj) {
    return Thread.holdsLock(obj);
  }
}
