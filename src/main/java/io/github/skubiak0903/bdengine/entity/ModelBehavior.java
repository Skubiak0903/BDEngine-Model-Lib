package io.github.skubiak0903.bdengine.entity;

import java.util.concurrent.CompletableFuture;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;

public abstract class ModelBehavior {
	protected Vec globalScale 	    = Vec.ONE;
	protected Vec globalTranslation = Vec.ZERO;
	protected Quaternionf globalRotation  = new Quaternionf(); // identity quaternion
	protected Vector3f pivotPoint         = new Vector3f(0f,0f,0f);
	
	protected int globalPosRotDuration	       = 0;
	protected int globalTransformationDuration = 0;

	public ModelBehavior() {}
	
	public ModelBehavior(Vec globalScale, Vec globalTranslation) {
		this.globalScale = globalScale;
		this.globalTranslation = globalTranslation;
	}
	
	/*
	 *  Entity Overrides
	 */
	public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
		// do nothing, fired after entity instance is set
		return CompletableFuture.completedFuture(null);
	}
	
	public void remove(boolean permanent) {
		// do nothing, fired before entity is removed
	}
	
	public void setView(float yaw, float pitch) {
		// do nothing, fired before entity view is set
	}
	
	public void teleport(Pos position) {
		// do nothing, fired before entity is teleported
	}
	
	
	
	/*
	 *  INTERPOLATION DURATION
	 */

	public abstract void setTransformationInterpolationDuration(int duration, boolean force);
	public abstract void setPosRotInterpolationDuration(int duration, boolean force);
	
	
	
	/*
	 * 	 SCALE
	 */

	public abstract void setModelScale(Vec newScale, int duration, boolean force);
	
	
	
	/*
	 *  TRANSLATIONS
	 */
	
	public abstract void setModelTranslation(Vec newTranslation, int duration, boolean force);
	
	
	
	/*
	 *  LERP ROTATIONS
	 */

	public abstract void setModelRotationLerp(Quaternionf newRotation, int duration, boolean force);
	
	
	
	/*
	 *  SLERP ROTATIONS
	 */

	public abstract void setModelRotationSlerp(Quaternionf newRotation, int duration, float stepFactor, boolean force);
	public abstract void cancelRotationTasks(Quaternionf finalRotation, boolean finish);
	
	
	
	
	/*
	 *  DEBUG
	 */
	
	@Deprecated
	public abstract void setLeftRotation(Quaternionf leftQuat, int duration);

	@Deprecated
	public abstract void setRightRotation(Quaternionf rightQuat, int duration);
}
