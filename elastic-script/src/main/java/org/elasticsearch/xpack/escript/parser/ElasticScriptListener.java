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
	 * Enter a parse tree produced by {@link ElasticScriptParser#authid_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuthid_clause(ElasticScriptParser.Authid_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#authid_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuthid_clause(ElasticScriptParser.Authid_clauseContext ctx);
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
	 * Enter a parse tree produced by the {@code showAllProcedures}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllProcedures(ElasticScriptParser.ShowAllProceduresContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllProcedures}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllProcedures(ElasticScriptParser.ShowAllProceduresContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showProcedureDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowProcedureDetail(ElasticScriptParser.ShowProcedureDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showProcedureDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowProcedureDetail(ElasticScriptParser.ShowProcedureDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_function_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_statement(ElasticScriptParser.Create_function_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_function_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_statement(ElasticScriptParser.Create_function_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#delete_function_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_function_statement(ElasticScriptParser.Delete_function_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#delete_function_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_function_statement(ElasticScriptParser.Delete_function_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllFunctions}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllFunctions(ElasticScriptParser.ShowAllFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllFunctions}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllFunctions(ElasticScriptParser.ShowAllFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctionDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctionDetail(ElasticScriptParser.ShowFunctionDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctionDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctionDetail(ElasticScriptParser.ShowFunctionDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#return_type}.
	 * @param ctx the parse tree
	 */
	void enterReturn_type(ElasticScriptParser.Return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#return_type}.
	 * @param ctx the parse tree
	 */
	void exitReturn_type(ElasticScriptParser.Return_typeContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#execute_immediate_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute_immediate_statement(ElasticScriptParser.Execute_immediate_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#execute_immediate_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute_immediate_statement(ElasticScriptParser.Execute_immediate_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#id_list}.
	 * @param ctx the parse tree
	 */
	void enterId_list(ElasticScriptParser.Id_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#id_list}.
	 * @param ctx the parse tree
	 */
	void exitId_list(ElasticScriptParser.Id_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#expression_list}.
	 * @param ctx the parse tree
	 */
	void enterExpression_list(ElasticScriptParser.Expression_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#expression_list}.
	 * @param ctx the parse tree
	 */
	void exitExpression_list(ElasticScriptParser.Expression_listContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#open_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_cursor_statement(ElasticScriptParser.Open_cursor_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#open_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_cursor_statement(ElasticScriptParser.Open_cursor_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#close_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void enterClose_cursor_statement(ElasticScriptParser.Close_cursor_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#close_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void exitClose_cursor_statement(ElasticScriptParser.Close_cursor_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#fetch_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void enterFetch_cursor_statement(ElasticScriptParser.Fetch_cursor_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#fetch_cursor_statement}.
	 * @param ctx the parse tree
	 */
	void exitFetch_cursor_statement(ElasticScriptParser.Fetch_cursor_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void enterForall_statement(ElasticScriptParser.Forall_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void exitForall_statement(ElasticScriptParser.Forall_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#forall_action}.
	 * @param ctx the parse tree
	 */
	void enterForall_action(ElasticScriptParser.Forall_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#forall_action}.
	 * @param ctx the parse tree
	 */
	void exitForall_action(ElasticScriptParser.Forall_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#save_exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void enterSave_exceptions_clause(ElasticScriptParser.Save_exceptions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#save_exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void exitSave_exceptions_clause(ElasticScriptParser.Save_exceptions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#bulk_collect_statement}.
	 * @param ctx the parse tree
	 */
	void enterBulk_collect_statement(ElasticScriptParser.Bulk_collect_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#bulk_collect_statement}.
	 * @param ctx the parse tree
	 */
	void exitBulk_collect_statement(ElasticScriptParser.Bulk_collect_statementContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#catch_block}.
	 * @param ctx the parse tree
	 */
	void enterCatch_block(ElasticScriptParser.Catch_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#catch_block}.
	 * @param ctx the parse tree
	 */
	void exitCatch_block(ElasticScriptParser.Catch_blockContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#namespaced_function_call}.
	 * @param ctx the parse tree
	 */
	void enterNamespaced_function_call(ElasticScriptParser.Namespaced_function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#namespaced_function_call}.
	 * @param ctx the parse tree
	 */
	void exitNamespaced_function_call(ElasticScriptParser.Namespaced_function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#method_name}.
	 * @param ctx the parse tree
	 */
	void enterMethod_name(ElasticScriptParser.Method_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#method_name}.
	 * @param ctx the parse tree
	 */
	void exitMethod_name(ElasticScriptParser.Method_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#namespace_id}.
	 * @param ctx the parse tree
	 */
	void enterNamespace_id(ElasticScriptParser.Namespace_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#namespace_id}.
	 * @param ctx the parse tree
	 */
	void exitNamespace_id(ElasticScriptParser.Namespace_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#simple_function_call}.
	 * @param ctx the parse tree
	 */
	void enterSimple_function_call(ElasticScriptParser.Simple_function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#simple_function_call}.
	 * @param ctx the parse tree
	 */
	void exitSimple_function_call(ElasticScriptParser.Simple_function_callContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#mapLiteral}.
	 * @param ctx the parse tree
	 */
	void enterMapLiteral(ElasticScriptParser.MapLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#mapLiteral}.
	 * @param ctx the parse tree
	 */
	void exitMapLiteral(ElasticScriptParser.MapLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#mapEntry}.
	 * @param ctx the parse tree
	 */
	void enterMapEntry(ElasticScriptParser.MapEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#mapEntry}.
	 * @param ctx the parse tree
	 */
	void exitMapEntry(ElasticScriptParser.MapEntryContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#cursorAttribute}.
	 * @param ctx the parse tree
	 */
	void enterCursorAttribute(ElasticScriptParser.CursorAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#cursorAttribute}.
	 * @param ctx the parse tree
	 */
	void exitCursorAttribute(ElasticScriptParser.CursorAttributeContext ctx);
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
	 * Enter a parse tree produced by {@link ElasticScriptParser#map_datatype}.
	 * @param ctx the parse tree
	 */
	void enterMap_datatype(ElasticScriptParser.Map_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#map_datatype}.
	 * @param ctx the parse tree
	 */
	void exitMap_datatype(ElasticScriptParser.Map_datatypeContext ctx);
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
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_statement}.
	 * @param ctx the parse tree
	 */
	void enterPackage_statement(ElasticScriptParser.Package_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_statement}.
	 * @param ctx the parse tree
	 */
	void exitPackage_statement(ElasticScriptParser.Package_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_package_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_statement(ElasticScriptParser.Create_package_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_package_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_statement(ElasticScriptParser.Create_package_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_spec_item}.
	 * @param ctx the parse tree
	 */
	void enterPackage_spec_item(ElasticScriptParser.Package_spec_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_spec_item}.
	 * @param ctx the parse tree
	 */
	void exitPackage_spec_item(ElasticScriptParser.Package_spec_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_procedure_spec(ElasticScriptParser.Package_procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_procedure_spec(ElasticScriptParser.Package_procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_function_spec(ElasticScriptParser.Package_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_function_spec(ElasticScriptParser.Package_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_variable_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_variable_spec(ElasticScriptParser.Package_variable_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_variable_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_variable_spec(ElasticScriptParser.Package_variable_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_package_body_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_body_statement(ElasticScriptParser.Create_package_body_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_package_body_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_body_statement(ElasticScriptParser.Create_package_body_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_body_item}.
	 * @param ctx the parse tree
	 */
	void enterPackage_body_item(ElasticScriptParser.Package_body_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_body_item}.
	 * @param ctx the parse tree
	 */
	void exitPackage_body_item(ElasticScriptParser.Package_body_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_procedure_impl}.
	 * @param ctx the parse tree
	 */
	void enterPackage_procedure_impl(ElasticScriptParser.Package_procedure_implContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_procedure_impl}.
	 * @param ctx the parse tree
	 */
	void exitPackage_procedure_impl(ElasticScriptParser.Package_procedure_implContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#package_function_impl}.
	 * @param ctx the parse tree
	 */
	void enterPackage_function_impl(ElasticScriptParser.Package_function_implContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#package_function_impl}.
	 * @param ctx the parse tree
	 */
	void exitPackage_function_impl(ElasticScriptParser.Package_function_implContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_package_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_package_statement(ElasticScriptParser.Drop_package_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_package_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_package_statement(ElasticScriptParser.Drop_package_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showPackageDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_packages_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowPackageDetail(ElasticScriptParser.ShowPackageDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showPackageDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_packages_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowPackageDetail(ElasticScriptParser.ShowPackageDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#permission_statement}.
	 * @param ctx the parse tree
	 */
	void enterPermission_statement(ElasticScriptParser.Permission_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#permission_statement}.
	 * @param ctx the parse tree
	 */
	void exitPermission_statement(ElasticScriptParser.Permission_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant_statement(ElasticScriptParser.Grant_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant_statement(ElasticScriptParser.Grant_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#revoke_statement}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_statement(ElasticScriptParser.Revoke_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#revoke_statement}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_statement(ElasticScriptParser.Revoke_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#privilege_list}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege_list(ElasticScriptParser.Privilege_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#privilege_list}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege_list(ElasticScriptParser.Privilege_listContext ctx);
	/**
	 * Enter a parse tree produced by the {@code executePrivilege}
	 * labeled alternative in {@link ElasticScriptParser#privilege}.
	 * @param ctx the parse tree
	 */
	void enterExecutePrivilege(ElasticScriptParser.ExecutePrivilegeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code executePrivilege}
	 * labeled alternative in {@link ElasticScriptParser#privilege}.
	 * @param ctx the parse tree
	 */
	void exitExecutePrivilege(ElasticScriptParser.ExecutePrivilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#object_type}.
	 * @param ctx the parse tree
	 */
	void enterObject_type(ElasticScriptParser.Object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#object_type}.
	 * @param ctx the parse tree
	 */
	void exitObject_type(ElasticScriptParser.Object_typeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterRolePrincipal(ElasticScriptParser.RolePrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitRolePrincipal(ElasticScriptParser.RolePrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterUserPrincipal(ElasticScriptParser.UserPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitUserPrincipal(ElasticScriptParser.UserPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_role_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_role_statement(ElasticScriptParser.Create_role_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_role_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_role_statement(ElasticScriptParser.Create_role_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_role_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_role_statement(ElasticScriptParser.Drop_role_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_role_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_role_statement(ElasticScriptParser.Drop_role_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllPermissions(ElasticScriptParser.ShowAllPermissionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllPermissions(ElasticScriptParser.ShowAllPermissionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showPrincipalPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowPrincipalPermissions(ElasticScriptParser.ShowPrincipalPermissionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showPrincipalPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowPrincipalPermissions(ElasticScriptParser.ShowPrincipalPermissionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoleDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_roles_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoleDetail(ElasticScriptParser.ShowRoleDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoleDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_roles_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoleDetail(ElasticScriptParser.ShowRoleDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterProfile_statement(ElasticScriptParser.Profile_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitProfile_statement(ElasticScriptParser.Profile_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#profile_exec_statement}.
	 * @param ctx the parse tree
	 */
	void enterProfile_exec_statement(ElasticScriptParser.Profile_exec_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#profile_exec_statement}.
	 * @param ctx the parse tree
	 */
	void exitProfile_exec_statement(ElasticScriptParser.Profile_exec_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllProfiles(ElasticScriptParser.ShowAllProfilesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllProfiles(ElasticScriptParser.ShowAllProfilesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowLastProfile(ElasticScriptParser.ShowLastProfileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowLastProfile(ElasticScriptParser.ShowLastProfileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowProfileFor(ElasticScriptParser.ShowProfileForContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowProfileFor(ElasticScriptParser.ShowProfileForContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterClearAllProfiles(ElasticScriptParser.ClearAllProfilesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitClearAllProfiles(ElasticScriptParser.ClearAllProfilesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterClearProfileFor(ElasticScriptParser.ClearProfileForContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitClearProfileFor(ElasticScriptParser.ClearProfileForContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyzeLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeLastProfile(ElasticScriptParser.AnalyzeLastProfileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyzeLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeLastProfile(ElasticScriptParser.AnalyzeLastProfileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyzeProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeProfileFor(ElasticScriptParser.AnalyzeProfileForContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyzeProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeProfileFor(ElasticScriptParser.AnalyzeProfileForContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#type_statement}.
	 * @param ctx the parse tree
	 */
	void enterType_statement(ElasticScriptParser.Type_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#type_statement}.
	 * @param ctx the parse tree
	 */
	void exitType_statement(ElasticScriptParser.Type_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_type_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_type_statement(ElasticScriptParser.Create_type_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_type_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_type_statement(ElasticScriptParser.Create_type_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#type_field_list}.
	 * @param ctx the parse tree
	 */
	void enterType_field_list(ElasticScriptParser.Type_field_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#type_field_list}.
	 * @param ctx the parse tree
	 */
	void exitType_field_list(ElasticScriptParser.Type_field_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#type_field}.
	 * @param ctx the parse tree
	 */
	void enterType_field(ElasticScriptParser.Type_fieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#type_field}.
	 * @param ctx the parse tree
	 */
	void exitType_field(ElasticScriptParser.Type_fieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_type_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type_statement(ElasticScriptParser.Drop_type_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_type_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type_statement(ElasticScriptParser.Drop_type_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllTypes}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllTypes(ElasticScriptParser.ShowAllTypesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllTypes}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllTypes(ElasticScriptParser.ShowAllTypesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTypeDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTypeDetail(ElasticScriptParser.ShowTypeDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTypeDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTypeDetail(ElasticScriptParser.ShowTypeDetailContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#application_statement}.
	 * @param ctx the parse tree
	 */
	void enterApplication_statement(ElasticScriptParser.Application_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#application_statement}.
	 * @param ctx the parse tree
	 */
	void exitApplication_statement(ElasticScriptParser.Application_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_application_statement(ElasticScriptParser.Create_application_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_application_statement(ElasticScriptParser.Create_application_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#application_section}.
	 * @param ctx the parse tree
	 */
	void enterApplication_section(ElasticScriptParser.Application_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#application_section}.
	 * @param ctx the parse tree
	 */
	void exitApplication_section(ElasticScriptParser.Application_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#sources_section}.
	 * @param ctx the parse tree
	 */
	void enterSources_section(ElasticScriptParser.Sources_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#sources_section}.
	 * @param ctx the parse tree
	 */
	void exitSources_section(ElasticScriptParser.Sources_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#source_definition}.
	 * @param ctx the parse tree
	 */
	void enterSource_definition(ElasticScriptParser.Source_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#source_definition}.
	 * @param ctx the parse tree
	 */
	void exitSource_definition(ElasticScriptParser.Source_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skills_section}.
	 * @param ctx the parse tree
	 */
	void enterSkills_section(ElasticScriptParser.Skills_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skills_section}.
	 * @param ctx the parse tree
	 */
	void exitSkills_section(ElasticScriptParser.Skills_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_definition}.
	 * @param ctx the parse tree
	 */
	void enterSkill_definition(ElasticScriptParser.Skill_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_definition}.
	 * @param ctx the parse tree
	 */
	void exitSkill_definition(ElasticScriptParser.Skill_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#intents_section}.
	 * @param ctx the parse tree
	 */
	void enterIntents_section(ElasticScriptParser.Intents_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#intents_section}.
	 * @param ctx the parse tree
	 */
	void exitIntents_section(ElasticScriptParser.Intents_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#intent_mapping}.
	 * @param ctx the parse tree
	 */
	void enterIntent_mapping(ElasticScriptParser.Intent_mappingContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#intent_mapping}.
	 * @param ctx the parse tree
	 */
	void exitIntent_mapping(ElasticScriptParser.Intent_mappingContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#jobs_section}.
	 * @param ctx the parse tree
	 */
	void enterJobs_section(ElasticScriptParser.Jobs_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#jobs_section}.
	 * @param ctx the parse tree
	 */
	void exitJobs_section(ElasticScriptParser.Jobs_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#job_definition}.
	 * @param ctx the parse tree
	 */
	void enterJob_definition(ElasticScriptParser.Job_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#job_definition}.
	 * @param ctx the parse tree
	 */
	void exitJob_definition(ElasticScriptParser.Job_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#triggers_section}.
	 * @param ctx the parse tree
	 */
	void enterTriggers_section(ElasticScriptParser.Triggers_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#triggers_section}.
	 * @param ctx the parse tree
	 */
	void exitTriggers_section(ElasticScriptParser.Triggers_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#trigger_definition}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_definition(ElasticScriptParser.Trigger_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#trigger_definition}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_definition(ElasticScriptParser.Trigger_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#install_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterInstall_application_statement(ElasticScriptParser.Install_application_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#install_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitInstall_application_statement(ElasticScriptParser.Install_application_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#config_item}.
	 * @param ctx the parse tree
	 */
	void enterConfig_item(ElasticScriptParser.Config_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#config_item}.
	 * @param ctx the parse tree
	 */
	void exitConfig_item(ElasticScriptParser.Config_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_application_statement(ElasticScriptParser.Drop_application_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_application_statement(ElasticScriptParser.Drop_application_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterApplicationConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterApplicationConfig(ElasticScriptParser.AlterApplicationConfigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterApplicationConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterApplicationConfig(ElasticScriptParser.AlterApplicationConfigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterApplicationEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterApplicationEnableDisable(ElasticScriptParser.AlterApplicationEnableDisableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterApplicationEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterApplicationEnableDisable(ElasticScriptParser.AlterApplicationEnableDisableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllApplications}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllApplications(ElasticScriptParser.ShowAllApplicationsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllApplications}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllApplications(ElasticScriptParser.ShowAllApplicationsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showApplicationDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowApplicationDetail(ElasticScriptParser.ShowApplicationDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showApplicationDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowApplicationDetail(ElasticScriptParser.ShowApplicationDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showApplicationSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowApplicationSkills(ElasticScriptParser.ShowApplicationSkillsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showApplicationSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowApplicationSkills(ElasticScriptParser.ShowApplicationSkillsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showApplicationIntents}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowApplicationIntents(ElasticScriptParser.ShowApplicationIntentsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showApplicationIntents}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowApplicationIntents(ElasticScriptParser.ShowApplicationIntentsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showApplicationHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowApplicationHistory(ElasticScriptParser.ShowApplicationHistoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showApplicationHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowApplicationHistory(ElasticScriptParser.ShowApplicationHistoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extendApplicationAdd}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterExtendApplicationAdd(ElasticScriptParser.ExtendApplicationAddContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extendApplicationAdd}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitExtendApplicationAdd(ElasticScriptParser.ExtendApplicationAddContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extendApplicationRemove}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 */
	void enterExtendApplicationRemove(ElasticScriptParser.ExtendApplicationRemoveContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extendApplicationRemove}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 */
	void exitExtendApplicationRemove(ElasticScriptParser.ExtendApplicationRemoveContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void enterAddSkillExtension(ElasticScriptParser.AddSkillExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void exitAddSkillExtension(ElasticScriptParser.AddSkillExtensionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void enterAddIntentExtension(ElasticScriptParser.AddIntentExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void exitAddIntentExtension(ElasticScriptParser.AddIntentExtensionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void enterAddSourceExtension(ElasticScriptParser.AddSourceExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 */
	void exitAddSourceExtension(ElasticScriptParser.AddSourceExtensionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code removeSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void enterRemoveSkillExtension(ElasticScriptParser.RemoveSkillExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code removeSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void exitRemoveSkillExtension(ElasticScriptParser.RemoveSkillExtensionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code removeIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void enterRemoveIntentExtension(ElasticScriptParser.RemoveIntentExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code removeIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void exitRemoveIntentExtension(ElasticScriptParser.RemoveIntentExtensionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code removeSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void enterRemoveSourceExtension(ElasticScriptParser.RemoveSourceExtensionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code removeSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 */
	void exitRemoveSourceExtension(ElasticScriptParser.RemoveSourceExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#application_control_statement}.
	 * @param ctx the parse tree
	 */
	void enterApplication_control_statement(ElasticScriptParser.Application_control_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#application_control_statement}.
	 * @param ctx the parse tree
	 */
	void exitApplication_control_statement(ElasticScriptParser.Application_control_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code appStatusOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void enterAppStatusOperation(ElasticScriptParser.AppStatusOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code appStatusOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void exitAppStatusOperation(ElasticScriptParser.AppStatusOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code appPauseOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void enterAppPauseOperation(ElasticScriptParser.AppPauseOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code appPauseOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void exitAppPauseOperation(ElasticScriptParser.AppPauseOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code appResumeOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void enterAppResumeOperation(ElasticScriptParser.AppResumeOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code appResumeOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void exitAppResumeOperation(ElasticScriptParser.AppResumeOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code appHistoryOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void enterAppHistoryOperation(ElasticScriptParser.AppHistoryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code appHistoryOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 */
	void exitAppHistoryOperation(ElasticScriptParser.AppHistoryOperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterSkill_statement(ElasticScriptParser.Skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitSkill_statement(ElasticScriptParser.Skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_skill_statement(ElasticScriptParser.Create_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_skill_statement(ElasticScriptParser.Create_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterSkill_parameters_clause(ElasticScriptParser.Skill_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitSkill_parameters_clause(ElasticScriptParser.Skill_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_param_list}.
	 * @param ctx the parse tree
	 */
	void enterSkill_param_list(ElasticScriptParser.Skill_param_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_param_list}.
	 * @param ctx the parse tree
	 */
	void exitSkill_param_list(ElasticScriptParser.Skill_param_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_param}.
	 * @param ctx the parse tree
	 */
	void enterSkill_param(ElasticScriptParser.Skill_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_param}.
	 * @param ctx the parse tree
	 */
	void exitSkill_param(ElasticScriptParser.Skill_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_skill_pack_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_skill_pack_statement(ElasticScriptParser.Create_skill_pack_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_skill_pack_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_skill_pack_statement(ElasticScriptParser.Create_skill_pack_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_skill_statement(ElasticScriptParser.Drop_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_skill_statement(ElasticScriptParser.Drop_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllSkills(ElasticScriptParser.ShowAllSkillsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllSkills(ElasticScriptParser.ShowAllSkillsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSkillDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSkillDetail(ElasticScriptParser.ShowSkillDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSkillDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSkillDetail(ElasticScriptParser.ShowSkillDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSkillVersion}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSkillVersion(ElasticScriptParser.ShowSkillVersionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSkillVersion}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSkillVersion(ElasticScriptParser.ShowSkillVersionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSkillPackDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSkillPackDetail(ElasticScriptParser.ShowSkillPackDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSkillPackDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSkillPackDetail(ElasticScriptParser.ShowSkillPackDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllSkillPacks}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllSkillPacks(ElasticScriptParser.ShowAllSkillPacksContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllSkillPacks}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllSkillPacks(ElasticScriptParser.ShowAllSkillPacksContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#alter_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlter_skill_statement(ElasticScriptParser.Alter_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#alter_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlter_skill_statement(ElasticScriptParser.Alter_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_property}.
	 * @param ctx the parse tree
	 */
	void enterSkill_property(ElasticScriptParser.Skill_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_property}.
	 * @param ctx the parse tree
	 */
	void exitSkill_property(ElasticScriptParser.Skill_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#test_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterTest_skill_statement(ElasticScriptParser.Test_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#test_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitTest_skill_statement(ElasticScriptParser.Test_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_test_args}.
	 * @param ctx the parse tree
	 */
	void enterSkill_test_args(ElasticScriptParser.Skill_test_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_test_args}.
	 * @param ctx the parse tree
	 */
	void exitSkill_test_args(ElasticScriptParser.Skill_test_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#skill_test_arg}.
	 * @param ctx the parse tree
	 */
	void enterSkill_test_arg(ElasticScriptParser.Skill_test_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#skill_test_arg}.
	 * @param ctx the parse tree
	 */
	void exitSkill_test_arg(ElasticScriptParser.Skill_test_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#generate_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterGenerate_skill_statement(ElasticScriptParser.Generate_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#generate_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitGenerate_skill_statement(ElasticScriptParser.Generate_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#run_skill_statement}.
	 * @param ctx the parse tree
	 */
	void enterRun_skill_statement(ElasticScriptParser.Run_skill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#run_skill_statement}.
	 * @param ctx the parse tree
	 */
	void exitRun_skill_statement(ElasticScriptParser.Run_skill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterConnector_statement(ElasticScriptParser.Connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitConnector_statement(ElasticScriptParser.Connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_connector_statement(ElasticScriptParser.Create_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_connector_statement(ElasticScriptParser.Create_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_connector_statement(ElasticScriptParser.Drop_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_connector_statement(ElasticScriptParser.Drop_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllConnectors}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllConnectors(ElasticScriptParser.ShowAllConnectorsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllConnectors}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllConnectors(ElasticScriptParser.ShowAllConnectorsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showConnectorDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowConnectorDetail(ElasticScriptParser.ShowConnectorDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showConnectorDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowConnectorDetail(ElasticScriptParser.ShowConnectorDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showConnectorStatus}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowConnectorStatus(ElasticScriptParser.ShowConnectorStatusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showConnectorStatus}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowConnectorStatus(ElasticScriptParser.ShowConnectorStatusContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#test_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterTest_connector_statement(ElasticScriptParser.Test_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#test_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitTest_connector_statement(ElasticScriptParser.Test_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#sync_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterSync_connector_statement(ElasticScriptParser.Sync_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#sync_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitSync_connector_statement(ElasticScriptParser.Sync_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#connector_entity_ref}.
	 * @param ctx the parse tree
	 */
	void enterConnector_entity_ref(ElasticScriptParser.Connector_entity_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#connector_entity_ref}.
	 * @param ctx the parse tree
	 */
	void exitConnector_entity_ref(ElasticScriptParser.Connector_entity_refContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterConnectorOptions}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterConnectorOptions(ElasticScriptParser.AlterConnectorOptionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterConnectorOptions}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterConnectorOptions(ElasticScriptParser.AlterConnectorOptionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterConnectorEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterConnectorEnableDisable(ElasticScriptParser.AlterConnectorEnableDisableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterConnectorEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterConnectorEnableDisable(ElasticScriptParser.AlterConnectorEnableDisableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#exec_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterExec_connector_statement(ElasticScriptParser.Exec_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#exec_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitExec_connector_statement(ElasticScriptParser.Exec_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#connector_action_call}.
	 * @param ctx the parse tree
	 */
	void enterConnector_action_call(ElasticScriptParser.Connector_action_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#connector_action_call}.
	 * @param ctx the parse tree
	 */
	void exitConnector_action_call(ElasticScriptParser.Connector_action_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#connector_args}.
	 * @param ctx the parse tree
	 */
	void enterConnector_args(ElasticScriptParser.Connector_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#connector_args}.
	 * @param ctx the parse tree
	 */
	void exitConnector_args(ElasticScriptParser.Connector_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#connector_arg}.
	 * @param ctx the parse tree
	 */
	void enterConnector_arg(ElasticScriptParser.Connector_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#connector_arg}.
	 * @param ctx the parse tree
	 */
	void exitConnector_arg(ElasticScriptParser.Connector_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#query_connector_statement}.
	 * @param ctx the parse tree
	 */
	void enterQuery_connector_statement(ElasticScriptParser.Query_connector_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#query_connector_statement}.
	 * @param ctx the parse tree
	 */
	void exitQuery_connector_statement(ElasticScriptParser.Query_connector_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterAgent_statement(ElasticScriptParser.Agent_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitAgent_statement(ElasticScriptParser.Agent_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#create_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_agent_statement(ElasticScriptParser.Create_agent_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#create_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_agent_statement(ElasticScriptParser.Create_agent_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_skill_list}.
	 * @param ctx the parse tree
	 */
	void enterAgent_skill_list(ElasticScriptParser.Agent_skill_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_skill_list}.
	 * @param ctx the parse tree
	 */
	void exitAgent_skill_list(ElasticScriptParser.Agent_skill_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_skill_ref}.
	 * @param ctx the parse tree
	 */
	void enterAgent_skill_ref(ElasticScriptParser.Agent_skill_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_skill_ref}.
	 * @param ctx the parse tree
	 */
	void exitAgent_skill_ref(ElasticScriptParser.Agent_skill_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_execution_mode}.
	 * @param ctx the parse tree
	 */
	void enterAgent_execution_mode(ElasticScriptParser.Agent_execution_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_execution_mode}.
	 * @param ctx the parse tree
	 */
	void exitAgent_execution_mode(ElasticScriptParser.Agent_execution_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_trigger_list}.
	 * @param ctx the parse tree
	 */
	void enterAgent_trigger_list(ElasticScriptParser.Agent_trigger_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_trigger_list}.
	 * @param ctx the parse tree
	 */
	void exitAgent_trigger_list(ElasticScriptParser.Agent_trigger_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#agent_trigger_def}.
	 * @param ctx the parse tree
	 */
	void enterAgent_trigger_def(ElasticScriptParser.Agent_trigger_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#agent_trigger_def}.
	 * @param ctx the parse tree
	 */
	void exitAgent_trigger_def(ElasticScriptParser.Agent_trigger_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#drop_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterDrop_agent_statement(ElasticScriptParser.Drop_agent_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#drop_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitDrop_agent_statement(ElasticScriptParser.Drop_agent_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllAgents}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllAgents(ElasticScriptParser.ShowAllAgentsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllAgents}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllAgents(ElasticScriptParser.ShowAllAgentsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAgentDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAgentDetail(ElasticScriptParser.ShowAgentDetailContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAgentDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAgentDetail(ElasticScriptParser.ShowAgentDetailContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAgentExecution(ElasticScriptParser.ShowAgentExecutionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAgentExecution(ElasticScriptParser.ShowAgentExecutionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAgentHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void enterShowAgentHistory(ElasticScriptParser.ShowAgentHistoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAgentHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 */
	void exitShowAgentHistory(ElasticScriptParser.ShowAgentHistoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterAgentConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterAgentConfig(ElasticScriptParser.AlterAgentConfigContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterAgentConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterAgentConfig(ElasticScriptParser.AlterAgentConfigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterAgentExecution(ElasticScriptParser.AlterAgentExecutionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterAgentExecution(ElasticScriptParser.AlterAgentExecutionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code enableDisableAgent}
	 * labeled alternative in {@link ElasticScriptParser#start_stop_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterEnableDisableAgent(ElasticScriptParser.EnableDisableAgentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code enableDisableAgent}
	 * labeled alternative in {@link ElasticScriptParser#start_stop_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitEnableDisableAgent(ElasticScriptParser.EnableDisableAgentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ElasticScriptParser#trigger_agent_statement}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_agent_statement(ElasticScriptParser.Trigger_agent_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ElasticScriptParser#trigger_agent_statement}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_agent_statement(ElasticScriptParser.Trigger_agent_statementContext ctx);
}