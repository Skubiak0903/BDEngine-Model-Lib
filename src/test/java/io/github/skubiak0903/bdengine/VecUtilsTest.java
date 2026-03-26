package io.github.skubiak0903.bdengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.minestom.server.coordinate.Vec;

public class VecUtilsTest {
	
	public static Vec vec3ToMinestomVec(Vector3f vec) {
		if (vec == null) throw new IllegalArgumentException("Vector cannot be null!");
		return new Vec(vec.x, vec.y, vec.z);
	}
	
	@Test
    @DisplayName("Checks if pointToJomlVec3 works properly")
    void pointToJomlVec3() {
		assertThrows(IllegalArgumentException.class, () -> {
			VecUtils.pointToJomlVec3(null);
		});
		
		Vector3f vec0 = VecUtils.pointToJomlVec3(new Vec(10f, -3f, 0f));
		assertEquals(10f, vec0.x, Vec.EPSILON, "vec0 - X doesnt match");
		assertEquals(-3f, vec0.y, Vec.EPSILON, "vec0 - Y doesnt match");
		assertEquals(0f,  vec0.z, Vec.EPSILON, "vec0 - Z doesnt match");
		
		Vector3f vec1 = VecUtils.pointToJomlVec3(new Vec(512491f, 0.4E-4f, 12312.85843f));
		assertEquals(512491f,      vec1.x, Vec.EPSILON, "vec1 - X doesnt match");
		assertEquals(0.4E-4f, 	   vec1.y, Vec.EPSILON, "vec1 - Y doesnt match");
		assertEquals(12312.85843f, vec1.z, Vec.EPSILON, "vec1 - Z doesnt match");
		
		Vector3f vec2 = VecUtils.pointToJomlVec3(new Vec(942.9E-3f, -66231.421f, 1.8E+5f));
		assertEquals(942.9E-3f,   vec2.x, Vec.EPSILON, "vec2 - X doesnt match");
		assertEquals(-66231.421f, vec2.y, Vec.EPSILON, "vec2 - Y doesnt match");
		assertEquals(1.8E+5f,  	  vec2.z, Vec.EPSILON, "vec2 - Z doesnt match");
    }
	
    @Test
    @DisplayName("Checks if vec3ToMinestomVec works properly")
    void vec3ToMinestomVec() {
		assertThrows(IllegalArgumentException.class, () -> {
			VecUtils.vec3ToMinestomVec(null);
		});
		
		Vec vec0 = VecUtils.vec3ToMinestomVec(new Vector3f(10f, -3f, 0f));
		assertEquals(10f, vec0.x(), Vec.EPSILON, "vec0 - X doesnt match");
		assertEquals(-3f, vec0.y(), Vec.EPSILON, "vec0 - Y doesnt match");
		assertEquals(0f,  vec0.z(), Vec.EPSILON, "vec0 - Z doesnt match");
		
		Vec vec1 = VecUtils.vec3ToMinestomVec(new Vector3f(512491f, 0.4E-4f, 12312.85843f));
		assertEquals(512491f,      vec1.x(), Vec.EPSILON, "vec1 - X doesnt match");
		assertEquals(0.4E-4f, 	   vec1.y(), Vec.EPSILON, "vec1 - Y doesnt match");
		assertEquals(12312.85843f, vec1.z(), Vec.EPSILON, "vec1 - Z doesnt match");
		
		Vec vec2 = VecUtils.vec3ToMinestomVec(new Vector3f(942.9E-3f, -66231.421f, 1.8E+5f));
		assertEquals(942.9E-3f,	  vec2.x(), Vec.EPSILON, "vec2 - X doesnt match");
		assertEquals(-66231.421f, vec2.y(), Vec.EPSILON, "vec2 - Y doesnt match");
		assertEquals(1.8E+5f,  	  vec2.z(), Vec.EPSILON, "vec2 - Z doesnt match");
    }
}
