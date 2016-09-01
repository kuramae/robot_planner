package knowledge;

import autovalue.shaded.com.google.common.common.collect.ImmutableMap;
import com.google.auto.value.AutoValue;

import java.util.*;
import java.util.stream.Collectors;

@AutoValue
public abstract class Predicate {
    public abstract String getName();

    public abstract List<String> getArguments();

    public static Builder builder() {
        return new AutoValue_Predicate.Builder();
    }

    public static Predicate parse(String predicate) {
        String[] split = predicate.trim().split(" ");
        List<String> arguments = Arrays.asList(split).subList(1, split.length);
        return Predicate.builder().setArguments(arguments).setName(split[0].trim()).build();
    }

    Unification unify(Predicate otherPredicate) {
        Unification.Builder builder = Unification.builder();

        if (!getName().equals(otherPredicate.getName()) || getArguments().size() != otherPredicate.getArguments().size()) {
            return builder.setValid(false).setSubstitutions(Collections.emptyMap()).build();
        } else {
            ImmutableMap.Builder<String, String> subs = ImmutableMap.builder();
            for (int i = 0; i < getArguments().size(); i++) {
                Unification varUnification = argumentUnify(getArguments().get(i), otherPredicate.getArguments().get(i));
                if (!varUnification.isValid()) {
                    return builder.setValid(false).setSubstitutions(Collections.emptyMap()).build();
                } else {
                    subs.putAll(varUnification.getSubstitutions());
                }
            }
            return builder.setValid(true).setSubstitutions(subs.build()).build();
        }
    }

    Set<Unification> instantiate(Problem problem) {
        // e.g. pr X Y z
        Set<String> vars = allVars();
        return problem.instantiateVariables(vars);
    }

    Predicate applyUnification(Unification unification) {
        return Predicate.builder()
                .setName(getName())
                .setArguments(getArguments().stream().map(s -> applyUnificationToArgument(s, unification)).collect(Collectors.toList()))
                .build();
    }

    Set<String> allVars() {
        Set<String> vars = new HashSet<>();
        for(String arg : getArguments()) {
            if (isVariable(arg)) {
                vars.add(arg);
            }
        }
        return vars;
    }

    private static Unification argumentUnify(String arg1, String arg2) {
        Unification.Builder builder = Unification.builder();
        if (isVariable(arg1)) {
            return builder.setSubstitutions(ImmutableMap.of(arg1, arg2)).setValid(true).build();
        } else {
            return builder.setSubstitutions(Collections.emptyMap()).setValid(arg1.equals(arg2)).build();
        }
    }

    private static boolean isVariable(String arg) {
        return Character.isUpperCase(arg.charAt(0));
    }


    private String applyUnificationToArgument(String input, Unification unification) {
        String change = unification.getSubstitutions().get(input);
        return change != null ? change : input;
    }

    @Override
    public String toString() {
        return getName() + (getArguments().isEmpty() ? "" : " " + String.join(" ", getArguments()));
    }

    public boolean wellFormed() {
        Set<String> seen = new HashSet<>();
        for(String arg : getArguments()) {
            if (seen.contains(arg)) {
                return false;
            } else {
                seen.add(arg);
            }
        }
        return true;
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setName(String newName);

        public abstract Builder setArguments(List<String> newArguments);

        public abstract Predicate build();
    }
}
