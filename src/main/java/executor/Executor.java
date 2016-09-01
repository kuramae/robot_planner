package executor;

import knowledge.State;
import planner.Plan;

public interface Executor {
    /**
     * It returns the current view of the world of the executor
     * @return State of the executor
     */
    State getExpectedWorldState();

    /**
     * It executes the given plan and updates the internal state
     * @param plan to execute
     */
    void executePlan(Plan plan);

    /**
     * Undoes the last move.
     * @return Plan containing each move that was actually performed by the robot to achieve the undo.
     * If the undo is not performed the plan is empty
     */
    Plan undoMove();

    /**
     * Redoes the last ondone move.
     * @return Plan containing each move that was actually performed by the robot to achieve the redo.
     * If the redo is not performed the plan is empty
     */
    Plan redoMove();
}
