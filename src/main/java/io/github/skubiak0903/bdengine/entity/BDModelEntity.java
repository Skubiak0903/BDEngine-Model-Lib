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
import lombok.AccessLevel;
import lombok.Getter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

@Getter
public class BDModelEntity extends BDBaseModelEntity {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDModelEntity.class);
	
	@Getter(value = AccessLevel.NONE)
	private final List<BDBaseModelEntity> waitingPassengersList;
	@Getter(value = AccessLevel.NONE)
	private final List<Task> ongoingRotations = new ArrayList<>();
	
	protected Vec globalScale 	    = Vec.ONE;
	protected Vec globalTranslation = Vec.ZERO;
	protected Quaternionf globalRotation  = new Quaternionf(); // identity quaternion
	protected int globalPosRotDuration	       = 0;
	protected int globalTransformationDuration = 0;
	
	protected Vector3f pivotPoint = new Vector3f(0.5f,0.5f,0.5f);
	
	
	
	public BDModelEntity(List<BDModelEntitySchema> entitySchemas) {
		this(entitySchemas, Vec.ONE, Vec.ZERO);
	}
		
	public BDModelEntity(List<BDModelEntitySchema> entitySchemas, Vec initialScale, Vec initialTranslation) {
		super(EntityType.BLOCK_DISPLAY);
		if (entitySchemas      == null) throw new IllegalArgumentException("entitySchemas cannot be null");
		if (initialScale 	   == null) throw new IllegalArgumentException("initialScale cannot be null");
		if (initialTranslation == null) throw new IllegalArgumentException("initialTranslation cannot be null");
				
		
		List<BDBaseModelEntity> tempList = new ArrayList<>();
 		/*if (entitySchemas.size() == 1) {
 			// don't do passengers, just init this entity as single model entity
 		} else {*/
 			for (var schema : entitySchemas) {
 				var entity = BDPassengerEntity.createPassengerEntity(schema, initialScale, initialTranslation);
 				tempList.add(entity);
 			}
 		//}
 		 		
 		
 		this.waitingPassengersList = new ArrayList<>(tempList);

		this.globalScale = initialScale;
		this.globalTranslation = initialTranslation;
	}
	

	@Override
	public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
		var future = super.setInstance(instance, spawnPosition);
		// we need to add passengers here becouse Entity#addPassenger requires for passenger entity to be registered (eg. instance need to be set)

		return future.thenAccept((_) -> {
			waitingPassengersList.forEach((entity) -> {
				entity.setInstance(instance, spawnPosition);
				this.acquirable().sync((root) -> {
					root.addPassenger(entity);
				});
			});
			
			// clear list after successful passenger initialization
			waitingPassengersList.clear();
		});
	}

	
	@Override
	protected void remove(boolean permanent) {
		forEachPassenger((passenger) -> {
			passenger.remove();
		});
		super.remove(permanent);
	}
	
	@Override
	public void setView(float yaw, float pitch) {
		super.setView(yaw, pitch);
		forEachPassenger((passenger) -> {
			passenger.setView(yaw, pitch);
		});
	}
	
	
	/**
     * Changes model view with animation.
     *
     * @param yaw		New yaw rotation.
     * @param pitch		New pitch rotation.
     * @param duration  Interpolation duration in ticks.
     */
	public void setView(float yaw, float pitch, int duration) {
		setPosRotInterpolationDuration(duration);
		setView(yaw, pitch);
	}
	
	/**
     * Teleports model with animation.
     * <p>stepFactor
     * INFO: This function waits 1 tick before teleporting, to
     * properly set the interpolation duration.
     * </p>
     *
     * @param position	New entity position.
     * @param duration  Interpolation duration in ticks.
     */
	public void teleport(Pos position, int duration) {		
		setPosRotInterpolationDuration(duration);
		
		this.scheduleNextTick((entity) -> {
			entity.teleport(position);
		});

	}
	
	
	
	/**
	 * WARNING: Doesn't update the model after changing.
	 * 
	 * @param newPivotPoint New pivot point vector
	 */
	public void setPivotPoint(Vector3f newPivotPoint) {
		this.pivotPoint.set(newPivotPoint);
	}

	/**
	 * WARNING: Doesn't update the model after changing.
	 * 
	 * @param newPivotPoint New pivot point vector
	 */
	public void setPivotPoint(Vec newPivotPoint) {
		this.setPivotPoint(VecUtils.pointToJomlVec3(newPivotPoint));
	}
	
	
	
	
	/*
	 *  INTERPOLATION DURATION
	 */
	
	
	/**
     * Sets the interpolation duration for transformation animations (scale, rotation, translation).
     *
     * @param duration  Interpolation duration in ticks.
     */
	public void setTransformationInterpolationDuration(int duration) {
		setTransformationInterpolationDuration(duration, false);
	}
	
	/**
     * Sets the interpolation duration for transformation animations (scale, rotation, translation) with an optional force update.
     *
     * @param duration  Interpolation duration in ticks.
     * @param force     If {@code true}, updates all passengers regardless of the current duration.
     */
	public void setTransformationInterpolationDuration(int duration, boolean force) {
		if (!force && globalTransformationDuration == duration) return;
		
		globalTransformationDuration = duration;
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
		});
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setTransformationInterpolationDuration(duration);
			});
		});
	}
	
	
	
	/**
     * Sets the interpolation duration for teleport animations.
     *
     * @param duration  Interpolation duration in ticks.
     */
	public void setPosRotInterpolationDuration(int duration) {
		setPosRotInterpolationDuration(duration, false);
	}
	
	/**
     * Sets the interpolation duration for teleport animations with an optional force update.
     *
     * @param duration  Interpolation duration in ticks.
     * @param force     If {@code true}, updates all passengers regardless of the current duration.
     */
	public void setPosRotInterpolationDuration(int duration, boolean force) {
		if (!force && globalPosRotDuration == duration) return;
		
		globalPosRotDuration = duration;
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setPosRotInterpolationDuration(duration);
		});
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setPosRotInterpolationDuration(duration);
			});
		});
	}
	
	
	
	/*
	 * 	 SCALE
	 */
	
	
	
	/**
     * Rescales the model by a multiplier relative to its current scale.
     *
     * @param scalar   The multiplier to apply to the current global scale.
     * @param duration Interpolation duration in ticks.
     */
	public void scaleModel(Vec scalar, int duration) {
		scaleModel(scalar, duration, false);
	}
	
	/**
     * Rescales the model by a multiplier relative to its current scale.
     * <p>
     * Note: This is cumulative. Calling this with (2,2,2) twice will result 
     * in a 4x total scale increase.
     * </p>
     *
     * @param scalar    The multiplier to apply to the current global scale.
     * @param duration  Interpolation duration in ticks.
     * @param force     If {@code true}, bypasses the similarity check and forces a metadata update.
     */
	public void scaleModel(Vec scalar, int duration, boolean force) {
		Vec newScale = globalScale.mul(scalar);
		setModelScale(newScale, duration, force);
	}
	
	
	
	/**
     * Updates the model's global scale, proportionally resizing and repositioning all elements.
     *
     * @param newScale  The target scale vector (1.0 is default).
     * @param duration  Interpolation duration in ticks.
     */
	public void setModelScale(Vec newScale, int duration) {
		setModelScale(newScale, duration, false);
	}
	
	/**
	 * Updates the model's global scale, proportionally resizing and repositioning all elements.
     * <p>
     * Elements are scaled relative to the model's origin (0,0,0). Their translation is
     * recalculated to maintain structural integrity at different sizes.
     * </p>
	 * 
	 * @param newScale  The target scale vector (1.0 is default).
     * @param duration  Interpolation duration in ticks.
     * @param force     If {@code true}, bypasses the similarity check and forces a metadata update.
	 */
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
	
	
	
	/**
     * Translates (offsets) the model.
     *
     * @param delta		The relative translation vector.
     * @param duration	Interpolation duration in ticks.
     */
	public void translate(Vec delta, int duration) {
		Vec newTranslation = this.globalTranslation.add(delta);
	    setModelTranslation(newTranslation, duration, false);
	}
	
	/**
     * Translates (offsets) the model.
     * <p>
     * Note: This is cumulative. Calling this with (2,2,2) twice will result 
     * in a (4,4,4) translation
     * </p>
     *
     * @param delta		The relative translation vector.
     * @param duration	Interpolation duration in ticks.
     * @param force     If {@code true}, bypasses the similarity check and forces a metadata update.
     */
	public void translate(Vec delta, int duration, boolean force) {
		Vec newTranslation = this.globalTranslation.add(delta);
	    setModelTranslation(newTranslation, duration, force);
	}
	
	
	
	/**
     * Sets the model translation.
     *
     * @param newTranslation The target translation vector.
     * @param duration       Interpolation duration in ticks.
     */
    public void setModelTranslation(Vec newTranslation, int duration) {
        setModelTranslation(newTranslation, duration, false);
    }
	
	/**
     * Sets the model translation.
     * <p>
     * This moves elements relative to their parent's position. It updates 
     * transformation interpolation durations to ensure a smooth transition.
     * </p>
     *
     * @param newTranslation The target translation vector.
     * @param duration       Interpolation duration in ticks.
     * @param force          If {@code true}, bypasses the similarity check and forces a metadata update.
     */
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
	
	
	/**
	 * Rotates the model relatively by a given delta quaternion.
	 * 
	 * @param delta    The rotation change to apply to the current state.
	 * @param duration Interpolation duration in ticks.
	 */
	public void rotateModelLerp(Quaternionf delta, int duration) {						
		rotateModelLerp(delta, duration, false);
	}
	
	
	/**
	 * Rotates the model relatively by a given delta quaternion.
	 * 
	 * <p>
	 * This is cumulative: it adds the delta to the current global rotation.
	 * </p>
	 * 
	 * @param delta    The rotation change to apply to the current state.
	 * @param duration Interpolation duration in ticks.
	 * @param force    If {@code true}, bypasses the similarity check and forces a metadata update.
	 */
	public void rotateModelLerp(Quaternionf delta, int duration, boolean force) {						
		Quaternionf targetRotation = new Quaternionf(this.globalRotation).premul(delta);
		setModelRotationLerp(targetRotation, duration, force);
	}
	
	
	/**
	 * Basic linear rotation (LERP). 
	 * Faster but might look "off" during large rotations.
	 * 
     * @param newRotation	The target quaternion rotation.
     * @param duration      Interpolation duration in ticks.
	 */
	public void setModelRotationLerp(Quaternionf newRotation, int duration) {						
		setModelRotationLerp(newRotation, duration, false);
	}
	
	/**
	 * Basic linear rotation (LERP). 
	 * Faster but might look "off" during large rotations.
	 * 
     * @param newRotation	The target quaternion rotation.
     * @param duration      Interpolation duration in ticks.
     * @param force         If {@code true}, bypasses the similarity check and forces a metadata update.
	 */
	public void setModelRotationLerp(Quaternionf newRotation, int duration, boolean force) {					
		if (!force && globalRotation.equals(newRotation, 1e-6f)) return;
		
		cancelRotationTasks(globalRotation, false);
		
		this.globalRotation.set(newRotation);
		this.globalTransformationDuration = duration;
		
		forEachPassenger((passenger) -> {
			setPassengerRotation(passenger, newRotation, duration);
		});
	}
	
	
	
	
	
	/*
	 *  SLERP ROTATIONS
	 */
	
	
	/**
	 * Rotates the model relativly, by a given delta quaternion.
	 * Using smooth spherical rotation (SLERP)
	 * 
	 * <p>
	 * This is cumulative: it adds the delta to the current global rotation.
	 * </p>
	 * 
	 * @param delta    		The rotation change to apply to the current state.
	 * @param duration 		Interpolation duration in ticks.
	 */
	public void rotateModelSlerp(Quaternionf delta, int duration) {
		rotateModelSlerp(delta, duration, 22.5f);
	}
	
	/**
	 * Rotates the model relativly, by a given delta quaternion.
	 * Using smooth spherical rotation (SLERP)
	 * 
	 * <p>
	 * This is cumulative: it adds the delta to the current global rotation.
	 * </p>
	 * 
	 * @param delta    		The rotation change to apply to the current state.
	 * @param duration 		Interpolation duration in ticks.
	 * @param stepFactor	How much degrees needed for single update. Default 22.5f.
	 */
	public void rotateModelSlerp(Quaternionf delta, int duration, float stepFactor) {
		rotateModelSlerp(delta, duration, stepFactor, false);
	}
	
	/**
	 * Rotates the model relativly, by a given delta quaternion.
	 * Using smooth spherical rotation (SLERP)
	 * 
	 * <p>
	 * This is cumulative: it adds the delta to the current global rotation.
	 * </p>
	 * 
	 * @param delta    		The rotation change to apply to the current state.
	 * @param duration 		Interpolation duration in ticks.
	 * @param stepFactor	How much degrees needed for single update. Default 22.5f.
	 * @param force 	 	If {@code true}, bypasses the similarity check and forces a metadata update.
	 */
	public void rotateModelSlerp(Quaternionf delta, int duration, float stepFactor, boolean force) {
		Quaternionf targetRotation = new Quaternionf(this.globalRotation).premul(delta);
		setModelRotationSlerp(targetRotation, duration, stepFactor, force);
	}
	
	/**
	 * Smooth spherical rotation (SLERP). 
	 * Best for natural movements and constant angular velocity.
	 * 
	 * @param newRotation   New model rotation.
     * @param duration  	Interpolation duration in ticks.
	 */
	public void setModelRotationSlerp(Quaternionf newRotation, int duration) {
		setModelRotationSlerp(newRotation, duration, 22.5f);
	}

	/**
	 * Smooth spherical rotation (SLERP). 
	 * Best for natural movements and constant angular velocity.
	 * 
	 * @param newRotation   New model rotation.
     * @param duration  	Interpolation duration in ticks.
     * @param stepFactor	How much degrees needed for single update. Default 22.5f.
	 */
	public void setModelRotationSlerp(Quaternionf newRotation, int duration, float stepFactor) {
		setModelRotationSlerp(newRotation, duration, stepFactor, false);
	}
	
	
	/**
	 * Smooth spherical rotation (SLERP). 
	 * Best for natural movements and constant angular velocity.
	 * 
	 * @param newRotation   New model rotation.
     * @param duration  	Interpolation duration in ticks.
     * @param stepFactor	How much degrees needed for single update. Default 22.5f.
     * @param force     	If {@code true}, bypasses the similarity check and forces a metadata update.
	 */
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
			setModelRotationLerp(newRotation, duration, force);
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
			holder.task = scheduler().buildTask(() -> {
				applyRotationToAll(nextRotation, stepDuration);
	        	ongoingRotations.remove(holder.task);
			}).delay(TaskSchedule.tick(delayTicks)).schedule();
			
			ongoingRotations.add(holder.task);
		}
		
	}
	
	private static class TaskHolder {
		Task task;
	}
	
	private void applyRotationToAll(Quaternionf rot, int dur) {
	    forEachPassenger(p -> setPassengerRotation(p, rot, dur));
	}
	
	
	
	/**
	 * Cancels ongoing rotations quaternion rotations
	 * 
	 * @param finalRotation	If finish is {@code true}, this specifies 
	 * 		  model's final rotation 
	 * @param finish		If {@code true}, sets final rotation of the model
	 * 						
	 */
	public void cancelRotationTasks(Quaternionf finalRotation, boolean finish) {
		if (ongoingRotations.isEmpty()) return;
		
		for (Task rotTask : ongoingRotations) {
			rotTask.cancel();
		}
		
		if (finish) setModelRotationLerp(finalRotation, 0);
		
		ongoingRotations.clear();
	}
	

	
	
	/*public void interpolateModelRotation3(Quaternionf newRotation, int duration) {
//		 1. Policz jaki jest całkowity obrót
//		 2. podziel ten całkowity obrót w każdej osi przez 22.5
//		 3. weź największą wartosć z tych 3 wartości
//		 4. przesuń tak jak powinno być od razu (tylko po stronie serwera)
//		 5. zrób każdy updatepacket dla każdej fazy obrotu
//		 6. podziel duration przez tyle ile jest pakietów
//		 7. wysyał kolejny packet raz na duration 
		
		AbstractDisplayMeta rootMeta = (AbstractDisplayMeta) getEntityMeta();
		
		Point point = rootMeta.getTranslation();
		Vec scale = rootMeta.getScale();
		Vector3f pos = VecUtils.pointToJomlVec3(point);
		Vector3f center = VecUtils.pointToJomlVec3(scale.div(2));
		System.out.println("Pos: " + pos);
		System.out.println("Center: " + center);
		Vector3f pivotPoint = new Vector3f(pos).add(center);
		
		Quaternionf startRot = MatrixUtils.arrayToQuaternion(rootMeta.getLeftRotation());
		Quaternionf diff = new Quaternionf(startRot).difference(newRotation);
		
		float diffAngleRad = diff.angle();
		float diffAngleDeg = (float) Math.toDegrees(diffAngleRad);
		
		System.out.println("Diff  angle: " + diffAngleDeg);		
		
		int numSteps = (int) Math.ceil(diffAngleDeg / 22.5f);
		
		if (numSteps < 1) numSteps = 1;
		System.out.println("Num steps: " + numSteps);		
		
		float ticksPerStep = (float) duration / numSteps;
		System.out.println("Tick per step: " + ticksPerStep);		
		
		Quaternionf stepRotation = new Quaternionf().slerp(diff, 1.0f / numSteps);
		System.out.println("Step rotation: " + stepRotation);
		
		 if (ticksPerStep <= 0) {
	       	forEachPassenger((passenger) -> {	        		
	       		applyRotationStep(passenger, diff, pivotPoint, 0);
	       	});
	       	return;
       }
				
		for (int i = 0; i < numSteps; i++) {
			// 7. Wysyłaj pakiet z opóźnieniem
			int delayTicks = (int) (i * ticksPerStep);

	        System.out.println("Step " + i + ", delay: " + delayTicks);		
	        
	        if (delayTicks == 0) {
	        	forEachPassenger((passenger) -> {	        		
	        		applyRotationStep(passenger, stepRotation, pivotPoint, (int) Math.ceil(ticksPerStep));
	        	});
	        	continue;
	        }
	        
	        scheduler().buildTask(() -> {
	            // Tutaj Twoja metoda rotateBy, ale uważaj:
	            // Musisz przekazać kwaternion RELATYWNY do poprzedniego kroku
	            // ALBO (lepiej) zmodyfikować rotateBy, by przyjmowała absolutny cel od startu
	        	forEachPassenger((passenger) -> {	        		
	        		applyRotationStep(passenger, stepRotation, pivotPoint, (int) Math.ceil(ticksPerStep));
	        	});
	        }).delay(TaskSchedule.tick(delayTicks)).schedule();
	    }
		
	}*/
	
	
	private void setPassengerRotation(BDPassengerEntity entity, Quaternionf newRotation, int duration) {
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
	
	private void applyRotation(BDPassengerEntity entity, Vector3f baseTranslation, Quaternionf baseLeftRot, 
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
	
	
	
	/*
	 *  DEBUG
	 */
	
	
	/**
     * Directly sets the left rotation of all passenger entities.
     * <p>
     * <b>DANGER:</b> This is a raw internal method used primarily for debugging. 
     * It does not update the model's internal state (globalRotation), meaning 
     * subsequent calls to {@code setRotation()} or {@code rotateModel()} 
     * will completely override these changes.
     * </p>
     * * @deprecated Use {@link #setModelRotation(Quaternionf, int)} for consistent behavior.
     */
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

	/**
     * Directly sets the right rotation of all passenger entities.
     * <p>
     * <b>DANGER:</b> This is a raw internal method used primarily for debugging. 
     * It does not update the model's internal state (globalRotation), meaning 
     * subsequent calls to {@code setRotation()} or {@code rotateModel()} 
     * will completely override these changes.
     * </p>
     * * @deprecated Use {@link #setModelRotation(Quaternionf, int)} for consistent behavior.
     */
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
	
	private void forEachPassenger(Consumer<BDPassengerEntity> action) {
	    for (Entity entity : getPassengers()) {
	        if (entity instanceof BDPassengerEntity passenger) {
	            action.accept(passenger);
	        } else {
	        	LOGGER.warn("Unexpected passenger type: {}", entity.getClass().getSimpleName());
	        }
	    }
	}
}
