/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.intent.IntentDefinition;
import org.elasticsearch.xpack.escript.intent.IntentRegistry;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for INTENT invocation statements.
 * 
 * Executes an intent by:
 * 1. Looking up the intent in the registry
 * 2. Evaluating and binding arguments
 * 3. Evaluating REQUIRES conditions (pre-conditions)
 * 4. Executing ACTIONS statements
 * 5. On failure, executing ON_FAILURE statements
 * 
 * Syntax:
 * INTENT name(arg1, arg2);
 * INTENT name WITH param1 = value1, param2 = value2;
 */
public class IntentStatementHandler {

    private final ProcedureExecutor executor;

    /**
     * Constructs an IntentStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance.
     */
    public IntentStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the INTENT invocation statement asynchronously.
     *
     * @param ctx      The Intent_statementContext from the parser.
     * @param listener The ActionListener to handle async callbacks.
     */
    public void handleAsync(ElasticScriptParser.Intent_statementContext ctx, ActionListener<Object> listener) {
        try {
            String intentName;
            
            // Check if this is positional args or named args
            if (ctx instanceof ElasticScriptParser.IntentCallWithArgsContext argsCtx) {
                intentName = argsCtx.ID().getText();
                handleIntentWithArgs(intentName, argsCtx, listener);
            } else if (ctx instanceof ElasticScriptParser.IntentCallWithNamedArgsContext namedCtx) {
                intentName = namedCtx.ID().getText();
                handleIntentWithNamedArgs(intentName, namedCtx, listener);
            } else {
                listener.onFailure(new RuntimeException("Unknown intent statement type"));
            }
            
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    private void handleIntentWithArgs(
            String intentName,
            ElasticScriptParser.IntentCallWithArgsContext argsCtx,
            ActionListener<Object> listener) {
        
        // Look up the intent
        IntentRegistry registry = IntentRegistry.getInstance();
        IntentDefinition intent = registry.get(intentName);
        
        if (intent == null) {
            listener.onFailure(new RuntimeException("Intent '" + intentName + "' is not defined."));
            return;
        }

        Map<String, Object> argValues = new HashMap<>();
        List<Parameter> parameters = intent.getParameters();
        
        if (argsCtx.argument_list() != null) {
            List<ElasticScriptParser.ExpressionContext> argExprs = 
                argsCtx.argument_list().expression();
            
            if (argExprs.size() != parameters.size()) {
                listener.onFailure(new RuntimeException(
                    "Intent '" + intentName + "' expects " + parameters.size() + 
                    " arguments, but " + argExprs.size() + " were provided."));
                return;
            }
            
            // Evaluate positional arguments
            evaluateArgumentsAsync(argExprs, parameters, argValues, 0, 
                ActionListener.wrap(
                    v -> executeIntent(intent, argValues, listener),
                    listener::onFailure
                )
            );
        } else if (parameters.isEmpty()) {
            // No arguments expected or provided
            executeIntent(intent, argValues, listener);
        } else {
            listener.onFailure(new RuntimeException(
                "Intent '" + intentName + "' expects " + parameters.size() + " arguments."));
        }
    }

    private void handleIntentWithNamedArgs(
            String intentName,
            ElasticScriptParser.IntentCallWithNamedArgsContext namedCtx,
            ActionListener<Object> listener) {
        
        // Look up the intent
        IntentRegistry registry = IntentRegistry.getInstance();
        IntentDefinition intent = registry.get(intentName);
        
        if (intent == null) {
            listener.onFailure(new RuntimeException("Intent '" + intentName + "' is not defined."));
            return;
        }

        Map<String, Object> argValues = new HashMap<>();
        List<Parameter> parameters = intent.getParameters();
        
        if (namedCtx.intent_named_args() != null) {
            List<ElasticScriptParser.Intent_named_argContext> namedArgs = 
                namedCtx.intent_named_args().intent_named_arg();
            
            evaluateNamedArgumentsAsync(namedArgs, parameters, argValues, 0, intentName,
                ActionListener.wrap(
                    v -> executeIntent(intent, argValues, listener),
                    listener::onFailure
                )
            );
        } else if (parameters.isEmpty()) {
            // No arguments expected or provided
            executeIntent(intent, argValues, listener);
        } else {
            listener.onFailure(new RuntimeException(
                "Intent '" + intentName + "' expects " + parameters.size() + " arguments."));
        }
    }

    /**
     * Evaluates positional arguments recursively.
     */
    private void evaluateArgumentsAsync(
            List<ElasticScriptParser.ExpressionContext> argExprs,
            List<Parameter> parameters,
            Map<String, Object> argValues,
            int index,
            ActionListener<Void> listener) {
        
        if (index >= argExprs.size()) {
            listener.onResponse(null);
            return;
        }
        
        ExpressionEvaluator evaluator = new ExpressionEvaluator(executor);
        evaluator.evaluateExpressionAsync(argExprs.get(index), ActionListener.wrap(
            value -> {
                argValues.put(parameters.get(index).getName(), value);
                evaluateArgumentsAsync(argExprs, parameters, argValues, index + 1, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Evaluates named arguments recursively.
     */
    private void evaluateNamedArgumentsAsync(
            List<ElasticScriptParser.Intent_named_argContext> namedArgs,
            List<Parameter> parameters,
            Map<String, Object> argValues,
            int index,
            String intentName,
            ActionListener<Void> listener) {
        
        if (index >= namedArgs.size()) {
            // Verify all required parameters are provided
            for (Parameter param : parameters) {
                if (!argValues.containsKey(param.getName())) {
                    listener.onFailure(new RuntimeException(
                        "Missing required parameter '" + param.getName() + 
                        "' for intent '" + intentName + "'."));
                    return;
                }
            }
            listener.onResponse(null);
            return;
        }
        
        ElasticScriptParser.Intent_named_argContext namedArg = namedArgs.get(index);
        String paramName = namedArg.ID().getText();
        
        // Verify parameter name is valid
        boolean validParam = parameters.stream().anyMatch(p -> p.getName().equals(paramName));
        if (!validParam) {
            listener.onFailure(new RuntimeException(
                "Unknown parameter '" + paramName + "' for intent '" + intentName + "'."));
            return;
        }
        
        ExpressionEvaluator evaluator = new ExpressionEvaluator(executor);
        evaluator.evaluateExpressionAsync(namedArg.expression(), ActionListener.wrap(
            value -> {
                argValues.put(paramName, value);
                evaluateNamedArgumentsAsync(namedArgs, parameters, argValues, index + 1, intentName, listener);
            },
            listener::onFailure
        ));
    }

    /**
     * Executes the intent with the evaluated arguments.
     */
    private void executeIntent(IntentDefinition intent, Map<String, Object> argValues, ActionListener<Object> listener) {
        // Create a child context for the intent execution
        ExecutionContext intentContext = new ExecutionContext(executor.getContext());
        
        // Declare and assign parameters
        for (Parameter param : intent.getParameters()) {
            intentContext.declareVariable(param.getName(), param.getType());
            Object value = argValues.get(param.getName());
            if (value != null) {
                intentContext.setVariable(param.getName(), value);
            }
        }
        
        // Create an executor for the intent context
        ProcedureExecutor intentExecutor = new ProcedureExecutor(
            intentContext,
            executor.getThreadPool(),
            executor.getClient(),
            executor.getTokenStream()
        );
        
        // Evaluate REQUIRES conditions
        if (intent.hasRequires()) {
            evaluateRequiresAsync(intent, intentExecutor, 0, ActionListener.wrap(
                allPassed -> {
                    if (allPassed) {
                        executeActionsWithFailureHandling(intent, intentExecutor, listener);
                    } else {
                        listener.onFailure(new RuntimeException(
                            "Intent '" + intent.getName() + "' REQUIRES conditions not met."));
                    }
                },
                listener::onFailure
            ));
        } else {
            executeActionsWithFailureHandling(intent, intentExecutor, listener);
        }
    }

    /**
     * Evaluates REQUIRES conditions recursively.
     */
    private void evaluateRequiresAsync(
            IntentDefinition intent,
            ProcedureExecutor intentExecutor,
            int index,
            ActionListener<Boolean> listener) {
        
        List<ElasticScriptParser.Requires_conditionContext> conditions = intent.getRequiresConditions();
        
        if (index >= conditions.size()) {
            listener.onResponse(true); // All conditions passed
            return;
        }
        
        ElasticScriptParser.Requires_conditionContext condCtx = conditions.get(index);
        ExpressionEvaluator evaluator = new ExpressionEvaluator(intentExecutor);
        
        evaluator.evaluateExpressionAsync(condCtx.expression(), ActionListener.wrap(
            result -> {
                boolean passed = false;
                if (result instanceof Boolean) {
                    passed = (Boolean) result;
                } else if (result != null) {
                    // Truthy check: non-null, non-zero, non-empty
                    if (result instanceof Number) {
                        passed = ((Number) result).doubleValue() != 0;
                    } else if (result instanceof String) {
                        passed = !((String) result).isEmpty();
                    } else if (result instanceof List) {
                        passed = !((List<?>) result).isEmpty();
                    } else {
                        passed = true;
                    }
                }
                
                if (!passed) {
                    listener.onResponse(false);
                } else {
                    evaluateRequiresAsync(intent, intentExecutor, index + 1, listener);
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Executes ACTIONS and handles ON_FAILURE if an error occurs.
     */
    private void executeActionsWithFailureHandling(
            IntentDefinition intent,
            ProcedureExecutor intentExecutor,
            ActionListener<Object> listener) {
        
        intentExecutor.executeStatementsAsync(intent.getActions(), 0, new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                Object returnValue = result instanceof ReturnValue ? ((ReturnValue) result).getValue() : result;
                listener.onResponse(returnValue);
            }
            
            @Override
            public void onFailure(Exception e) {
                // Check if this is a ReturnValue (not really a failure)
                if (e instanceof ReturnValue) {
                    listener.onResponse(((ReturnValue) e).getValue());
                    return;
                }
                
                // Execute ON_FAILURE statements if available
                if (intent.hasOnFailure()) {
                    // Store the error message in context for access in ON_FAILURE
                    intentExecutor.getContext().declareVariable("ERROR_MESSAGE", "STRING");
                    intentExecutor.getContext().setVariable("ERROR_MESSAGE", e.getMessage());
                    
                    intentExecutor.executeStatementsAsync(intent.getOnFailureActions(), 0, new ActionListener<>() {
                        @Override
                        public void onResponse(Object onFailResult) {
                            // ON_FAILURE completed, but original error still occurred
                            listener.onFailure(e);
                        }
                        
                        @Override
                        public void onFailure(Exception onFailError) {
                            // Both ACTIONS and ON_FAILURE failed
                            listener.onFailure(new RuntimeException(
                                "Intent '" + intent.getName() + "' failed: " + e.getMessage() +
                                ". ON_FAILURE also failed: " + onFailError.getMessage(), e));
                        }
                    });
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }
}
