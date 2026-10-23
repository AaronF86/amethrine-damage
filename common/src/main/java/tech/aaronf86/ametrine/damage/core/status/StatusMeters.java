package tech.aaronf86.ametrine.damage.core.status;

import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import tech.aaronf86.ametrine.damage.api.status.StatusProcContext;
import tech.aaronf86.ametrine.damage.api.status.StatusType;

public final class StatusMeters {

  private final Reference2FloatOpenHashMap<StatusType> buildup = new Reference2FloatOpenHashMap<>();
  private List<StatusProcContext> pending = new ArrayList<>(0);

  public float get(StatusType status) {

    return buildup.getFloat(status);
  }

  public void set(StatusType status, float amount) {

    if (amount > 0) {
      buildup.put(status, amount);
    } else {
      buildup.removeFloat(status);
    }
  }

  public boolean add(StatusType status, float amount, float resistance) {

    float total = get(status) + amount;
    if (total >= resistance) {
      buildup.removeFloat(status);
      return true;
    }
    set(status, total);
    return false;
  }

  public void decay(float seconds) {

    if (buildup.isEmpty()) {
      return;
    }
    for (StatusType status : buildup.keySet().toArray(StatusType[]::new)) {
      set(status, buildup.getFloat(status) - status.decayPerSecond() * seconds);
    }
  }

  public void queue(StatusProcContext proc) {

    pending.add(proc);
  }

  public List<StatusProcContext> drain() {

    if (pending.isEmpty()) {
      return List.of();
    }
    List<StatusProcContext> taken = pending;
    pending = new ArrayList<>(0);
    return taken;
  }
}
