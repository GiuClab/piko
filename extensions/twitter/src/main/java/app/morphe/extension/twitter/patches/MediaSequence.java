package app.morphe.extension.twitter.patches;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Append source pages without moving an already visible media item or repeating media IDs. */
final class MediaSequence {
    static <T> List<T> appendNew(List<T> current, List<T> page, Function<T, String> key) {
        LinkedHashMap<String, T> result = new LinkedHashMap<>();
        for (T item : current) result.putIfAbsent(Objects.requireNonNull(key.apply(item)), item);
        for (T item : page) result.putIfAbsent(Objects.requireNonNull(key.apply(item)), item);
        return new ArrayList<>(result.values());
    }
}

