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
import org.elasticsearch.xpack.escript.functions.community.FunctionLoader;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.inference.InferenceFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.OpenAIFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.S3Functions;
import org.elasticsearch.xpack.escript.functions.builtin.thirdparty.SlackFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.KubernetesFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.PagerDutyFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.TerraformFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.CICDFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.AWSFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.runbooks.GenericFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.introspection.IntrospectionFunctions;
import org.elasticsearch.xpack.escript.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;
import org.elasticsearch.xpack.escript.visitors.ProcedureDefinitionVisitor;
import org.elasticsearch.xpack.escript.handlers.JobStatementHandler;
import org.elasticsearch.xpack.escript.handlers.TriggerStatementHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElasticScriptExecutor {

    private final ThreadPool threadPool;
    private final Client client;

    private static final Logger LOGGER = LogManager.getLogger(ElasticScriptExecutor.class);

    @Inject
    public ElasticScriptExecutor(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
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
                                EsqlBuiltInFunctions.registerAll(executionContext, procedureExecutor, client);
                                ESFunctions.registerGetDocumentFunction(executionContext, client);
                                ESFunctions.registerUpdateDocumentFunction(executionContext, client);
                                ESFunctions.registerIndexBulkFunction(executionContext, client);
                                ESFunctions.registerIndexDocumentFunction(executionContext, client);
                                ESFunctions.registerRefreshIndexFunction(executionContext, client);
                                StringBuiltInFunctions.registerAll(executionContext);
                                NumberBuiltInFunctions.registerAll(executionContext);
                                ArrayBuiltInFunctions.registerAll(executionContext);
                                DateBuiltInFunctions.registerAll(executionContext);
                                DocumentBuiltInFunctions.registerAll(executionContext);
                                OpenAIFunctions.registerAll(executionContext);
                                SlackFunctions.registerAll(executionContext);
                                S3Functions.registerAll(executionContext);
                                InferenceFunctions.registerAll(executionContext, client);
                                
                                // Runbook integrations
                                KubernetesFunctions.registerAll(executionContext);
                                PagerDutyFunctions.registerAll(executionContext);
                                TerraformFunctions.registerAll(executionContext);
                                CICDFunctions.registerAll(executionContext);
                                AWSFunctions.registerAll(executionContext);
                                GenericFunctions.registerAll(executionContext);
                                
                                // Introspection functions (must be registered last to see all other functions)
                                IntrospectionFunctions.registerAll(executionContext, client);

                                FunctionLoader.loadCommunityFunctions(executionContext);

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
                } else if (programContext.job_statement() != null) {
                    // Handle JOB statements (CREATE JOB, ALTER JOB, DROP JOB, SHOW JOBS)
                    handleJobStatement(programContext.job_statement(), tokens, listener);
                } else if (programContext.trigger_statement() != null) {
                    // Handle TRIGGER statements (CREATE TRIGGER, ALTER TRIGGER, DROP TRIGGER, SHOW TRIGGERS)
                    handleTriggerStatement(programContext.trigger_statement(), tokens, listener);
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
            return nsCtx.namespace_id().getText().toUpperCase() + "_" + nsCtx.ID().getText().toUpperCase();
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
}

