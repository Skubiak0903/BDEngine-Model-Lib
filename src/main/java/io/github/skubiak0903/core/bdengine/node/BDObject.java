package io.github.skubiak0903.core.bdengine.node;

import java.util.List;

public class BDObject implements BDNode{
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
