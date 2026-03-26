package io.github.skubiak0903.core.bdengine.node;
import com.google.gson.*;

import io.github.skubiak0903.core.bdengine.node.BDProject.BDObject;

import java.lang.reflect.Type;

public class BDNodeAdapter implements JsonDeserializer<BDNode> {
    @Override
    public BDNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Sprawdzamy unikalne pole, które odróżnia te dwa typy
        if (jsonObject.has("isCollection") && jsonObject.get("isCollection").getAsBoolean()) {
            return context.deserialize(json, BDProject.class);
        } else {
            return context.deserialize(json, BDObject.class);
        }
    }
}