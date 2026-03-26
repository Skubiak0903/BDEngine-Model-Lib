package io.github.skubiak0903.core.bdengine;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import io.github.skubiak0903.core.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.core.bdengine.entity.BDModelEntity.BDModelEntitySchema;
import io.github.skubiak0903.core.bdengine.node.BDNode;
import io.github.skubiak0903.core.bdengine.node.BDNodeAdapter;
import io.github.skubiak0903.core.bdengine.node.BDProject;
import io.github.skubiak0903.core.utils.MatrixUtils;
import io.github.skubiak0903.core.utils.VecUtils;
import io.github.skubiak0903.core.utils.MatrixUtils.SVDDecomposition;
import io.github.skubiak0903.core.bdengine.node.BDObject;
import net.minestom.server.coordinate.Vec;

/**
 * Interpreter for converting BDEngine JSON into model.
 */
public class BDProjectInterpreter {
	private static final Logger LOGGER = LoggerFactory.getLogger(BDProjectInterpreter.class);
	
	private static final Gson GSON = new GsonBuilder()
		    .registerTypeAdapter(BDNode.class, new BDNodeAdapter())
		    .setStrictness(Strictness.LENIENT) // allow for comments, keys and values without quotes, etc.
		    .create();

	// no transformation! -> scale is [1,1,1], rotation [0,0,0,1], etc.
	private static final float[] IDENTITY_MATRIX = {
	    1, 0, 0, 0,
	    0, 1, 0, 0,
	    0, 0, 1, 0,
	    0, 0, 0, 1
	};
	
	
	/**
	 * Parses BDEngine JSON and creates a {@link BDModelEntity}.
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
		
		return processNode(project, IDENTITY_MATRIX);
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
	
	
	private static List<BDModelEntitySchema> processNode(BDNode node, float[] parentMatrix) {
		List<BDModelEntitySchema> schemas = new ArrayList<>();
		
		float[] currentMatrix;
	    
	    switch (node) {
		    case BDProject projectNode: {
			    float[] localTransform = listToFloatArray(projectNode.transforms);
			    currentMatrix = MatrixUtils.multiply4x4RowMajor(parentMatrix, localTransform);
			    
			    // recursive child processing
		    	for (BDNode child : projectNode.children) {
		    		var childSchema = processNode(child, currentMatrix);
		    		schemas.addAll(childSchema);
		    	}
		    	break;
		    }
			case BDObject objectNode: {
			    float[] localTransform = listToFloatArray(objectNode.transforms);
			    currentMatrix = MatrixUtils.multiply4x4RowMajor(parentMatrix, localTransform);
			    
				var schema = convertObjectToSchema(objectNode, currentMatrix);
				if (schema != null) schemas.add(schema);
				
				return schemas;
			}
			default:
				throw new AssertionError("Unreachable");
	    }
	    
	    return schemas;
	}
	
	private static BDModelEntitySchema convertObjectToSchema(BDObject node, float[] matrixArray) {
		if (matrixArray.length != 16) {
			LOGGER.error("Invalid transformation matrix length: {}, should be 16", matrixArray.length);
			return null;
		}
		
		Matrix4f matrix = new Matrix4f();
		matrix.set(matrixArray);

		
		// Extract Translation
		float translateX = matrix.m03();
		float translateY = matrix.m13();
		float translateZ = matrix.m23();
		Vec translation = new Vec(translateX, translateY, translateZ);
		

		// Extract 3x3 matrix (rotation & scale)
 		Matrix3f m3x3 = new Matrix3f();
		matrix.get3x3(m3x3);
		
        
        // Decompose 3x3 matrix to leftRotation, scale, rightRotation
		SVDDecomposition decomposition = MatrixUtils.svdDecompose(m3x3);

		Quaternionf leftRotation  = decomposition.leftRotation();
		Vec 	 	scale 		  = VecUtils.vec3ToMinestomVec(decomposition.scale());
		Quaternionf rightRotation = decomposition.rightRotation();		
		
		
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
        
        
        return new BDModelEntitySchema(
        		node.isItemDisplay, node.isBlockDisplay, 
        		scale, translation, leftRotation, rightRotation, node.name, 
        		brightnessBlock, brightnessSky, width, height,
        		headTexture);
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
	
	
}
