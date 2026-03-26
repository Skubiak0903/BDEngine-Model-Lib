package io.github.skubiak0903.core.bdengine.node;

import java.util.List;

public class BDProject implements BDNode {
	public boolean isCollection;
	public boolean isBackCollection;
	public String name;
	public String nbt;
	public BDSettings settings;
	public String mainNbt;
	public List<Double> transforms; // default state
	public List<BDNode> children;
	public List<BDAnim> listAnim;
	public List<BDSound> listSound;
	public BDDefaultTransform defaultTransform;


    public static class BDObject implements BDNode{
    	public boolean isItemDisplay;
    	public boolean isBlockDisplay;
    	public String name;
        public Brightness brightness;
        public int emissiveIntensity;
        public String nbt;
        public TagHead tagHead;
        //public List<?> textureValueList;
        //public ? paintTexture;
        public List<Double> transforms; // 4x4 Matrix (16 values)
        public String defaultTextureValue;
        
        public static class TagHead {
            public String Value; // Base64 head texture
        }
        
        public static class Brightness {
            public int sky;
            public int block;
        } 
    }
    
    public static class BDSettings {
    	public boolean defaultBrightness;
    }
    
    public static class BDAnim {
    	public int id;
    	public String name;
    }
    
    public static class BDSound {
    	public int id;
    	public String name;
    	public int tick;
    	//public List<?> tracks;
    }
    
    public static class BDDefaultTransform {
		public List<Double> position;
		public BDRotation rotation;
		public List<Double> scale;
		
		public static class BDRotation {
			double x,y,z;
		}
	}
}
