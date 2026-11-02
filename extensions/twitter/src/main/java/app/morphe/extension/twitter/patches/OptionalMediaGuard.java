package app.morphe.extension.twitter.patches;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Optional UI integrations must not break the native caller on linkage or initialization failure. */
final class OptionalMediaGuard {
    static <T> T value(Supplier<T> action, T fallback, Consumer<Throwable> report) {
        try { return action.get(); }
        catch (RuntimeException | LinkageError failure) {
            try { report.accept(failure); }
            catch (RuntimeException | LinkageError ignored) { }
            return fallback;
        }
    }
    static void run(Runnable action, Consumer<Throwable> report) {
        value(() -> { action.run(); return true; }, false, report);
    }
}
