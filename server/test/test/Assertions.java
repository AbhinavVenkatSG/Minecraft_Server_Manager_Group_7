package test;

import java.util.Objects;

public final class Assertions {
    private Assertions() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " Expected: " + expected + ", actual: " + actual);
        }
    }

    public static void assertNotNull(Object value, String message) {
        assertTrue(value != null, message);
    }

    public static void assertContains(String text, String expectedFragment, String message) {
        assertTrue(text != null && text.contains(expectedFragment), message + " Missing fragment: " + expectedFragment);
    }

    public static <T extends Throwable> T expectThrows(Class<T> expectedType, ThrowingRunnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return expectedType.cast(throwable);
            }
            throw new AssertionError(message + " Threw " + throwable.getClass().getName() + " instead of " + expectedType.getName(), throwable);
        }
        throw new AssertionError(message + " Expected exception: " + expectedType.getName());
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
