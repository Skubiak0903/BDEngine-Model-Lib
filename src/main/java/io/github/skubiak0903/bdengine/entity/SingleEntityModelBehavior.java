package io.github.skubiak0903.bdengine.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.skubiak0903.bdengine.math.Transformation;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import lombok.AccessLevel;
import lombok.Getter;
import net.minestom.server.coordinate.Vec;

@Getter
public class SingleEntityModelBehavior extends ModelBehavior {
	//private static final Logger LOGGER = LoggerFactory.getLogger(BDSingleModelEntity.class);
	
	@Getter(value = AccessLevel.NONE)
	private final BDBaseModelEntity entity;	
	
	private final Transformation defTransform;

	
	
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
		
		this.defTransform = entitySchema.getTransformation();
	}
	
	
	
	/*
	 *  INTERPOLATION DURATION
	 */
	
	@Override
	public void setTransformationInterpolationDuration(int duration, boolean force) {
		if (!force && globalTransformationDuration == duration) return;
		
		globalTransformationDuration = duration;
		setEntityTransformationInterpolationDuration(entity, duration);
	}
	
	
	@Override
	public void setPosRotInterpolationDuration(int duration, boolean force) {
		if (!force && globalPosRotDuration == duration) return;
		
		globalPosRotDuration = duration;
		setEntityPosRotInterpolationDuration(entity, duration);
	}
	
	
	
	/*
	 * 	 SCALE
	 */
	
	@Override
	public void setModelScale(Vec newScale, int duration, boolean force) {
		if (!force && VecUtils.isSimilar(globalScale, newScale)) return; // skip if scale is similar
		
		this.globalScale = newScale;
	    this.globalTransformationDuration = duration; // no need to check if duration is the same
	    
        setEntityScale(entity, newScale, defTransform.getScaleAsVec(), defTransform.getTranslationAsVec(), duration);
	}
	
	
	/*
	 *  TRANSLATIONS
	 */

	@Override
	public void setModelTranslation(Vec newTranslation, int duration, boolean force) {
		if (!force && VecUtils.isSimilar(globalTranslation, newTranslation)) return;
		
		this.globalTranslation = newTranslation;
		this.globalTransformationDuration = duration;

        setEntityTranslation(entity, newTranslation, defTransform.getTranslationAsVec(), duration);
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
		
		setThisEntityRotation(newRotation, duration);
	}
	
	
	
	
	
	/*
	 *  SLERP ROTATIONS
	 */
	
	@Override
	public void setModelRotationSlerp(Quaternionf newRotation, int duration, float stepFactor, boolean force) {
		internalSlerpRotation((nextRotation, stepDuration) -> {
			setThisEntityRotation(nextRotation, stepDuration);
		},newRotation, duration, stepFactor, force);

		
	}
	
	
	private void setThisEntityRotation(Quaternionf newRotation, int duration) {
		Vector3f baseTranslation = VecUtils.pointToJomlVec3(defTransform.getTranslationAsVec().mul(globalScale).add(globalTranslation));
		Quaternionf defLeftRot = new Quaternionf(defTransform.getLeftRotation());
		Quaternionf defRightRot = new Quaternionf(defTransform.getRightRotation());
		
		setEntityRotation(
				entity,
				baseTranslation, 
				defLeftRot, 
				defRightRot,
				newRotation, 
				duration
			);
	}
	
	
	@Override
	@Deprecated
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		setEntityLeftRotation(entity, leftQuat, duration);
	}

	@Override
	@Deprecated
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		setEntityRightRotation(entity, rightQuat, duration);
	}
}
