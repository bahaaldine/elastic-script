/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ElasticScriptParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ElasticScriptVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(ElasticScriptParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure(ElasticScriptParser.ProcedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#authid_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthid_clause(ElasticScriptParser.Authid_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_procedure_statement(ElasticScriptParser.Create_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#delete_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_procedure_statement(ElasticScriptParser.Delete_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllProcedures}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllProcedures(ElasticScriptParser.ShowAllProceduresContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showProcedureDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_procedures_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowProcedureDetail(ElasticScriptParser.ShowProcedureDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_statement(ElasticScriptParser.Create_function_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#delete_function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_function_statement(ElasticScriptParser.Delete_function_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllFunctions}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllFunctions(ElasticScriptParser.ShowAllFunctionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showFunctionDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_functions_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowFunctionDetail(ElasticScriptParser.ShowFunctionDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#return_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_type(ElasticScriptParser.Return_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(ElasticScriptParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#call_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_procedure_statement(ElasticScriptParser.Call_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#async_procedure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsync_procedure_statement(ElasticScriptParser.Async_procedure_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code onDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnDoneContinuation(ElasticScriptParser.OnDoneContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code onFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnFailContinuation(ElasticScriptParser.OnFailContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code finallyContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyContinuation(ElasticScriptParser.FinallyContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code trackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrackAsContinuation(ElasticScriptParser.TrackAsContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#pipe_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeoutContinuation(ElasticScriptParser.TimeoutContinuationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#continuation_handler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinuation_handler(ElasticScriptParser.Continuation_handlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#continuation_arg_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinuation_arg_list(ElasticScriptParser.Continuation_arg_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#continuation_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinuation_arg(ElasticScriptParser.Continuation_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#lambda_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda_continuation(ElasticScriptParser.Lambda_continuationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#execution_control_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecution_control_statement(ElasticScriptParser.Execution_control_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code statusOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatusOperation(ElasticScriptParser.StatusOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cancelOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCancelOperation(ElasticScriptParser.CancelOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code retryOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRetryOperation(ElasticScriptParser.RetryOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code waitOperation}
	 * labeled alternative in {@link ElasticScriptParser#execution_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitOperation(ElasticScriptParser.WaitOperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parallel_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_statement(ElasticScriptParser.Parallel_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parallel_procedure_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_procedure_list(ElasticScriptParser.Parallel_procedure_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parallel_procedure_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_procedure_call(ElasticScriptParser.Parallel_procedure_callContext ctx);
	/**
	 * Visit a parse tree produced by the {@code onAllDoneContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnAllDoneContinuation(ElasticScriptParser.OnAllDoneContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code onAnyFailContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnAnyFailContinuation(ElasticScriptParser.OnAnyFailContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parallelTrackAsContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallelTrackAsContinuation(ElasticScriptParser.ParallelTrackAsContinuationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parallelTimeoutContinuation}
	 * labeled alternative in {@link ElasticScriptParser#parallel_continuation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallelTimeoutContinuation(ElasticScriptParser.ParallelTimeoutContinuationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#print_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_statement(ElasticScriptParser.Print_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#break_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_statement(ElasticScriptParser.Break_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#continue_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_statement(ElasticScriptParser.Continue_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#switch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_statement(ElasticScriptParser.Switch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#case_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_clause(ElasticScriptParser.Case_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#default_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_clause(ElasticScriptParser.Default_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(ElasticScriptParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expression_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_statement(ElasticScriptParser.Expression_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(ElasticScriptParser.Execute_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#execute_immediate_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_immediate_statement(ElasticScriptParser.Execute_immediate_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#id_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_list(ElasticScriptParser.Id_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expression_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_list(ElasticScriptParser.Expression_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_assignment(ElasticScriptParser.Variable_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_query_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_query_content(ElasticScriptParser.Esql_query_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_into_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_into_statement(ElasticScriptParser.Esql_into_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_process_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_process_statement(ElasticScriptParser.Esql_process_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#index_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_command(ElasticScriptParser.Index_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#index_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_target(ElasticScriptParser.Index_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#delete_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_command(ElasticScriptParser.Delete_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#search_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearch_command(ElasticScriptParser.Search_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#refresh_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefresh_command(ElasticScriptParser.Refresh_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_index_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_command(ElasticScriptParser.Create_index_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_options(ElasticScriptParser.Create_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#declare_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_statement(ElasticScriptParser.Declare_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_binding_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_binding_type(ElasticScriptParser.Esql_binding_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_binding_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_binding_query(ElasticScriptParser.Esql_binding_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#esql_binding_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEsql_binding_content(ElasticScriptParser.Esql_binding_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#var_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_statement(ElasticScriptParser.Var_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#var_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_declaration_list(ElasticScriptParser.Var_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#var_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_declaration(ElasticScriptParser.Var_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#const_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConst_statement(ElasticScriptParser.Const_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#const_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConst_declaration_list(ElasticScriptParser.Const_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#const_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConst_declaration(ElasticScriptParser.Const_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#cursor_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_query(ElasticScriptParser.Cursor_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#cursor_query_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_query_content(ElasticScriptParser.Cursor_query_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#open_cursor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_cursor_statement(ElasticScriptParser.Open_cursor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#close_cursor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClose_cursor_statement(ElasticScriptParser.Close_cursor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#fetch_cursor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_cursor_statement(ElasticScriptParser.Fetch_cursor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#forall_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForall_statement(ElasticScriptParser.Forall_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#forall_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForall_action(ElasticScriptParser.Forall_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#save_exceptions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSave_exceptions_clause(ElasticScriptParser.Save_exceptions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#bulk_collect_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBulk_collect_statement(ElasticScriptParser.Bulk_collect_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration_list(ElasticScriptParser.Variable_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(ElasticScriptParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#assignment_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_statement(ElasticScriptParser.Assignment_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(ElasticScriptParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#elseif_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseif_block(ElasticScriptParser.Elseif_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(ElasticScriptParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(ElasticScriptParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#for_range_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_range_loop(ElasticScriptParser.For_range_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#for_array_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_array_loop(ElasticScriptParser.For_array_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#for_esql_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_esql_loop(ElasticScriptParser.For_esql_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#inline_esql_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_esql_query(ElasticScriptParser.Inline_esql_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#inline_esql_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_esql_content(ElasticScriptParser.Inline_esql_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#while_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_loop(ElasticScriptParser.While_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#range_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_loop_expression(ElasticScriptParser.Range_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#array_loop_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_loop_expression(ElasticScriptParser.Array_loop_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#try_catch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_catch_statement(ElasticScriptParser.Try_catch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#catch_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatch_block(ElasticScriptParser.Catch_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#throw_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_statement(ElasticScriptParser.Throw_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_definition(ElasticScriptParser.Function_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_call_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call_statement(ElasticScriptParser.Function_call_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(ElasticScriptParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#namespaced_function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespaced_function_call(ElasticScriptParser.Namespaced_function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#method_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_name(ElasticScriptParser.Method_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#namespace_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_id(ElasticScriptParser.Namespace_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#simple_function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_function_call(ElasticScriptParser.Simple_function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_list(ElasticScriptParser.Parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(ElasticScriptParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_list(ElasticScriptParser.Argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ElasticScriptParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#ternaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpression(ElasticScriptParser.TernaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#nullCoalesceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullCoalesceExpression(ElasticScriptParser.NullCoalesceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(ElasticScriptParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(ElasticScriptParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#equalityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpression(ElasticScriptParser.EqualityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparisonExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonExpr(ElasticScriptParser.ComparisonExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsNullExpr(ElasticScriptParser.IsNullExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isNotNullExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsNotNullExpr(ElasticScriptParser.IsNotNullExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInListExpr(ElasticScriptParser.InListExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notInListExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotInListExpr(ElasticScriptParser.NotInListExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInArrayExpr(ElasticScriptParser.InArrayExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notInArrayExpr}
	 * labeled alternative in {@link ElasticScriptParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotInArrayExpr(ElasticScriptParser.NotInArrayExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(ElasticScriptParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(ElasticScriptParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(ElasticScriptParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(ElasticScriptParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(ElasticScriptParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#documentLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocumentLiteral(ElasticScriptParser.DocumentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#documentField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocumentField(ElasticScriptParser.DocumentFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#mapLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapLiteral(ElasticScriptParser.MapLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#mapEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapEntry(ElasticScriptParser.MapEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#pairList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPairList(ElasticScriptParser.PairListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#pair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPair(ElasticScriptParser.PairContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(ElasticScriptParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#accessExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessExpression(ElasticScriptParser.AccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#bracketExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketExpression(ElasticScriptParser.BracketExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#safeNavExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSafeNavExpression(ElasticScriptParser.SafeNavExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#simplePrimaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimplePrimaryExpression(ElasticScriptParser.SimplePrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#cursorAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursorAttribute(ElasticScriptParser.CursorAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#lambdaExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpression(ElasticScriptParser.LambdaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#lambdaParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParamList(ElasticScriptParser.LambdaParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#varRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarRef(ElasticScriptParser.VarRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(ElasticScriptParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#array_datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_datatype(ElasticScriptParser.Array_datatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#map_datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_datatype(ElasticScriptParser.Map_datatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#persist_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPersist_clause(ElasticScriptParser.Persist_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#severity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeverity(ElasticScriptParser.SeverityContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#define_intent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_intent_statement(ElasticScriptParser.Define_intent_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#requires_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequires_clause(ElasticScriptParser.Requires_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#requires_condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequires_condition(ElasticScriptParser.Requires_conditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#actions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActions_clause(ElasticScriptParser.Actions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#on_failure_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_failure_clause(ElasticScriptParser.On_failure_clauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intentCallWithArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntentCallWithArgs(ElasticScriptParser.IntentCallWithArgsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intentCallWithNamedArgs}
	 * labeled alternative in {@link ElasticScriptParser#intent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntentCallWithNamedArgs(ElasticScriptParser.IntentCallWithNamedArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#intent_named_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntent_named_args(ElasticScriptParser.Intent_named_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#intent_named_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntent_named_arg(ElasticScriptParser.Intent_named_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#job_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJob_statement(ElasticScriptParser.Job_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_job_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_job_statement(ElasticScriptParser.Create_job_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterJobEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterJobEnableDisable(ElasticScriptParser.AlterJobEnableDisableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterJobSchedule}
	 * labeled alternative in {@link ElasticScriptParser#alter_job_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterJobSchedule(ElasticScriptParser.AlterJobScheduleContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_job_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_job_statement(ElasticScriptParser.Drop_job_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllJobs}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllJobs(ElasticScriptParser.ShowAllJobsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showJobDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowJobDetail(ElasticScriptParser.ShowJobDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showJobRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_jobs_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowJobRuns(ElasticScriptParser.ShowJobRunsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_statement(ElasticScriptParser.Trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_trigger_statement(ElasticScriptParser.Create_trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#interval_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_expression(ElasticScriptParser.Interval_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterTriggerEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterTriggerEnableDisable(ElasticScriptParser.AlterTriggerEnableDisableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterTriggerInterval}
	 * labeled alternative in {@link ElasticScriptParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterTriggerInterval(ElasticScriptParser.AlterTriggerIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_trigger_statement(ElasticScriptParser.Drop_trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllTriggers}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllTriggers(ElasticScriptParser.ShowAllTriggersContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTriggerDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTriggerDetail(ElasticScriptParser.ShowTriggerDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTriggerRuns}
	 * labeled alternative in {@link ElasticScriptParser#show_triggers_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTriggerRuns(ElasticScriptParser.ShowTriggerRunsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_statement(ElasticScriptParser.Package_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_package_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_statement(ElasticScriptParser.Create_package_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_spec_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_spec_item(ElasticScriptParser.Package_spec_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_procedure_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_procedure_spec(ElasticScriptParser.Package_procedure_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_function_spec(ElasticScriptParser.Package_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_variable_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_variable_spec(ElasticScriptParser.Package_variable_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_package_body_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_body_statement(ElasticScriptParser.Create_package_body_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_body_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_body_item(ElasticScriptParser.Package_body_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_procedure_impl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_procedure_impl(ElasticScriptParser.Package_procedure_implContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#package_function_impl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_function_impl(ElasticScriptParser.Package_function_implContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_package_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_package_statement(ElasticScriptParser.Drop_package_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showPackageDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_packages_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowPackageDetail(ElasticScriptParser.ShowPackageDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#permission_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermission_statement(ElasticScriptParser.Permission_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#grant_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_statement(ElasticScriptParser.Grant_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#revoke_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevoke_statement(ElasticScriptParser.Revoke_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#privilege_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivilege_list(ElasticScriptParser.Privilege_listContext ctx);
	/**
	 * Visit a parse tree produced by the {@code executePrivilege}
	 * labeled alternative in {@link ElasticScriptParser#privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecutePrivilege(ElasticScriptParser.ExecutePrivilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#object_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type(ElasticScriptParser.Object_typeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRolePrincipal(ElasticScriptParser.RolePrincipalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link ElasticScriptParser#principal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserPrincipal(ElasticScriptParser.UserPrincipalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_role_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_role_statement(ElasticScriptParser.Create_role_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_role_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_role_statement(ElasticScriptParser.Drop_role_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllPermissions(ElasticScriptParser.ShowAllPermissionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showPrincipalPermissions}
	 * labeled alternative in {@link ElasticScriptParser#show_permissions_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowPrincipalPermissions(ElasticScriptParser.ShowPrincipalPermissionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showRoleDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_roles_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowRoleDetail(ElasticScriptParser.ShowRoleDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_statement(ElasticScriptParser.Profile_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#profile_exec_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_exec_statement(ElasticScriptParser.Profile_exec_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllProfiles(ElasticScriptParser.ShowAllProfilesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowLastProfile(ElasticScriptParser.ShowLastProfileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#show_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowProfileFor(ElasticScriptParser.ShowProfileForContext ctx);
	/**
	 * Visit a parse tree produced by the {@code clearAllProfiles}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClearAllProfiles(ElasticScriptParser.ClearAllProfilesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code clearProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#clear_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClearProfileFor(ElasticScriptParser.ClearProfileForContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyzeLastProfile}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyzeLastProfile(ElasticScriptParser.AnalyzeLastProfileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyzeProfileFor}
	 * labeled alternative in {@link ElasticScriptParser#analyze_profile_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyzeProfileFor(ElasticScriptParser.AnalyzeProfileForContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#type_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_statement(ElasticScriptParser.Type_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_type_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_type_statement(ElasticScriptParser.Create_type_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#type_field_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_field_list(ElasticScriptParser.Type_field_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#type_field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_field(ElasticScriptParser.Type_fieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_type_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_type_statement(ElasticScriptParser.Drop_type_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllTypes}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllTypes(ElasticScriptParser.ShowAllTypesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTypeDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_types_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTypeDetail(ElasticScriptParser.ShowTypeDetailContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplication_statement(ElasticScriptParser.Application_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_application_statement(ElasticScriptParser.Create_application_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#application_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplication_section(ElasticScriptParser.Application_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#sources_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSources_section(ElasticScriptParser.Sources_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#source_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSource_definition(ElasticScriptParser.Source_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skills_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkills_section(ElasticScriptParser.Skills_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_definition(ElasticScriptParser.Skill_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#intents_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntents_section(ElasticScriptParser.Intents_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#intent_mapping}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntent_mapping(ElasticScriptParser.Intent_mappingContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#jobs_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJobs_section(ElasticScriptParser.Jobs_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#job_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJob_definition(ElasticScriptParser.Job_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#triggers_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggers_section(ElasticScriptParser.Triggers_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#trigger_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_definition(ElasticScriptParser.Trigger_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#install_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstall_application_statement(ElasticScriptParser.Install_application_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#config_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_item(ElasticScriptParser.Config_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_application_statement(ElasticScriptParser.Drop_application_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterApplicationConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterApplicationConfig(ElasticScriptParser.AlterApplicationConfigContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterApplicationEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterApplicationEnableDisable(ElasticScriptParser.AlterApplicationEnableDisableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllApplications}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllApplications(ElasticScriptParser.ShowAllApplicationsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showApplicationDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowApplicationDetail(ElasticScriptParser.ShowApplicationDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showApplicationSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowApplicationSkills(ElasticScriptParser.ShowApplicationSkillsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showApplicationIntents}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowApplicationIntents(ElasticScriptParser.ShowApplicationIntentsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showApplicationHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_applications_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowApplicationHistory(ElasticScriptParser.ShowApplicationHistoryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extendApplicationAdd}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendApplicationAdd(ElasticScriptParser.ExtendApplicationAddContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extendApplicationRemove}
	 * labeled alternative in {@link ElasticScriptParser#extend_application_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendApplicationRemove(ElasticScriptParser.ExtendApplicationRemoveContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSkillExtension(ElasticScriptParser.AddSkillExtensionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddIntentExtension(ElasticScriptParser.AddIntentExtensionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_extension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddSourceExtension(ElasticScriptParser.AddSourceExtensionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code removeSkillExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemoveSkillExtension(ElasticScriptParser.RemoveSkillExtensionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code removeIntentExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemoveIntentExtension(ElasticScriptParser.RemoveIntentExtensionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code removeSourceExtension}
	 * labeled alternative in {@link ElasticScriptParser#application_removal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemoveSourceExtension(ElasticScriptParser.RemoveSourceExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#application_control_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplication_control_statement(ElasticScriptParser.Application_control_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code appStatusOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAppStatusOperation(ElasticScriptParser.AppStatusOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code appPauseOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAppPauseOperation(ElasticScriptParser.AppPauseOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code appResumeOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAppResumeOperation(ElasticScriptParser.AppResumeOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code appHistoryOperation}
	 * labeled alternative in {@link ElasticScriptParser#application_control_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAppHistoryOperation(ElasticScriptParser.AppHistoryOperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_statement(ElasticScriptParser.Skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_skill_statement(ElasticScriptParser.Create_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_parameters_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_parameters_clause(ElasticScriptParser.Skill_parameters_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_param_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_param_list(ElasticScriptParser.Skill_param_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_param(ElasticScriptParser.Skill_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_skill_pack_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_skill_pack_statement(ElasticScriptParser.Create_skill_pack_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_skill_statement(ElasticScriptParser.Drop_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllSkills}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllSkills(ElasticScriptParser.ShowAllSkillsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showSkillDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowSkillDetail(ElasticScriptParser.ShowSkillDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showSkillVersion}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowSkillVersion(ElasticScriptParser.ShowSkillVersionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showSkillPackDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowSkillPackDetail(ElasticScriptParser.ShowSkillPackDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllSkillPacks}
	 * labeled alternative in {@link ElasticScriptParser#show_skills_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllSkillPacks(ElasticScriptParser.ShowAllSkillPacksContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#alter_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_skill_statement(ElasticScriptParser.Alter_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_property(ElasticScriptParser.Skill_propertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#test_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTest_skill_statement(ElasticScriptParser.Test_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_test_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_test_args(ElasticScriptParser.Skill_test_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#skill_test_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkill_test_arg(ElasticScriptParser.Skill_test_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#generate_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerate_skill_statement(ElasticScriptParser.Generate_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#run_skill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRun_skill_statement(ElasticScriptParser.Run_skill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector_statement(ElasticScriptParser.Connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_connector_statement(ElasticScriptParser.Create_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_connector_statement(ElasticScriptParser.Drop_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllConnectors}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllConnectors(ElasticScriptParser.ShowAllConnectorsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showConnectorDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowConnectorDetail(ElasticScriptParser.ShowConnectorDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showConnectorStatus}
	 * labeled alternative in {@link ElasticScriptParser#show_connectors_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowConnectorStatus(ElasticScriptParser.ShowConnectorStatusContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#test_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTest_connector_statement(ElasticScriptParser.Test_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#sync_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSync_connector_statement(ElasticScriptParser.Sync_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#connector_entity_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector_entity_ref(ElasticScriptParser.Connector_entity_refContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterConnectorOptions}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterConnectorOptions(ElasticScriptParser.AlterConnectorOptionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterConnectorEnableDisable}
	 * labeled alternative in {@link ElasticScriptParser#alter_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterConnectorEnableDisable(ElasticScriptParser.AlterConnectorEnableDisableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#exec_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExec_connector_statement(ElasticScriptParser.Exec_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#connector_action_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector_action_call(ElasticScriptParser.Connector_action_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#connector_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector_args(ElasticScriptParser.Connector_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#connector_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector_arg(ElasticScriptParser.Connector_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#query_connector_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_connector_statement(ElasticScriptParser.Query_connector_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_statement(ElasticScriptParser.Agent_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#create_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_agent_statement(ElasticScriptParser.Create_agent_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_skill_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_skill_list(ElasticScriptParser.Agent_skill_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_skill_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_skill_ref(ElasticScriptParser.Agent_skill_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_execution_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_execution_mode(ElasticScriptParser.Agent_execution_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_trigger_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_trigger_list(ElasticScriptParser.Agent_trigger_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#agent_trigger_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgent_trigger_def(ElasticScriptParser.Agent_trigger_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#drop_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_agent_statement(ElasticScriptParser.Drop_agent_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAllAgents}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAllAgents(ElasticScriptParser.ShowAllAgentsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAgentDetail}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAgentDetail(ElasticScriptParser.ShowAgentDetailContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAgentExecution(ElasticScriptParser.ShowAgentExecutionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showAgentHistory}
	 * labeled alternative in {@link ElasticScriptParser#show_agents_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowAgentHistory(ElasticScriptParser.ShowAgentHistoryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterAgentConfig}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterAgentConfig(ElasticScriptParser.AlterAgentConfigContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterAgentExecution}
	 * labeled alternative in {@link ElasticScriptParser#alter_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterAgentExecution(ElasticScriptParser.AlterAgentExecutionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code enableDisableAgent}
	 * labeled alternative in {@link ElasticScriptParser#start_stop_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnableDisableAgent(ElasticScriptParser.EnableDisableAgentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ElasticScriptParser#trigger_agent_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_agent_statement(ElasticScriptParser.Trigger_agent_statementContext ctx);
}