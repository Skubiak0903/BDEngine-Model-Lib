package io.github.skubiak0903.bdengine.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skubiak0903.bdengine.utils.MatrixUtils;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

public class MutliEntityModelBehavior extends ModelBehavior {
	private static final Logger LOGGER = LoggerFactory.getLogger(MutliEntityModelBehavior.class);
	
	private final List<BDBaseModelEntity> waitingPassengersList;
	private final List<Task> ongoingRotations = new ArrayList<>();
	
	private final BDBaseModelEntity entity;
	
	
	public MutliEntityModelBehavior(BDBaseModelEntity entity, List<BDModelEntitySchema> entitySchemas) {
		this(entity, entitySchemas, Vec.ONE, Vec.ZERO);
	}
		
	public MutliEntityModelBehavior(BDBaseModelEntity entity, List<BDModelEntitySchema> entitySchemas, Vec initialScale, Vec initialTranslation) {
		if (entity		       == null) throw new IllegalArgumentException("entity cannot be null");
		if (entitySchemas      == null) throw new IllegalArgumentException("entitySchemas cannot be null");
		if (initialScale 	   == null) throw new IllegalArgumentException("initialScale cannot be null");
		if (initialTranslation == null) throw new IllegalArgumentException("initialTranslation cannot be null");
				
		
		List<BDBaseModelEntity> tempList = new ArrayList<>();
		for (var schema : entitySchemas) {
			var passenger = PassengerEntity.createPassengerEntity(schema, initialScale, initialTranslation);
			tempList.add(passenger);
		}
 		 		
 		
 		this.waitingPassengersList = new ArrayList<>(tempList);

		this.globalScale = initialScale;
		this.globalTranslation = initialTranslation;
		this.entity = entity;
	}
	

	@Override
	public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
		var future = CompletableFuture.completedFuture(null);
		
		return future.thenAccept((_) -> {
			waitingPassengersList.forEach((passenger) -> {
				passenger.setInstance(instance, spawnPosition);
				entity.acquirable().sync((root) -> {
					root.addPassenger(passenger);
				});
			});
			
			// clear list after successful passenger initialization
			waitingPassengersList.clear();
		});
	}

	
	@Override
	public void remove(boolean permanent) {
		forEachPassenger((passenger) -> {
			passenger.remove();
		});
	}
	
	@Override
	public void setView(float yaw, float pitch) {
		forEachPassenger((passenger) -> {
			passenger.setView(yaw, pitch);
		});
	}
	

	/*@Override
	public void teleport(Pos position) {
		// do nothing -> teleporting this entity will cause all passengers to also teleport
	}*/
	
	
	/*
	 *  INTERPOLATION DURATION
	 */
	
	@Override
	public void setTransformationInterpolationDuration(int duration, boolean force) {
		if (!force && globalTransformationDuration == duration) return;
		
		globalTransformationDuration = duration;
		forEachPassengerAndSelf((target) -> {
			target.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setTransformationInterpolationDuration(duration);
			});
		});
	}
	
	
	@Override
	public void setPosRotInterpolationDuration(int duration, boolean force) {
		if (!force && globalPosRotDuration == duration) return;
		
		globalPosRotDuration = duration;
		forEachPassengerAndSelf((target) -> {
			target.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setPosRotInterpolationDuration(duration);
			});
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
	    forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	        	Vec defScale = passenger.getDefaultScale();
	            Vec defTranslation = passenger.getDefaultTranslation();

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
		forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	        	Vec defTranslation = passenger.getDefaultTranslation();
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
		
		applyRotationToAll(newRotation, duration);
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
				applyRotationToAll(nextRotation, stepDuration);
	        	continue;
			}
			
			TaskHolder holder = new TaskHolder();
			holder.task = entity.scheduler().buildTask(() -> {
				applyRotationToAll(nextRotation, stepDuration);
	        	ongoingRotations.remove(holder.task);
			}).delay(TaskSchedule.tick(delayTicks)).schedule();
			
			ongoingRotations.add(holder.task);
		}
		
	}
	
	private class TaskHolder {
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
	
	private void applyRotationToAll(Quaternionf rot, int dur) {
	    forEachPassenger(p -> setPassengerRotation(p, rot, dur));
	}
	
	
	private void setPassengerRotation(PassengerEntity entity, Quaternionf newRotation, int duration) {
		Vector3f baseTranslation = VecUtils.pointToJomlVec3(entity.getDefaultTranslation().mul(globalScale).add(globalTranslation));
		Quaternionf defRotation = entity.getDefaultLeftRotation();
		
		applyRotation(
				entity,
				baseTranslation, 
				defRotation, 
				newRotation, 
				duration
			);
	}
	
	private void applyRotation(PassengerEntity entity, Vector3f baseTranslation, Quaternionf baseLeftRot, 
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
	
	
	
	@Override
	@Deprecated
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setLeftRotation(MatrixUtils.toArray(leftQuat));
				meta.setTransformationInterpolationDuration(duration);
				meta.setTransformationInterpolationStartDelta(0);
			});
		});
	}

	@Override
	@Deprecated
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setRightRotation(MatrixUtils.toArray(rightQuat));
				meta.setTransformationInterpolationDuration(duration);
				meta.setTransformationInterpolationStartDelta(0);
			});
		});
	}
	
	
	
	
	
	
	/*
	 * Private methods
	 */
	
	private void forEachPassenger(Consumer<PassengerEntity> action) {
	    for (Entity entity : entity.getPassengers()) {
	        if (entity instanceof PassengerEntity passenger) {
	            action.accept(passenger);
	        } else {
	        	LOGGER.warn("Unexpected passenger type: {}", entity.getClass().getSimpleName());
	        }
	    }
	}
	
	private void forEachPassengerAndSelf(Consumer<BDBaseModelEntity> action) {
		action.accept(entity);
	    forEachPassenger((passenger) -> action.accept(passenger));
	}
}
