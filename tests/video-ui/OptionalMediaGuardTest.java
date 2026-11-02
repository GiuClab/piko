package app.morphe.extension.twitter.patches;

import java.util.ArrayList;
import java.util.List;

public final class OptionalMediaGuardTest {
    static final class BrokenModule {
        static final Object INSTANCE = fail();
        static Object fail() { throw new IllegalStateException("Context not ready"); }
    }
    public static void main(String[] args) {
        List<Throwable> failures = new ArrayList<>();
        Object nativeValue = new Object();
        require(OptionalMediaGuard.value(() -> nativeValue, null, failures::add) == nativeValue);
        require(failures.isEmpty());
        require(OptionalMediaGuard.value(() -> BrokenModule.INSTANCE, nativeValue, failures::add) == nativeValue);
        require(failures.get(0) instanceof ExceptionInInitializerError);
        require(OptionalMediaGuard.value(() -> BrokenModule.INSTANCE, nativeValue, failures::add) == nativeValue);
        require(failures.get(1) instanceof NoClassDefFoundError);
        OptionalMediaGuard.run(() -> { throw new VerifyError("Native integration rejected"); }, failures::add);
        require(failures.get(2) instanceof VerifyError);
        OptionalMediaGuard.run(() -> { throw new IllegalArgumentException(); }, e -> { throw new IllegalStateException("Reporter failed"); });
        require(OptionalMediaGuard.value(() -> nativeValue, null, failures::add) == nativeValue);
    }
    static void require(boolean condition) { if (!condition) throw new AssertionError(); }
}
