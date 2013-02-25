package geometry;

import dagger.Assisted;

import javax.inject.Inject;

public class PlaneCircle implements Circle {

  private double radius;

  @Inject
  public PlaneCircle(@Assisted double radius) {
    this.radius = radius;
  }

  @Override
  public double getRadius() {
    return radius;
  }

  @Override
  public double getLength() {
    return Math.PI * 2 * radius;
  }
}
