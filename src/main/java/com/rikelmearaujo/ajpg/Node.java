package com.rikelmearaujo.ajpg;

import java.util.ArrayList;
import java.util.List;

public class Node {

    String type;
    String value;
    public List<Node> childrens;

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
