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
}
