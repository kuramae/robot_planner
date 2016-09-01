package planner.graphplan;

import knowledge.Fact;
import knowledge.Problem;
import planner.Plan;
import planner.Planner;

import java.util.Optional;

public class GraphplanPlanner implements Planner {

    private final int maxDepth;

    public GraphplanPlanner(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public Optional<Plan> plan(Fact goal, Problem problem) {
        Graph graph = Graph.fromInitialState(problem.getInitialState());
        int depth = 1;
        while(depth < maxDepth) {
            graph = graph.extendByOneLevel(problem);
            // At each level we check if we have a plan and return if that's the case
            Optional<Plan> plan = graph.extractPlan(goal);
            if (plan.isPresent()) {
                return plan;
            }
            depth++;
        }
        return Optional.empty();
    }
}
