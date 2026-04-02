package io.github.skubiak0903.bdengine.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.skubiak0903.bdengine.utils.MatrixUtils;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

public abstract class ModelBehavior {
	public static final float ROTATION_EPSILON = 1e-6f;
	
	private final List<Task> ongoingRotations = new ArrayList<>();
	
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
	
	
	public void cancelRotationTasks(Quaternionf finalRotation, boolean finish) {
		if (ongoingRotations.isEmpty()) return;
		
		for (Task rotTask : ongoingRotations) {
			rotTask.cancel();
		}
		
		if (finish) setModelRotationLerp(finalRotation, 0, true);
		
		ongoingRotations.clear();
	}
	
	
	
	
	/*
	 *  DEBUG
	 */
	
	@Deprecated
	public abstract void setLeftRotation(Quaternionf leftQuat, int duration);

	@Deprecated
	public abstract void setRightRotation(Quaternionf rightQuat, int duration);
	
	
	/*
	 * Internal methods
	 */
	
	protected void setEntityTransformationInterpolationDuration(BDBaseModelEntity entity, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
		});
	}
	
	protected void setEntityPosRotInterpolationDuration(BDBaseModelEntity entity, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setPosRotInterpolationDuration(duration);
		});
	}
	
	protected void setEntityScale(BDBaseModelEntity entity, Vec newScale, Vec defScale, Vec defTranslation, int duration) {
        entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
        	
            // scale
            Vec newEntityScale = defScale.mul(newScale);
            
            
            // translation
            Vec baseTranslation = defTranslation.mul(newScale).add(globalTranslation);
            
    		// rotate translation around pivotPoint
    		Vector3f newEntityTranslation = new Vector3f(VecUtils.pointToJomlVec3(baseTranslation))
    	            .sub(pivotPoint)
    	            .rotate(globalRotation)
    	            .add(pivotPoint);
            
            meta.setScale(newEntityScale);
            meta.setTranslation(VecUtils.vec3ToMinestomVec(newEntityTranslation));

            meta.setTransformationInterpolationDuration(duration);
            meta.setTransformationInterpolationStartDelta(0);
            
        });
	}
	
	protected void setEntityTranslation(BDBaseModelEntity entity, Vec newTranslation, Vec defTranslation, int duration) {
        entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
        	Vec baseTranslation = defTranslation.mul(globalScale).add(newTranslation);
        	
    		// rotate translation around pivotPoint
    		Vector3f newEntityTranslation = new Vector3f(VecUtils.pointToJomlVec3(baseTranslation))
    	            .sub(pivotPoint)
    	            .rotate(globalRotation)
    	            .add(pivotPoint);
        	
            meta.setTranslation(VecUtils.vec3ToMinestomVec(newEntityTranslation));
            
            meta.setTransformationInterpolationDuration(duration);
            meta.setTransformationInterpolationStartDelta(0);
        });
	}
	
	protected void setEntityRotation(BDBaseModelEntity entity, Vector3f baseTranslation, Quaternionf baseLeftRot, 
			Quaternionf rotationModifier, int duration) {
		
		// rotate translation around pivotPoint
		Vector3f newTranslation = new Vector3f(baseTranslation)
	            .sub(pivotPoint)
	            .rotate(rotationModifier)
	            .add(pivotPoint);
		
		// local rotation
		Quaternionf newLeftRot = new Quaternionf(rotationModifier)
	            .mul(baseLeftRot)
	            .normalize();
		
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTranslation(VecUtils.vec3ToMinestomVec(newTranslation));
			meta.setLeftRotation(MatrixUtils.toArray(newLeftRot));
			
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}
	
	protected void internalSlerpRotation(BiConsumer<Quaternionf, Integer> rotateAction, Quaternionf newRotation, int duration, float stepFactor, boolean force) {
		if (!force && globalRotation.equals(newRotation, ROTATION_EPSILON)) return;
		
		cancelRotationTasks(globalRotation, false); // cancel ongoing rotations

		// we need previous rotation, before changing it to newRotation
		Quaternionf startRot = new Quaternionf(globalRotation);
		
		this.globalRotation.set(newRotation);
		this.globalTransformationDuration = duration;
		
		
		Quaternionf diff = new Quaternionf(startRot).difference(newRotation);
		float diffAngle = (float) Math.toDegrees(diff.angle());
		
		int numSteps = (int) Math.ceil(diffAngle / stepFactor);
		if (numSteps < 1) numSteps = 1;
		
		float ticksPerStep = (float) duration / numSteps;
		int stepDuration = (int) Math.ceil(ticksPerStep);
		
		if (ticksPerStep <= 0) {			
			setModelRotationLerp(newRotation, duration, true); // needs to be forced, otherwise it will fail becouse current rot and new rotation are identical
			return;
		}
		
		for (int i = 0; i < numSteps; i++) {
			float alpha = (float) (i + 1) / numSteps;
			int delayTicks = (int) (i * ticksPerStep);
			
			Quaternionf nextRotation = new Quaternionf(startRot).slerp(newRotation, alpha);
			
			if (delayTicks == 0) {
				rotateAction.accept(nextRotation, stepDuration);
	        	continue;
			}
						
			addRotationTask(() -> {
				rotateAction.accept(nextRotation, stepDuration);
			}, TaskSchedule.tick(delayTicks));
		}
		
	}
	
	
	/*
	 *  Tasks
	 */
	
	protected void addRotationTask(Runnable action, TaskSchedule schedule) {		
		Task[] task = { null }; // bypass java uninitialized variable, and final variables in lambdas
		task[0] = MinecraftServer.getSchedulerManager().buildTask(() -> {
			action.run();
        	ongoingRotations.remove(task[0]);
		}).delay(schedule).schedule();
		
		ongoingRotations.add(task[0]);
	}
	
	
	@Deprecated
	protected void setEntityLeftRotation(BDBaseModelEntity entity, Quaternionf leftQuat, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setLeftRotation(MatrixUtils.toArray(leftQuat));
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}
	
	@Deprecated
	protected void setEntityRightRotation(BDBaseModelEntity entity, Quaternionf rightQuat, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setRightRotation(MatrixUtils.toArray(rightQuat));
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}
}
