package io.github.skubiak0903.bdengine.entity;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.skubiak0903.bdengine.utils.MatrixUtils;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import lombok.AccessLevel;
import lombok.Getter;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

@Getter
public class SingleEntityModelBehavior extends ModelBehavior {
	//private static final Logger LOGGER = LoggerFactory.getLogger(BDSingleModelEntity.class);
	
	@Getter(value = AccessLevel.NONE)
	private final BDBaseModelEntity entity;
	
	@Getter(value = AccessLevel.NONE)
	private final List<Task> ongoingRotations = new ArrayList<>();
	
	private final Vec defaultScale;
	private final Vec defaultTranslation;
	private final Quaternionf defaultRightRotation;
	private final Quaternionf defaultLeftRotation;

	
	
	public SingleEntityModelBehavior(BDBaseModelEntity entity, BDModelEntitySchema entitySchema) {
		this(entity, entitySchema, Vec.ONE, Vec.ZERO);
	}
	
		
	public SingleEntityModelBehavior(BDBaseModelEntity entity, BDModelEntitySchema entitySchema, Vec initialScale, Vec initialTranslation) {
		if (entitySchema       == null) throw new IllegalArgumentException("entitySchemas cannot be null");
		if (initialScale 	   == null) throw new IllegalArgumentException("initialScale cannot be null");
		if (initialTranslation == null) throw new IllegalArgumentException("initialTranslation cannot be null");
		
		super(initialScale, initialTranslation);
		
		PassengerEntity.setupEntity(entity, entitySchema, initialScale, initialTranslation);
 		 		
		this.globalScale = initialScale;
		this.globalTranslation = initialTranslation;
		this.entity = entity;
		
		this.defaultScale = entitySchema.getScale();
		this.defaultTranslation = entitySchema.getTranslation();
		this.defaultRightRotation = entitySchema.getRotationRight().conjugate(new Quaternionf());
		this.defaultLeftRotation = entitySchema.getRotationLeft();
	}
	
	
	
	/*
	 *  INTERPOLATION DURATION
	 */
	
	@Override
	public void setTransformationInterpolationDuration(int duration, boolean force) {
		if (!force && globalTransformationDuration == duration) return;
		
		globalTransformationDuration = duration;
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
		});
	}
	
	
	@Override
	public void setPosRotInterpolationDuration(int duration, boolean force) {
		if (!force && globalPosRotDuration == duration) return;
		
		globalPosRotDuration = duration;
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setPosRotInterpolationDuration(duration);
		});
	}
	
	
	
	/*
	 * 	 SCALE
	 */
	
	@Override
	public void setModelScale(Vec newScale, int duration, boolean force) {
		if (!force && VecUtils.isSimilar(globalScale, newScale)) return; // skip if scale is similar
		
		this.globalScale = newScale;
	    this.globalTransformationDuration = duration; // no need to check if duration is the same
        entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
        	Vec defScale = defaultScale;
            Vec defTranslation = defaultTranslation;

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
	
	
	/*
	 *  TRANSLATIONS
	 */

	@Override
	public void setModelTranslation(Vec newTranslation, int duration, boolean force) {
		if (!force && VecUtils.isSimilar(globalTranslation, newTranslation)) return;
		
		this.globalTranslation = newTranslation;
		this.globalTransformationDuration = duration;
        entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
        	Vec defTranslation = defaultTranslation;
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
	
	
	
	
	/*
	 *  LERP ROTATIONS
	 */
	
	@Override
	public void setModelRotationLerp(Quaternionf newRotation, int duration, boolean force) {					
		if (!force && globalRotation.equals(newRotation, 1e-6f)) return;
		
		cancelRotationTasks(globalRotation, false);
		
		this.globalRotation.set(newRotation);
		this.globalTransformationDuration = duration;
		
		setEntityRotation(newRotation, duration);
	}
	
	
	
	
	
	/*
	 *  SLERP ROTATIONS
	 */
	
	@Override
	public void setModelRotationSlerp(Quaternionf newRotation, int duration, float stepFactor, boolean force) {
		if (!force && globalRotation.equals(newRotation, 1e-6f)) return;
		
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
				setEntityRotation(nextRotation, stepDuration);
	        	continue;
			}
			
			TaskHolder holder = new TaskHolder();
			holder.task = entity.scheduler().buildTask(() -> {
				setEntityRotation(nextRotation, stepDuration);
	        	ongoingRotations.remove(holder.task);
			}).delay(TaskSchedule.tick(delayTicks)).schedule();
			
			ongoingRotations.add(holder.task);
		}
		
	}
	
	private static class TaskHolder {
		Task task;
	}
	
	
	
	@Override
	public void cancelRotationTasks(Quaternionf finalRotation, boolean finish) {
		if (ongoingRotations.isEmpty()) return;
		
		for (Task rotTask : ongoingRotations) {
			rotTask.cancel();
		}
		
		if (finish) setModelRotationLerp(finalRotation, 0, true);
		
		ongoingRotations.clear();
	}
	
	
	private void setEntityRotation(Quaternionf newRotation, int duration) {
		Vector3f baseTranslation = VecUtils.pointToJomlVec3(defaultTranslation.mul(globalScale).add(globalTranslation));
		Quaternionf defRotation = defaultLeftRotation;
		
		applyRotation(
				baseTranslation, 
				defRotation, 
				newRotation, 
				duration
			);
	}
	
	private void applyRotation(Vector3f baseTranslation, Quaternionf baseLeftRot, Quaternionf rotationModifier, int duration) {
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
	
	
	
	@Override
	@Deprecated
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setLeftRotation(MatrixUtils.toArray(leftQuat));
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}

	@Override
	@Deprecated
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setRightRotation(MatrixUtils.toArray(rightQuat));
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}
}
