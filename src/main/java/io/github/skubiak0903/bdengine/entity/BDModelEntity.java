package io.github.skubiak0903.bdengine.entity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.skubiak0903.bdengine.utils.VecUtils;
import lombok.Getter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Instance;

@Getter
public class BDModelEntity extends BDBaseModelEntity {
	public static final float SLERP_ANGLE = 22.5f;
	
    private final ModelBehavior behavior;
	
	
	public BDModelEntity(BDModelEntitySchema schema) {
		this(List.of(schema));
	}
	
	public BDModelEntity(BDModelEntitySchema schema, Vec initialScale, Vec initialTranslation) {
		this(List.of(schema), initialScale, initialTranslation);
	}
	
	public BDModelEntity(List<BDModelEntitySchema> schemas) {
		this(schemas, Vec.ONE, Vec.ZERO);
	}
	
	public BDModelEntity(List<BDModelEntitySchema> schemas, Vec initialScale, Vec initialTranslation) {
		EntityType type = schemas.size() > 1 ? EntityType.BLOCK_DISPLAY : schemas.get(0).getType().getEntityType();
		super(type);
		
		this.behavior = schemas.size() > 1 ? 
				new MultiEntityModelBehavior(this, schemas, initialScale, initialTranslation) :
				new SingleEntityModelBehavior(this, schemas.get(0), initialScale, initialTranslation);
	}
	
	
	
	/*
	 * Overrides
	 */
	@Override
	public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
		var future = super.setInstance(instance, spawnPosition);
		return future.thenAccept((_) -> {
			behavior.setInstance(instance, spawnPosition);
		});
	}
	
	@Override
	protected void remove(boolean permanent) {
		behavior.remove(permanent);
		super.remove(permanent);
	}
	
	@Override
	public void setView(float yaw, float pitch, float headRotation) {
		behavior.setView(yaw, pitch);
		super.setView(yaw, pitch, headRotation);
	}
	
	@Override
	public CompletableFuture<Void> teleport(Pos position, Vec velocity, long @Nullable [] chunks, int flags,
			boolean shouldConfirm) {
		behavior.teleport(position);
		return super.teleport(position, velocity, chunks, flags, shouldConfirm);
	}
	
	
	
	
	/*
	 * Getters
	 */
	public Vec getGlobalScale() {
		return behavior.globalScale;
	}

	public Vec getGlobalTranslation() {
		return behavior.globalTranslation;
	}

	public Quaternionf getGlobalRotation() {
		return behavior.globalRotation;
	}

	public Vector3f getPivotPoint() {
		return behavior.pivotPoint;
	}

	public int getGlobalPosRotDuration() {
		return behavior.globalPosRotDuration;
	}

	public int getGlobalTransformationDuration() {
		return behavior.globalTransformationDuration;
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
	 * Changes the model's pivot point, used in model rotation.	 
	 * <b>WARNING</b>: Doesn't update the model after changing.
	 * 
	 * @param newPivotPoint New pivot point vector
	 */
	public void setPivotPoint(Vec newPivotPoint) {
		this.setPivotPoint(VecUtils.pointToJomlVec3(newPivotPoint));
	}
	
	/**
	 * Changes the model's pivot point, used in model rotation.	 
	 * <b>WARNING</b>: Doesn't update the model after changing.
	 * 
	 * @param newPivotPoint New pivot point vector
	 */
	public void setPivotPoint(Vector3f newPivotPoint) {
		this.setPivotPoint(newPivotPoint, false);
	}
	
	/**
	 * Changes the model's pivot point, used in model rotation.	 
	 * 
	 * @param newPivotPoint New pivot point vector
	 * @param update 		Updates model positon with correct translation with new pivot point.
	 */
	public void setPivotPoint(Vec newPivotPoint, boolean update) {
		this.setPivotPoint(VecUtils.pointToJomlVec3(newPivotPoint), update);
	}
	
	/**
	 * Changes the model's pivot point, used in model rotation.	 
	 * 
	 * @param newPivotPoint New pivot point vector
	 * @param update 		Updates model positon with correct translation with new pivot point.
	 */
	public void setPivotPoint(Vector3f newPivotPoint, boolean update) {
		behavior.pivotPoint.set(newPivotPoint);
		if (update) translateModel(Vec.ZERO, 0, true); // translate by empty vector to update position
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
		behavior.setTransformationInterpolationDuration(duration, force);
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
		behavior.setPosRotInterpolationDuration(duration, force);
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
		Vec newScale = behavior.globalScale.mul(scalar);
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
		behavior.setModelScale(newScale, duration, force);
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
	public void translateModel(Vec delta, int duration) {
		translateModel(delta, duration, false);
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
	public void translateModel(Vec delta, int duration, boolean force) {
		Vec newTranslation = behavior.globalTranslation.add(delta);
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
		behavior.setModelTranslation(newTranslation, duration, force);
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
		Quaternionf targetRotation = new Quaternionf(behavior.globalRotation).premul(delta);
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
		behavior.setModelRotationLerp(newRotation, duration, force);
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
		rotateModelSlerp(delta, duration, SLERP_ANGLE);
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
		Quaternionf targetRotation = new Quaternionf(behavior.globalRotation).premul(delta);
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
		setModelRotationSlerp(newRotation, duration, SLERP_ANGLE);
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
		behavior.setModelRotationSlerp(newRotation, duration, stepFactor, force);
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
		behavior.cancelRotationTasks(finalRotation, finish);
	}
	
	
	
	
	/*
	 *  DEBUG
	 */
	
	
	/**
     * Directly sets the left rotation of all passenger entities.
     * <p>
     * <b>DANGER:</b> This is a raw internal method used primarily for debugging. 
     * It does not update the model's internal state (globalRotation), meaning 
     * subsequent calls to {@code setModelRotationLerp()} or other methods 
     * manipulating rotation will completely override these changes.
     * </p>
     * @deprecated Use {@link #setModelRotationLerp(Quaternionf, int)} for consistent behavior.
     */
	@Deprecated
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		behavior.setLeftRotation(leftQuat, duration);
	}

	/**
     * Directly sets the right rotation of all passenger entities.
     * <p>
     * <p>
     * <b>DANGER:</b> This is a raw internal method used primarily for debugging. 
     * It does not update the model's internal state (globalRotation), meaning 
     * subsequent calls to {@code setModelRotationLerp()} or other methods 
     * manipulating rotation will completely override these changes.
     * </p>
     * @deprecated Use {@link #setModelRotationLerp(Quaternionf, int)} for consistent behavior.
     */
	@Deprecated
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		behavior.setRightRotation(rightQuat, duration);
	}
	
}
