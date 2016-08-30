package knowledge;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.stream.Collectors;

@AutoValue
public abstract class Problem {
    public abstract Set<Action> getActions();

    public abstract State getInitialState();

    public abstract Set<TypeDeclaration> getTypes();

    public static Builder builder() {
        return new AutoValue_Problem.Builder();
    }

    // Returns a set of unified actions (so variables are instantiated)
    public Set<Action> matchingActionsFor(Fact currentGoal) {
        return getActions().stream().map(action -> action.match(currentGoal))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public Set<Unification> instantiateVariable(String var) {
        Set<String> destinations = getTypes().stream()
                .filter(typeDeclaration -> typeDeclaration.getSource().contains(var))
                .map(TypeDeclaration::getDestination)
                .findFirst()
                .orElse(Collections.emptySet());
        HashSet<Unification> output = new HashSet<>();
        for(String destination : destinations) {
            output.add(Unification.builder().setValid(true).setSubstitutions(ImmutableMap.of(var, destination)).build());
        }
        return output;
    }

    Set<Unification> instantiateVariables(Set<String> vars) {
        List<Set<Unification>> instantiatedArgs = vars.stream().map(this::instantiateVariable).collect(Collectors.toList());
        // e. g. instantiatedArgs = [ {X/s1, X/s2}, {Y/s1, Y/s2} ]
        Set<List<Unification>> allInstantiations = Sets.cartesianProduct(instantiatedArgs);
        // e. g. allInstantiations = { [X/s1, Y/s1], [X/s1, Y/s2] }
        return allInstantiations.stream().map(
                instantiation -> instantiation.stream().reduce(Unification.empty(), Unification::merge)
                ).collect(Collectors.toSet());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setActions(Set<Action> newActions);

        public abstract Builder setInitialState(State newInitialState);

        public abstract Builder setTypes(Set<TypeDeclaration> newTypes);

        public abstract Problem build();
    }
}
