package com.rikelmearaujo.ajpg;
import dev.rikelmearaujo.devtools.data.Node;
import org.junit.Test;

public class ParserTest {

    @Test
    public void testRecursiveRuleReference() {

        Parser parser = new Parser("src/main/java/sintax.txt");
        Node<String, String> result = null;
        result = parser.parse("-65");
        System.out.println(result.toString());
    }

}
