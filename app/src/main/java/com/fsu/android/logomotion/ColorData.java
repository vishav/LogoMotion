package com.fsu.android.logomotion;

public class ColorData {
    private int topColorId;
    private int rChange;
    private int gChange;
    private int bChange;

    public ColorData() {
        this.topColorId = -1;
        this.rChange = 0;
        this.gChange = 0;
        this.bChange = 0;
    }

    public ColorData(int topColorId, int r, int g, int b){
        this.topColorId = topColorId;
        this.rChange = r;
        this.gChange = g;
        this.bChange = b;
    }

    public void set(int topColorId, int[] rgb){
        this.topColorId = topColorId;
        this.rChange = rgb[0];
        this.gChange = rgb[1];
        this.bChange = rgb[2];
    }

    public int getTopColorId() {
        return topColorId;
    }

    public int getrChange() {
        return rChange;
    }

    public int getgChange() {
        return gChange;
    }

    public int getbChange() {
        return bChange;
    }

    public void setTopColorId(int topColorId){
        this.topColorId = topColorId;
    }

    public void setrChange(int rChange) {
        this.rChange = rChange;
    }

    public void setgChange(int gChange) {
        this.gChange = gChange;
    }

    public void setbChange(int bChange) {
        this.bChange = bChange;
    }
}
