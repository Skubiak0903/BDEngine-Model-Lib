# BDEngine Model Lib
[![](https://jitpack.io/v/Skubiak0903/BDEngine-Model-Lib.svg)](https://jitpack.io/#Skubiak0903/BDEngine-Model-Lib)

A simple and lightweight library for **Minestom**. It allows for easy importing [BDEngine](https://bdengine.app/) models into Minecraft, without having to worry about incorrect interpolation or other quaternion magic.

**Currently does NOT support BDEngine animations**
But you can freely rotate/scale them by using model functions!

---
<br>

# Model Creation

To get model JSON see [Exporting model JSON](#exporting-model-json).

<br>

## Registering model

By using ModelRegistry you can easily import big models from BDEngine into Minecraft.

```java
String modelJson = """
  [{"isCollection":true,"name":"Project", "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],"children":[{"isBlockDisplay":true,"name":"spruce_fence_gate","transforms":[1,0,0,-0.5,0,1,0,-0.5,0,0,1,-0.5,0,0,0,1]}]}]
""";

BDModelRegistry.register("model", modelJson);
BDModelEntity model = BDModelRegistry.get("model");
```

<br>

## Extending ModelEntity

Once you have registered a model you can extend BDModelEntity class and use it like a normal entity.

```java
public class CustomModelEntity extends BDModelEntity {

  // retrieve our registered model
	public static final List<BDModelEntitySchema> SCHEMA = BDModelRegistry.getSchema("model");

  public static final Vec INITIAL_SCALE = new Vec(0.2f, 0.2f, 0.2f);
	
	public CustomModelEntity() {
    // init model with initial scale and initial translation
		super(SCHEMA, INITIAL_SCALE, Vec.ZERO); 
	}

	@Override
	public void spawn() {
		super.spawn();
		spawnAnimation();
	}
	
	
	private void spawnAnimation() {
    // schedule growing animation	
		schedule((entity) -> {
			entity.setModelScale(new Vec(0.6f, 0.6f, 0.6f), 3);
		}, TaskSchedule.tick(2));

    // schedule shrinking animation after previous one
		schedule((entity) -> {
			entity.setModelScale(new Vec(0.5f, 0.5f, 0.5f), 1);
		}, TaskSchedule.tick(6));
	}

}
```

<br>
You can also register a model inside an extending class, but be aware that java will register this model, when you try to access this class.
<br><br>

```java
public class CustomModelEntity extends BDModelEntity {
  public static final String modelJson = """
    [{"isCollection":true,"name":"Project", "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],"children":[{"isBlockDisplay":true,"name":"spruce_fence_gate","transforms":[1,0,0,-0.5,0,1,0,-0.5,0,0,1,-0.5,0,0,0,1]}]}]
  """;

  // retrieve our registered model
	public static final List<BDModelEntitySchema> SCHEMA = BDModelRegistry.register("model", modelJson);

	public CustomModelEntity() {
    // init model without any initial values
		super(SCHEMA); 
	}

  ...
}
```

---

<br>

# Model Operations

## Translation

Applies translation to an entity. Useful in animation when you don't want to change entity position. Because Minecraft client only renders entity that are on the screen, we recommend using it with small translations (max 10 blocks), because with larger translations entity can sometimes disappear/not render on the client side, which may cause flickering. For large entity offsets, just use teleportation, which also allows for smooth interpolation. 

```java
// relatively translate model 1 block up with 10 ticks of interpolation
model.translateModel(new Vec(0, 1, 0), 10);

// set model translation to vector (0, 1, 0) with 10 ticks of interpolation.
model.setModelTranslation(new Vec(0, 1, 0), 10);
```

https://github.com/user-attachments/assets/6dc30ece-3890-4e12-8d7d-770d8140452d

---

<br>

## Scale

Changes the model scale.

```java
// scale model 2 times with 10 ticks of interpolation
model.scaleModel(new Vec(2, 2, 2), 10);

// set model scale to vector (1, 1, 1) with 10 ticks of interpolation.
model.setModelScale(new Vec(1, 1, 1), 10);
```

https://github.com/user-attachments/assets/f1aaa988-b61e-4b6c-93ea-50d5d1f2cda8

---

<br>

## Pivot point

Changes the pivot point location used in rotation. By changing it, you can set the point around which the model rotates.

```java
// Change pivot point location
model.setPivotPoint(new Vec(0, 0, 0));

// Change pivot point location, and update model to start from correct location, and avoid weird teleport in the first rotation after change.
model.setPivotPoint(new Vec(0, 0, 0), true);
```

https://github.com/user-attachments/assets/c2f30866-8c06-46ba-8eb2-a312920fe18b

---

<br>

## Rotations (LERP & SLERP)

There are 2 types of rotation: LERP and SLERP. The lerp rotation will go with the shortest path, for example rotating by 180 degrees will look like the model is passing through its center. And the Slerp (Spherical) rotation follows a path on the sphere to get to the target position, rotating in a more natural way. 

```java
// calculate new quaternion rotation from XYZ rotation
Quaternionf newRotation = new Quaternionf().rotationXYZ((float)Math.toRadians(45), (float)Math.toRadians(90), (float)Math.toRadians(45)).normalize();

// Set rotation to `newRotation` with 20 ticks of interpolation, using LERP rotation
model.setModelRotationLerp(newRotation, 20);

// Set rotation to `newRotation` with 20 ticks of interpolation, using SLERP rotation
model.setModelRotationSlerp(newRotation, 20);
```

https://github.com/user-attachments/assets/c9f91789-3e61-45b9-b5d1-15f31bc19d9c

---

<br>

# Model Command (Debug Only)

`⚠️ **Warning**: This command is for testing only. Do not use in production!`

```java
// Register new Model Command
MinecraftServer.getCommandManager().register(new BDModelCMD());
```

---

<br>

# Exporting model JSON

After you create [BDEngine](https://bdengine.app/) model, you can save it as Project file, and pass it into [CyberChef](https://gchq.github.io/CyberChef/#recipe=From_Base64('A-Za-z0-9%2B/%3D',true,false)Gunzip()) with Base64 decoding and unzipping. This will produce raw model JSON that we can use when registering new model.
