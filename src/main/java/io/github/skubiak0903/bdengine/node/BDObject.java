package io.github.skubiak0903.bdengine.node;

public class BDObject extends BDNode {
	public boolean isItemDisplay;
	public boolean isBlockDisplay;
	public boolean isTextDisplay;
	public String name;
    public Brightness brightness;
    public int emissiveIntensity;
    public String nbt;
    public TagHead tagHead;
    //public List<?> textureValueList;
    //public ? paintTexture;
    public String defaultTextureValue;
    
    public static class TagHead {
        public String Value; // Base64 head texture
    }
    
    public static class Brightness {
        public int sky;
        public int block;
    } 
}
