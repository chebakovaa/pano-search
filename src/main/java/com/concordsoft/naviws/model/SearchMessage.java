package com.concordsoft.naviws.model;

public class SearchMessage {
    private NaviNode parent;
    private String text;

    public NaviNode getParent() { return parent; }

    public void setParent(NaviNode object) {
        this.parent = object;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
