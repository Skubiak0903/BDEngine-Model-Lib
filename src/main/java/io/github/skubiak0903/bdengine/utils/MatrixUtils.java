package io.github.skubiak0903.bdengine.utils;

import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/*
 *  NOTE: Stolen from minecraft implementation: com.mojang.math
 *  Added: toArray, multiply4x4RowMajor
 */

public class MatrixUtils {
	private static final float G = (float) (3.0F + 2.0F * Math.sqrt(2.0F));

	private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle(0.7853982F);

	public static Matrix4f mulComponentWise(Matrix4f m, float factor) {
		return m.set(m.m00() * factor, m.m01() * factor, m.m02() * factor, m.m03() * factor, m.m10() * factor,
				m.m11() * factor, m.m12() * factor, m.m13() * factor, m.m20() * factor, m.m21() * factor,
				m.m22() * factor, m.m23() * factor, m.m30() * factor, m.m31() * factor, m.m32() * factor,
				m.m33() * factor);
	}

	public static SVDDecomposition svdDecompose(Matrix3f matrix) {
		Matrix3f b = new Matrix3f((Matrix3fc) matrix);
		b.transpose();
		b.mul((Matrix3fc) matrix);
		Quaternionf v = eigenvalueJacobi(b, 5);
		float columnScaleSquare0 = b.m00;
		float columnScaleSquare1 = b.m11;
		boolean zeroColumn0 = (columnScaleSquare0 < 1.0E-6D);
		boolean zeroColumn1 = (columnScaleSquare1 < 1.0E-6D);
		Matrix3f scratch = b;
		Matrix3f u012s = matrix.rotate((Quaternionfc) v);
		Quaternionf u = new Quaternionf();
		Quaternionf tmpQ = new Quaternionf();
		GivensParameters p1;
		if (zeroColumn0) {
			p1 = qrGivensQuat(u012s.m11, -u012s.m10);
		} else {
			p1 = qrGivensQuat(u012s.m00, u012s.m01);
		}
		Quaternionf qt0 = p1.aroundZ(tmpQ);
		Matrix3f u12s = p1.aroundZ(scratch);
		u.mul((Quaternionfc) qt0);
		u12s.transpose().mul((Matrix3fc) u012s);
		scratch = u012s;
		if (zeroColumn0) {
			p1 = qrGivensQuat(u12s.m22, -u12s.m20);
		} else {
			p1 = qrGivensQuat(u12s.m00, u12s.m02);
		}
		GivensParameters p = p1.inverse();
		Quaternionf qt1 = p.aroundY(tmpQ);
		Matrix3f u2s = p.aroundY(scratch);
		u.mul((Quaternionfc) qt1);
		u2s.transpose().mul((Matrix3fc) u12s);
		scratch = u12s;
		if (zeroColumn1) {
			p = qrGivensQuat(u2s.m22, -u2s.m21);
		} else {
			p = qrGivensQuat(u2s.m11, u2s.m12);
		}
		Quaternionf qt2 = p.aroundX(tmpQ);
		Matrix3f s = p.aroundX(scratch);
		u.mul((Quaternionfc) qt2);
		s.transpose().mul((Matrix3fc) u2s);
		Vector3f scale = new Vector3f(s.m00, s.m11, s.m22);
		return new SVDDecomposition(u, scale, v.conjugate());
	}

	private static GivensParameters approxGivensQuat(float a11, float a12, float a22) {
		float ch = 2.0F * (a11 - a22);
		float sh = a12;
		if (G * sh * sh < ch * ch)
			return GivensParameters.fromUnnormalized(sh, ch);
		return PI_4;
	}

	private static GivensParameters qrGivensQuat(float a1, float a2) {
		float p = (float) Math.hypot(a1, a2);
		float sh = (p > 1.0E-6F) ? a2 : 0.0F;
		float ch = Math.abs(a1) + Math.max(p, 1.0E-6F);
		if (a1 < 0.0F) {
			float f = sh;
			sh = ch;
			ch = f;
		}
		return GivensParameters.fromUnnormalized(sh, ch);
	}

	public static Quaternionf eigenvalueJacobi(Matrix3f inOut, int steps) {
		Quaternionf v = new Quaternionf();
		Matrix3f scratchMat = new Matrix3f();
		Quaternionf scratchQ = new Quaternionf();
		for (int i = 0; i < steps; i++)
			stepJacobi(inOut, scratchMat, scratchQ, v);
		v.normalize();
		return v;
	}

	private static void similarityTransform(Matrix3f a, Matrix3f q) {
		a.mul((Matrix3fc) q);
		q.transpose();
		q.mul((Matrix3fc) a);
		a.set((Matrix3fc) q);
	}

	private static void stepJacobi(Matrix3f m, Matrix3f tmpMat, Quaternionf tmpQ, Quaternionf output) {
		if (m.m01 * m.m01 + m.m10 * m.m10 > 1.0E-6F) {
			GivensParameters p = approxGivensQuat(m.m00, 0.5F * (m.m01 + m.m10), m.m11);
			Quaternionf qt = p.aroundZ(tmpQ);
			output.mul((Quaternionfc) qt);
			p.aroundZ(tmpMat);
			similarityTransform(m, tmpMat);
		}
		if (m.m02 * m.m02 + m.m20 * m.m20 > 1.0E-6F) {
			GivensParameters p = approxGivensQuat(m.m00, 0.5F * (m.m02 + m.m20), m.m22).inverse();
			Quaternionf qt = p.aroundY(tmpQ);
			output.mul((Quaternionfc) qt);
			p.aroundY(tmpMat);
			similarityTransform(m, tmpMat);
		}
		if (m.m12 * m.m12 + m.m21 * m.m21 > 1.0E-6F) {
			GivensParameters p = approxGivensQuat(m.m11, 0.5F * (m.m12 + m.m21), m.m22);
			Quaternionf qt = p.aroundX(tmpQ);
			output.mul((Quaternionfc) qt);
			p.aroundX(tmpMat);
			similarityTransform(m, tmpMat);
		}
	}
	
	public static float[] toArray(Quaternionf q) {
		if (q == null) throw new IllegalArgumentException("Quaternion cannot be null!");
		return new float[] {  q.x, q.y, q.z, q.w };
	}
	
	public static Quaternionf arrayToQuaternion(float[] floatArray) {
		if (floatArray.length != 4) throw new AssertionError("float array doesnt have required lenght 4. It has lenght: " + floatArray.length);
		return new Quaternionf(floatArray[0], floatArray[1], floatArray[2], floatArray[3]);
	}
	
	public static float[] multiply4x4RowMajor(float[] parentMatrix, float[] local) {
	    float[] result = new float[16];
	    
	    result[0]  = parentMatrix[0] * local[0]  + parentMatrix[1] * local[4]  + parentMatrix[2] * local[8]  + parentMatrix[3] * local[12];
	    result[1]  = parentMatrix[0] * local[1]  + parentMatrix[1] * local[5]  + parentMatrix[2] * local[9]  + parentMatrix[3] * local[13];
	    result[2]  = parentMatrix[0] * local[2]  + parentMatrix[1] * local[6]  + parentMatrix[2] * local[10] + parentMatrix[3] * local[14];
	    result[3]  = parentMatrix[0] * local[3]  + parentMatrix[1] * local[7]  + parentMatrix[2] * local[11] + parentMatrix[3] * local[15];
	    
	    result[4]  = parentMatrix[4] * local[0]  + parentMatrix[5] * local[4]  + parentMatrix[6] * local[8]  + parentMatrix[7] * local[12];
	    result[5]  = parentMatrix[4] * local[1]  + parentMatrix[5] * local[5]  + parentMatrix[6] * local[9]  + parentMatrix[7] * local[13];
	    result[6]  = parentMatrix[4] * local[2]  + parentMatrix[5] * local[6]  + parentMatrix[6] * local[10] + parentMatrix[7] * local[14];
	    result[7]  = parentMatrix[4] * local[3]  + parentMatrix[5] * local[7]  + parentMatrix[6] * local[11] + parentMatrix[7] * local[15];
	    
	    result[8]  = parentMatrix[8] * local[0]  + parentMatrix[9] * local[4]  + parentMatrix[10] * local[8] + parentMatrix[11] * local[12];
	    result[9]  = parentMatrix[8] * local[1]  + parentMatrix[9] * local[5]  + parentMatrix[10] * local[9] + parentMatrix[11] * local[13];
	    result[10] = parentMatrix[8] * local[2]  + parentMatrix[9] * local[6]  + parentMatrix[10] * local[10] + parentMatrix[11] * local[14];
	    result[11] = parentMatrix[8] * local[3]  + parentMatrix[9] * local[7]  + parentMatrix[10] * local[11] + parentMatrix[11] * local[15];
	    
	    result[12] = parentMatrix[12] * local[0] + parentMatrix[13] * local[4] + parentMatrix[14] * local[8] + parentMatrix[15] * local[12];
	    result[13] = parentMatrix[12] * local[1] + parentMatrix[13] * local[5] + parentMatrix[14] * local[9] + parentMatrix[15] * local[13];
	    result[14] = parentMatrix[12] * local[2] + parentMatrix[13] * local[6] + parentMatrix[14] * local[10] + parentMatrix[15] * local[14];
	    result[15] = parentMatrix[12] * local[3] + parentMatrix[13] * local[7] + parentMatrix[14] * local[11] + parentMatrix[15] * local[15];
	    
	    return result;
	}
	
	public record SVDDecomposition(
			Quaternionf leftRotation,
			Vector3f    scale,
			Quaternionf rightRotation
			) {}
	
}
