package knowledge;

import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class Fact {
    public abstract Predicate getPredicate();

    public abstract boolean getSign();

    public static Builder builder() {
        return new AutoValue_Fact.Builder();
    }

    public static Fact parse(String fact) {
        String[] splitFact = fact.trim().split(" ", 2);
        boolean hasNot = splitFact[0].equals(Constants.NOT);
        Predicate predicate = Predicate.parse(hasNot ? splitFact[1] : fact);
        boolean sign = !hasNot;
        return Fact.builder().setPredicate(predicate).setSign(sign).build();
    }

    public Unification unify(Fact aFact) {
        Unification predicateUnification = getPredicate().unify(aFact.getPredicate());
        return Unification.builder()
                .setSubstitutions(predicateUnification.getSubstitutions())
                .setValid(predicateUnification.isValid() && getSign() == aFact.getSign()).build();
    }

    public Fact flip() {
        return builder().setPredicate(getPredicate()).setSign(!getSign()).build();
    }

    public Fact applyUnification(Unification unification) {
        return builder()
                .setSign(getSign())
                .setPredicate(getPredicate().applyUnification(unification))
                .build();
    }

    public Set<String> allVars() {
        return getPredicate().allVars();
    }

    public boolean wellFormed() {
        return getPredicate().wellFormed();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setPredicate(Predicate newPredicate);

        public abstract Builder setSign(boolean newSign);

        public abstract Fact build();
    }
}
