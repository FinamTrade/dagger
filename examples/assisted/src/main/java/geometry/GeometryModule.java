package geometry;

import dagger.Factory;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(injects = GeometryApp.class)
public class GeometryModule {
  @Provides
  @Factory(CircleFactory.class)
  public Circle provideCircle(EllipticWorldCircle circle) {
    return circle;
  }

  @Provides
  @Factory(CircleFactory.class)
  @Named("simpleCircle")
  public Circle provideSimpleCircle(PlaneCircle circle) {
    return circle;
  }

  @Provides
  @Named("worldRadius")
  public double provideWorldRadius() {
    return 1050;
  }
}
