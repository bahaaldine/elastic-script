/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.executors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.escript.functions.BuiltInFunctionRegistry;
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;
import org.elasticsearch.xpack.escript.visitors.ProcedureDefinitionVisitor;
import org.elasticsearch.xpack.escript.handlers.JobStatementHandler;
import org.elasticsearch.xpack.escript.handlers.TriggerStatementHandler;
import org.elasticsearch.xpack.escript.handlers.PackageStatementHandler;
import org.elasticsearch.xpack.escript.handlers.PermissionStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ProfileStatementHandler;
import org.elasticsearch.xpack.escript.profiling.ProfileResult;
import org.elasticsearch.xpack.escript.handlers.TypeStatementHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElasticScriptExecutor {

    private final ThreadPool threadPool;
    private final Client client;
    private final BuiltInFunctionRegistry functionRegistry;

    private static final Logger LOGGER = LogManager.getLogger(ElasticScriptExecutor.class);

    @Inject
    public ElasticScriptExecutor(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
        this.functionRegistry = BuiltInFunctionRegistry.getInstance(client);
    }

    /**
     * Asynchronously executes a Elastic Script procedure.
     *
     * This method:
     *  1. Parses the input using the 'procedure' rule.
     *  2. Creates a fresh global ExecutionContext.
     *  3. Visits the procedure block to register any procedure definitions.
     *  4. Iterates over the statements to detect and synchronously process procedure calls
     *     (to update the global context).
     *  5. Removes those definition/call statements from the list.
     *  6. Creates a ProcedureExecutor with the updated global context and executes the remaining statements.
     *
     * @param procedureText The Elastic Script procedure text.
     * @param listener      The ActionListener to notify when execution is complete.
     */
    public void executeProcedure(String procedureText, Map<String, Object> args, ActionListener<Object> listener) {
        threadPool.generic().execute(() -> {
            try {
                // 1. Parse the procedure
                ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procedureText));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                ElasticScriptParser parser = new ElasticScriptParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new ElasticScriptErrorListener());

                ElasticScriptParser.ProgramContext programContext = parser.program();

                if (programContext.call_procedure_statement() != null) {
                    getProcedureAsync(programContext.call_procedure_statement().ID().getText(), new ActionListener<Map<String, Object>>() {
                        @Override
                        public void onResponse(Map<String, Object> stringObjectMap) {
                            Object procedureContentObj = stringObjectMap.get("procedure");
                            if (procedureContentObj == null) {
                                listener.onFailure(new IllegalArgumentException("Procedure content is missing"));
                                return;
                            }

                            String procedureContent = procedureContentObj.toString();
                            LOGGER.debug("Executing stored procedure [{}]: {}",
                                programContext.call_procedure_statement().ID().getText(), procedureContent);

                            try {
                                ElasticScriptLexer storedLexer = new ElasticScriptLexer(CharStreams.fromString(procedureContent));
                                CommonTokenStream storedTokens = new CommonTokenStream(storedLexer);
                                ElasticScriptParser storedParser = new ElasticScriptParser(storedTokens);
                                storedParser.removeErrorListeners();
                                storedParser.addErrorListener(new ElasticScriptErrorListener());
                                ElasticScriptParser.ProcedureContext procCtx = storedParser.procedure();

                                // Create fresh context and bind parameters
                                ExecutionContext executionContext = new ExecutionContext();
                                List<ElasticScriptParser.ParameterContext> parameterContexts =
                                    procCtx.parameter_list() != null ? procCtx.parameter_list().parameter() : List.of();

                                if (parameterContexts.isEmpty() == false) {
                                    // Parse call arguments directly from the call_procedure_statement
                                    List<ElasticScriptParser.ExpressionContext> callArgs =
                                        programContext.call_procedure_statement().argument_list() != null
                                            ? programContext.call_procedure_statement().argument_list().expression()
                                            : List.of();

                                    if (parameterContexts.size() != callArgs.size()) {
                                        listener.onFailure(
                                            new IllegalArgumentException("Mismatch between declared parameters and call arguments"));
                                        return;
                                    }

                                    for (int i = 0; i < parameterContexts.size(); i++) {
                                        var param = parameterContexts.get(i);
                                        String paramName = param.ID().getText();
                                        String paramType = param.datatype().getText().toUpperCase(java.util.Locale.ROOT);
                                        String rawValue = callArgs.get(i).getText();

                                        Object value;
                                        switch (paramType) {
                                            case "INT":
                                                value = Integer.valueOf(rawValue);
                                                break;
                                            case "NUMBER":
                                            case "FLOAT":
                                                value = Double.valueOf(rawValue);
                                                break;
                                            case "BOOLEAN":
                                                value = Boolean.parseBoolean(rawValue);
                                                break;
                                            case "STRING":
                                                value = rawValue.replaceAll("^['\\\"]|['\\\"]$", ""); // strip quotes
                                                break;
                                            case "ARRAY":
                                            case "ARRAYOFSTRING":
                                            case "ARRAYOFNUMBER":
                                            case "ARRAYOFDOCUMENT":
                                                LOGGER.debug("Param name {} is an array of document", paramName);
                                                try (XContentParser parser =
                                                         org.elasticsearch.xcontent.XContentType.JSON.xContent().createParser(
                                                    org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
                                                    org.elasticsearch.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                                                    new java.io.ByteArrayInputStream(
                                                        rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                                ))) {
                                                    parser.nextToken();
                                                    value = parser.list();
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException(
                                                        "Failed to parse array input for parameter: " + paramName, e);
                                                }
                                                break;
                                            default:
                                                value = rawValue;
                                        }

                                        executionContext.declareVariable(paramName, paramType);
                                        executionContext.setVariable(paramName, value);
                                    }
                                }

                                // Visit inner procedure definitions
                                ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(executionContext);
                                for (ElasticScriptParser.StatementContext stmtCtx : procCtx.statement()) {
                                    if (stmtCtx.getChildCount() > 0 && "PROCEDURE".equalsIgnoreCase(stmtCtx.getChild(0).getText())) {
                                        procDefVisitor.visit(stmtCtx);
                                    }
                                }

                                ProcedureExecutor procedureExecutor =
                                    new ProcedureExecutor(executionContext, threadPool, client, storedTokens);
                                
                                // Register all built-in functions using cached registry
                                functionRegistry.registerAll(executionContext, procedureExecutor);

                                ActionListener<Object> execListener = ActionListenerUtils.withLogging(listener,
                                    this.getClass().getName(), "ExecuteStoredProcedure-" + procedureContent);

                                procedureExecutor.visitProcedureAsync(procCtx, execListener);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
                } else if (programContext.delete_procedure_statement() != null) {
                    ElasticScriptParser.Delete_procedure_statementContext deleteContext = programContext.delete_procedure_statement();
                    String procedureId = deleteContext.ID().getText();
                    LOGGER.debug("Deleting procedure {}", procedureId);
                    deleteProcedureAsync(procedureId, listener);
                } else if (programContext.create_procedure_statement() != null ) {
                    ElasticScriptParser.Create_procedure_statementContext createContext = programContext.create_procedure_statement();
                    String procedureId = createContext.procedure().ID().getText();

                    Token start = createContext.procedure().getStart();
                    Token stop = createContext.procedure().getStop();
                    String rawProcedureText = tokens.getText(start, stop);

                    LOGGER.debug("Storing procedure {}", procedureId);
                    storeProcedureAsync( procedureId, rawProcedureText, listener );
                } else if (programContext.define_intent_statement() != null) {
                    // Handle DEFINE INTENT at the program level
                    ElasticScriptParser.Define_intent_statementContext intentCtx = programContext.define_intent_statement();
                    String intentName = intentCtx.ID().getText();
                    LOGGER.debug("Defining intent {}", intentName);
                    
                    // Create a temporary executor context to handle the intent definition
                    ExecutionContext tempContext = new ExecutionContext();
                    ProcedureExecutor tempExecutor = new ProcedureExecutor(tempContext, threadPool, client, tokens);
                    org.elasticsearch.xpack.escript.handlers.DefineIntentStatementHandler handler = 
                        new org.elasticsearch.xpack.escript.handlers.DefineIntentStatementHandler(tempExecutor);
                    handler.handleAsync(intentCtx, listener);
                } else if (programContext.create_function_statement() != null) {
                    // Handle CREATE FUNCTION statement
                    ElasticScriptParser.Create_function_statementContext createFuncCtx = programContext.create_function_statement();
                    String functionId = createFuncCtx.ID().getText();
                    
                    // Extract the full function text including CREATE keyword
                    Token start = createFuncCtx.getStart();
                    Token stop = createFuncCtx.getStop();
                    String rawFunctionText = tokens.getText(start, stop);
                    
                    // Extract return type
                    String returnType = createFuncCtx.return_type().getText().toUpperCase();
                    
                    LOGGER.debug("Storing function {} with return type {}", functionId, returnType);
                    storeFunctionAsync(functionId, rawFunctionText, returnType, listener);
                } else if (programContext.delete_function_statement() != null) {
                    // Handle DELETE FUNCTION statement
                    ElasticScriptParser.Delete_function_statementContext deleteFuncCtx = programContext.delete_function_statement();
                    String functionId = deleteFuncCtx.ID().getText();
                    LOGGER.debug("Deleting function {}", functionId);
                    deleteFunctionAsync(functionId, listener);
                } else if (programContext.job_statement() != null) {
                    // Handle JOB statements (CREATE JOB, ALTER JOB, DROP JOB, SHOW JOBS)
                    handleJobStatement(programContext.job_statement(), tokens, listener);
                } else if (programContext.trigger_statement() != null) {
                    // Handle TRIGGER statements (CREATE TRIGGER, ALTER TRIGGER, DROP TRIGGER, SHOW TRIGGERS)
                    handleTriggerStatement(programContext.trigger_statement(), tokens, listener);
                } else if (programContext.package_statement() != null) {
                    // Handle PACKAGE statements (CREATE PACKAGE, CREATE PACKAGE BODY, DROP PACKAGE, SHOW PACKAGE)
                    handlePackageStatement(programContext.package_statement(), tokens, listener);
                } else if (programContext.permission_statement() != null) {
                    // Handle PERMISSION statements (GRANT, REVOKE, CREATE ROLE, DROP ROLE, SHOW PERMISSIONS)
                    handlePermissionStatement(programContext.permission_statement(), listener);
                } else if (programContext.profile_statement() != null) {
                    // Handle PROFILE statements (PROFILE CALL, SHOW PROFILE, CLEAR PROFILE, ANALYZE PROFILE)
                    handleProfileStatement(programContext.profile_statement(), tokens, listener);
                } else if (programContext.type_statement() != null) {
                    // Handle TYPE statements (CREATE TYPE, DROP TYPE, SHOW TYPES)
                    handleTypeStatement(programContext.type_statement(), listener);
                } else {
                    listener.onFailure(new IllegalArgumentException("Unsupported top-level statement"));
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Extracts the function name from a function_call context.
     * Handles both simple (MY_FUNC) and namespaced (NAMESPACE.METHOD) function calls.
     */
    private String getFunctionName(ElasticScriptParser.Function_callContext ctx) {
        if (ctx.namespaced_function_call() != null) {
            ElasticScriptParser.Namespaced_function_callContext nsCtx = ctx.namespaced_function_call();
            return nsCtx.namespace_id().getText().toUpperCase() + "_" + nsCtx.method_name().getText().toUpperCase();
        } else {
            return ctx.simple_function_call().ID().getText();
        }
    }

    /**
     * Extracts the argument list from a function_call context.
     * Handles both simple and namespaced function calls.
     */
    private ElasticScriptParser.Argument_listContext getArgumentList(ElasticScriptParser.Function_callContext ctx) {
        if (ctx.namespaced_function_call() != null) {
            return ctx.namespaced_function_call().argument_list();
        } else {
            return ctx.simple_function_call().argument_list();
        }
    }

    /**
     * Helper method to determine if a statement is a procedure call.
     * In this example, we assume procedure calls are written using function_call_statement,
     * and we check if the called name's definition is an instance of ProcedureDefinition.
     *
     * @param stmt          The statement context.
     * @param globalContext The global ExecutionContext.
     * @return true if the statement is a procedure call; false otherwise.
     */
    private boolean isProcedureCall(ElasticScriptParser.StatementContext stmt, ExecutionContext globalContext) {
        if (stmt.function_call_statement() != null) {
            String callName = getFunctionName(stmt.function_call_statement().function_call());
            try {
                return globalContext.getFunction(callName) instanceof ProcedureDefinition;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Extracts the procedure name from a statement assumed to be a procedure call.
     *
     * @param stmt The statement context.
     * @return The procedure name.
     */
    private String extractProcedureName(ElasticScriptParser.StatementContext stmt) {
        return getFunctionName(stmt.function_call_statement().function_call());
    }

    /**
     * Extracts the procedure arguments from a statement assumed to be a procedure call.
     * This example simply evaluates the argument expressions as Doubles.
     *
     * @param stmt The statement context.
     * @return A list of evaluated argument values.
     */
    private List<Object> extractProcedureArguments(ElasticScriptParser.StatementContext stmt) {
        ElasticScriptParser.Argument_listContext argList = getArgumentList(stmt.function_call_statement().function_call());
        if (argList != null) {
            List<ElasticScriptParser.ExpressionContext> exprs = argList.expression();
            List<Object> args = new ArrayList<>();
            for (ElasticScriptParser.ExpressionContext expr : exprs) {
                try {
                    args.add(Double.valueOf(expr.getText()));
                } catch (NumberFormatException e) {
                    args.add(expr.getText());
                }
            }
            return args;
        }
        return new ArrayList<>();
    }

    /**
     * Asynchronously stores the procedure text into a dedicated Elasticsearch index.
     *
     * @param id            The document ID to use for the procedure.
     * @param procedureText The procedure text to store.
     * @param listener      The ActionListener to notify on completion or error.
     */

    public void storeProcedureAsync(String id, String procedureText, ActionListener<Object> listener) throws IOException {
        String indexName = ".elastic_script_procedures";

        LOGGER.debug( "Storing procedure {}", procedureText );

        // Parse the procedure to extract parameters
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procedureText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        ElasticScriptParser.ProcedureContext procCtx = parser.procedure();

        List<Map<String, Object>> parameters = new ArrayList<>();
        if (procCtx != null && procCtx.parameter_list() != null) {
            for (var paramCtx : procCtx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();
                parameters.add(Map.of("name", paramName, "type", paramType));
            }
        }

        GetIndexRequest request = new GetIndexRequest(TimeValue.timeValueSeconds(30)).indices(indexName);
        client.admin().indices().getIndex(request, ActionListener.wrap(
            getIndexResponse -> {
                indexProcedureDocument(id, procedureText, parameters, listener);
            },
            error -> {
                if (ExceptionsHelper.unwrapCause(error) instanceof IndexNotFoundException) {
                    LOGGER.debug( "Index {} does not exist, creating it ...", indexName );
                    CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        createResponse -> indexProcedureDocument(id, procedureText, parameters, listener),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(error);
                }
            }
        ));
    }

    private void indexProcedureDocument(String id, String procedureText, List<Map<String,
        Object>> parameters, ActionListener<Object> listener) {
        client.prepareIndex(".elastic_script_procedures")
            .setId(id)
            .setSource(Map.of(
                "procedure", procedureText,
                "parameters", parameters
            ))
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase()
                    );
                    listener.onResponse(resultMap);
                },
                listener::onFailure
            ));
    }

    public void deleteProcedureAsync(String id, ActionListener<Object> acknowledged) {
        client.prepareDelete(".elastic_script_procedures", id)
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase()
                    );
                    acknowledged.onResponse(resultMap);
                },
                acknowledged::onFailure
            ));
    }

    /**
     * Asynchronously retrieves a stored procedure by ID from the .elastic_script_procedures index.
     *
     * @param id       The procedure document ID.
     * @param listener The ActionListener to notify with the procedure source or null if not found.
     */
    public void getProcedureAsync(String id, ActionListener<Map<String, Object>> listener) {
        client.prepareGet(".elastic_script_procedures", id)
            .execute(ActionListener.wrap(
                resp -> {
                    if (resp.isExists()) {
                        listener.onResponse(resp.getSource());
                    } else {
                        listener.onResponse(null);
                    }
                },
                listener::onFailure
            ));
    }

    // =======================
    // Stored Function Management
    // =======================

    private static final String FUNCTIONS_INDEX = ".elastic_script_functions";

    /**
     * Asynchronously stores a user-defined function into the .elastic_script_functions index.
     *
     * @param id           The function ID (name)
     * @param functionText The full function definition text
     * @param returnType   The declared return type
     * @param listener     The ActionListener to notify on completion
     */
    public void storeFunctionAsync(String id, String functionText, String returnType, ActionListener<Object> listener) {
        LOGGER.debug("Storing function {}", functionText);

        // Parse the function to extract parameters
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(functionText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        ElasticScriptParser.Create_function_statementContext funcCtx = parser.create_function_statement();

        List<Map<String, Object>> parameters = new ArrayList<>();
        if (funcCtx != null && funcCtx.parameter_list() != null) {
            for (var paramCtx : funcCtx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();
                String paramMode = "IN"; // Default mode
                if (paramCtx.IN() != null) paramMode = "IN";
                if (paramCtx.OUT() != null) paramMode = "OUT";
                if (paramCtx.INOUT() != null) paramMode = "INOUT";
                parameters.add(Map.of("name", paramName, "type", paramType, "mode", paramMode));
            }
        }

        GetIndexRequest request = new GetIndexRequest(TimeValue.timeValueSeconds(30)).indices(FUNCTIONS_INDEX);
        client.admin().indices().getIndex(request, ActionListener.wrap(
            getIndexResponse -> {
                indexFunctionDocument(id, functionText, returnType, parameters, listener);
            },
            error -> {
                if (ExceptionsHelper.unwrapCause(error) instanceof IndexNotFoundException) {
                    LOGGER.debug("Index {} does not exist, creating it ...", FUNCTIONS_INDEX);
                    CreateIndexRequest createRequest = new CreateIndexRequest(FUNCTIONS_INDEX);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        createResponse -> indexFunctionDocument(id, functionText, returnType, parameters, listener),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(error);
                }
            }
        ));
    }

    private void indexFunctionDocument(String id, String functionText, String returnType,
                                       List<Map<String, Object>> parameters, ActionListener<Object> listener) {
        client.prepareIndex(FUNCTIONS_INDEX)
            .setId(id)
            .setSource(Map.of(
                "function", functionText,
                "return_type", returnType,
                "parameters", parameters
            ))
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase(),
                        "return_type", returnType
                    );
                    listener.onResponse(resultMap);
                },
                listener::onFailure
            ));
    }

    /**
     * Asynchronously deletes a stored function by ID.
     *
     * @param id       The function ID
     * @param listener The ActionListener to notify on completion
     */
    public void deleteFunctionAsync(String id, ActionListener<Object> listener) {
        client.prepareDelete(FUNCTIONS_INDEX, id)
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase()
                    );
                    listener.onResponse(resultMap);
                },
                listener::onFailure
            ));
    }

    /**
     * Asynchronously retrieves a stored function by ID.
     *
     * @param id       The function ID
     * @param listener The ActionListener to notify with the function source or null
     */
    public void getFunctionAsync(String id, ActionListener<Map<String, Object>> listener) {
        client.prepareGet(FUNCTIONS_INDEX, id)
            .execute(ActionListener.wrap(
                resp -> {
                    if (resp.isExists()) {
                        listener.onResponse(resp.getSource());
                    } else {
                        listener.onResponse(null);
                    }
                },
                listener::onFailure
            ));
    }

    /**
     * Handles JOB statements (CREATE JOB, ALTER JOB, DROP JOB, SHOW JOBS).
     */
    private void handleJobStatement(ElasticScriptParser.Job_statementContext ctx, 
                                    CommonTokenStream tokens,
                                    ActionListener<Object> listener) {
        JobStatementHandler handler = new JobStatementHandler(client);
        
        if (ctx.create_job_statement() != null) {
            ElasticScriptParser.Create_job_statementContext createCtx = ctx.create_job_statement();
            // Extract the procedure body (statements between BEGIN and END JOB)
            String rawProcedureBody = extractJobProcedureBody(createCtx, tokens);
            handler.handleCreateJob(createCtx, rawProcedureBody, listener);
        } else if (ctx.alter_job_statement() != null) {
            ElasticScriptParser.Alter_job_statementContext alterCtx = ctx.alter_job_statement();
            if (alterCtx instanceof ElasticScriptParser.AlterJobEnableDisableContext) {
                handler.handleAlterJobEnableDisable((ElasticScriptParser.AlterJobEnableDisableContext) alterCtx, listener);
            } else if (alterCtx instanceof ElasticScriptParser.AlterJobScheduleContext) {
                handler.handleAlterJobSchedule((ElasticScriptParser.AlterJobScheduleContext) alterCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown ALTER JOB variant"));
            }
        } else if (ctx.drop_job_statement() != null) {
            handler.handleDropJob(ctx.drop_job_statement(), listener);
        } else if (ctx.show_jobs_statement() != null) {
            ElasticScriptParser.Show_jobs_statementContext showCtx = ctx.show_jobs_statement();
            if (showCtx instanceof ElasticScriptParser.ShowAllJobsContext) {
                handler.handleShowAllJobs(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowJobDetailContext) {
                handler.handleShowJobDetail((ElasticScriptParser.ShowJobDetailContext) showCtx, listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowJobRunsContext) {
                handler.handleShowJobRuns((ElasticScriptParser.ShowJobRunsContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW JOB variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown JOB statement"));
        }
    }

    /**
     * Handles TRIGGER statements (CREATE TRIGGER, ALTER TRIGGER, DROP TRIGGER, SHOW TRIGGERS).
     */
    private void handleTriggerStatement(ElasticScriptParser.Trigger_statementContext ctx,
                                        CommonTokenStream tokens,
                                        ActionListener<Object> listener) {
        TriggerStatementHandler handler = new TriggerStatementHandler(client);
        
        if (ctx.create_trigger_statement() != null) {
            ElasticScriptParser.Create_trigger_statementContext createCtx = ctx.create_trigger_statement();
            // Extract condition and procedure body
            String rawCondition = extractTriggerCondition(createCtx, tokens);
            String rawProcedureBody = extractTriggerProcedureBody(createCtx, tokens);
            handler.handleCreateTrigger(createCtx, rawCondition, rawProcedureBody, listener);
        } else if (ctx.alter_trigger_statement() != null) {
            ElasticScriptParser.Alter_trigger_statementContext alterCtx = ctx.alter_trigger_statement();
            if (alterCtx instanceof ElasticScriptParser.AlterTriggerEnableDisableContext) {
                handler.handleAlterTriggerEnableDisable((ElasticScriptParser.AlterTriggerEnableDisableContext) alterCtx, listener);
            } else if (alterCtx instanceof ElasticScriptParser.AlterTriggerIntervalContext) {
                handler.handleAlterTriggerInterval((ElasticScriptParser.AlterTriggerIntervalContext) alterCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown ALTER TRIGGER variant"));
            }
        } else if (ctx.drop_trigger_statement() != null) {
            handler.handleDropTrigger(ctx.drop_trigger_statement(), listener);
        } else if (ctx.show_triggers_statement() != null) {
            ElasticScriptParser.Show_triggers_statementContext showCtx = ctx.show_triggers_statement();
            if (showCtx instanceof ElasticScriptParser.ShowAllTriggersContext) {
                handler.handleShowAllTriggers(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowTriggerDetailContext) {
                handler.handleShowTriggerDetail((ElasticScriptParser.ShowTriggerDetailContext) showCtx, listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowTriggerRunsContext) {
                handler.handleShowTriggerRuns((ElasticScriptParser.ShowTriggerRunsContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW TRIGGER variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown TRIGGER statement"));
        }
    }

    /**
     * Extract the procedure body from a CREATE JOB statement.
     */
    private String extractJobProcedureBody(ElasticScriptParser.Create_job_statementContext ctx,
                                           CommonTokenStream tokens) {
        // Get all statements between BEGIN and END JOB
        StringBuilder body = new StringBuilder();
        for (ElasticScriptParser.StatementContext stmt : ctx.statement()) {
            body.append(tokens.getText(stmt.getStart(), stmt.getStop()));
            body.append(" ");
        }
        return body.toString().trim();
    }

    /**
     * Extract the WHEN condition from a CREATE TRIGGER statement.
     */
    private String extractTriggerCondition(ElasticScriptParser.Create_trigger_statementContext ctx,
                                           CommonTokenStream tokens) {
        if (ctx.expression() != null) {
            return tokens.getText(ctx.expression().getStart(), ctx.expression().getStop());
        }
        return null;
    }

    /**
     * Extract the procedure body from a CREATE TRIGGER statement.
     */
    private String extractTriggerProcedureBody(ElasticScriptParser.Create_trigger_statementContext ctx,
                                               CommonTokenStream tokens) {
        StringBuilder body = new StringBuilder();
        for (ElasticScriptParser.StatementContext stmt : ctx.statement()) {
            body.append(tokens.getText(stmt.getStart(), stmt.getStop()));
            body.append(" ");
        }
        return body.toString().trim();
    }

    /**
     * Handles PACKAGE statements (CREATE PACKAGE, CREATE PACKAGE BODY, DROP PACKAGE, SHOW PACKAGE).
     */
    private void handlePackageStatement(ElasticScriptParser.Package_statementContext ctx,
                                        CommonTokenStream tokens,
                                        ActionListener<Object> listener) {
        PackageStatementHandler handler = new PackageStatementHandler(client);

        if (ctx.create_package_statement() != null) {
            ElasticScriptParser.Create_package_statementContext createCtx = ctx.create_package_statement();
            // Extract the full specification text
            String rawSpecText = tokens.getText(createCtx.getStart(), createCtx.getStop());
            handler.handleCreatePackage(createCtx, rawSpecText, listener);
        } else if (ctx.create_package_body_statement() != null) {
            ElasticScriptParser.Create_package_body_statementContext bodyCtx = ctx.create_package_body_statement();
            // Extract the full body text
            String rawBodyText = tokens.getText(bodyCtx.getStart(), bodyCtx.getStop());
            handler.handleCreatePackageBody(bodyCtx, rawBodyText, listener);
        } else if (ctx.drop_package_statement() != null) {
            handler.handleDropPackage(ctx.drop_package_statement(), listener);
        } else if (ctx.show_packages_statement() != null) {
            ElasticScriptParser.Show_packages_statementContext showCtx = ctx.show_packages_statement();
            if (showCtx instanceof ElasticScriptParser.ShowPackageDetailContext) {
                handler.handleShowPackage((ElasticScriptParser.ShowPackageDetailContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW PACKAGE variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown PACKAGE statement type"));
        }
    }

    /**
     * Handles PERMISSION statements (GRANT, REVOKE, CREATE ROLE, DROP ROLE, SHOW PERMISSIONS).
     */
    private void handlePermissionStatement(ElasticScriptParser.Permission_statementContext ctx,
                                          ActionListener<Object> listener) {
        PermissionStatementHandler handler = new PermissionStatementHandler(client);

        if (ctx.grant_statement() != null) {
            handler.handleGrant(ctx.grant_statement(), listener);
        } else if (ctx.revoke_statement() != null) {
            handler.handleRevoke(ctx.revoke_statement(), listener);
        } else if (ctx.create_role_statement() != null) {
            handler.handleCreateRole(ctx.create_role_statement(), listener);
        } else if (ctx.drop_role_statement() != null) {
            handler.handleDropRole(ctx.drop_role_statement(), listener);
        } else if (ctx.show_permissions_statement() != null) {
            ElasticScriptParser.Show_permissions_statementContext showCtx = ctx.show_permissions_statement();
            if (showCtx instanceof ElasticScriptParser.ShowAllPermissionsContext) {
                handler.handleShowAllPermissions(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowPrincipalPermissionsContext) {
                handler.handleShowPrincipalPermissions(
                    (ElasticScriptParser.ShowPrincipalPermissionsContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW PERMISSIONS variant"));
            }
        } else if (ctx.show_roles_statement() != null) {
            ElasticScriptParser.Show_roles_statementContext showCtx = ctx.show_roles_statement();
            if (showCtx instanceof ElasticScriptParser.ShowRoleDetailContext) {
                handler.handleShowRole((ElasticScriptParser.ShowRoleDetailContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW ROLE variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown PERMISSION statement type"));
        }
    }

    /**
     * Handles PROFILE statements (PROFILE CALL, SHOW PROFILE, CLEAR PROFILE, ANALYZE PROFILE).
     */
    private void handleProfileStatement(ElasticScriptParser.Profile_statementContext ctx,
                                       CommonTokenStream tokens,
                                       ActionListener<Object> listener) {
        ProfileStatementHandler handler = new ProfileStatementHandler(client);

        if (ctx.profile_exec_statement() != null) {
            // PROFILE CALL procedure_name(args)
            ElasticScriptParser.Profile_exec_statementContext execCtx = ctx.profile_exec_statement();
            ElasticScriptParser.Call_procedure_statementContext callCtx = execCtx.call_procedure_statement();

            // Get procedure name from call statement
            String procedureName = callCtx.ID().getText();

            // Execute with profiling - record start time
            long startTime = System.currentTimeMillis();

            // Re-construct the CALL statement and execute it using the same path
            String callStatement = tokens.getText(callCtx.getStart(), callCtx.getStop());

            // Execute the call statement using executeProcedure
            executeProcedure(callStatement, java.util.Collections.emptyMap(), ActionListener.wrap(
                result -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    // Build a basic profile result
                    ProfileResult profile = ProfileResult.builder()
                        .procedureName(procedureName)
                        .startTime(java.time.Instant.ofEpochMilli(startTime))
                        .endTime(java.time.Instant.ofEpochMilli(endTime))
                        .totalDurationMs(duration)
                        .build();

                    // Store the profile and return the result
                    handler.storeProfile(profile, ActionListener.wrap(
                        profileResult -> {
                            // Return both the procedure result and profile info
                            java.util.Map<String, Object> response = new java.util.HashMap<>();
                            response.put("result", result);
                            response.put("profile", profileResult);
                            listener.onResponse(response);
                        },
                        listener::onFailure
                    ));
                },
                listener::onFailure
            ));
        } else if (ctx.show_profile_statement() != null) {
            ElasticScriptParser.Show_profile_statementContext showCtx = ctx.show_profile_statement();
            if (showCtx instanceof ElasticScriptParser.ShowAllProfilesContext) {
                handler.handleShowAllProfiles(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowLastProfileContext) {
                handler.handleShowLastProfile(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowProfileForContext) {
                handler.handleShowProfileFor((ElasticScriptParser.ShowProfileForContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW PROFILE variant"));
            }
        } else if (ctx.clear_profile_statement() != null) {
            ElasticScriptParser.Clear_profile_statementContext clearCtx = ctx.clear_profile_statement();
            if (clearCtx instanceof ElasticScriptParser.ClearAllProfilesContext) {
                handler.handleClearAllProfiles(listener);
            } else if (clearCtx instanceof ElasticScriptParser.ClearProfileForContext) {
                handler.handleClearProfileFor((ElasticScriptParser.ClearProfileForContext) clearCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown CLEAR PROFILE variant"));
            }
        } else if (ctx.analyze_profile_statement() != null) {
            ElasticScriptParser.Analyze_profile_statementContext analyzeCtx = ctx.analyze_profile_statement();
            if (analyzeCtx instanceof ElasticScriptParser.AnalyzeLastProfileContext) {
                handler.handleAnalyzeLastProfile(listener);
            } else if (analyzeCtx instanceof ElasticScriptParser.AnalyzeProfileForContext) {
                handler.handleAnalyzeProfileFor((ElasticScriptParser.AnalyzeProfileForContext) analyzeCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown ANALYZE PROFILE variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown PROFILE statement type"));
        }
    }

    /**
     * Handles TYPE statements (CREATE TYPE, DROP TYPE, SHOW TYPES).
     */
    private void handleTypeStatement(ElasticScriptParser.Type_statementContext ctx,
                                    ActionListener<Object> listener) {
        TypeStatementHandler handler = new TypeStatementHandler(client);

        if (ctx.create_type_statement() != null) {
            handler.handleCreateType(ctx.create_type_statement(), listener);
        } else if (ctx.drop_type_statement() != null) {
            handler.handleDropType(ctx.drop_type_statement(), listener);
        } else if (ctx.show_types_statement() != null) {
            ElasticScriptParser.Show_types_statementContext showCtx = ctx.show_types_statement();
            if (showCtx instanceof ElasticScriptParser.ShowAllTypesContext) {
                handler.handleShowAllTypes(listener);
            } else if (showCtx instanceof ElasticScriptParser.ShowTypeDetailContext) {
                handler.handleShowTypeDetail((ElasticScriptParser.ShowTypeDetailContext) showCtx, listener);
            } else {
                listener.onFailure(new IllegalArgumentException("Unknown SHOW TYPE variant"));
            }
        } else {
            listener.onFailure(new IllegalArgumentException("Unknown TYPE statement type"));
        }
    }
}

