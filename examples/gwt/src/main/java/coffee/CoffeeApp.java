package coffee;

import com.google.gwt.core.client.EntryPoint;
import dagger.GwtObjectGraph;
import dagger.ObjectGraph;

public class CoffeeApp implements EntryPoint {

  @Override
  public void onModuleLoad() {
    ObjectGraph objectGraph = GwtObjectGraph.create(new DripCoffeeModule());
    CoffeeMaker coffeeMaker = objectGraph.get(CoffeeMaker.class);
    coffeeMaker.brew();
  }
}
