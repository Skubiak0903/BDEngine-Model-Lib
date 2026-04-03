package io.github.skubiak0903.bdengine;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import io.github.skubiak0903.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.bdengine.entity.BDModelEntitySchema;
import io.github.skubiak0903.bdengine.entity.BDModelEntitySchema.DisplayType;
import io.github.skubiak0903.bdengine.exception.InterpretationException;
import io.github.skubiak0903.bdengine.math.Transformation;
import io.github.skubiak0903.bdengine.node.BDNode;
import io.github.skubiak0903.bdengine.node.BDNodeAdapter;
import io.github.skubiak0903.bdengine.node.BDObject;
import io.github.skubiak0903.bdengine.node.BDProject;

/**
 * Interpreter for converting BDEngine JSON into model.
 */
public class BDProjectInterpreter {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDProjectInterpreter.class);
	
	private static final Gson GSON = new GsonBuilder()
		    .registerTypeAdapter(BDNode.class, new BDNodeAdapter())
		    .setStrictness(Strictness.LENIENT) // allow for comments, keys and values without quotes, etc.
		    .create();
	
	
	/**
	 * Parses BDEngine JSON and creates a {@link REMOVE_BDModelEntity}.
	 * Supports both single project and array of projects, but only first element will be processed.
	 * 
	 * @param jsonStr BDEngine model JSON string
	 * @return BDModelEntity instance
	 * @throws IllegalArgumentException if jsonStr is null or empty
	 * @throws RuntimeException if JSON parsing fails
	 */
	public static BDModelEntity fromJson(String jsonStr) {
		return new BDModelEntity(getSchemaFromJson(jsonStr));
	}
	
	
	/**
	 * Parses BDEngine JSON and creates a List of {@link BDModelEntitySchema}.
	 * Supports both single project and array of projects, but only first element will be processed.
	 * 
	 * @param jsonStr BDEngine model JSON string
	 * @return BDModelEntity instance
	 * @throws IllegalArgumentException if jsonStr is null or empty
	 * @throws RuntimeException if JSON parsing fails
	 */
	public static List<BDModelEntitySchema> getSchemaFromJson(String jsonStr) {
		if (jsonStr == null || jsonStr.trim().isEmpty()) throw new IllegalArgumentException("JSON string cannot be null or empty");
		
		List<BDProject> projectList = parseBDProject(jsonStr);
		if (projectList.isEmpty()) return List.of();
		
		if (projectList.size() > 1) {
		    LOGGER.warn("Model JSON contains {} project objects. Only the first one will be used as multiple projects are currently unsupported. "
		            + "If you have information about BDEngine's data structure, please report it at: "
		            + "https://github.com/Skubiak0903/BDEngine-Model-Lib/issues", projectList.size());
		}
					
		// use just first object. Idk how it looks with more project in a file, but the object is contained in array
		BDProject project = projectList.get(0);
		
		return processNode(project, Transformation.identity());
	}
	
	
	
	private static List<BDProject> parseBDProject(String jsonStr) {
		try (JsonReader reader = GSON.newJsonReader(new StringReader(jsonStr))) {
			JsonToken token = reader.peek();
			
			
			if (token == JsonToken.BEGIN_ARRAY) {
				// list
				Type listType = new TypeToken<List<BDProject>>(){}.getType();
				return GSON.fromJson(jsonStr, listType);
				
			} else if (token == JsonToken.BEGIN_OBJECT) {
				// Singular object
				return List.of(GSON.fromJson(jsonStr, BDProject.class));
				
			} else {
				throw new IllegalArgumentException("Unexpected JSON type: `" + token + "`. Model json should start with `{` or `[`.");
			}
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse JSON! ", e);
		}
	}
	
	
	private static List<BDModelEntitySchema> processNode(BDNode node, Transformation parentTransform) {
		List<BDModelEntitySchema> schemas = new ArrayList<>();
		
		Transformation localTransform = extractTransformation(node.transforms);
		Transformation currentTransform = parentTransform.compose(localTransform);
	    
	    switch (node) {
		    case BDProject projectNode: {
			    // recursive child processing
		    	for (BDNode child : projectNode.children) {
		    		var childSchema = processNode(child, currentTransform);
		    		schemas.addAll(childSchema);
		    	}
		    	break;
		    }
			case BDObject objectNode: {
			    
				var schema = convertObjectToSchema(objectNode, currentTransform);
				if (schema != null) schemas.add(schema);
				
				return schemas;
			}
			default:
				throw new AssertionError("Unreachable");
	    }
	    
	    return schemas;
	}
	
	private static BDModelEntitySchema convertObjectToSchema(BDObject node, Transformation transformation) {		
        String nbt = node.nbt != null ? node.nbt : "";
        float width  = Float.valueOf(getNbtValue(nbt, "width", "1.0f"));
        float height = Float.valueOf(getNbtValue(nbt, "height", "1.0f"));
        
        
        int brightnessBlock = 0;
        int brightnessSky   = 15;
        
        if (node.brightness != null) {
        	brightnessBlock = node.brightness.block;
        	brightnessSky   = node.brightness.sky;
        }
        
        
        String headTexture = node.tagHead != null ?
        		node.tagHead.Value : null;
        
        
        // Type
        List<DisplayType> matches = new ArrayList<>();
        if (node.isBlockDisplay) matches.add(DisplayType.BLOCK);
        if (node.isItemDisplay)  matches.add(DisplayType.ITEM);
        if (node.isTextDisplay)  matches.add(DisplayType.TEXT);

        if (matches.size() != 1) {
            throw new InterpretationException("Error in node '" + node.name + "': Found " + matches.size() + " types " + matches + ", but exactly 1 is required.");
        }

        DisplayType type = matches.get(0);
        
        

        return new BDModelEntitySchema(
        		type, 
        		transformation,
        		brightnessBlock, brightnessSky, width, height,
        		node.name, headTexture);
  	}
	
	
	
	
	private static String getNbtValue(String input, String key, String def) {
        Pattern pattern = Pattern.compile(key + ":([0-9.]+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return def;
    }
	
	private static float[] listToFloatArray(List<Double> list) {
		if (list == null) return new float[0];
		
	    float[] result = new float[list.size()];
	    for (int i = 0; i < list.size(); i++) {
	    	Double value = list.get(i);
	        result[i] = value != null ? value.floatValue() : 0.0f;
	    }
	    return result;
	}
		
	private static Transformation extractTransformation(List<Double> list) {
		if (list == null) 	   throw new AssertionError("Transformation list cannot be null");
		if (list.size() != 16) throw new AssertionError("Transformation list size must be 16!");
		
		float[] rowMajor = listToFloatArray(list);
//		Matrix4f matrix = new Matrix4f().set(floatMatrix);
		
		Matrix4f colMajor = new Matrix4f();
		
		for (int row = 0; row < 4; row++) {
		    for (int col = 0; col < 4; col++) {
		        colMajor.set(col, row, rowMajor[row * 4 + col]);
		    }
		}
		
		return new Transformation(colMajor);
	}
	
	
}
