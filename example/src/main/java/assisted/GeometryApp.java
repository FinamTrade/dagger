package assisted;

import dagger.ObjectGraph;

import javax.inject.Inject;
import javax.inject.Named;

public class GeometryApp implements Runnable {

  @Inject
  @Named("simpleCircle")
  CircleFactory simpleCircleFactory;

  @Inject
  CircleFactory circleFactory;

  @Override
  public void run() {
    Circle firstCircle = circleFactory.createCircle(100);
    Circle secondCircle = simpleCircleFactory.createCircle(100);

    System.out.println("Different between circles length: "
        + Math.abs(firstCircle.getLength() - secondCircle.getLength()));
  }

  public static void main(String[] args) {
    ObjectGraph.create(new GeometryModule()).get(GeometryApp.class).run();
  }
}
