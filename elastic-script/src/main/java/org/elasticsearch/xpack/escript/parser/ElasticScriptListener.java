/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ElasticScriptParser}.
 */
public interface ElasticScriptListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 */
	void enterProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 */
	void exitProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#async_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void enterAsync_procedure_statement(ElasticScriptParser.Async_procedure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#async_procedure_statement}.
	 * @param ctx the parse tree
	 */
	void exitAsync_procedure_statement(ElasticScriptParser.Async_procedure_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code onDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void enterOnDoneContinuation(ElasticScriptParser.OnDoneContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code onDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void exitOnDoneContinuation(ElasticScriptParser.OnDoneContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code onFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void enterOnFailContinuation(ElasticScriptParser.OnFailContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code onFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void exitOnFailContinuation(ElasticScriptParser.OnFailContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code finallyContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void enterFinallyContinuation(ElasticScriptParser.FinallyContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code finallyContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void exitFinallyContinuation(ElasticScriptParser.FinallyContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void enterTrackAsContinuation(ElasticScriptParser.TrackAsContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void exitTrackAsContinuation(ElasticScriptParser.TrackAsContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void enterTimeoutContinuation(ElasticScriptParser.TimeoutContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 */
	void exitTimeoutContinuation(ElasticScriptParser.TimeoutContinuationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#continuation_handler}.
	 * @param ctx the parse tree
	 */
	void enterContinuation_handler(ElasticScriptParser.Continuation_handlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#continuation_handler}.
	 * @param ctx the parse tree
	 */
	void exitContinuation_handler(ElasticScriptParser.Continuation_handlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#continuation_arg_list}.
	 * @param ctx the parse tree
	 */
	void enterContinuation_arg_list(ElasticScriptParser.Continuation_arg_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#continuation_arg_list}.
	 * @param ctx the parse tree
	 */
	void exitContinuation_arg_list(ElasticScriptParser.Continuation_arg_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#continuation_arg}.
	 * @param ctx the parse tree
	 */
	void enterContinuation_arg(ElasticScriptParser.Continuation_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#continuation_arg}.
	 * @param ctx the parse tree
	 */
	void exitContinuation_arg(ElasticScriptParser.Continuation_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#lambda_continuation}.
	 * @param ctx the parse tree
	 */
	void enterLambda_continuation(ElasticScriptParser.Lambda_continuationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#lambda_continuation}.
	 * @param ctx the parse tree
	 */
	void exitLambda_continuation(ElasticScriptParser.Lambda_continuationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#execution_control_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecution_control_statement(ElasticScriptParser.Execution_control_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#execution_control_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecution_control_statement(ElasticScriptParser.Execution_control_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statusOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void enterStatusOperation(ElasticScriptParser.StatusOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statusOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void exitStatusOperation(ElasticScriptParser.StatusOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cancelOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void enterCancelOperation(ElasticScriptParser.CancelOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cancelOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void exitCancelOperation(ElasticScriptParser.CancelOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code retryOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void enterRetryOperation(ElasticScriptParser.RetryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code retryOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void exitRetryOperation(ElasticScriptParser.RetryOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code waitOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void enterWaitOperation(ElasticScriptParser.WaitOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code waitOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 */
	void exitWaitOperation(ElasticScriptParser.WaitOperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parallel_statement}.
	 * @param ctx the parse tree
	 */
	void enterParallel_statement(ElasticScriptParser.Parallel_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parallel_statement}.
	 * @param ctx the parse tree
	 */
	void exitParallel_statement(ElasticScriptParser.Parallel_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parallel_procedure_list}.
	 * @param ctx the parse tree
	 */
	void enterParallel_procedure_list(ElasticScriptParser.Parallel_procedure_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parallel_procedure_list}.
	 * @param ctx the parse tree
	 */
	void exitParallel_procedure_list(ElasticScriptParser.Parallel_procedure_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parallel_procedure_call}.
	 * @param ctx the parse tree
	 */
	void enterParallel_procedure_call(ElasticScriptParser.Parallel_procedure_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parallel_procedure_call}.
	 * @param ctx the parse tree
	 */
	void exitParallel_procedure_call(ElasticScriptParser.Parallel_procedure_callContext ctx);
	/**
	 * Enter a parse tree produced by the {@code onAllDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void enterOnAllDoneContinuation(ElasticScriptParser.OnAllDoneContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code onAllDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void exitOnAllDoneContinuation(ElasticScriptParser.OnAllDoneContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code onAnyFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void enterOnAnyFailContinuation(ElasticScriptParser.OnAnyFailContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code onAnyFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void exitOnAnyFailContinuation(ElasticScriptParser.OnAnyFailContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parallelTrackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void enterParallelTrackAsContinuation(ElasticScriptParser.ParallelTrackAsContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parallelTrackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void exitParallelTrackAsContinuation(ElasticScriptParser.ParallelTrackAsContinuationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parallelTimeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void enterParallelTimeoutContinuation(ElasticScriptParser.ParallelTimeoutContinuationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parallelTimeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 */
	void exitParallelTimeoutContinuation(ElasticScriptParser.ParallelTimeoutContinuationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void enterPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void exitPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void enterBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void exitBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void enterContinue_statement(ElasticScriptParser.Continue_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void exitContinue_statement(ElasticScriptParser.Continue_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#switch_statement}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_statement(ElasticScriptParser.Switch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#switch_statement}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_statement(ElasticScriptParser.Switch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#case_clause}.
	 * @param ctx the parse tree
	 */
	void enterCase_clause(ElasticScriptParser.Case_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#case_clause}.
	 * @param ctx the parse tree
	 */
	void exitCase_clause(ElasticScriptParser.Case_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#default_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_clause(ElasticScriptParser.Default_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#default_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_clause(ElasticScriptParser.Default_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void enterExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 */
	void exitExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void enterVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 */
	void exitVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void enterEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 */
	void exitEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_into_statement}.
	 * @param ctx the parse tree
	 */
	void enterEsql_into_statement(ElasticScriptParser.Esql_into_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_into_statement}.
	 * @param ctx the parse tree
	 */
	void exitEsql_into_statement(ElasticScriptParser.Esql_into_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_process_statement}.
	 * @param ctx the parse tree
	 */
	void enterEsql_process_statement(ElasticScriptParser.Esql_process_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_process_statement}.
	 * @param ctx the parse tree
	 */
	void exitEsql_process_statement(ElasticScriptParser.Esql_process_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#index_command}.
	 * @param ctx the parse tree
	 */
	void enterIndex_command(ElasticScriptParser.Index_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#index_command}.
	 * @param ctx the parse tree
	 */
	void exitIndex_command(ElasticScriptParser.Index_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#index_target}.
	 * @param ctx the parse tree
	 */
	void enterIndex_target(ElasticScriptParser.Index_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#index_target}.
	 * @param ctx the parse tree
	 */
	void exitIndex_target(ElasticScriptParser.Index_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#delete_command}.
	 * @param ctx the parse tree
	 */
	void enterDelete_command(ElasticScriptParser.Delete_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#delete_command}.
	 * @param ctx the parse tree
	 */
	void exitDelete_command(ElasticScriptParser.Delete_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#search_command}.
	 * @param ctx the parse tree
	 */
	void enterSearch_command(ElasticScriptParser.Search_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#search_command}.
	 * @param ctx the parse tree
	 */
	void exitSearch_command(ElasticScriptParser.Search_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#refresh_command}.
	 * @param ctx the parse tree
	 */
	void enterRefresh_command(ElasticScriptParser.Refresh_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#refresh_command}.
	 * @param ctx the parse tree
	 */
	void exitRefresh_command(ElasticScriptParser.Refresh_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_index_command}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_command(ElasticScriptParser.Create_index_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_index_command}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_command(ElasticScriptParser.Create_index_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_index_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_options(ElasticScriptParser.Create_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_index_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_options(ElasticScriptParser.Create_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_binding_type}.
	 * @param ctx the parse tree
	 */
	void enterEsql_binding_type(ElasticScriptParser.Esql_binding_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_binding_type}.
	 * @param ctx the parse tree
	 */
	void exitEsql_binding_type(ElasticScriptParser.Esql_binding_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_binding_query}.
	 * @param ctx the parse tree
	 */
	void enterEsql_binding_query(ElasticScriptParser.Esql_binding_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_binding_query}.
	 * @param ctx the parse tree
	 */
	void exitEsql_binding_query(ElasticScriptParser.Esql_binding_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#esql_binding_content}.
	 * @param ctx the parse tree
	 */
	void enterEsql_binding_content(ElasticScriptParser.Esql_binding_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#esql_binding_content}.
	 * @param ctx the parse tree
	 */
	void exitEsql_binding_content(ElasticScriptParser.Esql_binding_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#var_statement}.
	 * @param ctx the parse tree
	 */
	void enterVar_statement(ElasticScriptParser.Var_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#var_statement}.
	 * @param ctx the parse tree
	 */
	void exitVar_statement(ElasticScriptParser.Var_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#var_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterVar_declaration_list(ElasticScriptParser.Var_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#var_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitVar_declaration_list(ElasticScriptParser.Var_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#var_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVar_declaration(ElasticScriptParser.Var_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#var_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVar_declaration(ElasticScriptParser.Var_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#const_statement}.
	 * @param ctx the parse tree
	 */
	void enterConst_statement(ElasticScriptParser.Const_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#const_statement}.
	 * @param ctx the parse tree
	 */
	void exitConst_statement(ElasticScriptParser.Const_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#const_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterConst_declaration_list(ElasticScriptParser.Const_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#const_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitConst_declaration_list(ElasticScriptParser.Const_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#const_declaration}.
	 * @param ctx the parse tree
	 */
	void enterConst_declaration(ElasticScriptParser.Const_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#const_declaration}.
	 * @param ctx the parse tree
	 */
	void exitConst_declaration(ElasticScriptParser.Const_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#cursor_query}.
	 * @param ctx the parse tree
	 */
	void enterCursor_query(ElasticScriptParser.Cursor_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#cursor_query}.
	 * @param ctx the parse tree
	 */
	void exitCursor_query(ElasticScriptParser.Cursor_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#cursor_query_content}.
	 * @param ctx the parse tree
	 */
	void enterCursor_query_content(ElasticScriptParser.Cursor_query_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#cursor_query_content}.
	 * @param ctx the parse tree
	 */
	void exitCursor_query_content(ElasticScriptParser.Cursor_query_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void enterElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void exitElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#for_esql_loop}.
	 * @param ctx the parse tree
	 */
	void enterFor_esql_loop(ElasticScriptParser.For_esql_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#for_esql_loop}.
	 * @param ctx the parse tree
	 */
	void exitFor_esql_loop(ElasticScriptParser.For_esql_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#inline_esql_query}.
	 * @param ctx the parse tree
	 */
	void enterInline_esql_query(ElasticScriptParser.Inline_esql_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#inline_esql_query}.
	 * @param ctx the parse tree
	 */
	void exitInline_esql_query(ElasticScriptParser.Inline_esql_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#inline_esql_content}.
	 * @param ctx the parse tree
	 */
	void enterInline_esql_content(ElasticScriptParser.Inline_esql_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#inline_esql_content}.
	 * @param ctx the parse tree
	 */
	void exitInline_esql_content(ElasticScriptParser.Inline_esql_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void enterWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 */
	void exitWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void enterArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 */
	void exitArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void enterTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void exitTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void enterThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void exitThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void enterFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 */
	void exitFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void enterParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void exitParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void enterArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 */
	void exitArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpression(ElasticScriptParser.TernaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpression(ElasticScriptParser.TernaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#nullCoalesceExpression}.
	 * @param ctx the parse tree
	 */
	void enterNullCoalesceExpression(ElasticScriptParser.NullCoalesceExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#nullCoalesceExpression}.
	 * @param ctx the parse tree
	 */
	void exitNullCoalesceExpression(ElasticScriptParser.NullCoalesceExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpr(ElasticScriptParser.ComparisonExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpr(ElasticScriptParser.ComparisonExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterIsNullExpr(ElasticScriptParser.IsNullExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitIsNullExpr(ElasticScriptParser.IsNullExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNotNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterIsNotNullExpr(ElasticScriptParser.IsNotNullExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNotNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitIsNotNullExpr(ElasticScriptParser.IsNotNullExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterInListExpr(ElasticScriptParser.InListExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitInListExpr(ElasticScriptParser.InListExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notInListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotInListExpr(ElasticScriptParser.NotInListExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notInListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotInListExpr(ElasticScriptParser.NotInListExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterInArrayExpr(ElasticScriptParser.InArrayExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitInArrayExpr(ElasticScriptParser.InArrayExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notInArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterNotInArrayExpr(ElasticScriptParser.NotInArrayExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notInArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitNotInArrayExpr(ElasticScriptParser.NotInArrayExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void enterDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 */
	void exitDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 */
	void enterDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 */
	void exitDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 */
	void enterPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 */
	void exitPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 */
	void enterPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 */
	void exitPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#accessExpression}.
	 * @param ctx the parse tree
	 */
	void enterAccessExpression(ElasticScriptParser.AccessExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#accessExpression}.
	 * @param ctx the parse tree
	 */
	void exitAccessExpression(ElasticScriptParser.AccessExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 */
	void enterBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 */
	void exitBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#safeNavExpression}.
	 * @param ctx the parse tree
	 */
	void enterSafeNavExpression(ElasticScriptParser.SafeNavExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#safeNavExpression}.
	 * @param ctx the parse tree
	 */
	void exitSafeNavExpression(ElasticScriptParser.SafeNavExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpression(ElasticScriptParser.LambdaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpression(ElasticScriptParser.LambdaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#lambdaParamList}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParamList(ElasticScriptParser.LambdaParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#lambdaParamList}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParamList(ElasticScriptParser.LambdaParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 */
	void enterVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 */
	void exitVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void enterArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 */
	void exitArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void enterPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 */
	void exitPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 */
	void enterSeverity(ElasticScriptParser.SeverityContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 */
	void exitSeverity(ElasticScriptParser.SeverityContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#define_intent_statement}.
	 * @param ctx the parse tree
	 */
	void enterDefine_intent_statement(ElasticScriptParser.Define_intent_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#define_intent_statement}.
	 * @param ctx the parse tree
	 */
	void exitDefine_intent_statement(ElasticScriptParser.Define_intent_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#requires_clause}.
	 * @param ctx the parse tree
	 */
	void enterRequires_clause(ElasticScriptParser.Requires_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#requires_clause}.
	 * @param ctx the parse tree
	 */
	void exitRequires_clause(ElasticScriptParser.Requires_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#requires_condition}.
	 * @param ctx the parse tree
	 */
	void enterRequires_condition(ElasticScriptParser.Requires_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#requires_condition}.
	 * @param ctx the parse tree
	 */
	void exitRequires_condition(ElasticScriptParser.Requires_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#actions_clause}.
	 * @param ctx the parse tree
	 */
	void enterActions_clause(ElasticScriptParser.Actions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#actions_clause}.
	 * @param ctx the parse tree
	 */
	void exitActions_clause(ElasticScriptParser.Actions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#on_failure_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_failure_clause(ElasticScriptParser.On_failure_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#on_failure_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_failure_clause(ElasticScriptParser.On_failure_clauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intentCallWithArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 */
	void enterIntentCallWithArgs(ElasticScriptParser.IntentCallWithArgsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intentCallWithArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 */
	void exitIntentCallWithArgs(ElasticScriptParser.IntentCallWithArgsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intentCallWithNamedArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 */
	void enterIntentCallWithNamedArgs(ElasticScriptParser.IntentCallWithNamedArgsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intentCallWithNamedArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 */
	void exitIntentCallWithNamedArgs(ElasticScriptParser.IntentCallWithNamedArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#intent_named_args}.
	 * @param ctx the parse tree
	 */
	void enterIntent_named_args(ElasticScriptParser.Intent_named_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#intent_named_args}.
	 * @param ctx the parse tree
	 */
	void exitIntent_named_args(ElasticScriptParser.Intent_named_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#intent_named_arg}.
	 * @param ctx the parse tree
	 */
	void enterIntent_named_arg(ElasticScriptParser.Intent_named_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#intent_named_arg}.
	 * @param ctx the parse tree
	 */
	void exitIntent_named_arg(ElasticScriptParser.Intent_named_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#job_statement}.
	 * @param ctx the parse tree
	 */
	void enterJob_statement(ElasticScriptParser.Job_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#job_statement}.
	 * @param ctx the parse tree
	 */
	void exitJob_statement(ElasticScriptParser.Job_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_job_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_job_statement(ElasticScriptParser.Create_job_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_job_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_job_statement(ElasticScriptParser.Create_job_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterJobEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterJobEnableDisable(ElasticScriptParser.AlterJobEnableDisableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterJobEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterJobEnableDisable(ElasticScriptParser.AlterJobEnableDisableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterJobSchedule}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterJobSchedule(ElasticScriptParser.AlterJobScheduleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterJobSchedule}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterJobSchedule(ElasticScriptParser.AlterJobScheduleContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_job_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_job_statement(ElasticScriptParser.Drop_job_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_job_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_job_statement(ElasticScriptParser.Drop_job_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllJobs}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllJobs(ElasticScriptParser.ShowAllJobsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllJobs}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllJobs(ElasticScriptParser.ShowAllJobsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showJobDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowJobDetail(ElasticScriptParser.ShowJobDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showJobDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowJobDetail(ElasticScriptParser.ShowJobDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showJobRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowJobRuns(ElasticScriptParser.ShowJobRunsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showJobRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowJobRuns(ElasticScriptParser.ShowJobRunsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#trigger_statement}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_statement(ElasticScriptParser.Trigger_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#trigger_statement}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_statement(ElasticScriptParser.Trigger_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_trigger_statement(ElasticScriptParser.Create_trigger_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_trigger_statement(ElasticScriptParser.Create_trigger_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expression(ElasticScriptParser.Interval_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expression(ElasticScriptParser.Interval_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterTriggerEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTriggerEnableDisable(ElasticScriptParser.AlterTriggerEnableDisableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterTriggerEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTriggerEnableDisable(ElasticScriptParser.AlterTriggerEnableDisableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterTriggerInterval}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTriggerInterval(ElasticScriptParser.AlterTriggerIntervalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterTriggerInterval}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTriggerInterval(ElasticScriptParser.AlterTriggerIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger_statement(ElasticScriptParser.Drop_trigger_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_trigger_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger_statement(ElasticScriptParser.Drop_trigger_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllTriggers}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllTriggers(ElasticScriptParser.ShowAllTriggersContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllTriggers}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllTriggers(ElasticScriptParser.ShowAllTriggersContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTriggerDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTriggerDetail(ElasticScriptParser.ShowTriggerDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTriggerDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTriggerDetail(ElasticScriptParser.ShowTriggerDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTriggerRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTriggerRuns(ElasticScriptParser.ShowTriggerRunsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTriggerRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTriggerRuns(ElasticScriptParser.ShowTriggerRunsContext ctx);
}