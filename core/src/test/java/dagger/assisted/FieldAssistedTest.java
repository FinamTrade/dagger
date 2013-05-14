package dagger.assisted;

import dagger.*;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Singleton;

@RunWith(JUnit4.class)
public final class FieldAssistedTest {
  @Test
  public void simpleAssistedTest() {
    AFactory factory = ObjectGraph.create(TestModule.class).get(AFactory.class);
    A a = factory.create(new C("the C"));
    Assert.assertEquals("the C", a.getC().getName());
    Assert.assertEquals("b", a.getB().getName());
  }

  @Module(injects = AFactory.class, complete = false)
  static class TestModule {

    @Provides
    @Factory(AFactory.class)
    public A provideA(A a) {
      return a;
    }

    @Provides
    @Singleton
    public B provideB(BImpl b) {
      return b;
    }

  }

  static class A {

    @Inject
    B b;

    @Inject
    @Assisted
    C c;

    public C getC() {
      return c;
    }

    public B getB() {
      return b;
    }
  }


  static interface AFactory {

    A create(C c);

  }

  static interface B {
    String getName();
  }

  static class D {

    @Inject
    D() {
    }
  }

  static class BImpl implements B {

    @Inject
    D d;

    @Override
    public String getName() {
      return "b";
    }
  }

  static class C {
    private String name;

    public C(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
