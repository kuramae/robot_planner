package planner.strips;

import autovalue.shaded.com.google.common.common.collect.Collections2;
import autovalue.shaded.com.google.common.common.collect.Sets;
import knowledge.*;
import planner.Plan;
import planner.Planner;

import java.util.*;


public class StripsPlanner implements Planner {

    public static final int MAX_DEPTH = 30;

    public StripsPlanner() {

    }

    public Optional<Plan> plan(Fact goal, Problem problem) {
        Deque<StackElement> goalStack = new ArrayDeque<>();
        goalStack.add(new StackElement(goal));

        List<Predicate> plan = new ArrayList<>();

        State currentState = problem.getInitialState();

        // Recursive
        Optional<List<Predicate>> solution = stripStep(goalStack, currentState, problem, plan, 0);
        return Optional.ofNullable(solution.isPresent() ? Plan.builder().setSequence(solution.get()).build() : null);
    }

    private Optional<List<Predicate>> stripStep(Deque<StackElement> goalStack,
                                                State currentState,
                                                Problem problem,
                                                List<Predicate> plan,
                                                int depth) {
        if (depth > MAX_DEPTH) {
            return Optional.empty();
        }
        int nextDepth = depth + 1;
        if (!goalStack.isEmpty()) {
            StackElement currentGoal = goalStack.pop();
            debug(goalStack, depth, currentGoal);
            if (currentGoal.type.equals(StackElementType.GOAL)) {
                Fact goal = currentGoal.element;
                if (currentState.satisfies(goal)) {
                    //System.out.format("%s: goal %s matched by state %s\n", depth, currentGoal, currentState);
                    Deque<StackElement> newStack = new ArrayDeque<>(goalStack);
                    Optional<List<Predicate>> solution = stripStep(newStack, currentState, problem, plan, nextDepth);
                    if (solution.isPresent()) {
                        return solution;
                    }
                } else {
                    // System.out.format("%s: goal %s not matched by state\n", depth, currentGoal);
                    Set<Action> matchingActions = problem.matchingActionsFor(goal);
                    // TODO: Non determinism
                    for (Action a : matchingActions) {
                        for (Action instantiatedAction : a.instantiate(problem)) {
                            if (!instantiatedAction.wellFormed()) {
                                continue;
                            }
                            // Trying all orders to avoid Sussman anomaly
                            // Set<Deque<StackElement>> newStacks = newStackArrangementsWithAction(goalStack, instantiatedAction);
                            Deque<StackElement> newStack = newStackWithAction(goalStack, instantiatedAction);
                            //for (Deque<StackElement> stack : newStacks) {
                                Optional<List<Predicate>> solution = stripStep(newStack, currentState, problem, plan, nextDepth);
                                if (solution.isPresent()) {
                                    return solution;
                                }
                            //}
                        }
                    }
                }
            } else {
                boolean satisfiedAction = currentState.satisfiesAction(currentGoal.action);
                if(satisfiedAction) {
                    State newState = useAction(currentGoal.action, currentState);
                    List<Predicate> newPlan = addToPlan(currentGoal.action, plan);
                    return stripStep(goalStack, newState, problem, newPlan, nextDepth);
                } else {
                    System.out.format("%s: Action %s not satisfied! State is %s\n", depth, currentGoal.action, currentState);
                }
            }
        } else {
            // We have a solution, the goal stack is empty!
            return Optional.of(plan);
        }
        return Optional.empty();
    }

    private void debug(Deque<StackElement> goalStack, int depth, StackElement currentGoal) {
        if (goalStack.size() > 6) return;
        System.out.format("%s: Proving goal %s\n", depth, currentGoal);
        System.out.format("%s: stack ", depth);
        for(StackElement e : goalStack) {
            System.out.format("%s, " , e);
        }
        System.out.println("");
    }

    private Deque<StackElement> newStackWithAction(Deque<StackElement> goalStack, Action instantiatedAction) {
        Set<Fact> preconditions = instantiatedAction.getPreconditions();
        Deque<StackElement> newStack = new ArrayDeque<>(goalStack);
        newStack.push(
                new StackElement(instantiatedAction));
        for(Fact f : preconditions) {
            newStack.push(new StackElement(f));
        }
        return newStack;
    }

    private Set<Deque<StackElement>> newStackArrangementsWithAction(Deque<StackElement> goalStack, Action instantiatedAction) {
        Set<Fact> preconditions = instantiatedAction.getPreconditions();
        Collection<List<Fact>> permutations = Collections2.permutations(preconditions);
        Set<Deque<StackElement>> out = new HashSet<>();
        for (List<Fact> orderedPre : permutations) {
            Deque<StackElement> newStack = new ArrayDeque<>(goalStack);
            newStack.push(
                    new StackElement(instantiatedAction));
            for(Fact f : orderedPre) {
                newStack.push(new StackElement(f));
            }
            out.add(newStack);
        }
        return out;
    }

    private List<Predicate> addToPlan(Action action, List<Predicate> plan) {
        ArrayList<Predicate> newPlan = new ArrayList<>();
        newPlan.addAll(plan);
        newPlan.add(action.getPredicate());
        return newPlan;
    }

    private State useAction(Action action, State plan) {
        Set<Fact> negativeEffects = action.getNegativeEffects();
        Set<Fact> positiveEffects = action.getPositiveEffects();
        Sets.SetView<Fact> difference = Sets.difference(
                plan.getState(),
                negativeEffects);

        Sets.SetView<Fact> union = Sets.union(
                difference,
                positiveEffects);
        return State.builder().setState(union.immutableCopy()).build();
    }

    private class StackElement {
        final StackElementType type;
        final Fact element;
        final Action action;

        public StackElement(Fact element) {
            this.type = StackElementType.GOAL;
            this.element = element;
            this.action = null;
        }

        public StackElement(Action action) {
            this.type = StackElementType.ACTION;
            this.action = action;
            this.element = null;
        }

        @Override
        public String toString() {
            return type + ": " + (type.equals(StackElementType.ACTION) ? action.getPredicate() : element.getPredicate());
        }
    }

    private enum StackElementType {
        ACTION, GOAL;
    }
}
