package io.github.skubiak0903.bdengine.math;

import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import io.github.skubiak0903.bdengine.utils.MatrixUtils;
import io.github.skubiak0903.bdengine.utils.MatrixUtils.SVDDecomposition;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.minestom.server.coordinate.Vec;

public final class Transformation {
  private final Matrix4fc matrix;
  
  private boolean decomposed;
  
  private Vector3fc translation;
  private Quaternionfc leftRotation;
  private Vector3fc scale;
  private Quaternionfc rightRotation;
  
  private static final Transformation IDENTITY;
  
  public Transformation(Matrix4fc matrix) {
    if (matrix == null) {
      this.matrix = (Matrix4fc)new Matrix4f();
    } else {
      this.matrix = matrix;
    } 
  }
  
  public Transformation(Vector3fc translation, Quaternionfc leftRotation, Vector3fc scale, Quaternionfc rightRotation) {
    this.matrix 		= compose(translation, leftRotation, scale, rightRotation);
    this.translation 	= (translation 	 != null) ? translation   : new Vector3f();
    this.leftRotation 	= (leftRotation  != null) ? leftRotation  : new Quaternionf();
    this.scale 			= (scale 		 != null) ? scale 		  : new Vector3f(1.0F, 1.0F, 1.0F);
    this.rightRotation 	= (rightRotation != null) ? rightRotation : new Quaternionf();
    this.decomposed 	= true;
  }
  
  public Transformation(Vec translation, Quaternionfc leftRotation, Vec scale, Quaternionfc rightRotation) {
	  this(
			  VecUtils.pointToJomlVec3(translation), 
			  leftRotation, 
			  VecUtils.pointToJomlVec3(scale), 
			  rightRotation);
  }
  
  static {
    IDENTITY = new Transformation(
    		new Vector3f(), 				// translation				
    		new Quaternionf(),				// left rotation
    		new Vector3f(1.0f, 1.0f, 1.0f), // scale
    		new Quaternionf());				// right rotation
    IDENTITY.decomposed = true;
  }
  
  public static Transformation identity() {
    return IDENTITY;
  }
  
  public Transformation compose(Transformation that) {
    Matrix4f matrix = getMatrixCopy();
    matrix.mul(that.getMatrix());
    return new Transformation(matrix);
  }
  
  public Transformation inverse() {
    if (this == IDENTITY)
      return this; 
    Matrix4f matrix = getMatrixCopy().invertAffine();
    if (matrix.isFinite())
      return new Transformation(matrix); 
    return null;
  }
  
  private void ensureDecomposed() {
    if (!this.decomposed) {
      float scaleFactor = 1.0F / this.matrix.m33();
      SVDDecomposition svd = MatrixUtils.svdDecompose((new Matrix3f(this.matrix)).scale(scaleFactor));
      this.translation    =	this.matrix.getTranslation(new Vector3f()).mul(scaleFactor);
      this.leftRotation   =	new Quaternionf(svd.leftRotation());
      this.scale 		  =	new Vector3f(svd.scale());
      this.rightRotation  =	new Quaternionf(svd.rightRotation());
      this.decomposed 	  =	true;
    } 
  }
  
  private static Matrix4f compose(Vector3fc translation, Quaternionfc leftRotation, Vector3fc scale, Quaternionfc rightRotation) {
    Matrix4f result = new Matrix4f();
    if (translation != null)
      result.translation(translation); 
    if (leftRotation != null)
      result.rotate(leftRotation); 
    if (scale != null)
      result.scale(scale); 
    if (rightRotation != null)
      result.rotate(rightRotation); 
    return result;
  }
  
  public Matrix4fc getMatrix() {
    return this.matrix;
  }
  
  public Matrix4f getMatrixCopy() {
    return new Matrix4f(this.matrix);
  }
  
  public Vector3fc getTranslation() {
    ensureDecomposed();
    return this.translation;
  }
  
  public Vec getTranslationAsVec() {
	    return VecUtils.vec3ToMinestomVec((Vector3f) getTranslation());
	  }
  
  public Quaternionfc getLeftRotation() {
    ensureDecomposed();
    return this.leftRotation;
  }
  
  public Vector3fc getScale() {
    ensureDecomposed();
    return this.scale;
  }
  
  public Vec getScaleAsVec() {
    return VecUtils.vec3ToMinestomVec((Vector3f) getScale());
  }
  
  public Quaternionfc getRightRotation() {
    ensureDecomposed();
    return this.rightRotation;
  }
  
  public boolean equals(Object o) {
    if (this == o)
      return true; 
    if (o == null || getClass() != o.getClass())
      return false; 
    Transformation that = (Transformation)o;
    return Objects.equals(this.matrix, that.matrix);
  }
  
  public int hashCode() {
    return Objects.hash(new Object[] { this.matrix });
  }
  
  public Transformation slerp(Transformation that, float progress) {
    return new Transformation((Vector3fc)
        getTranslation().lerp (that.getTranslation(), progress, new Vector3f()),
        getLeftRotation().slerp(that.getLeftRotation(), progress, new Quaternionf()),
        getScale().lerp (that.getScale(), progress, new Vector3f()),
        getRightRotation().slerp(that.getRightRotation(), progress, new Quaternionf()));
  }
}