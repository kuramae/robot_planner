package planner.graphplan;

import knowledge.Fact;
import knowledge.Problem;
import planner.Plan;
import planner.Planner;

import java.util.Optional;

import static planner.strips.StripsPlanner.MAX_DEPTH;

public class GraphplanPlanner implements Planner {
    @Override
    public Optional<Plan> plan(Fact goal, Problem problem) {
        Graph graph = Graph.fromInitialState(problem.getInitialState());
        int depth = 1;
        while(depth < MAX_DEPTH) {
            graph = graph.extendByOneLevel(problem);
            Optional<Plan> plan = graph.extractPlan(goal);
            if (plan.isPresent()) {
                return plan;
            }
            depth++;
        }
        return Optional.empty();
    }
}
