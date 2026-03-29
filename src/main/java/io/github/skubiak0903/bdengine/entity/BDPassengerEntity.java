package io.github.skubiak0903.bdengine.entity;

import java.util.Collections;
import java.util.Set;

import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skubiak0903.bdengine.entity.BDModelEntitySchema.DisplayType;
import io.github.skubiak0903.bdengine.utils.MatrixUtils;
import lombok.Getter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta.DisplayContext;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;

@Getter
public class BDPassengerEntity extends BDBaseModelEntity {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDPassengerEntity.class);
	
	private final Vec defaultScale;
	private final Vec defaultTranslation;
	private final Quaternionf defaultRightRotation;
	private final Quaternionf defaultLeftRotation;
		
	public BDPassengerEntity(EntityType entityType) {
		this(entityType, Vec.ONE, Vec.ZERO, new Quaternionf(), new Quaternionf());
	}
	
	public BDPassengerEntity(EntityType entityType, Vec defaultScale, Vec defaultTranslation, Quaternionf defaultRightRotation, Quaternionf defaultLeftRotation) {
		super(entityType);
		this.defaultScale = defaultScale;
		this.defaultTranslation = defaultTranslation;
		this.defaultRightRotation = defaultRightRotation;
		this.defaultLeftRotation = defaultLeftRotation;
		
		setAutoViewable(true);
		setAutoViewEntities(false);
	}
	
	@Override
	public void tick(long time) {
		// do nothing
	}
	
	
	
	/*
	 * Disable Passengers
	 */
	
	@Override
	public void addPassenger(Entity entity) {
		LOGGER.warn("BDPassengerEntity is a sub-component and cannot host other passengers.");
		return;
	}
	
	@Override
	public void removePassenger(Entity entity) {
		LOGGER.warn("Attempted to remove a passenger from BDPassengerEntity, which never hosts any.");
	}
	
	@Override
	public boolean hasPassenger() {
		return false;
	}
	
	@Override
	public Set<Entity> getPassengers() {
		return Collections.emptySet();
	}
	
	
	
	
	/*
	 *  static
	 */
	
	public static BDPassengerEntity createPassengerEntity(BDModelEntitySchema schema, Vec initialScale, Vec initialTranslation) {
		EntityType type = displayTypeToEntityType(schema.getType());
		
		Vec scale       = schema.getScale();
		Vec translation = schema.getTranslation();
		
		
		// Very wierd; left rotation is right, and vice versa
		// also all we are changing signs to all, besides `w` parameter		
		Quaternionf rightRotation = schema.getRotationRight().conjugate(new Quaternionf());
		Quaternionf leftRotation  = schema.getRotationLeft();
		
		BDPassengerEntity entity = new BDPassengerEntity(type, scale, translation, rightRotation, leftRotation);
		
		entity.editEntityMeta(AbstractDisplayMeta.class, (meta) -> {
			// operating with initial scale, must be done here because defafaultScale and translation must be unaffected by it;
			meta.setScale(scale.mul(initialScale));
			meta.setTranslation(translation.mul(initialScale).add(initialTranslation));

			meta.setRightRotation(MatrixUtils.toArray(rightRotation));
			meta.setLeftRotation (MatrixUtils.toArray(leftRotation));
			
			meta.setBillboardRenderConstraints(BillboardConstraints.FIXED);
			meta.setBrightness(schema.getBlockLight(), schema.getSkyLight());
			
			meta.setWidth(schema.getWidth());
			meta.setHeight(schema.getHeight());
			
			meta.setPosRotInterpolationDuration(0);
			meta.setTransformationInterpolationDuration(0);
			meta.setTransformationInterpolationStartDelta(0);
		});
		
		switch (schema.getType()) {
		case BLOCK: {
			Block block = getBlockFromState(schema.getDisplayContent(), schema.getHeadTexture());
			
			entity.editEntityMeta(BlockDisplayMeta.class, (meta) -> {
				meta.setBlockState(block);
			});
			break;
		}
		
		case ITEM: {
			String itemSchema = schema.getDisplayContent();
			int splitIndex = itemSchema.lastIndexOf('['); // find first '[' and extract: name and properties, from the blockName
			
			String itemName;
			String propertyString = "";
						
			if (splitIndex != -1) {
				itemName = itemSchema.substring(0, splitIndex).trim().toLowerCase();
				propertyString = itemSchema.substring(splitIndex+1, itemSchema.length() -1).trim(); // remove brackets from start & end
			} else {
				itemName = itemSchema.trim().toLowerCase();
			}
			
			
			ItemStack item = getItemFromSchema(itemName, schema.getHeadTexture());
			DisplayContext context = propertyString.isBlank() ? DisplayContext.NONE : extractDisplayContext(propertyString);
			
			entity.editEntityMeta(ItemDisplayMeta.class, (meta) -> {
				meta.setItemStack(item);
				meta.setDisplayContext(context);
			});
			
			break;
		}
		
		case TEXT: {
			throw new AssertionError("Unimplemented");
		}
		
		default:
			throw new AssertionError("Unreachable");
		}

		return entity;		
	}
	
	
	private static EntityType displayTypeToEntityType(DisplayType type) {
		return switch(type) {
			case BLOCK -> EntityType.BLOCK_DISPLAY;
			case ITEM  -> EntityType.ITEM_DISPLAY;
			case TEXT  -> EntityType.TEXT_DISPLAY;
			default -> throw new AssertionError("Unreachable");
		};
	}
	
	private static ItemStack getItemFromSchema(String itemName, String headTexture) {
		Material mat = Material.fromKey(itemName);
		ItemStack item = ItemStack.of(mat);
		
		if (headTexture == null) return item;
		
		return item.with(DataComponents.PROFILE, new ResolvableProfile(new PlayerSkin(headTexture, null)));
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
	
}
