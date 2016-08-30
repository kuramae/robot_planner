package knowledge;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static knowledge.Constants.*;

@AutoValue
public abstract class Action {
    public abstract Predicate getPredicate();

    public abstract Set<Fact> getPreconditions();

    public abstract Set<Fact> getEffects();

    public static Builder builder() {
        return new AutoValue_Action.Builder();
    }

    public static Action parse(String action) {
        String[] splitFullAction = action.split(NAME_SEPARATOR);
        Predicate predicate = Predicate.parse(splitFullAction[0]);
        String[] splitAction = splitFullAction[1].split(ACTION_SEPARATOR);
        String[] pre = splitAction[0].trim().split(FACT_SEPARATOR);
        String[] post = splitAction[1].trim().split(FACT_SEPARATOR);
        Set<Fact> preconditions = new HashSet<Fact>();
        for(String s : pre) {
            if (s.trim().length() > 0 ) {
                preconditions.add(Fact.parse(s));
            }
        }
        Set<Fact> effects = new HashSet<Fact>();
        for(String s : post) {
            if (s.trim().length() > 0 ) {
                effects.add(Fact.parse(s));
            }
        }
        return Action.builder().setPreconditions(preconditions).setEffects(effects).setPredicate(predicate).build();
    }

    public Optional<Action> match(Fact currentGoal) {
        if (currentGoal.getSign()) {
            Optional<Unification> actionUnification = getEffects().stream().filter(effect -> effect.getSign())
                    .map(effect -> effect.unify(currentGoal))
                    .filter(unification -> unification.isValid())
                    // TODO: Ok, here there's another nondeterministic point
                    // Assuming an action effects can't include a goal in more than one way, this is fine
                    // otherwise we need to backtrack on this too
                    .findFirst();
            if (actionUnification.isPresent()) {
                return Optional.of(applyUnification(actionUnification.get()));

            }
        }
        return Optional.empty();
    }

    public boolean wellFormed() {
        return getPreconditions().stream().allMatch(Fact::wellFormed) &&
                getEffects().stream().allMatch(Fact::wellFormed) && getPredicate().wellFormed();
    }

    private Action applyUnification(Unification unification) {
        return builder()
            .setEffects(getEffects().stream().map(fact -> fact.applyUnification(unification)).collect(Collectors.toSet()))
            .setPreconditions(getPreconditions().stream().map(fact -> fact.applyUnification(unification)).collect(Collectors.toSet()))
            .setPredicate(getPredicate().applyUnification(unification))
            .build();
    }

    @Override
    public String toString() {
        return getPredicate().toString();
    }

    public Set<Action> instantiate(Problem problem) {
        Set<String> vars = allVars();
        Set<Unification> unifications = problem.instantiateVariables(vars);
        return unifications.stream().map(this::applyUnification).collect(Collectors.toSet());
    }

    private Set<String> allVars() {
        return Sets.union(Sets.union(
                getPredicate().allVars(),
                getPreconditions().stream()
                        .map(Fact::allVars)
                        .reduce(Collections.emptySet(), Sets::union)),
                getEffects().stream()
                        .map(Fact::allVars)
                        .reduce(Collections.emptySet(), Sets::union));
    }

    public Set<Fact> getPositiveEffects() {
        return getEffects().stream().filter(Fact::getSign).collect(Collectors.toSet());
    }

    public Set<Fact> getNegativeEffects() {
        return getEffects().stream().filter(a -> !a.getSign()).map(Fact::flip).collect(Collectors.toSet());
    }

    public Set<Fact> getPositivePreconditions() {
        return getPreconditions().stream().filter(Fact::getSign).collect(Collectors.toSet());
    }

    public Set<Fact> getNegativePreconditions() {
        return getPreconditions().stream().filter(a -> !a.getSign()).map(Fact::flip).collect(Collectors.toSet());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setPredicate(Predicate newPredicate);

        public abstract Builder setPreconditions(Set<Fact> newPreconditions);

        public abstract Builder setEffects(Set<Fact> newEffects);

        public abstract Action build();
    }
}
