import dagger.Assisted;
import dagger.Factory;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Inject;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.String;

public class TestApp implements Runnable {

  @Override
  public void run() {
    ObjectGraph.create(new AssistedModule()).get(A.class).run();
  }

  static class A implements Runnable {
    @Inject
    BFactory b;

    @Override
    public void run() {
      b.newB("This is B", "a", "b").run();
    }
  }

  static abstract class B implements Runnable {

  }

  interface BFactory {

    B newB(String name, @Named("a") String a, @Named("b") String b);
  }

  @Module(injects = A.class)
  static class AssistedModule {
    @Provides
    @Factory(BFactory.class)
    public B provideB(BImpl2 b) {
      return b;
    }
  }

  static class BImpl2 extends BImpl {
    @Inject
    @Assisted
    @Named("a")
    String a;

    String b;

    @Inject
    BImpl2(@Assisted @Named("b") String b) {
      this.b = b;
    }

    @Override
    public void run() {
      // Do nothing
    }
  }

  static abstract class BImpl extends B {

    @Inject
    @Assisted
    String name;
  }
}
