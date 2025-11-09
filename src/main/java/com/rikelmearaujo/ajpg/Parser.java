package com.rikelmearaujo.ajpg;

import com.rikelmearaujo.ajpg.exceptions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    
    private boolean hadError;
    Map<String, String> rawRules;
    private final Map<String, Pattern> rulesMap;
    private final List<RuleException> erros;
    private final Path grammarPath;
    private final Path outputPath;
    
    public Parser(String grammarPath) {
        this.rawRules  = new LinkedHashMap<>();
        this.rulesMap  = new LinkedHashMap<>();
        this.hadError = false;
        this.erros = new ArrayList<>();
        this.grammarPath = Path.of(grammarPath).toAbsolutePath();
        this.outputPath = Path.of("src/parser/ast");
    }
    public Parser(Path grammarPath) {
        this.rawRules  = new LinkedHashMap<>();
        this.rulesMap  = new LinkedHashMap<>();
        this.hadError = false;
        this.erros = new ArrayList<>();
        this.grammarPath = grammarPath.toAbsolutePath();
        this.outputPath = Path.of("src/main/java/parser/ast");
    }
    
    public Node parse(String input) throws IOException {
        String[] rules = loadGrammar();
        defineRules(rules);
        if(handle("!!! Erros encontrados na definicao das regras:")) return new Node("FAIL", null);
        for (String ruleName : rulesMap.keySet()) {
            this.generateNode(ruleName);
        }
        Node root = eval(input.split("\\s+"));
        return handle("!!! Erros encontrados durante parsing:", root);
    }
    
    private String[] loadGrammar() throws IOException {
        String content = Files.readString(grammarPath);
        String[] rules = content.split(";");
        return rules;
    }
    
    private void defineRules(String[] rules) {
        for (String rule : rules) {
            rule = rule.trim();
            if (rule.isEmpty()) continue;
            
            String[] sides = rule.split("=", 2);
            if (sides.length < 2) { this.erros.add(new InvalidRuleDeclaration("Rules declarations should be: <rulename> = <regex>;\n Encontrado: " + rule)); continue; }

            String name = sides[0].trim();
            String regex = sides[1].trim();
            this.rawRules.put(name, regex);
        }
        for (Map.Entry<String, String> entry : rawRules.entrySet()) {
            String resolved = resolveRegex(entry.getValue());
            rulesMap.put(entry.getKey(), Pattern.compile(resolved));
        }
    }
    
    private String resolveRegex(String rawRegex) {
        Pattern refPattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = refPattern.matcher(rawRegex);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String ruleName = matcher.group(1);

            if (!this.rawRules.containsKey(ruleName)) {
                erros.add(new InvalidRuleReference("Undefined rule: <" + ruleName + ">"));
                matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + ruleName + ">"));
                continue;
            }

            String replacement = this.rawRules.get(ruleName);
            replacement = resolveRegex(replacement);
            matcher.appendReplacement(sb, Matcher.quoteReplacement("(" + replacement + ")"));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public void generateNode(String ruleName) throws IOException {        
        Files.createDirectories(outputPath);
        String className = toClassName(ruleName);
        Path filePath = outputPath.resolve(className + ".java");

        String content = """
            package parser.ast;

            import com.rikelmearaujo.ajpg.Node;

            public class %s extends Node {
                public %s(String value) {
                    super("%s", value);
                }
            }
            """.formatted(className, className, ruleName);

        if (!Files.exists(filePath)) {
            Files.writeString(filePath, content);
        }
    }

    public Node eval(String[] input) {
        Node root = new Node("ROOT", null);
        for (String part : input) {
            ValidationResult res = validate(part);
            if(res.success()) {
                root.add(new Node(res.ruleName(), part));
            } else {
                this.erros.add(new NoRuleMatches(res.message()));
            }
        }
        return root;
    }
    
    public ValidationResult validate(String input) {
        for (Map.Entry<String, Pattern> entry : rulesMap.entrySet()) {
            Matcher matcher = entry.getValue().matcher(input);
            if (matcher.matches()) {
                return new ValidationResult(true, entry.getKey(), "Entrada valida para a regra " + entry.getKey());
            }
        }
        return new ValidationResult(false, null, "Nenhuma regra aplicavel para a entrada: " + input + ".");
    }
    
    private String toClassName(String ruleName) {
        ruleName = ruleName.trim().toLowerCase();
        ruleName = ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
        return ruleName + "Node";
    }
    
    public Node handle(String errorHeader, Node root) {
        this.hadError = !erros.isEmpty();
        if (this.hadError) {
            System.err.println(errorHeader);
            erros.forEach(e -> System.err.println(" - " + e.getMessage()));
            erros.clear();
            return new Node("FAIL", null);
        }
        return root;
    }
    public boolean handle(String errorHeader) {
        this.hadError = !erros.isEmpty();
        if (this.hadError) {
            System.err.println(errorHeader);
            erros.forEach(e -> System.err.println(" - " + e.getMessage()));
            erros.clear();
        }
        return this.hadError;
    }
    
}
