package com.github.fiodarks.project26.commons;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Commons {
    public static <T, R> R toNullable(T value, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return value == null ? null : mapper.apply(value);
    }
}
