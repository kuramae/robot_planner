# robot_planner

Currently a stub. Executor logic not implemented.

The planner passes a few nontrivial tests. 
Given the goal *filled s2 50* and the following problem

```
 Problem problem = Problem.builder()
                .setActions(
                        ImmutableSet.of(
                                Action.parse("pickup X: ontable X, clear X, handempty -> holding X, " +
                                        "not ontable X, not clear X, not handempty X"),
                                Action.parse("stack X Y: holding X, clear Y -> handempty, on X Y, clear X, " +
                                        "not holding X, not clear Y"),
                                Action.parse("fill X Q: clear X -> filled X Q"),
                                Action.parse("unstack X: on X Y, clear X, handempty -> holding X, clear Y, " +
                                        "not handempty, not clear X, not on X Y")
                        ))
                ...
                .setInitialState(
                        State.builder().setState(ImmutableSet.of(
                                Fact.parse("clear s1"),
                                Fact.parse("clear s6"),
                                Fact.parse("clear s4"),
                                Fact.parse("handempty"),
                                Fact.parse("ontable s1"),
                                Fact.parse("on s3 s2"),
                                Fact.parse("on s6 s3"),
                                Fact.parse("on s4 s5")))
                                .build()
                ).build();
```

the planner idenitfies correctly the plan  

```
                Predicate.parse("unstack s6"),
                Predicate.parse("stack s6 s4"),
                Predicate.parse("unstack s3"),
                Predicate.parse("fill s2 50")
```