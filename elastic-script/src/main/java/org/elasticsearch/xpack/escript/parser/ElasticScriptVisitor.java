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
}