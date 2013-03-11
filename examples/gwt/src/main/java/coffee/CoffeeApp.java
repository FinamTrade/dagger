package coffee;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import dagger.ObjectGraph;

public class CoffeeApp implements EntryPoint {

  @Override
  public void onModuleLoad() {
    final ObjectGraph objectGraph = ObjectGraph.create(new DripCoffeeModule());
    final CoffeeMaker coffeeMaker = objectGraph.get(CoffeeMaker.class);
    Button btn = Button.wrap(Document.get().getElementById("coffeeMaker"));

    btn.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        coffeeMaker.brew();
      }
    });
    btn.setEnabled(true);
  }
}
