package io.github.skubiak0903.bdengine;

import static io.github.skubiak0903.bdengine.utils.QuaternionAssert.assertEquals;
import static io.github.skubiak0903.bdengine.utils.VecAssert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.joml.Quaternionf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.skubiak0903.bdengine.entity.BDModelEntity.BDModelEntitySchema;
import net.minestom.server.coordinate.Vec;

public class BDProjectInterpreterTest {
	@Nested
    @DisplayName("Parsing single project")
    class SingleProjectTests {
    	
        @Test
        @DisplayName("Should parse valid JSON with single project")
        void shouldParseSingleProject() {
           /*String json = """
                {
	                "isCollection":true,
	                "name":"Project",
	                "nbt":"",
	                "settings":
	                {
	                "defaultBrightness":false
	                },
	                "mainNBT":"",
	                "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],
	                "children":[
            		{
		                "isBlockDisplay":true,
		                "name":"oak_log[axis=y]",
		                "brightness":{"sky":10,"block":5},
		                "emissiveIntensity":0,
		                "nbt":"",
		                "transforms":[0.9998476951563913,0,-0.01745240643728351,1,0,1,0,1,0.01745240643728351,0,0.9998476951563913,0,0,0,0,1]
	                },

	                {
		                "isTextDisplay":true,
		                "name":"abcd",
		                "options":
		                {
			                "color":"#FFFFFF",
			                "alpha":1,
			                "backgroundColor":"#000000",
			                "backgroundAlpha":1,
			                "bold":false,
			                "italic":false,
			                "underline":false,
			                "strikeThrough":false,
			                "obfuscated":false,
			                "lineLength":50,
			                "align":"center"
		                },
	            		"brightness":{"sky":15,"block":0},
	            		"nbt":"",
	            		"transforms":[0.8018452279175363,-0.5010485079004179,0.32556815445715676,-1.125,0.5699398868988617,0.4776670774922973,-0.6685825965441223,1,0.17947912354250867,0.7216540416368031,0.6685825965441221,0,0,0,0,1]
	                }],
	                "listAnim": [{
		                "id":1,
		                "name":"Default"
	                }],
            		"listSound": [{
		                "id":1,
		                "name":"Default",
		                "tick":2,
		                "tracks":[]
	                }]
                }
                """;*/
        	
            String json = """
                    {
    	                "isCollection":true,
    	                "name":"Project",
    	                "nbt":"",
    	                "settings":
    	                {
    	                "defaultBrightness":false
    	                },
    	                "mainNBT":"",
    	                "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],
    	                "children":[
                		{
    		                "isBlockDisplay":true,
    		                "name":"oak_log[axis=y]",
    		                "brightness":{"sky":10,"block":5},
    		                "emissiveIntensity":0,
    		                "nbt":"",
    		                "transforms":[0.9998476951563913,0,-0.01745240643728351,1,0,1,0,1,0.01745240643728351,0,0.9998476951563913,0,0,0,0,1]
    	                },
        	            {
			                "isItemDisplay":true,
			                "name":"torchflower[display=none]",
			                "brightness":{"sky":15,"block":0},
			                "emissiveIntensity":0,
			                "nbt":"",
			                "tagHead":{"Value":""},
			                "textureValueList":[],
			                "transforms":[2.3,0,0,0.0625,0,5,0,0.5,0,0,0.5,0.5,0,0,0,1]
		                }
    	                ],
    	                "listAnim": [{
    		                "id":1,
    		                "name":"Default"
    	                }],
                		"listSound": [{
    		                "id":1,
    		                "name":"Default",
    		                "tick":2,
    		                "tracks":[]
    	                }]
                    }
                    """;
            
            List<BDModelEntitySchema> schemas = BDProjectInterpreter.getSchemaFromJson(json);
            assertNotNull(schemas, "Schemas should not be null");
            assertEquals(schemas.size(), 2, "Should have 2 schema (2 children)");
            
            BDModelEntitySchema schema0 = schemas.get(0);
            assertNotNull(schema0, "First Schema should not be null");
            assertEquals(true, schema0.isBlockDisplay(), "First object should be a block display");
            assertEquals(false, schema0.isItemDisplay(), "First object shouldn't be a item display");
            assertEquals(5, schema0.blockLight(), "Block light should be 5");
            assertEquals(10, schema0.skyLight(), "Sky light should be 10");
            assertEquals("oak_log[axis=y]", schema0.displayThingSchema(), "Name should match");
            assertEquals(null, schema0.headTexture(), "Head texture should be null");
            assertEquals(1.0f, schema0.width(), "Width should be 1.0f");
            assertEquals(1.0f, schema0.height(), "Height should be 1.0f");
            assertEquals(new Quaternionf().rotateY((float) Math.toRadians(1)), schema0.rotationLeft(), "Left Rotation should match");
            assertEquals(new Quaternionf().identity(), schema0.rotationRight(), "Right Rotation should match");
            assertEquals(new Vec(1, 1, 0), schema0.translation(), "Translation should match");
            assertEquals(new Vec(1, 1, 1), schema0.scale(), "Scale should match");
            
            BDModelEntitySchema schema1 = schemas.get(1);
            assertNotNull(schema1, "Second Schema should not be null");
            assertEquals(false, schema1.isBlockDisplay(),"Second object shouldn't be a block display");
            assertEquals(true, schema1.isItemDisplay(), "Second object should be a item display");
            assertEquals(0, schema1.blockLight(), "Block light should be 5");
            assertEquals(15, schema1.skyLight(), "Sky light should be 10");
            assertEquals("torchflower[display=none]", schema1.displayThingSchema(), "Name should match");
            assertEquals("", schema1.headTexture(), "Head texture should be null");
            assertEquals(1.0f, schema1.width(), "Width should be 1.0f");
            assertEquals(1.0f, schema1.height(), "Height should be 1.0f");
            assertEquals(new Quaternionf().identity(), schema1.rotationLeft(), "Left Rotation should match");
            assertEquals(new Quaternionf().identity(), schema1.rotationRight(), "Right Rotation should match");
            assertEquals(new Vec(0.0625, 0.5, 0.5), schema1.translation(), "Translation should match");
            assertEquals(new Vec(2.3, 5, 0.5), schema1.scale(), "Scale should match");
        }
        
        @Test
        @DisplayName("Should parse block display")
        void shouldParseBlockDisplay() {
        	
            String json = """
                    {
    	                "isCollection":true,
    	                "name":"Project",
    	                "nbt":"",
    	                "settings":
    	                {
    	                "defaultBrightness":false
    	                },
    	                "mainNBT":"",
    	                "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],
    	                "children":[
                		{
    		                "isBlockDisplay":true,
    		                "name":"oak_log[axis=y]",
    		                "brightness":{"sky":10,"block":5},
    		                "emissiveIntensity":0,
    		                "nbt":"",
    		                "transforms":[0.9998476951563913,0,-0.01745240643728351,1,0,1,0,1,0.01745240643728351,0,0.9998476951563913,0,0,0,0,1]
    	                }
    	                ],
    	                "listAnim": [{
    		                "id":1,
    		                "name":"Default"
    	                }],
                		"listSound": [{
    		                "id":1,
    		                "name":"Default",
    		                "tick":2,
    		                "tracks":[]
    	                }]
                    }
                    """;
            
            List<BDModelEntitySchema> schemas = BDProjectInterpreter.getSchemaFromJson(json);
            assertNotNull(schemas, "Schemas should not be null");
            assertEquals(schemas.size(), 1, "Should have 1 schema (1 children)");
            
            BDModelEntitySchema schema0 = schemas.get(0);
            assertNotNull(schema0, "First Schema should not be null");
            assertEquals(true, schema0.isBlockDisplay(), "First object should be a block display");
            assertEquals(false, schema0.isItemDisplay(), "First object shouldn't be a item display");
            assertEquals(5, schema0.blockLight(), "Block light should be 5");
            assertEquals(10, schema0.skyLight(), "Sky light should be 10");
            assertEquals("oak_log[axis=y]", schema0.displayThingSchema(), "Name should match");
            assertEquals(null, schema0.headTexture(), "Head texture should be null");
            assertEquals(1.0f, schema0.width(), "Width should be 1.0f");
            assertEquals(1.0f, schema0.height(), "Height should be 1.0f");
            assertEquals(new Quaternionf().rotateY((float) Math.toRadians(1)), schema0.rotationLeft(), "Left Rotation should match");
            assertEquals(new Quaternionf().identity(), schema0.rotationRight(), "Right Rotation should match");
            assertEquals(new Vec(1, 1, 0), schema0.translation(), "Translation should match");
            assertEquals(new Vec(1, 1, 1), schema0.scale(), "Scale should match");
        }
        
        @Test
        @DisplayName("Should parse item display")
        void shouldParseItemDisplay() {
        	
            String json = """
                    {
    	                "isCollection":true,
    	                "name":"Project",
    	                "nbt":"",
    	                "settings":
    	                {
    	                "defaultBrightness":false
    	                },
    	                "mainNBT":"",
    	                "transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],
    	                "children":[
		                {
			                "isItemDisplay":true,
			                "name":"torchflower[display=none]",
			                "brightness":{"sky":15,"block":0},
			                "emissiveIntensity":0,
			                "nbt":"",
			                "tagHead":{"Value":""},
			                "textureValueList":[],
			                "transforms":[2.3,0,0,0.0625,0,5,0,0.5,0,0,0.5,0.5,0,0,0,1]
		                }
    	                ],
    	                "listAnim": [{
    		                "id":1,
    		                "name":"Default"
    	                }],
                		"listSound": [{
    		                "id":1,
    		                "name":"Default",
    		                "tick":2,
    		                "tracks":[]
    	                }]
                    }
                    """;
            
            List<BDModelEntitySchema> schemas = BDProjectInterpreter.getSchemaFromJson(json);
            assertNotNull(schemas, "Schemas should not be null");
            assertEquals(schemas.size(), 1, "Should have 1 schema (1 children)");
            
            BDModelEntitySchema schema0 = schemas.get(0);
            assertNotNull(schema0, "First Schema should not be null");
            assertEquals(false, schema0.isBlockDisplay(),"First object shouldn't be a block display");
            assertEquals(true, schema0.isItemDisplay(), "First object should be a item display");
            assertEquals(0, schema0.blockLight(), "Block light should be 5");
            assertEquals(15, schema0.skyLight(), "Sky light should be 10");
            assertEquals("torchflower[display=none]", schema0.displayThingSchema(), "Name should match");
            assertEquals("", schema0.headTexture(), "Head texture should be null");
            assertEquals(1.0f, schema0.width(), "Width should be 1.0f");
            assertEquals(1.0f, schema0.height(), "Height should be 1.0f");
            assertEquals(new Quaternionf().identity(), schema0.rotationLeft(), "Left Rotation should match");
            assertEquals(new Quaternionf().identity(), schema0.rotationRight(), "Right Rotation should match");
            assertEquals(new Vec(0.0625, 0.5, 0.5), schema0.translation(), "Translation should match");
            assertEquals(new Vec(2.3, 5, 0.5), schema0.scale(), "Scale should match");
        }
	}
	
	

}
