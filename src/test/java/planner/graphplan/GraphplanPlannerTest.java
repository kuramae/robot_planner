package planner.graphplan;

import planner.Planner;
import planner.PlannerTest;

public class GraphplanPlannerTest extends PlannerTest {
    @Override
    public Planner getPlanner() {
        return new GraphplanPlanner();
    }
}
