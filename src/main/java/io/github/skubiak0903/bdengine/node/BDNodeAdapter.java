package io.github.skubiak0903.bdengine.node;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BDNodeAdapter implements JsonDeserializer<BDNode> {
	
    @Override
    public BDNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (jsonObject.has("isCollection") && jsonObject.get("isCollection").getAsBoolean()) {
            return context.deserialize(json, BDProject.class);
        } else {
            return context.deserialize(json, BDObject.class);
        }
    }
    
}