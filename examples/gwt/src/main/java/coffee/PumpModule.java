package coffee;

import dagger.Module;
import dagger.Provides;

@Module(complete = false)
public class PumpModule {
  @Provides Pump providePump(Thermosiphon pump) {
    return pump;
  }
}
