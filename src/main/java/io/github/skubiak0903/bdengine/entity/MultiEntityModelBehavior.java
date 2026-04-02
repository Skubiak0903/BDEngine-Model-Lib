package io.github.skubiak0903.bdengine.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;

public class MultiEntityModelBehavior extends ModelBehavior {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiEntityModelBehavior.class);
	
	private final List<BDBaseModelEntity> waitingPassengersList;
	
	private final BDBaseModelEntity entity;
	
	
	public MultiEntityModelBehavior(BDBaseModelEntity entity, List<BDModelEntitySchema> entitySchemas) {
		this(entity, entitySchemas, Vec.ONE, Vec.ZERO);
	}
		
	public MultiEntityModelBehavior(BDBaseModelEntity entity, List<BDModelEntitySchema> entitySchemas, Vec initialScale, Vec initialTranslation) {
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
			setEntityTransformationInterpolationDuration(target, duration);
		});
	}
	
	
	@Override
	public void setPosRotInterpolationDuration(int duration, boolean force) {
		if (!force && globalPosRotDuration == duration) return;
		
		globalPosRotDuration = duration;
		forEachPassengerAndSelf((target) -> {
			setEntityPosRotInterpolationDuration(target, duration);
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
        	Vec defScale = passenger.getDefaultScale();
            Vec defTranslation = passenger.getDefaultTranslation();
            
            setEntityScale(passenger, newScale, defScale, defTranslation, duration);
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
            Vec defTranslation = passenger.getDefaultTranslation();

			setEntityTranslation(passenger, newTranslation, defTranslation, duration);
	    });
	}
	
	
	
	
	/*
	 *  LERP ROTATIONS
	 */
	
	@Override
	public void setModelRotationLerp(Quaternionf newRotation, int duration, boolean force) {					
		if (!force && globalRotation.equals(newRotation, ROTATION_EPSILON)) return;
		
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
		internalSlerpRotation((nextRotation, stepDuration) -> {
			applyRotationToAll(nextRotation, stepDuration);
		}, newRotation, duration, stepFactor, force);
	}
	
	
	private void applyRotationToAll(Quaternionf rot, int dur) {
	    forEachPassenger(p -> setPassengerRotation(p, rot, dur));
	}
	
	
	private void setPassengerRotation(PassengerEntity entity, Quaternionf newRotation, int duration) {
		Vector3f baseTranslation = VecUtils.pointToJomlVec3(entity.getDefaultTranslation().mul(globalScale).add(globalTranslation));
		Quaternionf defRotation = entity.getDefaultLeftRotation();
		
		setEntityRotation(
				entity,
				baseTranslation, 
				defRotation, 
				newRotation, 
				duration
			);
	}
	
	
	
	@Override
	@Deprecated
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		forEachPassenger((passenger) -> {
			setEntityLeftRotation(passenger, leftQuat, duration);
		});
	}

	@Override
	@Deprecated
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		forEachPassenger((passenger) -> {
			setEntityRightRotation(passenger, rightQuat, duration);
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
