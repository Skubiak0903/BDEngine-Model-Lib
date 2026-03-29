package io.github.skubiak0903.bdengine.entity;

import java.util.function.Consumer;

import lombok.Getter;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.timer.TaskSchedule;

@Getter
public class BDBaseModelEntity extends Entity {
	
	public BDBaseModelEntity(EntityType entityType) {
		super(entityType);
		
		setSynchronizationTicks(Long.MAX_VALUE); // disable synchronization ticks, messes with teleportation animation
	}

	
	@Override
	public void tick(long time) {
		scheduler().processTick();
		scheduler().processTickEnd();
	}
	
	public void schedule(Consumer<BDBaseModelEntity> callback, TaskSchedule duration) {
		scheduler().buildTask(
	 			() -> callback.accept(this)
		).delay(duration).schedule();
	}
	
}
