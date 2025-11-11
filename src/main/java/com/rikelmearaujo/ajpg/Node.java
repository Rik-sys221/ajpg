package com.rikelmearaujo.ajpg;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private String type;
    public String getType() {
        return type;
    }

    private String value;
    public String getValue() {
        return value;
    }

    private List<Node> childrens;
    public List<Node> getChildrens() {
        return childrens;
    }

    public Node(String type, String value) {
        this.type = type;
        this.value = value;
        this.childrens = new ArrayList<>();
    }
    
    public void add(Node node) {
        this.childrens.add(node);
    }

    @Override
    public String toString() {
        String childrenStr = childrens.stream()
                .map(Node::toString)
                .collect(java.util.stream.Collectors.joining(", "));

        return "{ type: " + type + ", value: " + value + ", childrens: [" + childrenStr + "] }";
    }

}
