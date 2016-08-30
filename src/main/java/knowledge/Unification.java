package knowledge;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class Unification {
    public abstract boolean isValid();

    public abstract Map<String, String> getSubstitutions();

    public static Builder builder() {
        return new AutoValue_Unification.Builder();
    }

    public Unification merge(Unification b) {
        HashMap<String, String> all = new HashMap<>();
        all.putAll(b.getSubstitutions());
        all.putAll(getSubstitutions());
        return builder().setValid(true).setSubstitutions(all).build();
    }

    public static Unification empty() {
        return builder().setValid(true).setSubstitutions(Collections.emptyMap()).build();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setValid(boolean newValid);

        public abstract Builder setSubstitutions(Map<String, String> newSubstitutions);

        public abstract Unification build();
    }
}
