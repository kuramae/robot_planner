package planner;

import com.google.auto.value.AutoValue;
import knowledge.Predicate;

import java.util.List;

@AutoValue
public abstract class Plan {
    public abstract List<Predicate> getSequence();

    public static Builder builder() {
        return new AutoValue_Plan.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setSequence(List<Predicate> newSequence);

        public abstract Plan build();
    }
}
