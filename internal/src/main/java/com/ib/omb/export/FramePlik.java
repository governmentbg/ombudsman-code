package com.ib.omb.export;

public class FramePlik {
    private float bm;
    private float height;
    private float lm;
    private float rm;
    private float tm;
    private float width;
    
    public FramePlik() {
        
        width   = mm2pt(229);
        height  = mm2pt(114);
        lm      = bm = rm = mm2pt(15);
        tm      = mm2pt(18);
        
    }
    
    public FramePlik(int height,
                     int width) {
        
        this();
        this.height = mm2pt(height);
        this.width  = mm2pt(width);
    }
    
    public FramePlik(int height,
                     int lm,
                     int rm,
                     int bm,
                     int tm,
                     int width) {
        
        this.height = mm2pt(height);
        this.lm     = mm2pt(lm);
        this.rm     = mm2pt(rm);
        this.bm     = mm2pt(bm);
        this.tm     = mm2pt(tm);
        this.width  = mm2pt(width);
    }

    public float getBm() {
        return bm;
    }

    public float getHeight() {
        return height;
    }

    public float getLm() {
        return lm;
    }

    public float getRm() {
        return rm;
    }

    public float getTm() {
        return tm;
    }

    public float getWidth() {
        return width;
    }

    private float mm2pt(int mm) {
        float r = (float) (1 / 25.4 * 72 * mm);
        return r;
    }

    public void setBm(float bm) {
        this.bm = bm;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setLm(float lm) {
        this.lm = lm;
    }

    public void setRm(float rm) {
        this.rm = rm;
    }

    public void setTm(float tm) {
        this.tm = tm;
    }

    public void setWidth(float width) {
        this.width = width;
    }

}
