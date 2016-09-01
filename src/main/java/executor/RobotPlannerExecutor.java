package executor;

import knowledge.State;
import planner.Plan;

import java.util.ArrayDeque;
import java.util.Deque;

public class RobotPlannerExecutor implements Executor {
    private State expectedWorldState;
    private Deque<Plan> executedPlans = new ArrayDeque<>();
    private Deque<Plan> undoPlans = new ArrayDeque<>();

    static RobotPlannerExecutor withInitialState(State initialState) {
        return new RobotPlannerExecutor(initialState);
    }

    private RobotPlannerExecutor(State expectedWorldState) {
        this.expectedWorldState = expectedWorldState;
    }

    public State getExpectedWorldState() {
        return this.expectedWorldState;
    }

    @Override
    public void executePlan(Plan plan) {

    }

    @Override
    public Plan undoMove() {
        return null;
    }

    @Override
    public Plan redoMove() {
        return null;
    }
}
