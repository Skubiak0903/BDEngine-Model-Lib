package io.github.skubiak0903.bdengine.command;

import java.util.Collection;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.github.skubiak0903.bdengine.BDModelRegistry;
import io.github.skubiak0903.bdengine.entity.PassengerEntity;
import io.github.skubiak0903.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.bdengine.utils.VecUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.CommandSyntax;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

public class BDModelCMD extends Command {
	public static final String COMMAND_NAME = "model";
	
	public static BDModelEntity lastModelEntity = null;
	
	// shared arguments
	private static final ArgumentFloat X_ARG = ArgumentType.Float("x");
    private static final ArgumentFloat Y_ARG = ArgumentType.Float("y");
    private static final ArgumentFloat Z_ARG = ArgumentType.Float("z");
    private static final ArgumentInteger DUR_ARG = ArgumentType.Integer("duration");
    
    static {
    	DUR_ARG.setDefaultValue(0);
    }
	
	
	public BDModelCMD() {
        super(COMMAND_NAME);
        
        setDefaultExecutor((sender, _) -> {
        	Component msg = Component.text("Correct usage: /%s <operation> ...".formatted(COMMAND_NAME), NamedTextColor.RED);
        	sender.sendMessage(msg);
        });
        
		addSubcommands(
				new SpawnSubCommand(),
				new SetViewSubCommand(),
				new TeleportSubCommand(),
				new TranslateSubCommand(),
				new SetTranslationSubCommand(),
				new ScaleSubCommand(),
				new SetScaleSubCommand(),
				new RotateLerpSubCommand(),
				new SetRotationLerpSubCommand(),
				new RotateSlerpSubCommand(),
				new SetRotationSlerpSubCommand(),
				new SetPivotPointSubCommand(),
				new VisualizePivotPointSubCommand(),
				new GetInfoSubCommand()
			);
        
    }
	
	private abstract static class ModelSub extends Command {
        public ModelSub(String name, String args) {
            super(name);
            
            setDefaultExecutor((sender, _) -> {
	        	Component msg = Component.text("Correct usage: /%s %s %s".formatted(COMMAND_NAME, name, args), NamedTextColor.RED);
	        	sender.sendMessage(msg);
			});
			
        }

        @Override
        public Collection<CommandSyntax> addSyntax(CommandExecutor executor, Argument<?>... args) {
        	return super.addSyntax((sender, context) -> {
        		if (!(sender instanceof Player player)) {
                	sender.sendMessage(Component.text("Only player can use this command!", NamedTextColor.RED));
                	return;
                }
        		
        		if (lastModelEntity == null) {
                    Component msg = Component.text("Spawn model first: /%s spawn <identifier>".formatted(COMMAND_NAME), NamedTextColor.RED);
                    player.sendMessage(msg);
                    return;		
                }
        		
        		executor.apply(sender, context);
        	}, args);
        }
    }
	
	private static class SpawnSubCommand extends ModelSub {		
		public SpawnSubCommand() {
			super("spawn", "<identifier>");
			
			var identArg = ArgumentType.String("identifier");
			
			
			identArg.setSuggestionCallback((_, _, suggestion) -> {
				for (String id : BDModelRegistry.getRegisteredModels()) {
					suggestion.addEntry(new SuggestionEntry(id));
				}
			});
			
			
			addConditionalSyntax(null, (sender, context) -> { // redirect to conditional syntax to avoid lastModel == null check
        		if (!(sender instanceof Player player)) {
                	sender.sendMessage(Component.text("Only player can use this command!", NamedTextColor.RED));
                	return;
                }
	            
	            final String ident = context.get(identArg);
	            var model = BDModelRegistry.get(ident);
	            
	    		if (model == null) {
	    			Component msg = Component.text("Model with identifier `%s`, deosn't exist!".formatted(ident), NamedTextColor.GRAY);
	    			player.sendMessage(msg);
	    			return;
	    		}
	            
	    		model.setInstance(player.getInstance(), player.getPosition().withView(0, 0));
	    		
	    		lastModelEntity = model;
				Component msg = Component.text("%s model has been spawned.".formatted(ident), NamedTextColor.GREEN);
	            sender.sendMessage(msg);
			}, identArg);
		}
    }
    
	private static class SetViewSubCommand extends ModelSub {
		public SetViewSubCommand() {
			super("set_view", "<yaw> <pitch> [duration]");
			
			var yawArg = ArgumentType.Float("yaw");
			var pitchArg = ArgumentType.Float("pitch");
			
			
			addSyntax((sender, context) -> {
	        	float yaw = context.get(yawArg);
	        	float pitch = context.get(pitchArg);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Component msg = Component.text("New view: [%.2f, %.2f]".formatted(yaw, pitch), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setView(yaw, pitch, duration);
	        	
			}, yawArg, pitchArg, DUR_ARG);
		}
    }
    
	private static class TeleportSubCommand extends ModelSub {    	
		public TeleportSubCommand() {
			super("teleport", "<x> <y> <z> [duration]");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Component msg = Component.text("Teleported to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.teleport(new Pos(x,y,z), duration);
			}, X_ARG, Y_ARG, Z_ARG, DUR_ARG);
		}
    }
    
	private static class TranslateSubCommand extends ModelSub {
		public TranslateSubCommand() {
			super("translate", "<x> <y> <z> [duration]");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        	int duration = context.get(DUR_ARG);
	        	
	        	lastModelEntity.translateModel(new Vec(x,y,z), duration);
	        	Vec n = lastModelEntity.getGlobalTranslation();
	        	
	        	Component msg = Component.text("Translated by: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY).appendNewline()
	        			.append(Component.text("New translation: [%.2f, %.2f, %.2f]".formatted(n.x(), n.y(), n.z())));
	        	sender.sendMessage(msg);
	        	
			}, X_ARG, Y_ARG, Z_ARG, DUR_ARG);
		}
    }
    
	private static class SetTranslationSubCommand extends ModelSub {
		public SetTranslationSubCommand() {
			super("set_translation", "<x> <y> <z> [duration]");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        	int duration = context.get(DUR_ARG);
	        		        	
	        	Component msg = Component.text("Set translation to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setModelTranslation(new Vec(x,y,z), duration);
			}, X_ARG, Y_ARG, Z_ARG, DUR_ARG);
		}
    }
    
	private static class ScaleSubCommand extends ModelSub {
		public ScaleSubCommand() {
			super("scale", "<x> <y> <z> [duration]");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        	int duration = context.get(DUR_ARG);
	        	
	        	lastModelEntity.scaleModel(new Vec(x,y,z), duration);
	        	Vec n = lastModelEntity.getGlobalScale();
	        	
	        	Component msg = Component.text("Scaled by: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY).appendNewline()
	        			.append(Component.text("New scale: [%.2f, %.2f, %.2f]".formatted(n.x(), n.y(), n.z())));
	        	sender.sendMessage(msg);
	        	
			}, X_ARG, Y_ARG, Z_ARG, DUR_ARG);
		}
    }
    
	private static class SetScaleSubCommand extends ModelSub {
		public SetScaleSubCommand() {
			super("set_scale", "<x> <y> <z> [duration]");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        	int duration = context.get(DUR_ARG);
	        		        	
	        	Component msg = Component.text("Set scale to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setModelScale(new Vec(x,y,z), duration);
			}, X_ARG, Y_ARG, Z_ARG, DUR_ARG);
		}
    }
    
	private static class RotateLerpSubCommand extends ModelSub {
		public RotateLerpSubCommand() {
			super("rotate_lerp", "<x-axis> <y-axis> <z-axis> [duration]");
			
			var xArg = ArgumentType.Float("x-axis");
			var yArg = ArgumentType.Float("y-axis");
			var zArg = ArgumentType.Float("z-axis");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(xArg);
	        	float y = context.get(yArg);
	        	float z = context.get(zArg);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Quaternionf delta = new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
	        	lastModelEntity.rotateModelLerp(delta, duration);
	        	Vec n = VecUtils.vec3ToMinestomVec(lastModelEntity.getGlobalRotation().getEulerAnglesXYZ(new Vector3f()));
	        	
	        	Component msg = Component.text("Rotated by: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY).appendNewline()
	        			.append(Component.text("New rotation: [%.2f, %.2f, %.2f]".formatted(Math.toDegrees(n.x()), Math.toDegrees(n.y()), Math.toDegrees(n.z()))));
	        	sender.sendMessage(msg);
	        	
			}, xArg, yArg, zArg, DUR_ARG);
		}
    }
    
	private static class SetRotationLerpSubCommand extends ModelSub {
		public SetRotationLerpSubCommand() {
			super("set_rotation_lerp", "<x-axis> <y-axis> <z-axis> [duration]");
			
			var xArg = ArgumentType.Float("x-axis");
			var yArg = ArgumentType.Float("y-axis");
			var zArg = ArgumentType.Float("z-axis");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(xArg);
	        	float y = context.get(yArg);
	        	float z = context.get(zArg);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Quaternionf delta = new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
	        	
	        	Component msg = Component.text("Set rotation to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setModelRotationLerp(delta, duration);
			}, xArg, yArg, zArg, DUR_ARG);
		}
    }
    
	private static class RotateSlerpSubCommand extends ModelSub {
		public RotateSlerpSubCommand() {
			super("rotate_slerp", "<x-axis> <y-axis> <z-axis> [duration]");
			
			var xArg = ArgumentType.Float("x-axis");
			var yArg = ArgumentType.Float("y-axis");
			var zArg = ArgumentType.Float("z-axis");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(xArg);
	        	float y = context.get(yArg);
	        	float z = context.get(zArg);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Quaternionf delta = new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
	        	lastModelEntity.rotateModelSlerp(delta, duration);
	        	Vec n = VecUtils.vec3ToMinestomVec(lastModelEntity.getGlobalRotation().getEulerAnglesXYZ(new Vector3f()));
	        	
	        	Component msg = Component.text("Rotated by: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY).appendNewline()
	        			.append(Component.text("New rotation: [%.2f, %.2f, %.2f]".formatted(Math.toDegrees(n.x()), Math.toDegrees(n.y()), Math.toDegrees(n.z()))));
	        	sender.sendMessage(msg);
	        	
			}, xArg, yArg, zArg, DUR_ARG);
		}
    }
    
	private static class SetRotationSlerpSubCommand extends ModelSub {
		public SetRotationSlerpSubCommand() {
			super("set_rotation_slerp", "<x-axis> <y-axis> <z-axis> [duration]");
			
			var xArg = ArgumentType.Float("x-axis");
			var yArg = ArgumentType.Float("y-axis");
			var zArg = ArgumentType.Float("z-axis");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(xArg);
	        	float y = context.get(yArg);
	        	float z = context.get(zArg);
	        	int duration = context.get(DUR_ARG);
	        	
	        	Quaternionf delta = new Quaternionf().rotationXYZ((float)Math.toRadians(x), (float)Math.toRadians(y), (float)Math.toRadians(z));
	        	
	        	Component msg = Component.text("Set rotation to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setModelRotationSlerp(delta, duration);
			}, xArg, yArg, zArg, DUR_ARG);
		}
    }
    
	private static class SetPivotPointSubCommand extends ModelSub {
		public SetPivotPointSubCommand() {
			super("set_pivot_point", "<x> <y> <z>");
			
			addSyntax((sender, context) -> {
	        	float x = context.get(X_ARG);
	        	float y = context.get(Y_ARG);
	        	float z = context.get(Z_ARG);
	        		        	
	        	Component msg = Component.text("Set pivot point to: [%.2f, %.2f, %.2f]".formatted(x, y, z), NamedTextColor.GRAY);
	        	sender.sendMessage(msg);
	        	
	        	lastModelEntity.setPivotPoint(new Vec(x,y,z));
			}, X_ARG, Y_ARG, Z_ARG);
		}
    }
    
	private static class VisualizePivotPointSubCommand extends ModelSub {
		public VisualizePivotPointSubCommand() {
			super("visualize_pivot_point", "[duration] [scale]");
			
			var durationArg = ArgumentType.Integer("duration").setDefaultValue(200);
			var scaleArg = ArgumentType.Float("scale").setDefaultValue(1f);
			
			
			addSyntax((sender, context) -> {
				int duration = context.get(durationArg);
	        	float scale = context.get(scaleArg);
	        	
	        	Vector3f pivot = lastModelEntity.getPivotPoint();
	        		        	
	        	Component msg = Component.text("Visualizing pivot point for %d seconds, with scale %.2f".formatted(duration / 20, scale), NamedTextColor.GRAY).appendNewline()
	        			.append(Component.text("Pivot point location: [%.2f, %.2f, %.2f]".formatted(pivot.x, pivot.y, pivot.z)));
	        	sender.sendMessage(msg);
	        	
	        	Vec scaleVec = new Vec(0.5f * scale);
	        	Vec translationVec = new Vec(-0.25f * scale).add(pivot.x, pivot.y, pivot.z); // half of a scale
	        	
	        	var pivotPassenger = new PassengerEntity(EntityType.BLOCK_DISPLAY, scaleVec, translationVec, new Quaternionf(), new Quaternionf());
	        	pivotPassenger.editEntityMeta(BlockDisplayMeta.class, (meta) -> {
	        		meta.setBlockState(Block.BLUE_STAINED_GLASS);
	        		meta.setHasGlowingEffect(true);
	        		meta.setGlowColorOverride(0x0000FF); // blue
	        		meta.setScale(scaleVec);
	        		meta.setTranslation(translationVec); 
	        	});
	        	
	        	Pos spawnPos = lastModelEntity.getPosition().add(VecUtils.vec3ToMinestomVec(pivot));
	        	pivotPassenger.setInstance(lastModelEntity.getInstance(), spawnPos);
	        	
	        	lastModelEntity.addPassenger(pivotPassenger);
	        	
	        	// schedule removal
	        	lastModelEntity.scheduler().buildTask(() -> {  // needs to be parent, passengers doesnt tick scheduler
	        		pivotPassenger.acquirable().sync((entity) -> {
	        			entity.remove();
	        		});
	        	}).delay(TaskSchedule.tick(duration)).schedule();
	        	
	        	
			}, durationArg, scaleArg);
		}
    }
	
	private static class GetInfoSubCommand extends ModelSub {
		public GetInfoSubCommand() {
			super("info", "");
			
			addSyntax((sender, _) -> {
				Pos pos =  lastModelEntity.getPosition();
				Vector3f pivot = lastModelEntity.getPivotPoint();
				
	        	Vec scale = lastModelEntity.getGlobalScale();
	        	Vec translation = lastModelEntity.getGlobalTranslation();
	        	Quaternionf rotation = lastModelEntity.getGlobalRotation();
	        	
	        	int objCount = lastModelEntity.getPassengers().size();
	        	
	        	Component headerPart = Component.text("=-----=", NamedTextColor.DARK_GRAY);
	        	Component header = headerPart.append(Component.text(" MODEL ENTITY INFO ", NamedTextColor.GOLD)).append(headerPart);
	        	
	        	Component msg = header.appendNewline()
	        			.appendNewline()
	        			.append(Component.text("Model: ", NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Pos: [%.2f, %.2f, %.2f] (%.2f, %.2f)".formatted(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch()), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Pivot: [%.2f, %.2f, %.2f]".formatted(pivot.x, pivot.y, pivot.z), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Scale: [%.2f, %.2f, %.2f]".formatted(scale.x(), scale.y(), scale.z()), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Translation: [%.2f, %.2f, %.2f]".formatted(translation.x(), translation.y(), translation.z()), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Rotation: [%.2f, %.2f, %.2f]".formatted(Math.toDegrees(rotation.x()), Math.toDegrees(rotation.y()), Math.toDegrees(rotation.z())), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- Rotation (Quaternion): %s".formatted(rotation.toString()), NamedTextColor.GRAY)).appendNewline()
	        			.appendNewline()
	        			.append(Component.text("Object count: %d".formatted(objCount + 1), NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- root: 1", NamedTextColor.GRAY)).appendNewline()
	        			.append(Component.text("- displays: %d".formatted(objCount), NamedTextColor.GRAY)).appendNewline()
	        			.appendNewline()
	        			.append(header);
	        	
	        	sender.sendMessage(msg);
			});
		}
    }
}