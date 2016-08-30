package knowledge;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class State {
    public abstract Set<Fact> getState();

    public static Builder builder() {
        return new AutoValue_State.Builder();
    }

    public boolean satisfies(Fact currentGoal) {
        return getState().stream().anyMatch(fact -> currentGoal.unify(fact).isValid());
    }

    public boolean satisfiesAction(Action currentGoal) {
        return getState().containsAll(currentGoal.getPositivePreconditions()) &&
                currentGoal.getNegativePreconditions().stream().noneMatch(p -> getState().contains(p));
    }

    public Set<Fact> preconditionSupportSet(Action currentGoal) {
        return Sets.intersection(getState(), currentGoal.getPositivePreconditions());
    }

    @Override
    public String toString() {
        return getState().toString();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setState(Set<Fact> newState);

        public abstract State build();
    }
}
