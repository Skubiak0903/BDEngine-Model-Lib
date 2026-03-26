package io.github.skubiak0903.core.bdengine.command;

import io.github.skubiak0903.core.bdengine.BDModelRegistry;
import io.github.skubiak0903.core.bdengine.entity.BDModelEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class BDModelCMD extends Command {

    public BDModelCMD() {
        super("model");

        var identArg = ArgumentType.String("identifier");
        
        
        setDefaultExecutor((sender, _) -> {
        	Component msg = Component.text("Correct Usage: /model <identifier>", NamedTextColor.RED);
        	sender.sendMessage(msg);
        });
        
        identArg.setCallback((sender, _) -> {
        	Component msg = Component.text("Correct Usage: /model <identifier>", NamedTextColor.RED);
        	sender.sendMessage(msg);
        });
        

        
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
            	Component msg = Component.text("Only player can use this command!", NamedTextColor.RED);
            	sender.sendMessage(msg);
            	return;
            }
            
            final String ident = context.get(identArg);
            
            BDModelEntity model = BDModelRegistry.get(ident);
    		if (model == null) {
    			Component msg = Component.text("Model with identifier `" + ident + "`, deosn't exist!", NamedTextColor.RED);
    			sender.sendMessage(msg);
    			return;
    		}
            
    		model.setInstance(player.getInstance(), player.getPosition().withPitch(0));
    		
    		Component msg = Component.text("Successfully spawned " + ident + " model.", NamedTextColor.GREEN);
            sender.sendMessage(msg);
        }, identArg);
    }
}