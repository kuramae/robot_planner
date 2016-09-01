package planner.graphplan;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMultimap;
import knowledge.Action;
import knowledge.Fact;
import knowledge.State;

import java.util.Collections;
import java.util.Set;

@AutoValue
public abstract class GraphplanLevel {
    // prop0 |LEVEL 0|-> act0 -> prop1 |LEVEL 1|-> act1 ...
    public abstract State getProposition();
    public abstract Set<Action> getAction();
    // Level 0 connects prop0 and act0, level 1 connects prop1 and act1
    // Coming from the previous level
    public abstract ImmutableMultimap<Fact, Action> getPropositionToActionConnections();
    // Level 0 connects act0 and prop1, level 1 connects act1 and prop2
    public abstract ImmutableMultimap<Action, Fact> getActionToPropositionConnections();
    public abstract ImmutableMultimap<Action, Action> getActionMutexes();
    public abstract ImmutableMultimap<Fact, Fact> getPropositionMutexes();

    public static Builder builder() {
        return new AutoValue_GraphplanLevel.Builder();
    }

    static GraphplanLevel initial(State initialState) {
            return builder()
                .setProposition(initialState)
                .setAction(Collections.emptySet())
                .setPropositionToActionConnections(ImmutableMultimap.of())
                .setActionToPropositionConnections(ImmutableMultimap.of())
                .setActionMutexes(ImmutableMultimap.of())
                .setPropositionMutexes(ImmutableMultimap.of())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setProposition(State newProposition);

        public abstract Builder setAction(Set<Action> newAction);

        public abstract Builder setPropositionToActionConnections(ImmutableMultimap<Fact, Action> newPropositionToActionConnections);

        public abstract Builder setActionToPropositionConnections(ImmutableMultimap<Action, Fact> newActionToPropositionConnections);

        public abstract Builder setActionMutexes(ImmutableMultimap<Action, Action> newActionMutexes);

        public abstract Builder setPropositionMutexes(ImmutableMultimap<Fact, Fact> newPropositionMutexes);

        public abstract GraphplanLevel build();
    }
}
