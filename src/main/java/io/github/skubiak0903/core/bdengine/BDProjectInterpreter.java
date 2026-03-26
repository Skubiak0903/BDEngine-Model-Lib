package io.github.skubiak0903.core.bdengine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.skubiak0903.core.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.core.bdengine.entity.BDModelEntity.BDModelEntitySchema;
import io.github.skubiak0903.core.bdengine.node.BDNode;
import io.github.skubiak0903.core.bdengine.node.BDNodeAdapter;
import io.github.skubiak0903.core.bdengine.node.BDProject;
import io.github.skubiak0903.core.bdengine.node.BDProject.BDObject;
import io.github.skubiak0903.core.math.MatrixUtils;
import net.minestom.server.coordinate.Vec;

public class BDProjectInterpreter {
	private static final Gson GSON = new GsonBuilder()
		    .registerTypeAdapter(BDNode.class, new BDNodeAdapter())
		    .create();
	
	public static BDModelEntity fromJson(String jsonStr) {
		return new BDModelEntity(getSchemafromJson(jsonStr));
	}
		
	public static List<BDModelEntitySchema> getSchemafromJson(String jsonStr) {
		BDProject project = GSON.fromJson(jsonStr, BDProject.class);
		
		// no transformation! -> scale is [1,1,1], rotation [0,0,0,1], etc.
		float[] identity = {
		        1, 0, 0, 0,
		        0, 1, 0, 0,
		        0, 0, 1, 0,
		        0, 0, 0, 1
		    };
		
		return processNode(project, identity);
	}
	
	
	private static List<BDModelEntitySchema> processNode(BDNode node, float[] parentMatrix) {
		List<BDModelEntitySchema> schemas = new ArrayList<>();
		
		float[] currentMatrix;
	    List<BDNode> children;
	    
	    switch (node) {
		    case BDProject projectNode: {
		    	float[] local = listToFloatArray(projectNode.transforms);
		    	currentMatrix = multiply4x4RowMajor(parentMatrix, local);
		    	children = projectNode.children;
		    	break;
		    }
			case BDObject objectNode: {
				float[] local = listToFloatArray(objectNode.transforms);
				currentMatrix = multiply4x4RowMajor(parentMatrix, local);
				
				schemas.add(convertObjectToSchema(objectNode, currentMatrix));
				return schemas;
			}
			default:
				throw new AssertionError("Unrechable");
	    }
	    
	    if (children != null) {
	    	for (BDNode child : children) {
	    		var childSchema = processNode(child, currentMatrix);
	    		schemas.addAll(childSchema);
	    	}
	    }
	    
	    return schemas;
	}
	
	private static float[] listToFloatArray(List<Double> list) {
	    float[] result = new float[list.size()];
	    for (int i = 0; i < list.size(); i++) {
	        result[i] = list.get(i).floatValue();
	    }
	    return result;
	}
	
	
	private static BDModelEntitySchema convertObjectToSchema(BDObject node, float[] matrixArray) {
		if (matrixArray.length != 16) throw new IllegalArgumentException("Transformation list is too short! Expected length 16");
		
		Matrix4f matrix = new Matrix4f();
		matrix.set(matrixArray);

		// Wyciągnij translację
		float tx = matrix.m03();
		float ty = matrix.m13();
		float tz = matrix.m23();
		Vec translation = new Vec(tx, ty, tz);

		// Wyciągnij część 3x3 (rotacja i skala)
		Matrix3f m3x3 = new Matrix3f();
		matrix.get3x3(m3x3);
        
        // 4. Wyciągamy rotację (Kwaternion)
		// Dekompozycja SVD
		kotlin.Triple<Quaternionf, Vector3f, Quaternionf> decomposition = MatrixUtils.svdDecompose(m3x3);

		Quaternionf leftRotation = decomposition.getFirst(); 	// U
		Vector3f scale2 = decomposition.getSecond(); 			// S
		Quaternionf rightRotation = decomposition.getThird(); 	// V^T
		
		Vec scale = new Vec(scale2.x, scale2.y, scale2.z);
				
        String nbt = node.nbt;
        float width  = Float.valueOf(getNbtValue(nbt, "width", "1.0f"));
        float height = Float.valueOf(getNbtValue(nbt, "height", "1.0f"));
        
        int brightnessBlock = node.brightness != null ? 
        		node.brightness.block : 0;
        
        int brightnessSky = node.brightness != null ? 
        		node.brightness.sky : 15;
        
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
	
	
	/**
	 * Mnoży dwie macierze 4x4 w formacie row-major.
	 * Macierz przechowywana jako tablica 16 elementów: [row0_col0, row0_col1, ..., row3_col3]
	 * 
	 * @param parentMatrix pierwsza macierz (16 elementów)
	 * @param local druga macierz (16 elementów)
	 * @return wynik mnożenia a * b (16 elementów)
	 */
	private static float[] multiply4x4RowMajor(float[] parentMatrix, float[] local) {
	    float[] result = new float[16];
	    
	    result[0]  = parentMatrix[0] * local[0]  + parentMatrix[1] * local[4]  + parentMatrix[2] * local[8]  + parentMatrix[3] * local[12];
	    result[1]  = parentMatrix[0] * local[1]  + parentMatrix[1] * local[5]  + parentMatrix[2] * local[9]  + parentMatrix[3] * local[13];
	    result[2]  = parentMatrix[0] * local[2]  + parentMatrix[1] * local[6]  + parentMatrix[2] * local[10] + parentMatrix[3] * local[14];
	    result[3]  = parentMatrix[0] * local[3]  + parentMatrix[1] * local[7]  + parentMatrix[2] * local[11] + parentMatrix[3] * local[15];
	    
	    result[4]  = parentMatrix[4] * local[0]  + parentMatrix[5] * local[4]  + parentMatrix[6] * local[8]  + parentMatrix[7] * local[12];
	    result[5]  = parentMatrix[4] * local[1]  + parentMatrix[5] * local[5]  + parentMatrix[6] * local[9]  + parentMatrix[7] * local[13];
	    result[6]  = parentMatrix[4] * local[2]  + parentMatrix[5] * local[6]  + parentMatrix[6] * local[10] + parentMatrix[7] * local[14];
	    result[7]  = parentMatrix[4] * local[3]  + parentMatrix[5] * local[7]  + parentMatrix[6] * local[11] + parentMatrix[7] * local[15];
	    
	    result[8]  = parentMatrix[8] * local[0]  + parentMatrix[9] * local[4]  + parentMatrix[10] * local[8] + parentMatrix[11] * local[12];
	    result[9]  = parentMatrix[8] * local[1]  + parentMatrix[9] * local[5]  + parentMatrix[10] * local[9] + parentMatrix[11] * local[13];
	    result[10] = parentMatrix[8] * local[2]  + parentMatrix[9] * local[6]  + parentMatrix[10] * local[10] + parentMatrix[11] * local[14];
	    result[11] = parentMatrix[8] * local[3]  + parentMatrix[9] * local[7]  + parentMatrix[10] * local[11] + parentMatrix[11] * local[15];
	    
	    result[12] = parentMatrix[12] * local[0] + parentMatrix[13] * local[4] + parentMatrix[14] * local[8] + parentMatrix[15] * local[12];
	    result[13] = parentMatrix[12] * local[1] + parentMatrix[13] * local[5] + parentMatrix[14] * local[9] + parentMatrix[15] * local[13];
	    result[14] = parentMatrix[12] * local[2] + parentMatrix[13] * local[6] + parentMatrix[14] * local[10] + parentMatrix[15] * local[14];
	    result[15] = parentMatrix[12] * local[3] + parentMatrix[13] * local[7] + parentMatrix[14] * local[11] + parentMatrix[15] * local[15];
	    
	    return result;
	}
}
