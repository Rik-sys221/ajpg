package com.rikelmearaujo.ajpg;

public record ValidationResult(boolean success, String ruleName, String message) {}