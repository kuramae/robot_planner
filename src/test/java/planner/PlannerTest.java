package planner;

import autovalue.shaded.com.google.common.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import knowledge.*;
import org.junit.Ignore;
import org.junit.Test;
import planner.strips.StripsPlanner;

import java.util.Optional;

import static org.assertj.core.api.StrictAssertions.assertThat;

public abstract class PlannerTest {
    public abstract Planner getPlanner();

    @Test
    public void testOntableGoal() {
        Problem problem = Problem.builder()
                .setActions(
                        ImmutableSet.of(
                                Action.parse("stack X Y: clear Y, holding X -> handempty, on X Y, clear X, " +
                                        "not holding X, not clear Y"),
                                Action.parse("pickup X: ontable X, clear X, handempty -> holding X, " +
                                        "not ontable X, not clear X, not handempty X")
                        ))
                .setTypes(ImmutableSet.of(
                        TypeDeclaration.parse("X, Y: s3, s5")
                ))
                .setInitialState(
                        State.builder().setState(ImmutableSet.of(
                                Fact.parse("clear s3"),
                                Fact.parse("clear s5"),
                                Fact.parse("handempty"),
                                Fact.parse("ontable s3"),
                                Fact.parse("ontable s5")))
                                .build()
                ).build();
        Planner planner = getPlanner();
        Optional<Plan> plan = planner.plan(Fact.parse("on s3 s5"), problem);
        assertThat(plan.isPresent()).isTrue();
        assertThat(plan.get()).isEqualTo(Plan.builder().setSequence(
                ImmutableList.of(Predicate.parse("pickup s3"), Predicate.parse("stack s3 s5"))).build());
    }

    @Test
    public void testOntableGoal2() {
        Problem problem = Problem.builder()
                .setActions(
                        ImmutableSet.of(
                                Action.parse("stack X Y: clear Y, holding X -> handempty, on X Y, clear X, " +
                                        "not holding X, not clear Y"),
                                Action.parse("unstack X: on X Y, clear X, handempty -> holding X, clear Y, " +
                                        "not handempty, not clear X, not on X Y")
                        ))
                .setTypes(ImmutableSet.of(
                        TypeDeclaration.parse("X, Y: s3, s2, s5")
                ))
                .setInitialState(
                        State.builder().setState(ImmutableSet.of(
                                Fact.parse("clear s3"),
                                Fact.parse("clear s5"),
                                Fact.parse("handempty"),
                                Fact.parse("on s3 s2"),
                                Fact.parse("ontable s5")))
                                .build()
                ).build();
        Planner planner = getPlanner();
        Optional<Plan> plan = planner.plan(Fact.parse("on s3 s5"), problem);
        assertThat(plan.isPresent()).isTrue();
        assertThat(plan.get()).isEqualTo(Plan.builder().setSequence(
                ImmutableList.of(Predicate.parse("unstack s3"), Predicate.parse("stack s3 s5"))).build());
    }

    @Ignore
    @Test(timeout=5000)
    public void testScenario1() {
        Problem problem = Problem.builder()
                .setActions(
                        ImmutableSet.of(
                            Action.parse("putdown X: holding X -> ontable X, handempty, clear X," +
                                    "not holding X"),
                            Action.parse("pickup X: ontable X, clear X, handempty -> holding X, " +
                                    "not ontable X, not clear X, not handempty X"),
                            Action.parse("stack X Y: holding X, clear Y -> handempty, on X Y, clear X, " +
                                    "not holding X, not clear Y"),
                            Action.parse("unstack X: on X Y, clear X, handempty -> holding X, clear Y, " +
                                    "not handempty, not clear X, not on X Y")
                            ))
                .setTypes(ImmutableSet.of(
                            TypeDeclaration.parse("X, Y: s2, s4, s3, s5")
                ))
                .setInitialState(
                        State.builder().setState(ImmutableSet.of(
                                Fact.parse("clear s3"),
                                Fact.parse("clear s4"),
                                Fact.parse("handempty"),
                                Fact.parse("on s3 s2"),
                                Fact.parse("on s4 s5")))
                            .build()
                ).build();
        Planner planner = new StripsPlanner();
        Optional<Plan> plan = planner.plan(Fact.parse("on s3 s5"), problem);
        assertThat(plan.isPresent()).isTrue();
        assertThat(plan).isEqualTo(Plan.builder().setSequence(ImmutableList.of(Predicate.parse("unstack s5 s4"))));
    }

    @Ignore
    @Test(timeout=5000)
    public void testScenario2() {
        Problem problem = Problem.builder()
                .setActions(
                        ImmutableSet.of(
                                Action.parse("putdown X: holding X -> ontable X, handempty, clear X," +
                                        "not holding X"),
                                Action.parse("unstack X: on X Y, clear X, handempty -> holding X, clear Y, " +
                                        "not handempty, not clear X, not on X Y")
                        ))
                .setTypes(ImmutableSet.of(
                        TypeDeclaration.parse("X, Y: s4, s5")
                ))
                .setInitialState(
                        State.builder().setState(ImmutableSet.of(
                                Fact.parse("clear s4"),
                                Fact.parse("handempty"),
                                Fact.parse("on s4 s5")))
                                .build()
                ).build();
        Planner planner = getPlanner();
        Optional<Plan> plan = planner.plan(Fact.parse("holding s5"), problem);
        assertThat(plan.isPresent()).isTrue();
        assertThat(plan).isEqualTo(Plan.builder().setSequence(ImmutableList.of(Predicate.parse("unstack s5 s4"))));
    }


}