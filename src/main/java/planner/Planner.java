package planner;

import knowledge.Fact;
import knowledge.Problem;

import java.util.Optional;

public interface Planner {
    Optional<Plan> plan(Fact goal, Problem problem);
}
