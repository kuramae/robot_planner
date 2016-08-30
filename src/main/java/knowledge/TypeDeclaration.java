package knowledge;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.Collectors;

import static knowledge.Constants.FACT_SEPARATOR;
import static knowledge.Constants.NAME_SEPARATOR;

@AutoValue
public abstract class TypeDeclaration {
    public abstract Set<String> getSource();

    public abstract Set<String> getDestination();

    public static Builder builder() {
        return new AutoValue_TypeDeclaration.Builder();
    }

    public static TypeDeclaration parse(String s) {
        String[] splitFullTypeDeclaration = s.split(NAME_SEPARATOR);
        Set<String> source = Sets.newHashSet(splitFullTypeDeclaration[0].trim().split(FACT_SEPARATOR));
        Set<String> destination = Sets.newHashSet(splitFullTypeDeclaration[1].trim().split(FACT_SEPARATOR));
        return TypeDeclaration.builder()
                .setSource(source.stream().map(String::trim).collect(Collectors.toSet()))
                .setDestination(destination.stream().map(String::trim).collect(Collectors.toSet()))
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setSource(Set<String> newSource);

        public abstract Builder setDestination(Set<String> newDestination);

        public abstract TypeDeclaration build();
    }
}
