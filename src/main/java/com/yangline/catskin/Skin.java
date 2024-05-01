package com.yangline.catskin;

public class Skin {
    private String texture;
    private String model;
    public Skin() {}
    public Skin(String texture, String model) { this.texture = texture; this.model = model; }
    public String getTexture() { return texture; }
    public void setTexture(String texture) {
        this.texture = texture;
    }
    public String getModel() { return model; }
    public void setModel(String model) {
        this.model = model;
    }
}
