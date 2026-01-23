/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.evaluators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;
import org.elasticsearch.xpack.escript.primitives.LambdaExpression;
import org.elasticsearch.xpack.escript.operators.OperatorHandlerRegistry;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpressionEvaluator {
    private final ProcedureExecutor executor;
    private final ThreadPool threadPool;
    private final ExecutionContext context;
    private static final Logger LOGGER = LogManager.getLogger(ExpressionEvaluator.class);

    public ExpressionEvaluator(ProcedureExecutor executor) {
        this.executor = executor;
        this.threadPool = executor.getThreadPool();
        this.context = executor.getContext();
    }

    /**
     * Evaluates an expression asynchronously.
     * If the expression contains concatenation (i.e. multiple ternaryExpressions separated by '||'),
     * then each operand is evaluated and the results are concatenated as strings.
     *
     * @param ctx the ExpressionContext from the parse tree.
     * @param listener an ActionListener to receive the evaluated result.
     */
    public void evaluateExpressionAsync(ElasticScriptParser.ExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx == null) {
            listener.onFailure(new RuntimeException("Null expression context"));
            return;
        }
        List<ElasticScriptParser.TernaryExpressionContext> operands = ctx.ternaryExpression();
        if (operands.size() == 1) {
            // No concatenation operator present – evaluate normally.
            evaluateTernaryExpressionAsync(operands.get(0), listener);
        } else {
            // Evaluate each operand and concatenate their results as strings.
            evaluateTernaryOperandsAndConcatenate(operands, 0, new ArrayList<>(), listener);
        }
    }

    /**
     * Evaluates an expression from a string representation.
     * This parses the string and evaluates it as an expression.
     *
     * @param expression The expression string to evaluate.
     * @param listener   The ActionListener to receive the result.
     */
    public void evaluateExpressionStringAsync(String expression, ActionListener<Object> listener) {
        try {
            org.antlr.v4.runtime.CharStream charStream = org.antlr.v4.runtime.CharStreams.fromString(expression);
            ElasticScriptLexer lexer = new ElasticScriptLexer(charStream);
            org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
            ElasticScriptParser parser = new ElasticScriptParser(tokens);
            ElasticScriptParser.ExpressionContext exprCtx = parser.expression();
            
            if (exprCtx != null) {
                evaluateExpressionAsync(exprCtx, listener);
            } else {
                listener.onFailure(new RuntimeException("Failed to parse expression: " + expression));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    private void evaluateTernaryOperandsAndConcatenate(List<ElasticScriptParser.TernaryExpressionContext> operands,
                                                       int index, List<Object> results, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            // Concatenate all results into one string.
            StringBuilder sb = new StringBuilder();
            for (Object res : results) {
                sb.append(String.valueOf(res));
            }
            listener.onResponse(sb.toString());
            return;
        }
        // Evaluate each operand asynchronously.
        evaluateTernaryExpressionAsync(operands.get(index), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                results.add(result);
                evaluateTernaryOperandsAndConcatenate(operands, index + 1, results, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Evaluates a ternary expression: condition ? trueValue : falseValue
     */
    private void evaluateTernaryExpressionAsync(ElasticScriptParser.TernaryExpressionContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.NullCoalesceExpressionContext> parts = ctx.nullCoalesceExpression();
        if (parts.size() == 1) {
            // No ternary operator – just evaluate the null coalesce expression
            evaluateNullCoalesceExpressionAsync(parts.get(0), listener);
        } else if (parts.size() == 3) {
            // Ternary: condition ? trueValue : falseValue
            evaluateNullCoalesceExpressionAsync(parts.get(0), ActionListener.wrap(
                conditionResult -> {
                    boolean condition = toTruthyValue(conditionResult);
                    if (condition) {
                        evaluateNullCoalesceExpressionAsync(parts.get(1), listener);
                    } else {
                        evaluateNullCoalesceExpressionAsync(parts.get(2), listener);
                    }
                },
                listener::onFailure
            ));
        } else {
            listener.onFailure(new RuntimeException("Invalid ternary expression"));
        }
    }

    /**
     * Evaluates a null coalesce expression: value ?? defaultValue
     */
    private void evaluateNullCoalesceExpressionAsync(ElasticScriptParser.NullCoalesceExpressionContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.LogicalOrExpressionContext> parts = ctx.logicalOrExpression();
        if (parts.size() == 1) {
            // No null coalesce operator
            evaluateLogicalOrExpressionAsync(parts.get(0), listener);
        } else {
            // Null coalesce: evaluate left, if null evaluate right
            evaluateNullCoalesceChainAsync(parts, 0, listener);
        }
    }

    private void evaluateNullCoalesceChainAsync(List<ElasticScriptParser.LogicalOrExpressionContext> parts,
                                                int index, ActionListener<Object> listener) {
        if (index >= parts.size()) {
            // All parts were null
            listener.onResponse(null);
            return;
        }
        evaluateLogicalOrExpressionAsync(parts.get(index), ActionListener.wrap(
            result -> {
                if (result != null) {
                    // Found non-null value
                    listener.onResponse(result);
                } else {
                    // Current is null, try next
                    evaluateNullCoalesceChainAsync(parts, index + 1, listener);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Converts a value to a truthy boolean (for ternary conditions).
     */
    private boolean toTruthyValue(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue() != 0;
        }
        if (obj instanceof String) {
            return !((String) obj).isEmpty();
        }
        if (obj instanceof List) {
            return !((List<?>) obj).isEmpty();
        }
        return true; // Non-null objects are truthy
    }

    private void evaluateLogicalOrExpressionAsync(ElasticScriptParser.LogicalOrExpressionContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.LogicalAndExpressionContext> andExprs = ctx.logicalAndExpression();
        if (andExprs.size() == 1) {
            evaluateLogicalAndExpressionAsync(andExprs.get(0), listener);
        } else {
            evaluateLogicalOrOperandsAsync(andExprs, 0, listener);
        }
    }

    private void evaluateLogicalOrOperandsAsync(List<ElasticScriptParser.LogicalAndExpressionContext> operands,
                                                int index, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(false);
            return;
        }
        ActionListener<Object> l = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if (booleanResult) {
                    listener.onResponse(true);
                } else {
                    evaluateLogicalOrOperandsAsync(operands, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> logger = ActionListenerUtils.withLogging(l, getClass().getName(),
            "Evaluate-Logical-Or-Operands: " + operands.get(index));
        evaluateLogicalAndExpressionAsync(operands.get(index), logger);
    }

    private void evaluateLogicalAndExpressionAsync(ElasticScriptParser.LogicalAndExpressionContext ctx, ActionListener<Object> listener) {
        List<ElasticScriptParser.EqualityExpressionContext> eqExprs = ctx.equalityExpression();
        if (eqExprs.size() == 1) {
            evaluateEqualityExpressionAsync(eqExprs.get(0), listener);
        } else {
            evaluateLogicalAndOperandsAsync(eqExprs, 0, listener);
        }
    }

    private void evaluateLogicalAndOperandsAsync(List<ElasticScriptParser.EqualityExpressionContext> operands,
                                                 int index, ActionListener<Object> listener) {
        if (index >= operands.size()) {
            listener.onResponse(true);
            return;
        }
        ActionListener<Object> l = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                boolean booleanResult = toBoolean(result);
                if ( booleanResult == false ) {
                    listener.onResponse(false);
                } else {
                    evaluateLogicalAndOperandsAsync(operands, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> logger = ActionListenerUtils.withLogging(l, getClass().getName(),
            "Evaluate-Logical-And-Operands: " + operands.get(index));
        evaluateEqualityExpressionAsync(operands.get(index), logger);
    }

    private void evaluateEqualityExpressionAsync(ElasticScriptParser.EqualityExpressionContext ctx, ActionListener<Object> listener) {
        // Single operand: delegate to relational
        if (ctx.relationalExpression().size() == 1) {
            evaluateRelationalExpressionAsync(ctx.relationalExpression(0), listener);
            return;
        }
        // Use operator registry for '==' and '<>'
        String operator = ctx.getChild(1).getText();
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);
        // Evaluate both sides asynchronously using named listeners for clarity
        final Object[] leftResultHolder = new Object[1];
        ActionListener<Object> rightListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object right) {
                try {
            if (leftResultHolder[0] == null || right == null) {
                listener.onResponse(leftResultHolder[0] == right);
            } else if (handler.isApplicable(leftResultHolder[0], right)) {
                listener.onResponse(handler.apply(leftResultHolder[0], right));
            } else {
                listener.onFailure(new RuntimeException(
                    "Operator '" + operator + "' not applicable for types: "
                    + leftResultHolder[0].getClass().getSimpleName() + ", " + right.getClass().getSimpleName()));
            }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> leftListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object left) {
                leftResultHolder[0] = left;
                evaluateRelationalExpressionAsync(ctx.relationalExpression(1), rightListener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        evaluateRelationalExpressionAsync(ctx.relationalExpression(0), leftListener);
    }

    private void evaluateRelationalExpressionAsync(ElasticScriptParser.RelationalExpressionContext ctx, ActionListener<Object> listener) {
        // Handle IS NULL expression
        if (ctx instanceof ElasticScriptParser.IsNullExprContext isNullCtx) {
            evaluateAdditiveExpressionAsync(isNullCtx.additiveExpression(), ActionListener.wrap(
                value -> listener.onResponse(value == null),
                listener::onFailure
            ));
            return;
        }
        
        // Handle IS NOT NULL expression
        if (ctx instanceof ElasticScriptParser.IsNotNullExprContext isNotNullCtx) {
            evaluateAdditiveExpressionAsync(isNotNullCtx.additiveExpression(), ActionListener.wrap(
                value -> listener.onResponse(value != null),
                listener::onFailure
            ));
            return;
        }
        
        // Handle IN (list) expression: value IN (1, 2, 3)
        if (ctx instanceof ElasticScriptParser.InListExprContext inListCtx) {
            evaluateAdditiveExpressionAsync(inListCtx.additiveExpression(), ActionListener.wrap(
                value -> evaluateExpressionListForIn(inListCtx.expressionList().expression(), value, false, listener),
                listener::onFailure
            ));
            return;
        }
        
        // Handle NOT IN (list) expression: value NOT IN (1, 2, 3)
        if (ctx instanceof ElasticScriptParser.NotInListExprContext notInListCtx) {
            evaluateAdditiveExpressionAsync(notInListCtx.additiveExpression(), ActionListener.wrap(
                value -> evaluateExpressionListForIn(notInListCtx.expressionList().expression(), value, true, listener),
                listener::onFailure
            ));
            return;
        }
        
        // Handle IN array expression: value IN arrayVar
        if (ctx instanceof ElasticScriptParser.InArrayExprContext inArrayCtx) {
            evaluateAdditiveExpressionAsync(inArrayCtx.additiveExpression(0), ActionListener.wrap(
                value -> evaluateAdditiveExpressionAsync(inArrayCtx.additiveExpression(1), ActionListener.wrap(
                    arrayValue -> {
                        try {
                            boolean found = checkValueInCollection(value, arrayValue);
                            listener.onResponse(found);
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    },
                    listener::onFailure
                )),
                listener::onFailure
            ));
            return;
        }
        
        // Handle NOT IN array expression: value NOT IN arrayVar
        if (ctx instanceof ElasticScriptParser.NotInArrayExprContext notInArrayCtx) {
            evaluateAdditiveExpressionAsync(notInArrayCtx.additiveExpression(0), ActionListener.wrap(
                value -> evaluateAdditiveExpressionAsync(notInArrayCtx.additiveExpression(1), ActionListener.wrap(
                    arrayValue -> {
                        try {
                            boolean found = checkValueInCollection(value, arrayValue);
                            listener.onResponse(!found);
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    },
                    listener::onFailure
                )),
                listener::onFailure
            ));
            return;
        }
        
        // Handle comparison expression (the original logic)
        if (ctx instanceof ElasticScriptParser.ComparisonExprContext compCtx) {
            // Single operand: delegate to additive
            if (compCtx.additiveExpression().size() == 1) {
                evaluateAdditiveExpressionAsync(compCtx.additiveExpression(0), listener);
                return;
            }
            // Use operator registry for '<', '<=', '>', '>='
            String operator = compCtx.getChild(1).getText();
            OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
            BinaryOperatorHandler handler = registry.getHandler(operator);
            // Evaluate both sides asynchronously
            evaluateAdditiveExpressionAsync(compCtx.additiveExpression(0), ActionListener.wrap(
                left -> evaluateAdditiveExpressionAsync(compCtx.additiveExpression(1), ActionListener.wrap(
                    right -> {
                        try {
                            if (left == null || right == null) {
                                listener.onResponse(false);
                            } else if (handler.isApplicable(left, right)) {
                                listener.onResponse(handler.apply(left, right));
                            } else {
                                listener.onFailure(new RuntimeException(
                                    "Operator '" + operator + "' not applicable for types: "
                                    + left.getClass().getSimpleName() + ", " + right.getClass().getSimpleName()));
                            }
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    }, listener::onFailure)),
                listener::onFailure));
            return;
        }
        
        // Fallback for unexpected context type
        listener.onFailure(new RuntimeException("Unsupported relational expression: " + ctx.getText()));
    }

    private void evaluateAdditiveExpressionAsync(ElasticScriptParser.AdditiveExpressionContext ctx, ActionListener<Object> listener) {
        if (ctx.multiplicativeExpression().size() == 1) {
            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), listener);
        } else {
            ActionListener<Object> operandListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateAdditiveOperandsAsync(ctx, 1, initialValue, listener);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> logger = ActionListenerUtils.withLogging(operandListener, getClass().getName(),
                "Evaluate-Additive Expression: " + ctx.multiplicativeExpression(0));
            evaluateMultiplicativeExpressionAsync(ctx.multiplicativeExpression(0), logger);
        }
    }

    private void evaluateAdditiveOperandsAsync(ElasticScriptParser.AdditiveExpressionContext ctx,
                                               int index, Object leftValue, ActionListener<Object> listener) {
        if (index >= ctx.multiplicativeExpression().size()) {
            listener.onResponse(leftValue);
            return;
        }
        ElasticScriptParser.MultiplicativeExpressionContext currentExpr = ctx.multiplicativeExpression(index);
        String operator = ctx.getChild(2 * index - 1).getText();

        // Instantiate the registry (or use a cached instance)
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);

        ActionListener<Object> evalOperandListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object rightValue) {
                try {
                    Object result = handler.apply(leftValue, rightValue);
                    evaluateAdditiveOperandsAsync(ctx, index + 1, result, listener);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> evalLogger = ActionListenerUtils.withLogging(evalOperandListener, getClass().getName(),
            "Evaluate-Additive Operands: " + currentExpr);
        evaluateMultiplicativeExpressionAsync(currentExpr, evalLogger);
    }

    private void evaluateMultiplicativeExpressionAsync(ElasticScriptParser.MultiplicativeExpressionContext ctx,
                                                       ActionListener<Object> listener) {
        if (ctx.unaryExpr().size() == 1) {
            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), listener);
        } else {
            ActionListener<Object> initListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object initialValue) {
                    evaluateMultiplicativeOperandsAsync(ctx, 1, initialValue, listener);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> initLogger = ActionListenerUtils.withLogging(initListener, getClass().getName(),
                "Evaluate-Multiplicative Expression: " + ctx.unaryExpr(0));
            evaluateUnaryExpressionAsync(ctx.unaryExpr(0), initLogger);
        }
    }

    private void evaluateMultiplicativeOperandsAsync(ElasticScriptParser.MultiplicativeExpressionContext ctx,
                                                     int index, Object leftValue, ActionListener<Object> listener) {
        if (index >= ctx.unaryExpr().size()) {
            listener.onResponse(leftValue);
            return;
        }
        // Compute using the operator registry for '*', '/', '%'
        ElasticScriptParser.UnaryExprContext currentExpr = ctx.unaryExpr(index);
        String operator = ctx.getChild(2 * index - 1).getText();
        OperatorHandlerRegistry registry = new OperatorHandlerRegistry();
        BinaryOperatorHandler handler = registry.getHandler(operator);
        // Evaluate the right operand asynchronously
        ActionListener<Object> rightListener = ActionListener.wrap(
            rightValue -> {
                try {
                    if (handler.isApplicable(leftValue, rightValue)) {
                        Object result = handler.apply(leftValue, rightValue);
                        evaluateMultiplicativeOperandsAsync(ctx, index + 1, result, listener);
                    } else {
                        listener.onFailure(new RuntimeException(
                            "Operator '" + operator + "' not applicable for types: "
                            + leftValue.getClass().getSimpleName() + ", "
                            + (rightValue != null ? rightValue.getClass().getSimpleName() : "null")));
                    }
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            },
            listener::onFailure
        );
        ActionListener<Object> rightLogger = ActionListenerUtils.withLogging(
            rightListener, getClass().getName(),
            "Evaluate-Multiplicative Right Operand: " + currentExpr
        );
        evaluateUnaryExpressionAsync(currentExpr, rightLogger);
    }

    @SuppressWarnings("checkstyle:DescendantToken")
    private void evaluateUnaryExpressionAsync(ElasticScriptParser.UnaryExprContext ctx, ActionListener<Object> listener) {
        if (ctx.primaryExpression() != null) {
            evaluatePrimaryExpressionAsync(ctx.primaryExpression(), listener);
        } else if (ctx.unaryExpr() != null) {
            String operator = ctx.getChild(0).getText();
            ActionListener<Object> unaryListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        if (operator.equals("-")) {
                            if (result instanceof Number) {
                                double value = ((Number) result).doubleValue();
                                listener.onResponse(-value);
                            } else {
                                listener.onFailure(new RuntimeException("Unary minus can only be applied to numbers."));
                            }
                        } else if (operator.equals("NOT") || operator.equals("!")) {
                            // Handle truthy/falsy for non-booleans
                            boolean boolValue;
                            if (result instanceof Boolean) {
                                boolValue = (Boolean) result;
                            } else if (result == null) {
                                boolValue = false;  // null is falsy
                            } else if (result instanceof Number) {
                                boolValue = ((Number) result).doubleValue() != 0;  // 0 is falsy
                            } else if (result instanceof String) {
                                boolValue = !((String) result).isEmpty();  // empty string is falsy
                            } else if (result instanceof List) {
                                boolValue = !((List<?>) result).isEmpty();  // empty list is falsy
                            } else {
                                boolValue = true;  // non-null objects are truthy
                            }
                            listener.onResponse(!boolValue);
                        } else {
                            listener.onFailure(new RuntimeException("Unknown unary operator: " + operator));
                        }
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };
            ActionListener<Object> unaryLogger = ActionListenerUtils.withLogging(unaryListener, getClass().getName(),
                "Evaluate-Unary Expression: " + ctx.unaryExpr());
            evaluateUnaryExpressionAsync(ctx.unaryExpr(), unaryLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported unary expression: " + ctx.getText()));
        }
    }

    private void evaluatePrimaryExpressionAsync(ElasticScriptParser.PrimaryExpressionContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> processResult = new ActionListener<Object>() {
            @Override
            public void onResponse(Object baseValue) {
                if (ctx.accessExpression() != null && ctx.accessExpression().isEmpty() == false) {
                    evaluateAccessExpressionRecursive(baseValue, ctx.accessExpression(), 0, listener);
                } else {
                    listener.onResponse(baseValue);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        if (ctx.simplePrimaryExpression().LPAREN() != null && ctx.simplePrimaryExpression().RPAREN() != null) {
            evaluateExpressionAsync(ctx.simplePrimaryExpression().expression(), processResult);
        } else if (ctx.simplePrimaryExpression().call_procedure_statement() != null) {
            // Delegate function call evaluation to the executor's function call handler.
            executor.visitCallProcedureAsync(ctx.simplePrimaryExpression().call_procedure_statement(), processResult);
        }  else if (ctx.simplePrimaryExpression().function_call() != null) {
            // Delegate function call evaluation to the executor's function call handler.
            executor.visitFunctionCallAsync(ctx.simplePrimaryExpression().function_call(), processResult);
        } else if (ctx.simplePrimaryExpression().INT() != null) {
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().INT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid integer literal: " + ctx.simplePrimaryExpression().INT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().FLOAT() != null) {
            try {
                processResult.onResponse(Double.valueOf(ctx.simplePrimaryExpression().FLOAT().getText()));
            } catch (NumberFormatException e) {
                listener.onFailure(new RuntimeException("Invalid float literal: " + ctx.simplePrimaryExpression().FLOAT().getText()));
            }
        } else if (ctx.simplePrimaryExpression().STRING() != null) {
            String text = ctx.simplePrimaryExpression().STRING().getText();
            String processedString = text.substring(1, text.length() - 1);
            // Unescape based on quote type
            if (text.startsWith("\"")) {
                processedString = processedString.replace("\\\"", "\"");
            } else {
                processedString = processedString.replace("\\'", "'");
            }
            // Check for string interpolation (only in double-quoted strings)
            if (text.startsWith("\"") && processedString.contains("${")) {
                evaluateInterpolatedStringAsync(processedString, processResult);
                return;
            }
            processResult.onResponse(processedString);
        } else if (ctx.simplePrimaryExpression().BOOLEAN() != null) {
            String boolText = ctx.simplePrimaryExpression().BOOLEAN().getText();
            // Handle case-insensitive TRUE/FALSE
            processResult.onResponse(boolText.equalsIgnoreCase("true"));
        } else if (ctx.simplePrimaryExpression().arrayLiteral() != null) {
            if (ctx.simplePrimaryExpression().arrayLiteral().expressionList() != null) {
                evaluateExpressionList(ctx.simplePrimaryExpression().arrayLiteral().expressionList().expression(), processResult);
            } else {
                processResult.onResponse(new ArrayList<>());
            }
        } else if (ctx.simplePrimaryExpression().documentLiteral() != null) {
            ElasticScriptParser.DocumentLiteralContext docCtx = ctx.simplePrimaryExpression().documentLiteral();
            evaluateDocumentLiteralAsync(docCtx, processResult);
            return;
        } else if (ctx.simplePrimaryExpression().mapLiteral() != null) {
            ElasticScriptParser.MapLiteralContext mapCtx = ctx.simplePrimaryExpression().mapLiteral();
            evaluateMapLiteralAsync(mapCtx, processResult);
            return;
        } else if (ctx.simplePrimaryExpression().lambdaExpression() != null) {
            // Create a LambdaExpression object without evaluating the body
            ElasticScriptParser.LambdaExpressionContext lambdaCtx = ctx.simplePrimaryExpression().lambdaExpression();
            List<String> params = new ArrayList<>();
            
            if (lambdaCtx.lambdaParamList() != null) {
                // Multiple parameters: (a, b) => expr
                for (var idNode : lambdaCtx.lambdaParamList().ID()) {
                    params.add(idNode.getText());
                }
            } else if (lambdaCtx.ID() != null) {
                // Single parameter without parens: x => expr
                params.add(lambdaCtx.ID().getText());
            }
            
            ElasticScriptParser.ExpressionContext bodyExpr = lambdaCtx.expression();
            LambdaExpression lambda = new LambdaExpression(params, bodyExpr);
            processResult.onResponse(lambda);
            return;
        } else if (ctx.simplePrimaryExpression().NULL() != null) {
            processResult.onResponse(null);
        } else if (ctx.simplePrimaryExpression().cursorAttribute() != null) {
            // Handle cursor%NOTFOUND and cursor%ROWCOUNT
            ElasticScriptParser.CursorAttributeContext attrCtx = ctx.simplePrimaryExpression().cursorAttribute();
            String cursorName = attrCtx.ID().getText();
            try {
                org.elasticsearch.xpack.escript.primitives.CursorDefinition cursor = context.getCursor(cursorName);
                if (cursor == null) {
                    listener.onFailure(new RuntimeException("Cursor '" + cursorName + "' not found"));
                    return;
                }
                if (attrCtx.NOTFOUND() != null) {
                    // cursor%NOTFOUND - true when last fetch returned no rows
                    processResult.onResponse(cursor.isNotFound());
                } else if (attrCtx.ROWCOUNT() != null) {
                    // cursor%ROWCOUNT - number of rows fetched
                    processResult.onResponse(cursor.getRowCount());
                } else {
                    listener.onFailure(new RuntimeException("Unknown cursor attribute"));
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
            return;
        } else if (ctx.simplePrimaryExpression().ID() != null) {
            String id = ctx.simplePrimaryExpression().ID().getText();
            Object var = context.getVariable(id);
            if (var == null) {
                listener.onFailure(new RuntimeException("Variable not defined: " + id));
            } else {
                // if there are accessExpressions, delegate into recursive lookup
                List<ElasticScriptParser.AccessExpressionContext> accesses = ctx.accessExpression();
                if (accesses != null && accesses.isEmpty() == false) {
                    evaluateAccessExpressionRecursive(var, accesses, 0, listener);
                } else {
                    listener.onResponse(var);
                }
            }

        } else {
            listener.onFailure(new RuntimeException("Unsupported primary expression: " + ctx.getText()));
        }
    }

    /**
     * Evaluates access expressions (bracket or safe navigation) recursively.
     */
    private void evaluateAccessExpressionRecursive(Object current, List<ElasticScriptParser.AccessExpressionContext> accessExprs,
                                                   int index, ActionListener<Object> listener) {
        if (index >= accessExprs.size()) {
            listener.onResponse(current);
            return;
        }

        ElasticScriptParser.AccessExpressionContext accessCtx = accessExprs.get(index);

        // Handle safe navigation (?.)
        if (accessCtx.safeNavExpression() != null) {
            if (current == null) {
                // Safe navigation: if current is null, result is null
                listener.onResponse(null);
                return;
            }
            String fieldName = accessCtx.safeNavExpression().ID().getText();
            if (current instanceof Map) {
                Object newValue = ((Map<?, ?>) current).get(fieldName);
                evaluateAccessExpressionRecursive(newValue, accessExprs, index + 1, listener);
            } else {
                listener.onFailure(new RuntimeException(
                    "Safe navigation requires a document/map, but got: " +
                    current.getClass().getSimpleName()));
            }
            return;
        }

        // Handle bracket expression ([...])
        if (accessCtx.bracketExpression() != null) {
            evaluateExpressionAsync(accessCtx.bracketExpression().expression(), new ActionListener<Object>() {
                @Override
                public void onResponse(Object keyObj) {
                    if ((keyObj instanceof String) == false) {
                        // Check for list indexing using an integer index
                        if (current instanceof List && keyObj instanceof Number) {
                            List<?> list = (List<?>) current;
                            int idx = ((Number) keyObj).intValue();
                            if (idx < 0 || idx >= list.size()) {
                                listener.onFailure(new RuntimeException("List index out of bounds: " + idx));
                                return;
                            }
                            Object newValue = list.get(idx);
                            evaluateAccessExpressionRecursive(newValue, accessExprs, index + 1, listener);
                        } else {
                            listener.onFailure(new RuntimeException(
                                "Document field access requires a string key or numeric index, but got: " + keyObj));
                        }
                        return;
                    }
                    String key = (String) keyObj;
                    if (current instanceof Map) {
                        Object newValue = ((Map<?, ?>) current).get(key);
                        evaluateAccessExpressionRecursive(newValue, accessExprs, index + 1, listener);
                    } else {
                        listener.onFailure(new RuntimeException("Attempted to index into unsupported type: " +
                            (current != null ? current.getClass().getName() : "null")));
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
            return;
        }

        listener.onFailure(new RuntimeException("Unsupported access expression: " + accessCtx.getText()));
    }

    private void evaluateDocumentFieldAccessRecursive(Object current, List<ElasticScriptParser.BracketExpressionContext> bracketExprs,
                                                      int index, ActionListener<Object> listener) {

        if (index >= bracketExprs.size()) {
            listener.onResponse(current);
            return;
        }

        evaluateExpressionAsync(bracketExprs.get(index).expression(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object keyObj) {
                if ((keyObj instanceof String) == false) {
                    // Check for list indexing using an integer index
                    if (current instanceof List && keyObj instanceof Number) {
                        List<?> list = (List<?>) current;
                        int idx = ((Number) keyObj).intValue();
                        if (idx < 0 || idx >= list.size()) {
                            listener.onFailure(new RuntimeException("List index out of bounds: " + idx));
                            return;
                        }
                        Object newValue = list.get(idx);
                        evaluateDocumentFieldAccessRecursive(newValue, bracketExprs, index + 1, listener);
                    } else {
                        listener.onFailure(
                            new RuntimeException("Document field access requires a string key or numeric index, but got: " + keyObj));
                    }
                    return;
                }
                String key = (String) keyObj;
                if (current instanceof Map) {
                    Object newValue = ((Map<?, ?>) current).get(key);
                    evaluateDocumentFieldAccessRecursive(newValue, bracketExprs, index + 1, listener);
                } else {
                    listener.onFailure(new RuntimeException("Attempted to index into unsupported type: " +
                        (current != null ? current.getClass().getName() : "null")));
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void evaluateExpressionList(List<ElasticScriptParser.ExpressionContext> exprs, ActionListener<Object> listener) {
        evaluateExpressionList(exprs, 0, new ArrayList<>(), listener);
    }

    private void evaluateExpressionList(List<ElasticScriptParser.ExpressionContext> exprs,
                                        int index, List<Object> results, ActionListener<Object> listener) {
        if (index >= exprs.size()) {
            listener.onResponse(results);
            return;
        }
        evaluateExpressionAsync(exprs.get(index), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                results.add(result);
                evaluateExpressionList(exprs, index + 1, results, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public void evaluateDocumentLiteralAsync(ElasticScriptParser.DocumentLiteralContext ctx, ActionListener<Object> listener) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<ElasticScriptParser.DocumentFieldContext> fields = ctx.documentField();

        if (fields.isEmpty()) {
            listener.onResponse(result);  // return empty document
            return;
        }

        AtomicInteger remaining = new AtomicInteger(fields.size());
        AtomicBoolean failed = new AtomicBoolean(false);

        for (ElasticScriptParser.DocumentFieldContext fieldCtx : fields) {
            String fieldName = stripQuotes(fieldCtx.STRING().getText());
            evaluateExpressionAsync(fieldCtx.expression(), ActionListener.wrap(
                value -> {
                    result.put(fieldName, value);
                    if (remaining.decrementAndGet() == 0 && failed.get() == false ) {
                        listener.onResponse(result);
                    }
                },
                e -> {
                    if (failed.compareAndSet(false, true)) {
                        listener.onFailure(e);
                    }
                }
            ));
        }
    }

    private String stripQuotes(String quotedString) {
        return quotedString.substring(1, quotedString.length() - 1); // remove surrounding quotes
    }

    /**
     * Evaluates a MAP literal: MAP { 'key' => value, 'key2' => value2 }
     */
    public void evaluateMapLiteralAsync(ElasticScriptParser.MapLiteralContext ctx, ActionListener<Object> listener) {
        Map<Object, Object> result = new LinkedHashMap<>();
        List<ElasticScriptParser.MapEntryContext> entries = ctx.mapEntry();

        if (entries.isEmpty()) {
            listener.onResponse(result);  // return empty map
            return;
        }

        // We need to evaluate entries sequentially to preserve order
        evaluateMapEntriesAsync(entries, 0, result, listener);
    }

    private void evaluateMapEntriesAsync(List<ElasticScriptParser.MapEntryContext> entries, 
                                         int index,
                                         Map<Object, Object> result,
                                         ActionListener<Object> listener) {
        if (index >= entries.size()) {
            listener.onResponse(result);
            return;
        }

        ElasticScriptParser.MapEntryContext entryCtx = entries.get(index);
        // Each mapEntry has two expressions: key => value
        ElasticScriptParser.ExpressionContext keyExpr = entryCtx.expression(0);
        ElasticScriptParser.ExpressionContext valueExpr = entryCtx.expression(1);

        // Evaluate the key first
        evaluateExpressionAsync(keyExpr, ActionListener.wrap(
            keyValue -> {
                // Then evaluate the value
                evaluateExpressionAsync(valueExpr, ActionListener.wrap(
                    valueResult -> {
                        result.put(keyValue, valueResult);
                        // Process next entry
                        evaluateMapEntriesAsync(entries, index + 1, result, listener);
                    },
                    listener::onFailure
                ));
            },
            listener::onFailure
        ));
    }

    /**
     * Evaluates a string with ${...} interpolation patterns.
     * Parses out all ${expression} segments and evaluates them asynchronously.
     */
    private void evaluateInterpolatedStringAsync(String template, ActionListener<Object> listener) {
        // Find all ${...} patterns
        java.util.List<InterpolationSegment> segments = new java.util.ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        int i = 0;
        
        while (i < template.length()) {
            if (i < template.length() - 1 && template.charAt(i) == '$' && template.charAt(i + 1) == '{') {
                // Found start of interpolation
                if (currentText.length() > 0) {
                    segments.add(new InterpolationSegment(currentText.toString(), false));
                    currentText = new StringBuilder();
                }
                
                // Find matching closing brace
                int braceCount = 1;
                int start = i + 2;
                int end = start;
                while (end < template.length() && braceCount > 0) {
                    if (template.charAt(end) == '{') braceCount++;
                    else if (template.charAt(end) == '}') braceCount--;
                    if (braceCount > 0) end++;
                }
                
                if (braceCount != 0) {
                    listener.onFailure(new RuntimeException("Unclosed interpolation expression in string: " + template));
                    return;
                }
                
                String expr = template.substring(start, end);
                segments.add(new InterpolationSegment(expr, true));
                i = end + 1;
            } else {
                currentText.append(template.charAt(i));
                i++;
            }
        }
        
        if (currentText.length() > 0) {
            segments.add(new InterpolationSegment(currentText.toString(), false));
        }
        
        // If no expressions to evaluate, return as-is
        if (segments.stream().noneMatch(s -> s.isExpression)) {
            listener.onResponse(template);
            return;
        }
        
        // Evaluate all expression segments
        evaluateInterpolationSegmentsAsync(segments, 0, new String[segments.size()], listener);
    }
    
    private void evaluateInterpolationSegmentsAsync(java.util.List<InterpolationSegment> segments, 
                                                    int index, String[] results, 
                                                    ActionListener<Object> listener) {
        if (index >= segments.size()) {
            // All segments processed, build final string
            StringBuilder sb = new StringBuilder();
            for (String s : results) {
                sb.append(s);
            }
            listener.onResponse(sb.toString());
            return;
        }
        
        InterpolationSegment segment = segments.get(index);
        if (!segment.isExpression) {
            // Plain text segment
            results[index] = segment.content;
            evaluateInterpolationSegmentsAsync(segments, index + 1, results, listener);
        } else {
            // Expression segment - parse and evaluate
            try {
                org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(segment.content);
                ElasticScriptLexer lexer = new ElasticScriptLexer(input);
                org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);
                ElasticScriptParser parser = new ElasticScriptParser(tokens);
                ElasticScriptParser.ExpressionContext exprCtx = parser.expression();
                
                evaluateExpressionAsync(exprCtx, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object value) {
                        results[index] = value != null ? value.toString() : "";
                        evaluateInterpolationSegmentsAsync(segments, index + 1, results, listener);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(new RuntimeException("Failed to evaluate interpolation expression '" + segment.content + "': " + e.getMessage(), e));
                    }
                });
            } catch (Exception e) {
                listener.onFailure(new RuntimeException("Failed to parse interpolation expression '" + segment.content + "': " + e.getMessage(), e));
            }
        }
    }
    
    /**
     * Helper class to represent a segment in an interpolated string.
     */
    private static class InterpolationSegment {
        final String content;
        final boolean isExpression;
        
        InterpolationSegment(String content, boolean isExpression) {
            this.content = content;
            this.isExpression = isExpression;
        }
    }

    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }

    /**
     * Evaluates a list of expressions and checks if a value is in the resulting list.
     */
    private void evaluateExpressionListForIn(List<ElasticScriptParser.ExpressionContext> exprs,
                                             Object value, boolean negate, ActionListener<Object> listener) {
        evaluateExpressionListForIn(exprs, 0, value, listener, negate);
    }

    private void evaluateExpressionListForIn(List<ElasticScriptParser.ExpressionContext> exprs,
                                             int index, Object value, ActionListener<Object> listener, boolean negate) {
        if (index >= exprs.size()) {
            // Value not found in any expression
            listener.onResponse(negate);
            return;
        }
        evaluateExpressionAsync(exprs.get(index), new ActionListener<Object>() {
            @Override
            public void onResponse(Object exprValue) {
                if (valuesEqual(value, exprValue)) {
                    // Found a match
                    listener.onResponse(!negate);
                } else {
                    // Continue checking
                    evaluateExpressionListForIn(exprs, index + 1, value, listener, negate);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Checks if a value is in a collection (array or list).
     */
    private boolean checkValueInCollection(Object value, Object collection) {
        if (collection == null) {
            return false;
        }
        if (collection instanceof List<?> list) {
            for (Object item : list) {
                if (valuesEqual(value, item)) {
                    return true;
                }
            }
            return false;
        }
        throw new RuntimeException("IN operator requires an array or list, but got: " + collection.getClass().getSimpleName());
    }

    /**
     * Compares two values for equality, handling type coercion for numbers.
     */
    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        // Handle numeric comparison with type coercion
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    /**
     * Invokes a lambda expression with the given arguments.
     * Creates a temporary scope with the lambda parameters bound to the arguments,
     * then evaluates the lambda body expression in that scope.
     *
     * @param lambda The lambda expression to invoke
     * @param args The arguments to pass to the lambda (one per parameter)
     * @param listener The listener to receive the result
     */
    public void invokeLambdaAsync(LambdaExpression lambda, List<Object> args, ActionListener<Object> listener) {
        List<String> params = lambda.getParameters();
        
        if (args.size() != params.size()) {
            listener.onFailure(new RuntimeException(
                "Lambda expects " + params.size() + " argument(s) but received " + args.size()));
            return;
        }
        
        // Save the current variable values for the parameter names (if any exist)
        Map<String, Object> savedValues = new java.util.HashMap<>();
        for (String param : params) {
            if (context.hasVariable(param)) {
                savedValues.put(param, context.getVariable(param));
            }
        }
        
        try {
            // Bind the arguments to the parameter names in the context
            for (int i = 0; i < params.size(); i++) {
                String param = params.get(i);
                Object value = args.get(i);
                
                // Declare and set the parameter variable
                if (!context.hasVariable(param)) {
                    context.declareVariable(param, "ANY");
                }
                context.setVariable(param, value);
            }
            
            // Evaluate the lambda body expression
            evaluateExpressionAsync(lambda.getBodyExpression(), new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    // Restore the original variable values
                    restoreVariables(params, savedValues);
                    listener.onResponse(result);
                }
                
                @Override
                public void onFailure(Exception e) {
                    // Restore the original variable values even on failure
                    restoreVariables(params, savedValues);
                    listener.onFailure(e);
                }
            });
        } catch (Exception e) {
            // Restore the original variable values on exception
            restoreVariables(params, savedValues);
            listener.onFailure(e);
        }
    }
    
    private void restoreVariables(List<String> params, Map<String, Object> savedValues) {
        for (String param : params) {
            if (savedValues.containsKey(param)) {
                context.setVariable(param, savedValues.get(param));
            }
            // Note: We don't remove variables that were newly declared - they stay in scope
        }
    }
}
