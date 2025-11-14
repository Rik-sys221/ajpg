package com.rikelmearaujo.ajpg;

import com.rikelmearaujo.ajpg.exceptions.*;
import dev.rikelmearaujo.devtools.data.Node;
import dev.rikelmearaujo.devtools.io.FileUtils;
import dev.rikelmearaujo.devtools.core.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    
    private boolean hadError;
    private final Map<String, String> rawRules;
    private final Map<String, Pattern> rulesMap;
    private final List<RuleException> erros;
    private final Path grammarPath;
    private final Path outputPath;
    
    public Parser(String grammarPath) {
        this.rawRules  = new LinkedHashMap<>();
        this.rulesMap  = new LinkedHashMap<>();
        this.hadError = false;
        this.erros = new ArrayList<>();
        this.grammarPath = Path.of(grammarPath);
        this.outputPath = Path.of("src/parser/ast");
    }
    public Parser(Path grammarPath) {
        this.rawRules  = new LinkedHashMap<>();
        this.rulesMap  = new LinkedHashMap<>();
        this.hadError = false;
        this.erros = new ArrayList<>();
        this.grammarPath = grammarPath.toAbsolutePath();
        this.outputPath = Path.of("src/parser/ast");
    }

    public Node<String, String> parse(String input) {
        String[] rules = loadGrammar();
        defineRules(rules);
        if(handle("!!! Erros encontrados na definicao das regras:")) return new Node<>("FAIL", null);
        for (String ruleName : rulesMap.keySet()) {
            this.generateNode(ruleName);
        }
        Node<String, String> root = eval(input.split("\\s+"));
        return handle("!!! Erros encontrados durante parsing:", root);
    }
    
    private String[] loadGrammar() {
        String content = FileUtils.readText(grammarPath);
        return content.split(";");
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
        return replaceVars(replaceRefs(rawRegex));
    }

    private String replaceVars(String rawRegex) {
        //define regex for variable references like <varname: rulename>
        Pattern varPattern = Pattern.compile("<\\s*(\\w+)\\s*:\\s*(\\w+)\\s*>");
        Matcher matcher = varPattern.matcher(rawRegex);
        String finalRegex = rawRegex;

        while(matcher.find()) {
            String varName = matcher.group(1); //<varname
            String ruleName = matcher.group(2); //rulename>
            if (!this.rawRules.containsKey(ruleName)) {
                erros.add(new InvalidRuleReference("Undefined rule: <" + ruleName + ">"));
                continue;
            }
            if(this.rulesMap.containsKey(ruleName)) {
                finalRegex = finalRegex.replace(matcher.group(), "(?<" + varName + ">(" + this.rulesMap.get(ruleName).pattern() + "))");
                continue;
            }
            finalRegex = finalRegex.replace(matcher.group(), "(?<" + varName + ">(" + replaceRefs("<" + ruleName + ">") + "))");
        }

        return finalRegex;
    }

    private String replaceRefs(String rawRegex) {
        Pattern refPattern = Pattern.compile("<\\s*(\\w+)\\s*>");
        Matcher matcher = refPattern.matcher(rawRegex);
        String finalRegex = rawRegex;
        while(matcher.find()) {
            String ruleName = matcher.group(1);
            if (!this.rawRules.containsKey(ruleName)) {
                erros.add(new InvalidRuleReference("Undefined rule: <" + ruleName + ">"));
                continue;
            }
            if(this.rulesMap.containsKey(ruleName)) {
                finalRegex = finalRegex.replace("<" + ruleName + ">", this.rulesMap.get(ruleName).pattern());
                continue;
            }
            finalRegex = finalRegex.replace("<" + ruleName + ">", "(" + this.rawRules.get(ruleName) + ")");
            finalRegex = replaceRefs(finalRegex);
        }
        return finalRegex;
    }

    private void generateNode(String ruleName) {        
        FileUtils.mkdir(outputPath);
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

        FileUtils.writeText(filePath, content);
    }

    private Node<String, String> eval(String[] input) {
        Node<String, String> root = new Node<>("ROOT", null);
        for (String part : input) {
            ValidationResult res = validate(part);
            if(res.success()) {
                root.add(new Node<>(res.ruleName(), part));
            } else {
                this.erros.add(new NoRuleMatches(res.message()));
            }
        }
        return root;
    }
    
    private ValidationResult validate(String input) {
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
    
    private Node<String, String> handle(String errorHeader, Node<String, String> root) {
        this.hadError = !erros.isEmpty();
        if (this.hadError) {
            Logger.error(errorHeader);
            erros.forEach(e -> Logger.error(" - " + e.getMessage()));
            erros.clear();
            return new Node<>("FAIL", null);
        }
        return root;
    }
    private boolean handle(String errorHeader) {
        this.hadError = !erros.isEmpty();
        if (this.hadError) {
            Logger.error(errorHeader);
            erros.forEach(e -> Logger.error(" - " + e.getMessage()));
            erros.clear();
        }
        return this.hadError;
    }
    
}
