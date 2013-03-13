package coffee;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import dagger.DaggerEntryPoint;

import javax.inject.Inject;

public class CoffeeApp extends DaggerEntryPoint {

  @Inject
  CoffeeMaker coffeeMaker;

  @Override
  protected void onLoad() {
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
