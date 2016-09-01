package planner.graphplan;

import planner.Planner;
import planner.PlannerTest;

public class GraphplanPlannerTest extends PlannerTest {
    public static final int MAX_DEPTH = 15;

    @Override
    public Planner getPlanner() {
        return new GraphplanPlanner(MAX_DEPTH);
    }
}
