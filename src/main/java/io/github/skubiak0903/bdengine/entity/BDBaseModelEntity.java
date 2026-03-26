package io.github.skubiak0903.bdengine.entity;

import java.util.function.Consumer;

import org.joml.Quaternionf;

import lombok.Getter;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.timer.TaskSchedule;

@Getter
public class BDBaseModelEntity extends Entity {
	private final Vec defaultScale;
	private final Vec defaultTranslation;
	private final Quaternionf defaultRightRotation;
	private final Quaternionf defaultLeftRotation;
		
	public BDBaseModelEntity(EntityType entityType) {
		this(entityType, Vec.ONE, Vec.ZERO, new Quaternionf().identity(), new Quaternionf().identity());
	}
	
	public BDBaseModelEntity(EntityType entityType, Vec defaultScale, Vec defaultTranslation, Quaternionf defaultRightRotation, Quaternionf defaultLeftRotation) {
		super(entityType);
		this.defaultScale = defaultScale;
		this.defaultTranslation = defaultTranslation;
		this.defaultRightRotation = defaultRightRotation;
		this.defaultLeftRotation = defaultLeftRotation;
	}
	
	@Override
	public void tick(long time) {
		// do nothing
		scheduler().processTick();
		scheduler().processTickEnd();
	}
	
	
	public void schedule(Consumer<BDBaseModelEntity> callback, TaskSchedule duration) {
		scheduler().buildTask(
	 			() -> callback.accept(this)
		).delay(duration).schedule();
	}
	
}
