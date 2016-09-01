package planner.graphplan;

import com.google.auto.value.AutoValue;
import com.google.common.collect.*;
import knowledge.*;
import planner.Plan;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph used for the graphplan algorithm. Compared to literature, this one follows the convention
 * that each level has a different index
 * e.g. prop0 |LEVEL 0|-> act0 -> prop1 |LEVEL 1|-> act1 ...
 * It also supports variables in actions (which is pretty cool) if they are declared.
 * This class was hacked together, it needs a few extra abstractions (e.g. use an undirected graph class
 * instead of a multimap). It should also construct each data structure separately.
 */
@AutoValue
public abstract class Graph {

    static final String KEEP_PREDICATE_NAME = "keep";

    public abstract ImmutableList<GraphplanLevel> getLevels();

    /**
     * Initialize graphplan given an initial state
     */
    static Graph fromInitialState(State initialState) {
        return builder()
                .setLevels(ImmutableList.of(GraphplanLevel.initial(initialState)))
                .build();
    }

    public static Builder builder() {
        return new AutoValue_Graph.Builder();
    }

    Optional<Plan> extractPlan(Fact goal) {
        return extractPlan(ImmutableSet.of(goal), level());
    }

    Graph extendByOneLevel(Problem problem) {
        HashSet<Action> nextLevelSatisfiedActions = new HashSet<>();
        ImmutableMultimap<Fact, Action> nextLevelPropositionToActionConnections =
                propositionsToActionConnectionsForNextLevel(problem, nextLevelSatisfiedActions);
        HashSet<Fact> nextLevelConditions = new HashSet<>();
        ImmutableMultimap<Action, Fact> newActToPrepConnections = propositionsForNextLevel(nextLevelSatisfiedActions, nextLevelConditions, problem);
        State state = State.builder().setState(nextLevelConditions).build();
        ImmutableMultimap<Action, Action> actionMutexesForLastLevel = establishActionMutexesForLastLevel(newActToPrepConnections, state);
        GraphplanLevel level = GraphplanLevel.builder()
                .setAction(nextLevelSatisfiedActions)
                .setProposition(state)
                .setActionToPropositionConnections(newActToPrepConnections)
                .setPropositionToActionConnections(nextLevelPropositionToActionConnections)
                .setActionMutexes(actionMutexesForLastLevel)
                .setPropositionMutexes(establishPropositionMutexesForLastLevel(state, actionMutexesForLastLevel, newActToPrepConnections))
                .build();

        return builder()
                .setLevels(ImmutableList.<GraphplanLevel>builder().addAll(this.getLevels()).add(level).build()).build();
    }

    private ImmutableMultimap<Action, Fact> propositionsForNextLevel(HashSet<Action> satisfiedActions, 
                                                                     HashSet<Fact> conditionsAtNewLevel,
                                                                     Problem problem) {
        Multimap<Action, Fact> newActToPrepConnections = HashMultimap.create();
        for(Action instA : satisfiedActions) {
            conditionsAtNewLevel.addAll(instA.getEffects());
            for(Fact effect : instA.getEffects()) {
                newActToPrepConnections.put(instA, effect);
            }
            Set<Fact> constraintEffects = instA.constraintEffects(problem);
            conditionsAtNewLevel.addAll(constraintEffects);
            for(Fact cEffect : constraintEffects) {
                newActToPrepConnections.put(instA, cEffect);
            }
        }
        conditionsAtNewLevel.addAll(lastLevel().getProposition().getState());
        for (Fact p : lastLevel().getProposition().getState()) {
            Action key = artificialAction(p);
            newActToPrepConnections.put(key, p);
        }
        return ImmutableMultimap.copyOf(newActToPrepConnections);
    }

    private ImmutableMultimap<Fact, Action> propositionsToActionConnectionsForNextLevel(Problem problem, HashSet<Action> satisfiedActions) {
        ImmutableMultimap.Builder<Fact, Action> newPropToActConnectionsBuilder = ImmutableMultimap.builder();
        for(Action a : problem.getActions()) {
            for (Action instA : a.instantiate(problem)) {
                if (this.lastLevel().getProposition().satisfiesAction(instA)) {
                    satisfiedActions.add(instA);
                    for (Fact pre : this.lastLevel().getProposition().preconditionSupportSet(instA)) {
                        newPropToActConnectionsBuilder.put(pre, instA);
                    }
                }
             }
        }
        for (Fact p : lastLevel().getProposition().getState()) {
            Action key = artificialAction(p);
            newPropToActConnectionsBuilder.put(p, key);
        }
        return newPropToActConnectionsBuilder.build();
    }

    private GraphplanLevel lastLevel() {
        return getLevels().get(level());
    }

    private ImmutableMultimap<Fact, Fact> establishPropositionMutexesForLastLevel(State lastProp, Multimap<Action, Action> actionMutexesForLastLevel, Multimap<Action, Fact> newActToPrepConnections) {
        Multimap<Fact, Fact> newMutexes = HashMultimap.create();
        for(Fact f1 : lastProp.getState()) {
            if (lastProp.getState().contains(f1.flip())) {
                putInMutexInBothDirections(newMutexes, f1, f1.flip());
            }
            for(Fact f2 : lastProp.getState()) {
                if (!f1.equals(f2) && haveAllPairwiseMutuallyExclusiveActions(actionMutexesForLastLevel, newActToPrepConnections, f1, f2)) {
                    putInMutexInBothDirections(newMutexes, f1, f2);
                }
            }
            // Variable uniqueness
        }
        return ImmutableMultimap.copyOf(newMutexes);
    }

    private boolean haveAllPairwiseMutuallyExclusiveActions(Multimap<Action, Action> actionMutexesForLastLevel, Multimap<Action, Fact> newActToPrepConnections, Fact f1, Fact f2) {
        Multimap<Fact, Action> invertedActToPrepConnections = HashMultimap.create();
        Multimaps.invertFrom(newActToPrepConnections, invertedActToPrepConnections);
        Collection<Action> supportF1 = invertedActToPrepConnections.get(f1);
        Collection<Action> supportF2 = invertedActToPrepConnections.get(f2);
        for(Action a1 : supportF1) {
            for(Action a2 : supportF2) {
                if (!actionMutexesForLastLevel.containsEntry(a1, a2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private ImmutableMultimap<Action, Action> establishActionMutexesForLastLevel(ImmutableMultimap<Action, Fact> newActToPrepConnections, State state) {
        Multimap<Action, Action> newMutexes = HashMultimap.create();
        State lastProp = lastLevel().getProposition();
        actionsWithMutuallyExclusiveEffects(newMutexes, state, newActToPrepConnections);
        Set<Action> lastActions = lastLevel().getAction();
        actionsWithMutuallyExclusivePreconditions(lastActions, newMutexes, lastProp);
        actionsWithMutuallyExclusiveEffectsAndPreconditions(lastActions, newMutexes, lastProp, newActToPrepConnections);
        return ImmutableMultimap.copyOf(newMutexes);
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

    private Action artificialAction(Fact p) {
        return Action.builder().setPredicate(Predicate.parse("keep " + p.toString()))
                .setPreconditions(ImmutableSet.of(p))
                .setEffects(ImmutableSet.of(p))
                .build();
    }

    private int level() {
        return getLevels().size() - 1;
    }

    private Optional<Plan> extractPlan(Set<Fact> goal, int level) {
        if (inconsistentFacts(goal)) {
            return Optional.empty();
        }
        if(level > 0 && getLevels().get(level).getProposition().getState().containsAll(goal)) {
            Set<Set<Action>> actionSet = getSupportSetForGoalsAtLevel(goal, level);
            for (Set<Action> support : actionSet) {
                Set<Fact> preconditions = preconditionsForActions(support, level);
                Optional<Plan> plan = extractPlan(preconditions, level - 1);
                if (plan.isPresent()) {
                    LinkedList<Predicate> newList = new LinkedList<>();
                    newList.addAll(plan.get().getSequence());
                    newList.addAll(support.stream().map(a -> a.getPredicate()).filter(a -> !a.getName().equals(KEEP_PREDICATE_NAME)).collect(Collectors.toSet()));
                    return Optional.of(Plan.builder().setSequence(newList).build());
                }
            }
        } else if (level == 0) {
            return Optional.of(Plan.builder().setSequence(ImmutableList.of()).build());
        }
        return Optional.empty();
    }

    private Set<Fact> preconditionsForActions(Set<Action> support, int level) {
        HashSet<Fact> preconditions = new HashSet<>();
        Multimap<Fact, Action> connections = getLevels().get(level).getPropositionToActionConnections();
        for(Map.Entry<Fact, Action> entry : connections.entries()) {
            if (support.contains(entry.getValue())) {
                preconditions.add(entry.getKey());
            }
        }
        return preconditions;
    }

    private boolean inconsistentFacts(Set<Fact> goal) {
        for(Fact g1 : goal) {
            for(Fact g2 : goal) {
                if (!g1.equals(g2)) { // TODO GET RID OF SIMMETRY (ALSO CHECK OTHER PLACES)
                    if (lastLevel().getPropositionMutexes().containsEntry(g1, g2)) {
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
        Multimaps.invertFrom(getLevels().get(level).getActionToPropositionConnections(), invertedActToPrepConnections);
        return new HashSet<>(invertedActToPrepConnections.get(partialGoal));
    }

    private boolean consistentSetOfActions(List<Action> actions, int level) {
        for(Action a1 : actions) {
            for (Action a2 : actions) {
                if (!a1.equals(a2) && getLevels().get(level).getActionMutexes().get(a1).contains(a2)) {
                    return false;
                }
            }
        }
        return true;
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setLevels(ImmutableList<GraphplanLevel> newLevels);

        public abstract Graph build();
    }
}
