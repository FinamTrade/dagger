package coffee;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import dagger.EntryPointInjector;

import javax.inject.Inject;

public class CoffeeApp implements EntryPoint {

  @Inject
  CoffeeMaker coffeeMaker;

  @Override
  public void onModuleLoad() {
    EntryPointInjector.inject(this);

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
