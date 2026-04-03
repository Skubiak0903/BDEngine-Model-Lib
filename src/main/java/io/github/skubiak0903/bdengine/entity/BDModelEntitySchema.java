package io.github.skubiak0903.bdengine.entity;

import io.github.skubiak0903.bdengine.math.Transformation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minestom.server.entity.EntityType;

@Getter
@RequiredArgsConstructor
public class BDModelEntitySchema {
	private final DisplayType type;
	
//	private final Vec scale;
//	private final Vec translation;
//	private final Quaternionf rotationRight;
//	private final Quaternionf rotationLeft;
	
	private final Transformation transformation;
	
	private final int blockLight;
	private final int skyLight;
	
	private final float width;
	private final float height;
	
	private final String displayContent; // can be block/item/text
	private final String headTexture;	// can be null
	
	
	/*public Transformation getTransformation() {
		return new Transformation(
				scale, 
				translation, 
				rotationRight.conjugate(new Quaternionf()), 
				rotationLeft);
	}*/
	
	
	@Getter
	@RequiredArgsConstructor
	public enum DisplayType {
		ITEM  (EntityType.ITEM_DISPLAY),
		BLOCK (EntityType.BLOCK_DISPLAY),
		TEXT  (EntityType.TEXT_DISPLAY);
		
		private final EntityType entityType;
	}
	
	/*@Getter
	@RequiredArgsConstructor
	public final class Transformation {
		private final Vec scale;
		private final Vec translation;
		private final Quaternionf rightRotation;
		private final Quaternionf leftRotation;
	}*/
}
