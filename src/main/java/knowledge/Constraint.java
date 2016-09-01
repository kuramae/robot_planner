package knowledge;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static knowledge.Constants.ACTION_SEPARATOR;
import static knowledge.Constants.FACT_SEPARATOR;

@AutoValue
public abstract class Constraint {
    public abstract Predicate getAntecendent();
    public abstract Set<Fact> getConsequents();

    public static Constraint parse(String s) {
        String[] splitAction = s.split(ACTION_SEPARATOR);
        String pre = splitAction[0];
        String[] post = splitAction[1].trim().split(FACT_SEPARATOR);
        Set<Fact> effects = new HashSet<Fact>();
        for(String p : post) {
            if (p.trim().length() > 0 ) {
                effects.add(Fact.parse(p.trim()));
            }
        }
        return Constraint.builder()
                .setAntecendent(Predicate.parse(pre))
                .setConsequents(effects).build();
    }

    private Set<String> allVars() {
        return Sets.union(
                getAntecendent().allVars(),
                getConsequents().stream()
                        .map(Fact::allVars)
                        .reduce(Collections.emptySet(), Sets::union));
    }

    public Set<Constraint> instantiate(Problem problem) {
        Set<String> vars = allVars();
        Set<Unification> unifications = problem.instantiateVariables(vars);
        return unifications.stream().map(this::applyUnification).filter(Constraint::wellFormed).collect(Collectors.toSet());
    }


    public Constraint applyUnification(Unification unification) {
        return builder()
                .setAntecendent(getAntecendent().applyUnification(unification))
                .setConsequents(getConsequents().stream().map(fact -> fact.applyUnification(unification)).collect(Collectors.toSet()))
                .build();
    }

    public boolean wellFormed() {
        return getConsequents().stream().allMatch(Fact::wellFormed)
                && getAntecendent().wellFormed()
                && !getConsequents().contains(Fact.builder().setPredicate(getAntecendent()).setSign(true).build())
                && !getConsequents().contains(Fact.builder().setPredicate(getAntecendent()).setSign(false).build());

    }

    public static Builder builder() {
        return new AutoValue_Constraint.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setAntecendent(Predicate newAntecendent);

        public abstract Builder setConsequents(Set<Fact> newConsequents);

        public abstract Constraint build();
    }
}
