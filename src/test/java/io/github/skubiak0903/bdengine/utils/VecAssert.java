package io.github.skubiak0903.bdengine.utils;

import net.minestom.server.coordinate.Vec;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Utility class for asserting Vec values in tests.
 */
public class VecAssert {
    
    private static final float DEFAULT_EPSILON = (float) Vec.EPSILON;
    
    private VecAssert() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Asserts that two vectors are equal within a tolerance.
     */
    public static void assertEquals(Vec expected, Vec actual) {
        assertEquals(expected, actual, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that two vectors are equal within a tolerance.
     */
    public static void assertEquals(Vec expected, Vec actual, float epsilon) {
        if (!actual.samePoint(expected, epsilon)) {
            fail(String.format("Vectors are not equal within tolerance %.6f\nExpected: %s\nActual: %s",
                epsilon, expected, actual));
        }
    }
    
    /**
     * Asserts that two vectors are equal with a custom message.
     */
    public static void assertEquals(Vec expected, Vec actual, String message) {
        assertEquals(expected, actual, DEFAULT_EPSILON, message);
    }
    
    /**
     * Asserts that two vectors are equal within a tolerance with a custom message.
     */
    public static void assertEquals(Vec expected, Vec actual, float epsilon, String message) {
        if (!actual.samePoint(expected, epsilon)) {
            fail(message + "\nExpected: " + expected + "\nActual: " + actual);
        }
    }
    
    /**
     * Asserts that a vector is zero (0, 0, 0).
     */
    public static void assertZero(Vec actual) {
        assertZero(actual, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that a vector is zero (0, 0, 0) within a tolerance.
     */
    public static void assertZero(Vec actual, float epsilon) {
        assertTrue(actual.distance(Vec.ZERO) < epsilon,
            String.format("Vector should be zero\nActual: %s", actual));
    }
    
    /**
     * Asserts that a vector is identity (1, 1, 1).
     */
    public static void assertIdentity(Vec actual) {
        assertIdentity(actual, DEFAULT_EPSILON);
    }
    
    /**
     * Asserts that a vector is identity (1, 1, 1) within a tolerance.
     */
    public static void assertIdentity(Vec actual, float epsilon) {
        assertTrue(actual.samePoint(new Vec(1, 1, 1), epsilon),
            String.format("Vector should be identity (1,1,1)\nActual: %s", actual));
    }
    
    /**
     * Asserts that each component of the vector is within expected range.
     */
    public static void assertEquals(float expectedX, float expectedY, float expectedZ, Vec actual) {
        assertEquals(new Vec(expectedX, expectedY, expectedZ), actual);
    }
    
    /**
     * Asserts that each component of the vector is within expected range with tolerance.
     */
    public static void assertEquals(float expectedX, float expectedY, float expectedZ, Vec actual, float epsilon) {
        assertEquals(new Vec(expectedX, expectedY, expectedZ), actual, epsilon);
    }
    
    /**
     * Asserts that vector components are equal to expected values with a custom message.
     */
    public static void assertEquals(float expectedX, float expectedY, float expectedZ, Vec actual, String message) {
        assertEquals(new Vec(expectedX, expectedY, expectedZ), actual, DEFAULT_EPSILON, message);
    }
    
    /**
     * Asserts that the distance between two vectors is within tolerance.
     */
    public static void assertDistance(Vec expected, Vec actual, float maxDistance) {
        double distance = actual.distance(expected);
        assertTrue(distance <= maxDistance,
            String.format("Distance %.6f exceeds maximum %.6f\nExpected: %s\nActual: %s",
                distance, maxDistance, expected, actual));
    }
    
    /**
     * Asserts that the vector is within a specific range.
     */
    public static void assertInRange(Vec actual, Vec min, Vec max) {
        boolean inRange = actual.x() >= min.x() && actual.x() <= max.x() &&
                          actual.y() >= min.y() && actual.y() <= max.y() &&
                          actual.z() >= min.z() && actual.z() <= max.z();
        
        assertTrue(inRange,
            String.format("Vector not in range\nMin: %s\nMax: %s\nActual: %s", min, max, actual));
    }
    
    /**
     * Asserts that the vector is approximately equal to a scalar multiple.
     */
    public static void assertScaled(Vec expected, float scale, Vec actual, float epsilon) {
        Vec scaledExpected = new Vec(expected.x() * scale, expected.y() * scale, expected.z() * scale);
        assertEquals(scaledExpected, actual, epsilon);
    }
}