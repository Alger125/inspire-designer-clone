package com.vdp.core.expression;

import com.vdp.core.model.DataNode;
import com.vdp.core.model.DataPathResolver;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates expressions against a DataNode.
 * 
 * Part of FASE 15 - EXPRESSION ENGINE.
 * Currently supports variable interpolation and basic functions.
 * Future versions will support a full AST with mathematical operators.
 */
public final class ExpressionEngine {

    private static final Pattern FIELD_REF = Pattern.compile("\\{([^{}]+)\\}");
    private static final Pattern FUNCTION =
            Pattern.compile("^(UPPER|LOWER|TRIM|LENGTH)\\((.*)\\)$", Pattern.DOTALL);

    private ExpressionEngine() {
        // Static utility class for now, later could be instantiated with context
    }

    /**
     * Evaluates an expression against a single record.
     */
    public static String evaluate(String expression, DataNode record) {
        String expr = expression == null ? "" : expression.trim();
        
        Matcher fn = FUNCTION.matcher(expr);
        if (fn.matches()) {
            String inner = evaluate(fn.group(2), record);
            return switch (fn.group(1)) {
                case "UPPER" -> inner.toUpperCase();
                case "LOWER" -> inner.toLowerCase();
                case "TRIM" -> inner.trim();
                default -> String.valueOf(inner.length());
            };
        }
        
        Matcher ref = FIELD_REF.matcher(expression == null ? "" : expression);
        StringBuilder sb = new StringBuilder();
        while (ref.find()) {
            String value = DataPathResolver.resolveValue(record, ref.group(1).trim());
            ref.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        ref.appendTail(sb);
        return sb.toString();
    }
}
