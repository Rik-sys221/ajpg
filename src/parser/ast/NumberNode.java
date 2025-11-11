package parser.ast;

import com.rikelmearaujo.ajpg.Node;

public class NumberNode extends Node {
    public NumberNode(String value) {
        super("number", value);
    }
}
