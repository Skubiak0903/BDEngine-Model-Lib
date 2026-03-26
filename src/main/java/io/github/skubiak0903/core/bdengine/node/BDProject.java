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
