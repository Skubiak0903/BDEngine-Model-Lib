package io.github.skubiak0903.bdengine.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;

/**
 * Utility class for asserting Quaternion values in tests.
 */
public class QuaternionAssert {
    
    private static final float DEFAULT_EPSILON = 0.0001f;
    
    private QuaternionAssert() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Asserts that two quaternions are equal within a tolerance.
     */
    public static void assertEquals(Quaternionf expected, Quaternionf actual) {
        assertEquals(expected, actual, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that two quaternions are equal within a tolerance.
     */
    public static void assertEquals(Quaternionf expected, Quaternionf actual, float epsilon) {
        assertTrue(actual.equals(expected, epsilon), 
            String.format("Quaternions are not equal within tolerance %.6f\nExpected: %s\nActual: %s", 
                epsilon, expected, actual));
    }
    
    /**
     * Asserts that two quaternions are equal with a custom message.
     */
    public static void assertEquals(Quaternionf expected, Quaternionf actual, String message) {
        assertEquals(expected, actual, DEFAULT_EPSILON, message);
    }
    
    /**
     * Asserts that two quaternions are equal within a tolerance with a custom message.
     */
    public static void assertEquals(Quaternionf expected, Quaternionf actual, float epsilon, String message) {
        assertTrue(actual.equals(expected, epsilon), 
            message + "\nExpected: " + expected + "\nActual: " + actual);
    }
    
    /**
     * Asserts that a quaternion is identity (no rotation).
     */
    public static void assertIdentity(Quaternionf actual) {
        assertIdentity(actual, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that a quaternion is identity (no rotation) within a tolerance.
     */
    public static void assertIdentity(Quaternionf actual, float epsilon) {
        Quaternionf identity = new Quaternionf(0, 0, 0, 1);
        assertTrue(actual.equals(identity, epsilon),
            String.format("Quaternion should be identity\nExpected: %s\nActual: %s", identity, actual));
    }
    
    /**
     * Asserts that the angle between two quaternions is within tolerance.
     */
    public static void assertAngleEquals(Quaternionf expected, Quaternionf actual, float maxAngleRadians) {
        float angle = Math.abs(expected.angle() - actual.angle());
        assertTrue(angle < maxAngleRadians,
            String.format("Angle difference %.6f rad exceeds maximum %.6f rad\nExpected: %s\nActual: %s", 
                angle, maxAngleRadians, expected, actual));
    }
    
    /**
     * Asserts that a quaternion represents a specific rotation angle around Y axis.
     */
    public static void assertRotationY(Quaternionf actual, float expectedAngleDegrees) {
        assertRotationY(actual, expectedAngleDegrees, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that a quaternion represents a specific rotation angle around Y axis.
     */
    public static void assertRotationY(Quaternionf actual, float expectedAngleDegrees, float epsilon) {
        Quaternionf expected = new Quaternionf().rotateY((float) Math.toRadians(expectedAngleDegrees));
        assertEquals(expected, actual, epsilon,
            String.format("Rotation around Y axis should be %.2f degrees", expectedAngleDegrees));
    }
}