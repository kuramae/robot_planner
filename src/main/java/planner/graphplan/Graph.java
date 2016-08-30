package planner.graphplan;

import com.google.auto.value.AutoValue;
import com.google.common.collect.*;
import knowledge.*;
import planner.Plan;

import java.util.*;
import java.util.stream.Collectors;

@AutoValue
public abstract class Graph {
    // prop0 |LEVEL 0|-> act0 -> prop1 |LEVEL 1|-> act1 ...
    public abstract ImmutableList<State> getPropositionLevels();
    public abstract ImmutableList<Set<Action>> getActionLevels();
    // Level 0 connects prop0 and act0, level 1 connects prop1 and act1
    public abstract ImmutableList<Multimap<Fact, Action>> getPropositionConnections();
    // Level 0 connects act0 and prop1, level 1 connects act1 and prop2
    public abstract ImmutableList<Multimap<Action, Fact>> getActionConnections();
    public abstract ImmutableList<Multimap<Action, Action>> getActionMutexes();
    public abstract ImmutableList<Multimap<Fact, Fact>> getPropositionMutexes();

    public static Graph fromInitialState(State initialState) {
        return builder()
                .setPropositionLevels(ImmutableList.of(initialState))
                .setActionLevels(ImmutableList.of())
                .setPropositionConnections(ImmutableList.of())
                .setActionConnections(ImmutableList.of())
                .setActionMutexes(ImmutableList.of())
                .setPropositionMutexes(ImmutableList.of())
                .build();
    }

    public Graph extendByOneLevel(Problem problem) {
        HashSet<Action> satisfiedActions = new HashSet<>();
        Multimap<Fact, Action> newPropToActConnections = HashMultimap.create();
        for(Action a : problem.getActions()) {
            for (Action instA : a.instantiate(problem)) {
                if (this.lastProposition().satisfiesAction(instA)) {
                    satisfiedActions.add(instA);
                    for (Fact pre : this.lastProposition().preconditionSupportSet(instA)) {
                        newPropToActConnections.put(pre, instA);
                    }
                }
             }
        }
        HashSet<Fact> conditionsAtNewLevel = new HashSet<>();
        Multimap<Action, Fact> newActToPrepConnections = HashMultimap.create();
        for(Action instA : satisfiedActions) {
            conditionsAtNewLevel.addAll(instA.getEffects());
            for(Fact effect : instA.getEffects()) {
                newActToPrepConnections.put(instA, effect);
            }
        }
        conditionsAtNewLevel.addAll(lastProposition().getState());
        for (Fact p : lastProposition().getState()) {
            newActToPrepConnections.put(artificialAction(p), p);
        }
        Multimap<Action, Action> actionMutexesForLastLevel = establishActionMutexesForLastLevel(this);
        return builder()
                .setActionConnections(ImmutableList.<Multimap<Action, Fact>>builder().addAll(this.getActionConnections()).add(newActToPrepConnections).build())
                .setPropositionConnections(ImmutableList.<Multimap<Fact, Action>>builder().addAll(this.getPropositionConnections()).add(newPropToActConnections).build())
                .setActionLevels(ImmutableList.<Set<Action>>builder().addAll(getActionLevels()).add(satisfiedActions).build())
                .setPropositionLevels(ImmutableList.<State>builder().addAll(getPropositionLevels()).add(State.builder().setState(conditionsAtNewLevel).build()).build())
                .setActionMutexes(ImmutableList.<Multimap<Action, Action>>builder().addAll(getActionMutexes()).add(actionMutexesForLastLevel).build())
                .setPropositionMutexes(ImmutableList.<Multimap<Fact, Fact>>builder().addAll(getPropositionMutexes()).add(establishPropositionMutexesForLastLevel(this, actionMutexesForLastLevel, newActToPrepConnections)).build())
                .build();
    }

    private Multimap<Fact, Fact> establishPropositionMutexesForLastLevel(Graph graph, Multimap<Action, Action> actionMutexesForLastLevel, Multimap<Action, Fact> newActToPrepConnections) {
        Multimap<Fact, Fact> newMutexes = HashMultimap.create();
        State lastProp = graph.lastProposition();
        for(Fact f1 : lastProp.getState()) {
            if (lastProp.getState().contains(f1.flip())) {
                putInMutexInBothDirections(newMutexes, f1, f1.flip());
            }
            for(Fact f2 : lastProp.getState()) {
                if (!f1.equals(f2) && havePairwiseMutuallyExclusiveActions(actionMutexesForLastLevel, newActToPrepConnections, f1, f2)) {
                    putInMutexInBothDirections(newMutexes, f1, f2);
                }
            }
        }
        return newMutexes;
    }

    private boolean havePairwiseMutuallyExclusiveActions(Multimap<Action, Action> actionMutexesForLastLevel, Multimap<Action, Fact> newActToPrepConnections, Fact f1, Fact f2) {
        Multimap<Fact, Action> invertedActToPrepConnections = HashMultimap.create();
        Multimaps.invertFrom(newActToPrepConnections, invertedActToPrepConnections);
        Collection<Action> supportF1 = invertedActToPrepConnections.get(f1);
        Collection<Action> supportF2 = invertedActToPrepConnections.get(f2);
        for(Action a1 : supportF1) {
            for(Action a2 : supportF2) {
                if (!actionMutexesForLastLevel.containsEntry(a1, a2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Multimap<Action, Action> establishActionMutexesForLastLevel(Graph graph) {
        Multimap<Action, Action> newMutexes = HashMultimap.create();
        State lastProp = graph.lastProposition();
        Multimap<Action, Fact> lastActionConnections = graph.getActionConnections().get(graph.getActionConnections().size() - 1);
        actionsWithMutuallyExclusiveEffects(newMutexes, lastProp, lastActionConnections);
        Set<Action> lastActions = graph.getActionLevels().get(graph.getActionLevels().size() - 1);
        actionsWithMutuallyExclusivePreconditions(lastActions, newMutexes, lastProp);
        actionsWithMutuallyExclusiveEffectsAndPreconditions(lastActions, newMutexes, lastProp, lastActionConnections);
        return newMutexes;
    }

    private void actionsWithMutuallyExclusiveEffectsAndPreconditions(Set<Action>  lastActions, Multimap<Action, Action> newMutexes, State lastProp, Multimap<Action, Fact> lastActionConnections) {
        for (Fact fact : lastProp.getState().stream().collect(Collectors.toList())) {
            Multimap<Fact, Action> invertedLastActionConnections =  HashMultimap.create();
            Multimaps.invertFrom(lastActionConnections, invertedLastActionConnections);
            HashSet<Action> actionCausingThisFact = new HashSet<>(invertedLastActionConnections.get(fact));
            for (Action action : lastActions) {
                if (action.getPreconditions().contains(fact)) {
                    actionCausingThisFact.stream().forEach(act2 -> putInMutexInBothDirections(newMutexes, action, act2));
                }
            }
        }
    }

    private void actionsWithMutuallyExclusiveEffects(Multimap<Action, Action> newMutexes, State lastProp, Multimap<Action, Fact> lastActionConnections) {
        for (Fact fact : lastProp.getState().stream().filter(Fact::getSign).collect(Collectors.toList())) {
            if(lastProp.getState().contains(fact.flip())) {
                Multimap<Fact, Action> invertedLastActionConnections =  HashMultimap.create();
                Multimaps.invertFrom(lastActionConnections, invertedLastActionConnections);
                HashSet<Action> positives = new HashSet<>(invertedLastActionConnections.get(fact));
                HashSet<Action> negatives = new HashSet<>(invertedLastActionConnections.get(fact.flip()));
                Set<List<Action>> cartesianProduct = Sets.cartesianProduct(positives, negatives);
                for(List<Action> singleMutex : cartesianProduct) {
                    Action a1 = singleMutex.get(0);
                    Action a2 = singleMutex.get(1);
                    putInMutexInBothDirections(newMutexes, a1, a2);
                }
            }
        }
    }

    private void actionsWithMutuallyExclusivePreconditions(Set<Action> lastActions, Multimap<Action, Action> newMutexes, State lastProp) {
        for(Action a1 : lastActions) {
            for(Action a2 : lastActions) {
                if (!a1.equals(a2)) {
                    if (mutuallyExclusiveFacts(a1.getPreconditions(), a2.getPreconditions())) {
                        putInMutexInBothDirections(newMutexes, a1, a2);
                    }
                }
            }
        }
    }

    private boolean mutuallyExclusiveFacts(Set<Fact> p1, Set<Fact> p2) {
        for (Fact f1 : p1) {
            if (p2.contains(f1.flip())) {
                return true;
            }
        }
        return false;
    }

    private <T> void putInMutexInBothDirections(Multimap<T, T> newMutexes, T a1, T a2) {
        newMutexes.put(a1, a2);
        newMutexes.put(a2, a1);
    }

    protected Action artificialAction(Fact p) {
        return Action.builder().setPredicate(Predicate.parse("keep " + p.toString()))
                .setPreconditions(ImmutableSet.<Fact>of(p))
                .setEffects(ImmutableSet.<Fact>of(p))
                .build();
    }

    protected State lastProposition() {
        return getPropositionLevels().get(level());
    }

    private int level() {
        return getPropositionLevels().size() - 1;
    }

    public static Builder builder() {
        return new AutoValue_Graph.Builder();
    }

    public Optional<Plan> extractPlan(Fact goal) {
        return extractPlan(ImmutableSet.of(goal), level());
    }

    public Optional<Plan> extractPlan(Set<Fact> goal, int level) {
        if(lastProposition().getState().containsAll(goal)) {
            if (inconsistentFacts(goal, level)) {
                return Optional.empty();
            }
            Set<Set<Action>> actionSet = getSupportSetForGoalsAtLevel(goal, level);
            for (Set<Action> support : actionSet) {
                Set<Fact> preconditions = preconditionsForActions(support, level);
                Optional<Plan> plan = extractPlan(preconditions, level - 1);
                if (plan.isPresent()) {
                    LinkedList<Predicate> newList = new LinkedList<>();
                    newList.addAll(plan.get().getSequence());
                    newList.addAll(support.stream().map(a -> a.getPredicate()).collect(Collectors.toSet()));
                    return Optional.of(Plan.builder().setSequence(newList).build());
                }
            }
        }
        return Optional.empty();
    }

    private Set<Fact> preconditionsForActions(Set<Action> support, int level) {
        HashSet<Fact> preconditions = new HashSet<>();
        Multimap<Fact, Action> connections = getPropositionConnections().get(level);
        for(Map.Entry<Fact, Action> entry : connections.entries()) {
            if (support.contains(entry.getValue())) {
                preconditions.add(entry.getKey());
            }
        }
        return preconditions;
    }

    private boolean inconsistentFacts(Set<Fact> goal, int level) {
        for(Fact g1 : goal) {
            for(Fact g2 : goal) {
                if (!g1.equals(g2)) { // TODO GET RID OF SIMMETRY (ALSO CHECK OTHER PLACES)
                    if (getPropositionMutexes().get(level).containsEntry(g1, g2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Set<Set<Action>> getSupportSetForGoalsAtLevel(Set<Fact> goal, int level) {
        List<Set<Action>> actionsForGoals = new LinkedList<>();
        for(Fact partialGoal : goal) {
            Set<Action> actionsForGoal = pickActionsForGoal(partialGoal, level);
            actionsForGoals.add(actionsForGoal);
        }
        Set<List<Action>> cartesianProduct = Sets.cartesianProduct(actionsForGoals);
        return cartesianProduct.stream()
                .filter(setOfAction -> consistentSetOfActions(setOfAction, level))
                .map(HashSet::new)
                .collect(Collectors.toSet());
    }

    private Set<Action> pickActionsForGoal(Fact partialGoal, int level) {
        Multimap<Fact, Action> invertedActToPrepConnections = HashMultimap.create();
        Multimaps.invertFrom(getActionConnections().get(level), invertedActToPrepConnections);
        return new HashSet<>(invertedActToPrepConnections.get(partialGoal));
    }

    private boolean consistentSetOfActions(List<Action> actions, int level) {
        for(Action a1 : actions) {
            for (Action a2 : actions) {
                if (!a1.equals(a2) && getActionMutexes().get(level).get(a1).contains(a2)) {
                    return false;
                }
            }
        }
        return false;
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setPropositionLevels(ImmutableList<State> newPropositionLevels);

        public abstract Builder setActionLevels(ImmutableList<Set<Action>> newActionLevels);

        public abstract Builder setPropositionConnections(ImmutableList<Multimap<Fact, Action>> newPropositionConnections);

        public abstract Builder setActionConnections(ImmutableList<Multimap<Action, Fact>> newActionConnections);

        public abstract Builder setActionMutexes(ImmutableList<Multimap<Action, Action>> newActionMutexes);

        public abstract Builder setPropositionMutexes(ImmutableList<Multimap<Fact, Fact>> newPropositionMutexes);

        public abstract Graph build();
    }
}
