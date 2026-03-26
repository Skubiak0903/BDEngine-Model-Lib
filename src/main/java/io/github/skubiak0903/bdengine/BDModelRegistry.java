package io.github.skubiak0903.bdengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.skubiak0903.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.bdengine.entity.BDModelEntity.BDModelEntitySchema;

/**
 * Registry for managing BDEngine models.
 */
public class BDModelRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDModelRegistry.class);
	private static final Map<String, List<BDModelEntitySchema>> MODEL_REGISTRY = new ConcurrentHashMap<>();
	
	
    private BDModelRegistry() {}
	
	
    /**
     * Registers a model from JSON string.
     * 
     * @param identifier unique model identifier
     * @param jsonStr BDEngine model JSON
     * @throws IllegalArgumentException if identifier or jsonStr is null/empty
     */
	public static void register(String identifier, String jsonStr) {
		List<BDModelEntitySchema> schema = BDProjectInterpreter.getSchemaFromJson(jsonStr);
		register(identifier, schema);
	}
	
    /**
     * Registers a model from schema.
     * 
     * @param identifier unique model identifier
     * @param schema model schema
     * @throws IllegalArgumentException if identifier or schema is null/empty
     */
	public static void register(String identifier, List<BDModelEntitySchema> schema) {
	    if (identifier == null || identifier.trim().isEmpty()) throw new IllegalArgumentException("Identifier cannot be null or empty");
	    if (schema == null) throw new IllegalArgumentException("BDModelEntitySchema list cannot be null");
		var previous = MODEL_REGISTRY.putIfAbsent(identifier,  Collections.unmodifiableList(new ArrayList<>(schema)));
		
		if (previous != null) {
			LOGGER.warn("Model with identifier '{}' already registered, skipping", identifier);
		}
	}
	
	
    /**
     * Creates a model instance.
     * 
     * @param identifier model identifier
     * @return BDModelEntity instance or null if not found
     */
	public static BDModelEntity get(String identifier) {
		var schema = MODEL_REGISTRY.get(identifier);
		if (schema == null) return null;
		return new BDModelEntity(schema);
	}
	
	
	/**
     * Gets the schema for a model.
     * 
     * @param identifier model identifier
     * @return unmodifiable list of schemas or null if not found
     */
	public static List<BDModelEntitySchema> getSchema(String identifier) {
	    return MODEL_REGISTRY.get(identifier);
	}
	
	
	/**
     * Unregisters a model.
     * 
     * @param identifier model identifier
     * @return true if model was removed, false otherwise
     */
	public static boolean unregister(String identifier) {
	    List<BDModelEntitySchema> removed = MODEL_REGISTRY.remove(identifier);
	    return removed != null;
	}
	
	
	/**
     * Checks if a model is registered.
     * 
     * @param identifier model identifier
     * @return true if model exists
     */
    public static boolean contains(String identifier) {
        return MODEL_REGISTRY.containsKey(identifier);
    }
	
	
    /**
     * Gets all registered model identifiers.
     * 
     * @return unmodifiable set of identifiers
     */
    public static Set<String> getRegisteredModels() {
        return Collections.unmodifiableSet(MODEL_REGISTRY.keySet());
    }
    

	/**
     * Clears all registered models.
     */
	public static void clear() {
	    MODEL_REGISTRY.clear();
	}
	
	
	/**
     * Gets the number of registered models.
     * 
     * @return number of models
     */
    public static int size() {
        return MODEL_REGISTRY.size();
    }
}
