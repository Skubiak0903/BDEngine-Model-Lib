package io.github.skubiak0903.bdengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.skubiak0903.bdengine.entity.BDModelEntity;
import io.github.skubiak0903.bdengine.entity.BDModelEntitySchema;

public class BDModelRegistryTest {
    private static final String TEST_MODEL_JSON = """
    		[{"isCollection":true,"name":"Project","nbt":"","settings":{"defaultBrightness":false},"mainNBT":"","transforms":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],"children":[{"isBlockDisplay":true,"name":"spruce_fence_gate","brightness":{"sky":15,"block":0},"emissiveIntensity":0,"nbt":"","transforms":[1,0,0,-0.5,0,1,0,-0.5,0,0,1,-0.5,0,0,0,1]}],"listAnim":[{"id":1,"name":"Default"}],"listSound":[{"id":1,"name":"Default","tick":2,"tracks":[]}]}]
    		""";
    
    @BeforeEach
    void setUp() {
        BDModelRegistry.clear();
    }
    
    @AfterEach
    void tearDown() {
        BDModelRegistry.clear();
    }
    
    @Nested
    @DisplayName("Registration tests")
    class RegistrationTests {
        
        @Test
        @DisplayName("Should register model from JSON string")
        void shouldRegisterFromJson() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            
            assertTrue(BDModelRegistry.contains("test"));
            assertEquals(1, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should register model from schema")
        void shouldRegisterFromSchema() {
            List<BDModelEntitySchema> schema = BDProjectInterpreter.getSchemaFromJson(TEST_MODEL_JSON);
            BDModelRegistry.register("test", schema);
            
            assertTrue(BDModelRegistry.contains("test"));
            assertEquals(1, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should not register duplicate models")
        void shouldNotRegisterDuplicate() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            
            assertEquals(1, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should throw for null identifier")
        void shouldThrowForNullIdentifier() {
            assertThrows(IllegalArgumentException.class, 
                () -> BDModelRegistry.register(null, TEST_MODEL_JSON));
        }
        
        @Test
        @DisplayName("Should throw for empty identifier")
        void shouldThrowForEmptyIdentifier() {
            assertThrows(IllegalArgumentException.class, 
                () -> BDModelRegistry.register("", TEST_MODEL_JSON));
            
            assertThrows(IllegalArgumentException.class, 
                () -> BDModelRegistry.register("   ", TEST_MODEL_JSON));
        }
        
        @Test
        @DisplayName("Should throw for null schema")
        void shouldThrowForNullSchema() {
            assertThrows(IllegalArgumentException.class, 
                () -> BDModelRegistry.register("test", (List<BDModelEntitySchema>) null));
        }
    }
    
    @Nested
    @DisplayName("Retrieval tests")
    class RetrievalTests {
        
        @BeforeEach
        void registerModel() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
        }
        
        @Test
        @DisplayName("Should get model by identifier")
        void shouldGetModel() {
            BDModelEntity model = BDModelRegistry.get("test");
            
            assertNotNull(model);
        }
        
        @Test
        @DisplayName("Should return null for non-existent model")
        void shouldReturnNullForNonExistent() {
            BDModelEntity model = BDModelRegistry.get("nonexistent");
            
            assertNull(model);
        }
        
        @Test
        @DisplayName("Should get schema")
        void shouldGetSchema() {
            List<BDModelEntitySchema> schema = BDModelRegistry.getSchema("test");
            
            assertNotNull(schema);
            assertFalse(schema.isEmpty());
        }
        
        @Test
        @DisplayName("Should return null for non-existent schema")
        void shouldReturnNullForNonExistentSchema() {
            List<BDModelEntitySchema> schema = BDModelRegistry.getSchema("nonexistent");
            
            assertNull(schema);
        }
        
        @Test
        @DisplayName("Should check if model exists")
        void shouldCheckExistence() {
            assertTrue(BDModelRegistry.contains("test"));
            assertFalse(BDModelRegistry.contains("nonexistent"));
        }
        
        @Test
        @DisplayName("Should get registered models set")
        void shouldGetRegisteredModels() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            BDModelRegistry.register("test2", TEST_MODEL_JSON);
            
            Set<String> models = BDModelRegistry.getRegisteredModels();
            
            assertEquals(2, models.size());
            assertTrue(models.contains("test"));
            assertTrue(models.contains("test2"));
        }
        
        @Test
        @DisplayName("Should return unmodifiable set")
        void shouldReturnUnmodifiableSet() {
            Set<String> models = BDModelRegistry.getRegisteredModels();
            
            assertThrows(UnsupportedOperationException.class, 
                () -> models.add("new"));
        }
        
        @Test
        @DisplayName("Should get size correctly")
        void shouldGetSize() {
            assertEquals(1, BDModelRegistry.size());
            
            BDModelRegistry.register("test2", TEST_MODEL_JSON);
            assertEquals(2, BDModelRegistry.size());
        }
    }
    
    @Nested
    @DisplayName("Removal tests")
    class RemovalTests {
        
        @Test
        @DisplayName("Should unregister model")
        void shouldUnregister() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            
            boolean removed = BDModelRegistry.unregister("test");
            
            assertTrue(removed);
            assertFalse(BDModelRegistry.contains("test"));
            assertEquals(0, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should return false when unregistering non-existent model")
        void shouldReturnFalseForNonExistent() {
            boolean removed = BDModelRegistry.unregister("nonexistent");
            
            assertFalse(removed);
        }
        
        @Test
        @DisplayName("Should clear all models")
        void shouldClearAll() {
            BDModelRegistry.register("test1", TEST_MODEL_JSON);
            BDModelRegistry.register("test2", TEST_MODEL_JSON);
            
            BDModelRegistry.clear();
            
            assertEquals(0, BDModelRegistry.size());
            assertFalse(BDModelRegistry.contains("test1"));
            assertFalse(BDModelRegistry.contains("test2"));
        }
        
        @Test
        @DisplayName("Should be able to re-register after unregister")
        void shouldReRegisterAfterUnregister() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            BDModelRegistry.unregister("test");
            
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            
            assertTrue(BDModelRegistry.contains("test"));
            assertEquals(1, BDModelRegistry.size());
        }
    }
    
    @Nested
    @DisplayName("Thread-safety tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should be thread-safe for concurrent registration")
        void shouldBeThreadSafeForConcurrentRegistration() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        BDModelRegistry.register("model-" + index, TEST_MODEL_JSON);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            assertEquals(threadCount, BDModelRegistry.size());
            Set<String> models = BDModelRegistry.getRegisteredModels();
            for (int i = 0; i < threadCount; i++) {
                assertTrue(models.contains("model-" + i));
            }
        }
        
        @Test
        @DisplayName("Should be thread-safe for concurrent reads and writes")
        void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
            BDModelRegistry.register("shared", TEST_MODEL_JSON);
            
            ExecutorService executor = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(20);
            
            // Writers
            for (int i = 0; i < 10; i++) {
                final int index = i;
                executor.submit(() -> {
                    BDModelRegistry.register("write-" + index, TEST_MODEL_JSON);
                    latch.countDown();
                });
            }
            
            // Readers
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    BDModelRegistry.get("shared");
                    BDModelRegistry.contains("shared");
                    BDModelRegistry.size();
                    latch.countDown();
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Should have 11 models (10 new + 1 shared)
            assertEquals(11, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should handle concurrent register and unregister")
        void shouldHandleConcurrentRegisterAndUnregister() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(10);
            
            for (int i = 0; i < 10; i++) {
                final int index = i;
                executor.submit(() -> {
                    String id = "test-" + index;
                    BDModelRegistry.register(id, TEST_MODEL_JSON);
                    BDModelRegistry.unregister(id);
                    latch.countDown();
                });
            }
            
            latch.await();
            executor.shutdown();
            
            assertEquals(0, BDModelRegistry.size());
        }
    }
    
    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should register and retrieve complex model")
        void shouldRegisterAndRetrieveModel() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            
            BDModelEntity model = BDModelRegistry.get("test");
            
            assertNotNull(model);
        }
        
        @Test
        @DisplayName("Should register multiple models and retrieve correctly")
        void shouldRegisterMultipleModels() {
            BDModelRegistry.register("test", TEST_MODEL_JSON);
            BDModelRegistry.register("test2", TEST_MODEL_JSON);
            
            assertNotNull(BDModelRegistry.get("test"));
            assertEquals(2, BDModelRegistry.size());
        }
        
        @Test
        @DisplayName("Should preserve schema immutability")
        void shouldPreserveSchemaImmutability() {
            List<BDModelEntitySchema> originalSchema = BDProjectInterpreter.getSchemaFromJson(TEST_MODEL_JSON);
            BDModelRegistry.register("test", originalSchema);
            
            List<BDModelEntitySchema> retrievedSchema = BDModelRegistry.getSchema("test");
            
            assertThrows(UnsupportedOperationException.class, 
                () -> retrievedSchema.add(null));
        }
    }
    
    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle empty registry")
        void shouldHandleEmptyRegistry() {
            assertEquals(0, BDModelRegistry.size());
            assertFalse(BDModelRegistry.contains("anything"));
            assertNull(BDModelRegistry.get("anything"));
            assertNull(BDModelRegistry.getSchema("anything"));
            assertTrue(BDModelRegistry.getRegisteredModels().isEmpty());
        }
        
        @Test
        @DisplayName("Should handle very long identifier")
        void shouldHandleLongIdentifier() {
            String longIdentifier = "a".repeat(1000);
            
            BDModelRegistry.register(longIdentifier, TEST_MODEL_JSON);
            
            assertTrue(BDModelRegistry.contains(longIdentifier));
        }
        
        @Test
        @DisplayName("Should handle special characters in identifier")
        void shouldHandleSpecialCharacters() {
            String identifier = "model_123-test@example.com";
            
            BDModelRegistry.register(identifier, TEST_MODEL_JSON);
            
            assertTrue(BDModelRegistry.contains(identifier));
        }
    }
}