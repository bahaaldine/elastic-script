/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.executors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;
import org.elasticsearch.xpack.escript.exceptions.BreakException;
import org.elasticsearch.xpack.escript.exceptions.ContinueException;
import org.elasticsearch.xpack.escript.logging.EScriptLogger;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.escript.handlers.AsyncProcedureStatementHandler;
import org.elasticsearch.xpack.escript.handlers.CallProcedureStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ConstStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ContinuationExecutor;
import org.elasticsearch.xpack.escript.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.escript.handlers.VarStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ExecuteStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ExecutionControlStatementHandler;
import org.elasticsearch.xpack.escript.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.escript.handlers.IfStatementHandler;
import org.elasticsearch.xpack.escript.handlers.SwitchStatementHandler;
import org.elasticsearch.xpack.escript.handlers.LoopStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ParallelStatementHandler;
import org.elasticsearch.xpack.escript.handlers.PrintStatementHandler;
import org.elasticsearch.xpack.escript.handlers.CursorStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ThrowStatementHandler;
import org.elasticsearch.xpack.escript.handlers.TryCatchStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ExecuteImmediateStatementHandler;
import org.elasticsearch.xpack.escript.handlers.DefineIntentStatementHandler;
import org.elasticsearch.xpack.escript.handlers.IntentStatementHandler;
import org.elasticsearch.xpack.escript.handlers.EsqlIntoStatementHandler;
import org.elasticsearch.xpack.escript.handlers.EsqlProcessStatementHandler;
import org.elasticsearch.xpack.escript.handlers.FirstClassCommandsHandler;
import org.elasticsearch.xpack.escript.execution.ExecutionRegistry;
import org.elasticsearch.xpack.escript.execution.ExecutionIndexTemplateRegistry;
import org.elasticsearch.xpack.escript.parser.ElasticScriptBaseVisitor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;
import org.elasticsearch.xpack.escript.procedure.StoredProcedureDefinition;
import org.elasticsearch.xpack.escript.functions.StoredFunctionDefinition;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import org.elasticsearch.xpack.core.esql.action.ColumnInfo;
import org.elasticsearch.xpack.esql.action.EsqlQueryAction;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.action.EsqlQueryResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ProcedureExecutor class is responsible for executing parsed procedural SQL statements.
 * It extends the ElasticScriptBaseVisitor to traverse the parse tree and execute corresponding actions.
 *
 * Note that expression evaluation has been refactored into a dedicated ExpressionEvaluator.
 */
public class ProcedureExecutor extends ElasticScriptBaseVisitor<Object> {

    private static final Logger LOGGER = LogManager.getLogger(ProcedureExecutor.class);

    private ExecutionContext context;
    private final ThreadPool threadPool;

    // Statement handlers
    private final AssignmentStatementHandler assignmentHandler;
    private final DeclareStatementHandler declareHandler;
    private final VarStatementHandler varHandler;
    private final ConstStatementHandler constHandler;
    private final IfStatementHandler ifHandler;
    private final SwitchStatementHandler switchHandler;
    private final LoopStatementHandler loopHandler;
    private final FunctionDefinitionHandler functionDefHandler;
    private final TryCatchStatementHandler tryCatchHandler;
    private final ThrowStatementHandler throwHandler;
    private final ExecuteStatementHandler executeHandler;
    private final ExecuteImmediateStatementHandler executeImmediateHandler;
    private final PrintStatementHandler printStatementHandler;
    private final CursorStatementHandler cursorHandler;
    private final CallProcedureStatementHandler callProcedureStatementHandler;
    private final DefineIntentStatementHandler defineIntentHandler;
    private final IntentStatementHandler intentHandler;
    private final EsqlIntoStatementHandler esqlIntoHandler;
    private final EsqlProcessStatementHandler esqlProcessHandler;
    private final FirstClassCommandsHandler firstClassCommandsHandler;
    private final Client client;
    
    // Async execution handlers
    private final ExecutionRegistry executionRegistry;
    private final ContinuationExecutor continuationExecutor;
    private final AsyncProcedureStatementHandler asyncProcedureHandler;
    private final ExecutionControlStatementHandler executionControlHandler;
    private final ParallelStatementHandler parallelHandler;

    private final CommonTokenStream tokenStream;

    // Expression evaluation is delegated to ExpressionEvaluator.
    private final ExpressionEvaluator expressionEvaluator;

    /**
     * Constructs a ProcedureExecutor with the given execution context and thread pool.
     *
     * @param context    The execution context containing variables, functions, etc.
     * @param threadPool The thread pool for executing asynchronous tasks.
     * @param client     The client for executing queries.
     * @param tokenStream The token stream from the parser.
     */
    @SuppressWarnings("this-escape")
    public ProcedureExecutor(ExecutionContext context, ThreadPool threadPool, Client client, CommonTokenStream tokenStream) {
        this.context = context;
        this.client = client;
        this.threadPool = threadPool;
        this.executeHandler = new ExecuteStatementHandler(this, client);
        this.assignmentHandler = new AssignmentStatementHandler(this);
        this.declareHandler = new DeclareStatementHandler(this);
        this.varHandler = new VarStatementHandler(this);
        this.constHandler = new ConstStatementHandler(this);
        this.ifHandler = new IfStatementHandler(this);
        this.switchHandler = new SwitchStatementHandler(this);
        this.loopHandler = new LoopStatementHandler(this);
        this.functionDefHandler = new FunctionDefinitionHandler(this);
        this.tryCatchHandler = new TryCatchStatementHandler(this);
        this.throwHandler = new ThrowStatementHandler(this);
        this.executeImmediateHandler = new ExecuteImmediateStatementHandler(this);
        this.printStatementHandler = new PrintStatementHandler(this);
        this.cursorHandler = new CursorStatementHandler(this);
        this.callProcedureStatementHandler = new CallProcedureStatementHandler(this);
        this.defineIntentHandler = new DefineIntentStatementHandler(this);
        this.intentHandler = new IntentStatementHandler(this);
        this.esqlIntoHandler = new EsqlIntoStatementHandler(this, client);
        this.esqlProcessHandler = new EsqlProcessStatementHandler(this, client);
        this.firstClassCommandsHandler = new FirstClassCommandsHandler(this, client);
        this.tokenStream = tokenStream;
        // Initialize ExpressionEvaluator with this executor instance.
        this.expressionEvaluator = new ExpressionEvaluator(this);
        
        // Set up lambda invocation support in the context
        context.setLambdaInvoker((lambda, args, listener) -> 
            expressionEvaluator.invokeLambdaAsync(lambda, args, listener));

        // Initialize async execution handlers
        // Note: For testing without ClusterService, we use a null for the template registry
        this.executionRegistry = new ExecutionRegistry(client, "local-node");
        this.continuationExecutor = new ContinuationExecutor(this);
        this.asyncProcedureHandler = new AsyncProcedureStatementHandler(this, executionRegistry, continuationExecutor);
        this.executionControlHandler = new ExecutionControlStatementHandler(executionRegistry);
        this.parallelHandler = new ParallelStatementHandler(this, executionRegistry, continuationExecutor);
    }

    /**
     * Retrieves the current thread pool.
     *
     * @return The current ThreadPool.
     */
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    /**
     * Retrieves the current execution context.
     *
     * @return The current ExecutionContext.
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Retrieves the expression evaluator.
     *
     * @return The ExpressionEvaluator instance.
     */
    public ExpressionEvaluator getExpressionEvaluator() {
        return expressionEvaluator;
    }

    /**
     * Retrieves the raw text from the context.
     *
     * @param ctx The ParserRuleContext.
     * @return The raw text.
     */
    public String getRawText(ParserRuleContext ctx) {
        return tokenStream.getText(ctx);
    }

    /**
     * Updates the execution context to a new context.
     *
     * @param newContext The new ExecutionContext to set.
     */
    public void setContext(ExecutionContext newContext) {
        this.context = newContext;
    }

    /**
     * Asynchronously visits the entire procedure and executes each statement.
     *
     * @param ctx      The ProcedureContext representing the entire procedure.
     * @param listener The ActionListener to notify upon completion or failure.
     */
    public void visitProcedureAsync(ElasticScriptParser.ProcedureContext ctx, ActionListener<Object> listener) {
        // Start asynchronous execution of the procedure.
        executeProcedureAsync(ctx, listener);
    }

    /**
     * Initiates asynchronous execution of the procedure.
     * Accepts optional arguments to bind to procedure parameters.
     */
    public void executeProcedureAsync(ElasticScriptParser.ProcedureContext ctx, ActionListener<Object> listener) {
        executeStatementsAsync(ctx.statement(), 0, listener);
    }

    /**
     * Executes a list of statements asynchronously.
     *
     * @param statements The list of statements.
     * @param index      The current statement index.
     * @param listener   The ActionListener to notify upon completion.
     */
    public void executeStatementsAsync(List<ElasticScriptParser.StatementContext> statements,
                                       int index, ActionListener<Object> listener) {
        if (index >= statements.size()) {
            listener.onResponse(null); // Execution completed.
            return;
        }

        ElasticScriptParser.StatementContext statement = statements.get(index);

        ActionListener<Object> statementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    listener.onResponse(o);
                } else {
                    executeStatementsAsync(statements, index + 1, listener);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> statementLogger =
            ActionListenerUtils.withLogging(statementListener, this.getClass().getName(),
                "Execute-Statement-Async");

        visitStatementAsync(statement, statementLogger);
    }

    /**
     * Visits a statement asynchronously and delegates handling to the appropriate handler.
     *
     * @param ctx      The StatementContext.
     * @param listener The ActionListener for asynchronous callbacks.
     */
    public void visitStatementAsync(ElasticScriptParser.StatementContext ctx, ActionListener<Object> listener) {
        String executionId = context.getExecutionId();
        
        // Log statement execution at TRACE level (very verbose)
        EScriptLogger.statementExec(executionId, "STATEMENT", ctx.getText());

        if (ctx.declare_statement() != null) {
            declareHandler.handleAsync(ctx.declare_statement(), listener);
        } else if (ctx.var_statement() != null) {
            varHandler.handleAsync(ctx.var_statement(), listener);
        } else if (ctx.const_statement() != null) {
            constHandler.handleAsync(ctx.const_statement(), listener);
        } else if (ctx.assignment_statement() != null) {
            assignmentHandler.handleAsync(ctx.assignment_statement(), listener);
        } else if (ctx.if_statement() != null) {
            ifHandler.handleAsync(ctx.if_statement(), listener);
        } else if (ctx.switch_statement() != null) {
            switchHandler.handleAsync(ctx.switch_statement(), listener);
        } else if (ctx.loop_statement() != null) {
            loopHandler.handleAsync(ctx.loop_statement(), listener);
        } else if (ctx.function_definition() != null) {
            functionDefHandler.handleAsync(ctx.function_definition(), listener);
        } else if (ctx.try_catch_statement() != null) {
            tryCatchHandler.handleAsync(ctx.try_catch_statement(), listener);
        } else if (ctx.throw_statement() != null) {
            throwHandler.handleAsync(ctx.throw_statement(), listener);
        } else if (ctx.execute_statement() != null) {
            executeHandler.handleAsync(ctx.execute_statement(), listener);
        } else if (ctx.execute_immediate_statement() != null) {
            executeImmediateHandler.handleAsync(ctx.execute_immediate_statement(), listener);
        } else if (ctx.print_statement() != null) {
            printStatementHandler.execute(ctx.print_statement(), listener);
        }  else if (ctx.call_procedure_statement() != null) {
            callProcedureStatementHandler.handleAsync(ctx.call_procedure_statement(), listener);
        } else if (ctx.intent_statement() != null) {
            intentHandler.handleAsync(ctx.intent_statement(), listener);
        } else if (ctx.return_statement() != null) {
            visitReturn_statementAsync(ctx.return_statement(), listener);
        } else if (ctx.break_statement() != null) {
            // Handle break statement.
            listener.onFailure(new BreakException("Break encountered"));
        } else if (ctx.continue_statement() != null) {
            // Handle continue statement.
            listener.onFailure(new ContinueException("Continue encountered"));
        } else if (ctx.expression_statement() != null) {
            // Evaluate the expression asynchronously and ignore the result.
            ActionListener<Object> exprListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object value) {
                    listener.onResponse(value);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> exprLogger = ActionListenerUtils.withLogging(exprListener,
                this.getClass().getName(),
                "Expression-Eval: " + ctx.expression_statement().expression());

            evaluateExpressionAsync(ctx.expression_statement().expression(), exprLogger);
        } else if (ctx.function_call_statement() != null) {
            // Handle function call statement.
            ActionListener<Object> funcCallListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    listener.onResponse(null); // Function call completed.
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> funcCallLogger = ActionListenerUtils.withLogging(funcCallListener, this.getClass().getName(),
                "Function-Call: " + ctx.function_call_statement().function_call());

            visitFunctionCallAsync(ctx.function_call_statement().function_call(), funcCallLogger);
        } else if (ctx.async_procedure_statement() != null) {
            // Handle async procedure with pipe continuations
            asyncProcedureHandler.handleAsync(ctx.async_procedure_statement(), listener);
        } else if (ctx.execution_control_statement() != null) {
            // Handle execution control (STATUS, CANCEL, RETRY, WAIT)
            executionControlHandler.handleAsync(ctx.execution_control_statement(), listener);
        } else if (ctx.parallel_statement() != null) {
            // Handle parallel execution
            parallelHandler.handleAsync(ctx.parallel_statement(), listener);
        } else if (ctx.esql_into_statement() != null) {
            // Handle ES|QL INTO statement
            esqlIntoHandler.handleAsync(ctx.esql_into_statement(), listener);
        } else if (ctx.esql_process_statement() != null) {
            // Handle ES|QL PROCESS WITH statement
            esqlProcessHandler.handleAsync(ctx.esql_process_statement(), listener);
        } else if (ctx.open_cursor_statement() != null) {
            // Handle OPEN cursor;
            cursorHandler.handleOpenAsync(ctx.open_cursor_statement(), listener);
        } else if (ctx.close_cursor_statement() != null) {
            // Handle CLOSE cursor;
            cursorHandler.handleCloseAsync(ctx.close_cursor_statement(), listener);
        } else if (ctx.fetch_cursor_statement() != null) {
            // Handle FETCH cursor INTO variable;
            cursorHandler.handleFetchAsync(ctx.fetch_cursor_statement(), listener);
        } else if (ctx.index_command() != null) {
            // Handle INDEX command: INDEX document INTO 'index-name';
            firstClassCommandsHandler.handleIndexCommand(ctx.index_command(), listener);
        } else if (ctx.delete_command() != null) {
            // Handle DELETE command: DELETE FROM 'index-name' WHERE condition;
            firstClassCommandsHandler.handleDeleteCommand(ctx.delete_command(), listener);
        } else if (ctx.search_command() != null) {
            // Handle SEARCH command: SEARCH 'index-name' QUERY {...};
            firstClassCommandsHandler.handleSearchCommand(ctx.search_command(), listener);
        } else if (ctx.refresh_command() != null) {
            // Handle REFRESH command: REFRESH 'index-name';
            firstClassCommandsHandler.handleRefreshCommand(ctx.refresh_command(), listener);
        } else if (ctx.create_index_command() != null) {
            // Handle CREATE INDEX command: CREATE INDEX 'name' WITH {...};
            firstClassCommandsHandler.handleCreateIndexCommand(ctx.create_index_command(), listener);
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Handles return statements asynchronously.
     *
     * @param ctx      The Return_statementContext.
     * @param listener The ActionListener to notify with the return value.
     */
    private void visitReturn_statementAsync(ElasticScriptParser.Return_statementContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> returnListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                listener.onResponse(new ReturnValue(value)); // Signal return value.
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> returnLogger = ActionListenerUtils.withLogging(returnListener,
            this.getClass().getName(),
            "Visit-Return-Statement: " + ctx.expression().getText());

        evaluateExpressionAsync(ctx.expression(), returnLogger);
    }

    /**
     * Delegates expression evaluation to the ExpressionEvaluator.
     *
     * @param ctx      The ExpressionContext to evaluate.
     * @param listener The ActionListener to receive the evaluated result.
     */
    public void evaluateExpressionAsync(ElasticScriptParser.ExpressionContext ctx, ActionListener<Object> listener) {
        expressionEvaluator.evaluateExpressionAsync(ctx, listener);
    }

    /**
     * Evaluates a condition asynchronously.
     *
     * @param ctx      The ConditionContext.
     * @param listener The ActionListener to receive the boolean result.
     */
    public void evaluateConditionAsync(ElasticScriptParser.ConditionContext ctx, ActionListener<Object> listener) {
        if (ctx.expression() != null) {
            ActionListener<Object> condListener = new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    if (result instanceof Boolean) {
                        listener.onResponse(result);
                    } else {
                        listener.onFailure(new RuntimeException("Condition does not evaluate to a boolean: " + ctx.getText()));
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            };

            ActionListener<Object> condLogger = ActionListenerUtils.withLogging(condListener, this.getClass().getName(),
                "Evaluate-Condition: " + ctx.expression());

            evaluateExpressionAsync(ctx.expression(), condLogger);
        } else {
            listener.onFailure(new RuntimeException("Unsupported condition: " + ctx.getText()));
        }
    }

    /**
     * Visits a function call asynchronously.
     *
     * @param ctx      The Function_callContext representing the function call.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void visitFunctionCallAsync(ElasticScriptParser.Function_callContext ctx, ActionListener<Object> listener) {
        String functionName;
        List<ElasticScriptParser.ExpressionContext> argContexts;
        
        // Handle namespaced function calls (e.g., ARRAY.MAP, STRING.UPPER, K8S.GET_PODS)
        if (ctx.namespaced_function_call() != null) {
            ElasticScriptParser.Namespaced_function_callContext nsCtx = ctx.namespaced_function_call();
            String namespace = nsCtx.namespace_id().getText();
            String method = nsCtx.method_name().getText();
            // Convert to unified function name: NAMESPACE.METHOD -> NAMESPACE_METHOD
            functionName = namespace.toUpperCase() + "_" + method.toUpperCase();
            argContexts = nsCtx.argument_list() != null ? nsCtx.argument_list().expression() : new ArrayList<>();
        } else {
            // Simple function call (e.g., MY_FUNCTION)
            ElasticScriptParser.Simple_function_callContext simpleCtx = ctx.simple_function_call();
            functionName = simpleCtx.ID().getText();
            argContexts = simpleCtx.argument_list() != null ? simpleCtx.argument_list().expression() : new ArrayList<>();
        }
        
        FunctionDefinition function = context.getFunction(functionName);

        if (function == null) {
            listener.onFailure(new RuntimeException("Unknown function: " + functionName));
            return;
        }

        ActionListener<List<Object>> funcCallListener = new ActionListener<List<Object>>() {
            @Override
            public void onResponse(List<Object> argValues) {
                functionDefHandler.executeFunctionAsync(functionName, argValues, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object result) {
                        listener.onResponse(result);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<List<Object>> funcCallLogger = ActionListenerUtils.withLogging(funcCallListener, this.getClass().getName(),
            "Visit-Function-Call: " + argContexts);

        evaluateArgumentsAsync(argContexts, funcCallLogger);
    }

    public void visitCallProcedureAsync(ElasticScriptParser.Call_procedure_statementContext ctx, ActionListener<Object> listener) {

        ActionListener<Object> callProcedureListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                listener.onResponse(result);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> callProcedureLogger = ActionListenerUtils.withLogging(callProcedureListener, this.getClass().getName(),
            "Call-Procedure: " + ctx.getText() );

        callProcedureStatementHandler.handleAsync(ctx, callProcedureLogger);
    }

    /**
     * Get the procedure executor client
     * @return Client
     */
    public Client getClient() {
        return this.client;
    }

    /**
     * Get the token stream
     * @return CommonTokenStream
     */
    public CommonTokenStream getTokenStream() {
        return this.tokenStream;
    }

    /**
     * Evaluates a list of arguments asynchronously.
     *
     * @param argContexts The list of ExpressionContexts representing arguments.
     * @param listener    The ActionListener to receive the list of evaluated arguments.
     */
    private void evaluateArgumentsAsync(List<ElasticScriptParser.ExpressionContext> argContexts, ActionListener<List<Object>> listener) {
        List<Object> argValues = new ArrayList<>();
        evaluateArgumentAsync(argContexts, 0, argValues, listener);
    }

    private void evaluateArgumentAsync(List<ElasticScriptParser.ExpressionContext> argContexts, int index, List<Object> argValues,
                                       ActionListener<List<Object>> listener) {
        if (index >= argContexts.size()) {
            listener.onResponse(argValues);
            return;
        }
        ActionListener<Object> argListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                argValues.add(value);
                evaluateArgumentAsync(argContexts, index + 1, argValues, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };
        ActionListener<Object> argLogger = ActionListenerUtils.withLogging(argListener, this.getClass().getName(),
            "Eval-Argument: " + argContexts.get(index));
        evaluateExpressionAsync(argContexts.get(index), argLogger);
    }

    /**
     * Helper method that converts an Object to a boolean.
     *
     * @param obj The object to convert.
     * @return The boolean value.
     */
    private boolean toBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        throw new RuntimeException("Expected a boolean value, but got: " + obj);
    }

    /**
     * Executes a runnable asynchronously using the thread pool.
     *
     * @param runnable The runnable to execute.
     */
    private void executeAsync(Runnable runnable) {
        threadPool.generic().execute(runnable);
    }

    public void getProcedureAsync(String procedureName, ActionListener<FunctionDefinition> listener) {
        GetRequest getRequest = new GetRequest(".elastic_script_procedures", procedureName);

        this.client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse response) {
                if (response.isExists()) {
                    try {
                        Map<String, Object> source = response.getSourceAsMap();
                        String name = response.getId();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rawParams = (List<Map<String, Object>>) source.get("parameters");
                        List<Parameter> parameters = rawParams.stream()
                            .map(param -> new Parameter(
                                (String) param.get("name"),
                                (String) param.get("type"),
                                ParameterMode.IN))
                            .toList();

                        String procedureText = (String) source.get("procedure");
                        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procedureText));
                        CommonTokenStream tokens = new CommonTokenStream(lexer);
                        ElasticScriptParser parser = new ElasticScriptParser(tokens);
                        ElasticScriptParser.ProcedureContext procCtx = parser.procedure();
                        List<ElasticScriptParser.StatementContext> body = procCtx.statement();;

                        // Just create a FunctionDefinition directly
                        StoredProcedureDefinition function = new StoredProcedureDefinition(name, parameters, body);
                        listener.onResponse(function);
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    listener.onResponse(null); // Procedure not found
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Loads a stored function from the .elastic_script_functions index and registers it in the context.
     *
     * @param functionName The name of the stored function to load.
     * @param listener     The ActionListener to notify with the function definition or null.
     */
    public void getStoredFunctionAsync(String functionName, ActionListener<FunctionDefinition> listener) {
        if (client == null) {
            listener.onResponse(null);
            return;
        }
        
        GetRequest getRequest = new GetRequest(".elastic_script_functions", functionName);

        this.client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse response) {
                if (response.isExists()) {
                    try {
                        Map<String, Object> source = response.getSourceAsMap();
                        String name = response.getId();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rawParams = (List<Map<String, Object>>) source.get("parameters");
                        List<Parameter> parameters = rawParams.stream()
                            .map(param -> new Parameter(
                                (String) param.get("name"),
                                (String) param.get("type"),
                                ParameterMode.valueOf(((String) param.getOrDefault("mode", "IN")).toUpperCase())))
                            .toList();

                        String functionText = (String) source.get("function");
                        String returnType = (String) source.get("return_type");
                        
                        // Parse the stored function to get the body
                        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(functionText));
                        CommonTokenStream tokens = new CommonTokenStream(lexer);
                        ElasticScriptParser parser = new ElasticScriptParser(tokens);
                        ElasticScriptParser.Create_function_statementContext funcCtx = parser.create_function_statement();
                        List<ElasticScriptParser.StatementContext> body = funcCtx.statement();

                        // Create a StoredFunctionDefinition
                        StoredFunctionDefinition function = new StoredFunctionDefinition(name, parameters, body, returnType);
                        listener.onResponse(function);
                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                } else {
                    listener.onResponse(null); // Function not found
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Calls a procedure by name with the given arguments.
     *
     * @param procedureName The name of the procedure to call.
     * @param args          The arguments to pass to the procedure.
     * @param listener      The ActionListener to receive the result.
     */
    public void callProcedureByNameAsync(String procedureName, List<Object> args, ActionListener<Object> listener) {
        getProcedureAsync(procedureName, ActionListener.wrap(
            procDef -> {
                if (procDef == null) {
                    listener.onFailure(new RuntimeException("Procedure not found: " + procedureName));
                    return;
                }

                // Set up the execution context with the arguments
                ExecutionContext childContext = new ExecutionContext(context);
                List<Parameter> params = procDef.getParameters();
                for (int i = 0; i < params.size() && i < args.size(); i++) {
                    Parameter param = params.get(i);
                    childContext.declareVariable(param.getName(), param.getType());
                    childContext.setVariable(param.getName(), args.get(i));
                }

                // Create a child executor for the procedure
                ProcedureExecutor childExecutor = new ProcedureExecutor(childContext, threadPool, client, tokenStream);
                
                // Execute the procedure body
                if (procDef instanceof StoredProcedureDefinition storedProc) {
                    childExecutor.executeStatementsAsync(storedProc.getBody(), 0, ActionListener.wrap(
                        result -> {
                            // Unwrap ReturnValue if needed
                            if (result instanceof ReturnValue rv) {
                                listener.onResponse(rv.getValue());
                            } else {
                                listener.onResponse(result);
                            }
                        },
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(new RuntimeException("Unsupported procedure type: " + procDef.getClass()));
                }
            },
            listener::onFailure
        ));
    }

    /**
     * Calls a function by name with the given arguments.
     *
     * @param functionName The name of the function to call.
     * @param args         The arguments to pass to the function.
     * @param listener     The ActionListener to receive the result.
     */
    public void callFunctionAsync(String functionName, List<Object> args, ActionListener<Object> listener) {
        FunctionDefinition func = context.getFunction(functionName);
        if (func == null) {
            listener.onFailure(new RuntimeException("Function not found: " + functionName));
            return;
        }
        
        func.execute(args, listener);
    }

    /**
     * Executes a statement block from a string.
     *
     * @param statementBlock The statement block as a string.
     * @param listener       The ActionListener to receive the result.
     */
    public void executeStatementBlockAsync(String statementBlock, ActionListener<Object> listener) {
        try {
            // Wrap the statement block in a procedure for parsing
            String wrappedCode = "PROCEDURE temp_lambda() BEGIN " + statementBlock + " END PROCEDURE";
            
            ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(wrappedCode));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ElasticScriptParser parser = new ElasticScriptParser(tokens);
            ElasticScriptParser.ProcedureContext procCtx = parser.procedure();
            
            if (procCtx != null && procCtx.statement() != null) {
                executeStatementsAsync(procCtx.statement(), 0, listener);
            } else {
                listener.onFailure(new RuntimeException("Failed to parse statement block"));
            }
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Executes an ESQL query asynchronously and returns the results as a List of Maps.
     * This is used by cursor iteration to execute the cursor's query.
     *
     * @param esqlQuery The ESQL query string to execute.
     * @param listener  The ActionListener to receive the query results.
     */
    @SuppressWarnings("unchecked")
    public void executeEsqlQueryAsync(String esqlQuery, ActionListener<Object> listener) {
        try {
            // Substitute variables in the query
            String substitutedQuery = substituteVariables(esqlQuery);
            
            EScriptLogger.esqlQuery(context.getExecutionId(), substitutedQuery);
            
            // Check if this is a FROM function() query
            if (isFunctionSourceQuery(substitutedQuery)) {
                executeFunctionSourceQuery(substitutedQuery, listener);
                return;
            }
            
            EsqlQueryRequest request = EsqlQueryRequest.syncEsqlQueryRequest(substitutedQuery);
            
            client.execute(
                EsqlQueryAction.INSTANCE,
                request,
                new ActionListener<EsqlQueryResponse>() {
                    @Override
                    public void onResponse(EsqlQueryResponse esqlQueryResponse) {
                        try {
                            // Convert columns+rows to a List<Map<String,Object>>
                            List<Map<String, Object>> rowMaps = rowsAsMaps(
                                (List<ColumnInfo>) esqlQueryResponse.response().columns(),
                                esqlQueryResponse.response().rows()
                            );
                            listener.onResponse(rowMaps);
                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                }
            );
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    
    /**
     * Checks if the ESQL query is a function source query (FROM FUNCTION()).
     * Function source queries start with FROM followed by a function call pattern.
     */
    private boolean isFunctionSourceQuery(String query) {
        String trimmed = query.trim();
        String upperTrimmed = trimmed.toUpperCase();
        // Check for pattern: FROM FUNCTION_NAME(...) or FROMFUNCTION_NAME(...) (without space from ANTLR getText)
        // Handle both cases: with and without space after FROM
        String afterFrom;
        if (upperTrimmed.startsWith("FROM ")) {
            afterFrom = trimmed.substring(5).trim();
        } else if (upperTrimmed.startsWith("FROM")) {
            afterFrom = trimmed.substring(4).trim();
        } else {
            return false;
        }
        // Check if it looks like a function call (WORD followed by parenthesis)
        boolean matches = afterFrom.matches("^[A-Za-z_][A-Za-z0-9_]*\\s*\\(.*\\).*");
        return matches;
    }
    
    /**
     * Executes a function source query: FROM FUNCTION() | ESQL_OPERATIONS
     * This allows using function results as ESQL data sources.
     */
    @SuppressWarnings("unchecked")
    private void executeFunctionSourceQuery(String query, ActionListener<Object> listener) {
        String trimmed = query.trim();
        String upperTrimmed = trimmed.toUpperCase();
        // Handle both cases: with and without space after FROM
        String afterFrom;
        if (upperTrimmed.startsWith("FROM ")) {
            afterFrom = trimmed.substring(5).trim();
        } else {
            afterFrom = trimmed.substring(4).trim(); // "FROM" without space
        }
        
        // Parse the function call and any ESQL operations
        int pipeIndex = findFirstUnparenthesizedPipe(afterFrom);
        String functionPart;
        String esqlOps = null;
        
        if (pipeIndex > 0) {
            functionPart = afterFrom.substring(0, pipeIndex).trim();
            esqlOps = afterFrom.substring(pipeIndex + 1).trim();
        } else {
            functionPart = afterFrom.trim();
        }
        
        EScriptLogger.functionCall(context.getExecutionId(), "FROM " + functionPart, esqlOps != null ? 1 : 0);
        
        // Parse the function call: FUNCTION_NAME(args)
        int openParen = functionPart.indexOf('(');
        int closeParen = functionPart.lastIndexOf(')');
        
        if (openParen < 0 || closeParen < openParen) {
            listener.onFailure(new RuntimeException("Invalid function call syntax in FROM: " + functionPart));
            return;
        }
        
        String functionName = functionPart.substring(0, openParen).trim();
        String argsString = functionPart.substring(openParen + 1, closeParen).trim();
        
        // Parse arguments (simple comma-separated for now)
        List<Object> args = parseSimpleArgs(argsString);
        
        final String finalEsqlOps = esqlOps;
        
        // Execute the function
        functionDefHandler.executeFunctionAsync(functionName, args, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                try {
                    // Convert the result to a list of maps
                    List<Map<String, Object>> rows = convertToRowMaps(result);
                    
                    // Apply ESQL operations if present
                    if (finalEsqlOps != null && !finalEsqlOps.isEmpty()) {
                        rows = applyEsqlOperations(rows, finalEsqlOps);
                    }
                    
                    listener.onResponse(rows);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
    
    /**
     * Finds the first pipe character that is not inside parentheses.
     */
    private int findFirstUnparenthesizedPipe(String str) {
        int parenDepth = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;
            else if (c == '|' && parenDepth == 0) return i;
        }
        return -1;
    }
    
    /**
     * Parses simple comma-separated arguments.
     */
    private List<Object> parseSimpleArgs(String argsString) {
        List<Object> args = new ArrayList<>();
        if (argsString.isEmpty()) {
            return args;
        }
        
        // Simple split by comma (doesn't handle nested commas in strings)
        String[] parts = argsString.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            // Try to parse as number, boolean, or keep as string
            if (trimmed.isEmpty()) continue;
            
            if (trimmed.matches("-?\\d+\\.?\\d*")) {
                args.add(Double.parseDouble(trimmed));
            } else if (trimmed.equalsIgnoreCase("true")) {
                args.add(true);
            } else if (trimmed.equalsIgnoreCase("false")) {
                args.add(false);
            } else if ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
                       (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
                args.add(trimmed.substring(1, trimmed.length() - 1));
            } else {
                // Check if it's a variable reference
                Object varValue = context.getVariable(trimmed);
                if (varValue != null) {
                    args.add(varValue);
                } else {
                    args.add(trimmed);
                }
            }
        }
        return args;
    }
    
    /**
     * Converts a function result to a list of row maps.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertToRowMaps(Object result) {
        List<Map<String, Object>> rows = new ArrayList<>();
        
        if (result instanceof List) {
            List<?> list = (List<?>) result;
            for (Object item : list) {
                if (item instanceof Map) {
                    rows.add((Map<String, Object>) item);
                } else {
                    // Wrap non-map items in a single-column map
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("value", item);
                    rows.add(row);
                }
            }
        } else if (result instanceof Map) {
            rows.add((Map<String, Object>) result);
        } else {
            // Single value - wrap in a map
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("value", result);
            rows.add(row);
        }
        
        return rows;
    }
    
    /**
     * Applies simple ESQL-like operations to the rows.
     * Supports: WHERE, SORT, LIMIT, KEEP
     */
    private List<Map<String, Object>> applyEsqlOperations(List<Map<String, Object>> rows, String ops) {
        // Split by pipe and process each operation
        String[] operations = ops.split("\\|");
        
        for (String op : operations) {
            String trimmedOp = op.trim();
            if (trimmedOp.isEmpty()) continue;
            
            String upperOp = trimmedOp.toUpperCase();
            
            // Handle cases with or without spaces after keywords (ANTLR getText may omit spaces)
            if (upperOp.startsWith("LIMIT")) {
                String limitStr = trimmedOp.substring(5).trim();
                int limit = Integer.parseInt(limitStr);
                rows = rows.subList(0, Math.min(limit, rows.size()));
            } else if (upperOp.startsWith("WHERE")) {
                String condition = trimmedOp.substring(5).trim();
                rows = applyWhereFilter(rows, condition);
            } else if (upperOp.startsWith("SORT")) {
                String sortSpec = trimmedOp.substring(4).trim();
                rows = applySortOperation(rows, sortSpec);
            } else if (upperOp.startsWith("KEEP")) {
                String fields = trimmedOp.substring(4).trim();
                rows = applyKeepOperation(rows, fields);
            }
            // Other operations can be added as needed
        }
        
        return rows;
    }
    
    /**
     * Applies a WHERE filter to the rows.
     * Supports simple equality: field == 'value' or field == value
     */
    private List<Map<String, Object>> applyWhereFilter(List<Map<String, Object>> rows, String condition) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        // Parse simple equality: field == value
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            String field = parts[0].trim();
            String valueStr = parts[1].trim();
            
            // Remove quotes if present
            if ((valueStr.startsWith("'") && valueStr.endsWith("'")) ||
                (valueStr.startsWith("\"") && valueStr.endsWith("\""))) {
                valueStr = valueStr.substring(1, valueStr.length() - 1);
            }
            
            final String matchValue = valueStr;
            
            for (Map<String, Object> row : rows) {
                Object fieldValue = row.get(field);
                if (fieldValue != null && fieldValue.toString().equals(matchValue)) {
                    filtered.add(row);
                }
            }
        } else {
            // If we can't parse the condition, return all rows
            return rows;
        }
        
        return filtered;
    }
    
    /**
     * Applies SORT operation.
     */
    private List<Map<String, Object>> applySortOperation(List<Map<String, Object>> rows, String sortSpec) {
        // Parse: field [ASC|DESC] - handle both with and without spaces
        String spec = sortSpec.trim();
        String field;
        boolean ascending = true;
        
        String upperSpec = spec.toUpperCase();
        if (upperSpec.endsWith("DESC")) {
            field = spec.substring(0, spec.length() - 4).trim();
            ascending = false;
        } else if (upperSpec.endsWith("ASC")) {
            field = spec.substring(0, spec.length() - 3).trim();
        } else {
            // Check for space-separated parts
            String[] parts = spec.split("\\s+");
            field = parts[0].trim();
            if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("DESC")) {
                ascending = false;
            }
        }
        
        final boolean asc = ascending;
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> {
            Object va = a.get(field);
            Object vb = b.get(field);
            
            if (va == null && vb == null) return 0;
            if (va == null) return asc ? -1 : 1;
            if (vb == null) return asc ? 1 : -1;
            
            if (va instanceof Comparable && vb instanceof Comparable) {
                @SuppressWarnings("unchecked")
                int cmp = ((Comparable<Object>) va).compareTo(vb);
                return asc ? cmp : -cmp;
            }
            
            return 0;
        });
        
        return sorted;
    }
    
    /**
     * Applies KEEP operation (select specific fields).
     */
    private List<Map<String, Object>> applyKeepOperation(List<Map<String, Object>> rows, String fields) {
        String[] fieldNames = fields.split(",");
        List<String> keepFields = new ArrayList<>();
        for (String f : fieldNames) {
            keepFields.add(f.trim());
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> newRow = new LinkedHashMap<>();
            for (String field : keepFields) {
                if (row.containsKey(field)) {
                    newRow.put(field, row.get(field));
                }
            }
            result.add(newRow);
        }
        
        return result;
    }

    /**
     * Substitutes placeholders like ":myVar" with actual values from the Executor's context.
     */
    private String substituteVariables(String original) {
        for (String varName : context.getVariableNames()) {
            Object val = context.getVariable(varName);
            original = original.replace(":" + varName, String.valueOf(val));
        }
        return original;
    }

    /**
     * Converts the given columns and row-iterator into a list of maps,
     * each map representing one row with columnName -> value.
     */
    private static List<Map<String, Object>> rowsAsMaps(
        List<ColumnInfo> columns,
        Iterable<Iterable<Object>> rowIter
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Iterable<Object> rowValues : rowIter) {
            List<Object> rowList = new ArrayList<>();
            rowValues.forEach(rowList::add);

            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).name();
                Object val = i < rowList.size() ? rowList.get(i) : null;
                rowMap.put(colName, val);
            }
            result.add(rowMap);
        }
        return result;
    }
}
