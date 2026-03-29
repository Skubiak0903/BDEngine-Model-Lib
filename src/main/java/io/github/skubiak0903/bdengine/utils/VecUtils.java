package io.github.skubiak0903.bdengine.utils;

import org.joml.Vector3f;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

public class VecUtils {
	public static Vector3f pointToJomlVec3(Point point) {
		if (point == null) throw new IllegalArgumentException("Point cannot be null!");
		return new Vector3f((float) point.x(), (float) point.y(), (float) point.z());
	}
	
	public static Vec vec3ToMinestomVec(Vector3f vec) {
		if (vec == null) throw new IllegalArgumentException("Vector cannot be null!");
		return new Vec(vec.x, vec.y, vec.z);
	}
	
	
	
	/*public static boolean isNegative(Point point) {
		return point.x() < 0 || point.y() < 0 || point.z() < 0; 
	}*/
	
	
	
	public static boolean isSimilar(Point p1, Point p2) {
		return isSimilar(p1, p2, Vec.EPSILON);
	}
	
	public static boolean isSimilar(Point p1, Point p2, double epsilon) {
		return p1.distanceSquared(p2) < (epsilon*epsilon);
	}
}
