package planner.strips;

import planner.Planner;
import planner.PlannerTest;

public class StripsPlannerTest extends PlannerTest {
    @Override
    public Planner getPlanner() {
        return new StripsPlanner();
    }

}