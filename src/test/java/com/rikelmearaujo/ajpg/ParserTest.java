package com.rikelmearaujo.ajpg;
import java.io.IOException;
import java.nio.file.Paths;

public class ParserTest {
    public void testRecursiveRuleReference() {

        Parser parser = new Parser(Paths.get("src/main/java/sintax.txt"));
        Node result = null;
        try {
            result = parser.parse("-65");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(result.toString());
    }
}
