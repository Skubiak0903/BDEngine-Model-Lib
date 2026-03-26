package io.github.skubiak0903.core.bdengine.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skubiak0903.core.utils.MatrixUtils;
import io.github.skubiak0903.core.utils.VecUtils;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta.DisplayContext;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.timer.TaskSchedule;

public class BDModelEntity extends BDBaseModelEntity {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDModelEntity.class);
	
	@Getter
	private final List<BDBaseModelEntity> waitingPassengersList;
	
	@Getter
	private Vec globalScale = Vec.ONE;
	
	public BDModelEntity(List<BDModelEntitySchema> entitySchemas) {
		this(entitySchemas, Vec.ONE);
	}
		
	public BDModelEntity(List<BDModelEntitySchema> entitySchemas, Vec initialScale) {
		super(EntityType.BLOCK_DISPLAY);
		if (entitySchemas == null) throw new IllegalArgumentException("entitySchemas cannot be null");
		
		List<BDBaseModelEntity> tempList = new ArrayList<>();
		
		for (var schema : entitySchemas) {
			var entity = createPassengerEntity(schema, initialScale);
			tempList.add(entity);
		}
		
		this.waitingPassengersList = new ArrayList<>(tempList);
		
		this.globalScale = initialScale;
	}
	

	@Override
	public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
		var future = super.setInstance(instance, spawnPosition);
		// we need to add passengers here becouse Entity#addPassenger requires for passenger entity to be registered (eg. instance need to be set)
		
		future.thenAccept((_) -> {
			waitingPassengersList.forEach((entity) -> {
				entity.setInstance(instance, spawnPosition);
				this.acquirable().sync((root) -> {
					root.addPassenger(entity);
				});
			});
			waitingPassengersList.clear();
		});
		
		// clear list after successful passenger initialization
		
		return future;
	}

	
	public void setModelScale(Vec scale, int duration) {
		this.globalScale = scale;
	    // Aplikuj na root
	    this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	        meta.setScale(scale);
	        meta.setTranslation(Vec.ZERO);
	    });
	    
	    // Aplikuj na każdego pasażera
	    forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	            // Oryginalne wartości
	            Vec origPos = passenger.getDefaultTranslation();
	            Vec origScale = passenger.getDefaultScale();
	            Quaternionf origLeft = passenger.getDefaultLeftRotation();
	            Quaternionf origRight = passenger.getDefaultRightRotation();
	            
	            // Przekształć pozycję przez macierz skalowania
	            float x = (float) (origPos.x() * scale.x());
	            float y = (float) (origPos.y() * scale.y());
	            float z = (float) (origPos.z() * scale.z());
	            Vec newPos = new Vec(x, y, z);
	            
	            // Skala jest mnożona
	            Vec newScale = origScale.mul(scale);
	            
	            // Rotacje pozostają bez zmian (skala nie wpływa na rotację)
	            meta.setTranslation(newPos);
	            meta.setScale(newScale);
	            meta.setLeftRotation(MatrixUtils.toArray(origRight));
	            meta.setRightRotation(MatrixUtils.toArray(origLeft));
	            
	            meta.setTransformationInterpolationDuration(duration);
	            meta.setPosRotInterpolationDuration(duration);
	            meta.setTransformationInterpolationStartDelta(0);
	            
	        });
	    });
	}
	
	@Override
	protected void remove(boolean permanent) {
		forEachPassenger((passenger) -> {
			passenger.remove();
		});
		super.remove(permanent);
	}
	
	@Deprecated
	public void setScaleNoHeightTranslation(Vec globalScale) {
		this.globalScale = globalScale;
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setScale(this.getDefaultScale().mul(globalScale));
			meta.setTranslation(this.getDefaultTranslation().mul(globalScale.x(), 1, globalScale.z()));
		});
		
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setScale(passenger.getDefaultScale().mul(globalScale));
				meta.setTranslation(passenger.getDefaultTranslation().mul(globalScale.x(), 1, globalScale.z()));
			});
		});
	}
	
	public void setTransformationInterpolationDuration(int duration) {
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
		});
		
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setTransformationInterpolationDuration(duration);
			});
		});
	}
	
	public void setPosRotInterpolationDuration(int duration) {
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setPosRotInterpolationDuration(duration);
		});
		
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setPosRotInterpolationDuration(duration);
			});
		});
	}
	
	public void setLeftRotation(Quaternionf leftQuat, int duration) {
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
			meta.setLeftRotation(MatrixUtils.toArray(leftQuat));
			meta.setTransformationInterpolationStartDelta(0);
		});
		
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setTransformationInterpolationDuration(duration);
				meta.setLeftRotation(MatrixUtils.toArray(leftQuat));
				meta.setTransformationInterpolationStartDelta(0);
			});
		});
	}
	
	public void setRightRotation(Quaternionf rightQuat, int duration) {
		this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTransformationInterpolationDuration(duration);
			meta.setRightRotation(MatrixUtils.toArray(rightQuat));
			meta.setTransformationInterpolationStartDelta(0);
		});
		
		forEachPassenger((passenger) -> {
			passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
				meta.setTransformationInterpolationDuration(duration);
				meta.setRightRotation(MatrixUtils.toArray(rightQuat));
				meta.setTransformationInterpolationStartDelta(0);
			});
		});
	}
	
	public void setModelRotation2(Quaternionf rotation, Vector3f pivot, int duration) {
		
		forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	            // Oryginalne wartości (lokalne, bez globalnej skali)
	            Vec origPos = passenger.getDefaultTranslation();
	            //Vec origScale = passenger.getDefaultScale();
	            Quaternionf origLeft = passenger.getDefaultLeftRotation();
	            //Quaternionf origRight = passenger.getDefaultRightRotation();
	            
	            Vector3f objectPos = new Vector3f((float)origPos.x(), (float)origPos.y(), (float)origPos.z());
	            Vector3f relativePos = objectPos.sub(pivot, new Vector3f());
	            
	    		Quaternionf relativePosAsQuat = new Quaternionf(relativePos.x, relativePos.y, relativePos.z, 0); // wektor w quaternionie, z długością/skalą (w) ustawioną na 0
	            
	    		Quaternionf rotatedQuat = new Quaternionf(relativePosAsQuat).conjugateBy(rotation);
	    		Vector3f rotatedPos = new Vector3f(rotatedQuat.x, rotatedQuat.y, rotatedQuat.z);
	    		Vector3f newPos = rotatedPos.add(pivot, new Vector3f());
	   
	    		Quaternionf newLeftQuat = new Quaternionf(rotatedQuat).mul(origLeft);
	            
	            // Zastosuj konwencję Minecraft (zamienione left/right)
	            float[] leftArr = MatrixUtils.toArray(newLeftQuat);
	            float[] rightArr = MatrixUtils.toArray(new Quaternionf().identity());
	            
	            meta.setTranslation(new Vec(newPos.x, newPos.y, newPos.z));
//	            meta.setScale(newScale);
	            meta.setRightRotation(new float[] {leftArr[0], leftArr[1], leftArr[2], leftArr[3]});
	            meta.setLeftRotation(new float[] {rightArr[0], rightArr[1], rightArr[2], rightArr[3]});
	            
	            meta.setTransformationInterpolationDuration(duration);
	            meta.setPosRotInterpolationDuration(duration);
	            meta.setTransformationInterpolationStartDelta(0);
	        });
	    });
	}
	
	public void setModelRotation3(Quaternionf newRotation, int duration) {
		AbstractDisplayMeta rootMeta = (AbstractDisplayMeta) getEntityMeta();
		Point point = rootMeta.getTranslation();
		Vec scale = rootMeta.getScale();
		
		Vector3f pos = VecUtils.pointToJomlVec3(point);
		Vector3f center = VecUtils.pointToJomlVec3(scale.div(2));
		
		System.out.println("Pos: " + pos);
		System.out.println("Center: " + center);
		
		Vector3f pivotPoint = new Vector3f(pos).add(center);
		
		System.out.println("Pivot Point: " + pivotPoint);
		
		forEachPassenger((passenger) -> {
			System.out.println("----- Passenger -----");
			rotateBy(passenger, newRotation, pivotPoint, duration);
		});
	}
	
	
	private static void rotateBy(BDBaseModelEntity entity, Quaternionf newRotation, Vector3f pivotPoint, int duration) {
		AbstractDisplayMeta entityMeta = (AbstractDisplayMeta) entity.getEntityMeta();
		
		Vector3f currentTranslation = VecUtils.pointToJomlVec3(entityMeta.getTranslation());
		System.out.println("Current Translation: " + currentTranslation);
		currentTranslation.sub(pivotPoint);
		System.out.println("Translation after sub: " + currentTranslation);
//		currentTranslation.rotate(newRotation);
		newRotation.transform(currentTranslation);
		System.out.println("Translation after rotate: " + currentTranslation);
		currentTranslation.add(pivotPoint);
		
		Vector3f newTranslation = currentTranslation;
		System.out.println("New Translation: " + newTranslation);
		
		// obróć lokalnie
		Quaternionf currentLeftRot = MatrixUtils.arrayToQuaternion(entityMeta.getLeftRotation());
		System.out.println("Current Left Rot: " + currentLeftRot);
		Quaternionf newLeftRot = new Quaternionf(newRotation).mul(currentLeftRot).normalize();
		System.out.println("New Left Rot: " + newLeftRot);
		
		Quaternionf currentRightRot = MatrixUtils.arrayToQuaternion(entityMeta.getRightRotation());
		System.out.println("Current Right Rot: " + currentRightRot);
		
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTranslation(VecUtils.vec3ToMinestomVec(newTranslation));
			meta.setLeftRotation(MatrixUtils.toArray(newLeftRot));
			meta.setRightRotation(MatrixUtils.toArray(currentRightRot));
			
			meta.setTransformationInterpolationDuration(duration);
			meta.setTransformationInterpolationStartDelta(0);
		});
	}
	
	
	public void interpolateModelRotation3(Quaternionf newRotation, int duration) {
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
		
	}
	
	private void applyRotationStep(BDBaseModelEntity entity, Quaternionf stepDelta, Vector3f pivotPoint, int duration) {
	    AbstractDisplayMeta entityMeta = (AbstractDisplayMeta) entity.getEntityMeta();
	    
	    // 1. Pozycja - obracamy o MAŁY KAWAŁEK (stepDelta)
	    Vector3f pos = VecUtils.pointToJomlVec3(entityMeta.getTranslation());
	    pos.sub(pivotPoint);
	    stepDelta.transform(pos); // Obrót o np. 22.5 stopnia
	    pos.add(pivotPoint);
	    
	    // 2. Rotacja Lewa - doobracamy o MAŁY KAWAŁEK
	    Quaternionf currentLeft = MatrixUtils.arrayToQuaternion(entityMeta.getLeftRotation());
	    Quaternionf newLeft = new Quaternionf(stepDelta).mul(currentLeft).normalize();
	    
	    // 3. Wysyłka do klienta
	    entity.editEntityMeta(AbstractDisplayMeta.class, meta -> {
	    	meta.setTranslation(VecUtils.vec3ToMinestomVec(pos));
	    	meta.setLeftRotation(MatrixUtils.toArray(newLeft));
	        
	    	meta.setTransformationInterpolationDuration(duration);
	    	meta.setTransformationInterpolationStartDelta(0);
	    });
	}
	
	public void setModelRotation(Vector3f xyzRot, int duration) {
		
		forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {

	            Quaternionf origLeft = passenger.getDefaultLeftRotation();
	   
	    		Quaternionf newLeftQuat = new Quaternionf(origLeft).rotateXYZ(xyzRot.x, xyzRot.y, xyzRot.z);
	            
	            meta.setLeftRotation (MatrixUtils.toArray(newLeftQuat));
	            meta.setRightRotation(MatrixUtils.toArray(new Quaternionf().identity()));
	            
	            meta.setTransformationInterpolationDuration(duration);
	            meta.setPosRotInterpolationDuration(duration);
	            meta.setTransformationInterpolationStartDelta(0);
	        });
	    });
	}
	
	public void setModelTranslation(Vec translation, int duration) {
		
		forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	            meta.setTranslation(translation);
	            
	            meta.setTransformationInterpolationDuration(duration);
	            meta.setPosRotInterpolationDuration(duration);
	            meta.setTransformationInterpolationStartDelta(0);
	        });
	    });
	}
	
	@Override
	public void setView(float yaw, float pitch) {
		super.setView(yaw, pitch);
		forEachPassenger((passenger) -> {
			passenger.setView(yaw, pitch);
		});
	}
	
	@Override
	public CompletableFuture<Void> teleport(Pos position, Vec velocity, long @Nullable [] chunks, int flags, boolean shouldConfirm) {
		forEachPassenger((passenger) -> {
			passenger.teleport(position, velocity, chunks, flags, shouldConfirm);
		});
		return super.teleport(position, velocity, chunks, flags, shouldConfirm);
	}
	
	public void setModelRotation(Quaternionf rotation, Vec pivotPoint, int duration) {	    
	    // Aplikuj na root
	    this.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	        float[] rot = MatrixUtils.toArray(rotation);
	        // Minecraft konwencja: left i right zamienione, x,y,z z minusem
	        meta.setLeftRotation(new float[] {-rot[0], -rot[1], -rot[2], rot[3]});
	        meta.setRightRotation(new float[] {0, 0, 0, 1}); // identity
	        meta.setTranslation(pivotPoint.mul(globalScale));
	    });
	    
	    // Aplikuj na każdego pasażera
	    forEachPassenger((passenger) -> {
	        passenger.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
	            // Oryginalne wartości (lokalne, bez globalnej skali)
	            Vec origPos = passenger.getDefaultTranslation();
	            Vec origScale = passenger.getDefaultScale();
	            Quaternionf origLeft = passenger.getDefaultLeftRotation();
	            Quaternionf origRight = passenger.getDefaultRightRotation();
	            
	            // Przesuń względem pivot point, przeskaluj, obróć
	            Vector3f relPos = new Vector3f(
	                (float) ((origPos.x() - pivotPoint.x()) * globalScale.x()),
	                (float) ((origPos.y() - pivotPoint.y()) * globalScale.y()),
	                (float) ((origPos.z() - pivotPoint.z()) * globalScale.z())
	            );
	            
	            // Obróć względną pozycję
	            relPos.rotate(rotation);
	            
	            // Nowa pozycja = pivotPoint + obrócona względna pozycja
	            Vec newPos = new Vec(
	                pivotPoint.x() * globalScale.x() + relPos.x(),
	                pivotPoint.y() * globalScale.y() + relPos.y(),
	                pivotPoint.z() * globalScale.z() + relPos.z()
	            );
	            
	            // Skala: oryginalna skala * globalScale (skala nie zmienia się przy rotacji)
	            Vec newScale = origScale.mul(globalScale);
	            
	            // Rotacje: left = rotation * origLeft, right = origRight
	            Quaternionf newLeft = rotation.mul(origLeft, new Quaternionf());
	            Quaternionf newRight = origRight;
	            
	            // Zastosuj konwencję Minecraft (zamienione left/right)
	            float[] leftArr = MatrixUtils.toArray(newRight);
	            float[] rightArr = MatrixUtils.toArray(newLeft);
	            
	            meta.setTranslation(newPos);
	            meta.setScale(newScale);
	            meta.setRightRotation(new float[] {leftArr[0], leftArr[1], leftArr[2], leftArr[3]});
	            meta.setLeftRotation(new float[] {rightArr[0], rightArr[1], rightArr[2], rightArr[3]});
	            
	            meta.setTransformationInterpolationDuration(duration);
	            meta.setPosRotInterpolationDuration(duration);
	            meta.setTransformationInterpolationStartDelta(0);
	        });
	    });
	}
	
	private void forEachPassenger(Consumer<BDBaseModelEntity> action) {
	    for (Entity entity : getPassengers()) {
	        if (entity instanceof BDBaseModelEntity passenger) {
	            action.accept(passenger);
	        } else {
	        	LOGGER.warn("Unexpected passenger type: {}", entity.getClass().getSimpleName());
	        }
	    }
	}
		
	private static BDBaseModelEntity createPassengerEntity(BDModelEntitySchema schema, Vec initialScale) {
		EntityType type = schema.isItemDisplay() ? EntityType.ITEM_DISPLAY : EntityType.BLOCK_DISPLAY;
		
		// Very wierd; left rotation is right, and vice versa
		// also all we are changing signs to all, besides `w` parameter
		
		Quaternionf oldRight = schema.rotationRight;
		Quaternionf oldLeft = schema.rotationLeft;
		Quaternionf rightRot = new Quaternionf(-oldRight.x , -oldRight.y , -oldRight.z , +oldRight.w);
		Quaternionf leftRot  = new Quaternionf(-oldLeft.x  , -oldLeft.y  , -oldLeft.z  , +oldLeft.w);
		
		BDBaseModelEntity entity = new BDBaseModelEntity(type, schema.scale(), schema.translation(), rightRot, leftRot);
		
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			meta.setTranslation(schema.translation().mul(initialScale));
			meta.setScale(schema.scale().mul(initialScale));

			meta.setRightRotation(MatrixUtils.toArray(leftRot));
			meta.setLeftRotation (MatrixUtils.toArray(rightRot));
			
			meta.setBillboardRenderConstraints(BillboardConstraints.FIXED);
			meta.setBrightness(schema.blockLight(), schema.skyLight());
			
			meta.setWidth(schema.width());
			meta.setHeight(schema.height());
			
			meta.setPosRotInterpolationDuration(0);
			meta.setTransformationInterpolationDuration(0);
			meta.setTransformationInterpolationStartDelta(0);
		});
		
		
		if (schema.isItemDisplay()) {
			String itemSchema = schema.displayThingSchema();
			int splitIndex = itemSchema.lastIndexOf('['); // find first '[' and extract: name and properties, from the blockName
						
			String itemName = itemSchema.substring(0, splitIndex).toLowerCase();
			String propertyString = itemSchema.substring(splitIndex+1, itemSchema.length() -1); // remove brackets from start & end
			
			// item
			Material mat = Material.fromKey(itemName);
			ItemStack item = ItemStack.of(mat, 1);
			
			if (schema.headTexture() != null) {
				item = item.with(DataComponents.PROFILE, new ResolvableProfile(new PlayerSkin(schema.headTexture(), null)));
			}
			
			// properties
			DisplayContext context = extractDisplayContext(propertyString);
			
			ItemStack finalItem = item;
			entity.editEntityMeta(ItemDisplayMeta.class, (meta) -> {
				meta.setItemStack(finalItem);
				meta.setDisplayContext(context);
			});
			
			return entity;
		} 

		if (schema.isBlockDisplay()){
			Block block = getBlockFromState(schema.displayThingSchema(), schema.headTexture());
			
			entity.editEntityMeta(BlockDisplayMeta.class, (meta) -> {
				meta.setBlockState(block);
			});
			
			return entity;
		}

		throw new AssertionError("Unreachable");
//		return entity;		
		
	}
	
	private static Block getBlockFromState(String blockState, String headTexture) {
		Block block = Block.fromState(blockState.toLowerCase());
		if (headTexture == null) return block;
		
		var nbt = getHeadTextureNbt(headTexture);
		
		return block.withNbt(nbt);
	}
	
	private static CompoundBinaryTag getHeadTextureNbt(String headTexture) {
		//minecraft:player_head{"profile":{"properties":{"value":"12322143=="}}}
		CompoundBinaryTag propertiesNbt = CompoundBinaryTag.builder()
				.putString("name", "textures")
				.putString("value", headTexture)
				.build();
		
		CompoundBinaryTag profileNbt = CompoundBinaryTag.builder()
				.put("properties", propertiesNbt)
				.build();
		
		CompoundBinaryTag nbt = CompoundBinaryTag.builder()
				.put("profile", profileNbt)
				.build();
		
		return nbt;
	}
	
	private static DisplayContext extractDisplayContext(String propertyString) {
		if (propertyString == null || propertyString.isBlank()) return DisplayContext.NONE;
	    
		String[] properties = propertyString.split(",");
		for(String property : properties) {
			String[] kv = property.split("=");
			if (kv.length != 2) {
				LOGGER.warn("Failed to decode property: " + property);
				continue;
			}
			
			String propKey = kv[0].trim();
			String propVal = kv[1].trim();
			
			if (!propKey.equals("display")) continue; //  for now we only care about display
			
			return DisplayContext.valueOf(propVal.toUpperCase());
		}
		
		return DisplayContext.NONE;
	}
	
	
	
	public record BDModelEntitySchema(
			boolean isItemDisplay,
			boolean isBlockDisplay,
			Vec scale,
			Vec translation,
			Quaternionf rotationLeft,
			Quaternionf rotationRight,
			String displayThingSchema,
			int blockLight,
			int skyLight,
			float width,
			float height,
			String headTexture
			
	) {
		
	}
}
