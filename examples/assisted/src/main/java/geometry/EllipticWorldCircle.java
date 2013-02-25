package geometry;

import dagger.Assisted;

import javax.inject.Inject;
import javax.inject.Named;

public class EllipticWorldCircle extends PlaneCircle {

  private double worldRadius;

  @Inject
  public EllipticWorldCircle(@Named("worldRadius") double worldRadius,
                             @Assisted double radius) {
    super(radius);
    this.worldRadius = worldRadius;
  }

  @Override
  public double getLength() {
    return 2 * Math.PI * worldRadius * Math.sin(getRadius() / worldRadius);
  }
}
