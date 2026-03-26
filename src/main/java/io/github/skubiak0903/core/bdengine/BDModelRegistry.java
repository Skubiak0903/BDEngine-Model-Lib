package io.github.skubiak0903.core.bdengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.skubiak0903.core.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.core.bdengine.entity.BDModelEntity.BDModelEntitySchema;

public class BDModelRegistry {
	private static final Map<String, List<BDModelEntitySchema>> MODEL_REGISTRY = new HashMap<>();
	
	
	public static void register(String identifier, String jsonStr) {
		List<BDModelEntitySchema> schema = BDProjectInterpreter.getSchemafromJson(jsonStr);
		register(identifier, schema);
	}
	
	public static void register(String identifier, List<BDModelEntitySchema> schema) {
		MODEL_REGISTRY.putIfAbsent(identifier, schema);
	}
	
	
	public static BDModelEntity get(String identifier) {
		var schema = MODEL_REGISTRY.get(identifier);
		if (schema == null) return null;
		return new BDModelEntity(schema);
	}
	
	public static List<BDModelEntitySchema> getSchema(String identifier) {
		return MODEL_REGISTRY.get(identifier);
	}
}
