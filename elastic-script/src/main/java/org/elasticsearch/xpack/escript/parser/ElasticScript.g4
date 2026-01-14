grammar ElasticScript;

// =======================
// Lexer Rules
// =======================

// Procedure
CREATE: 'CREATE';
DELETE: 'DELETE';
DROP: 'DROP';
CALL : 'CALL';
PROCEDURE: 'PROCEDURE';
IN: 'IN';
OUT: 'OUT';
INOUT: 'INOUT';

// Intent
DEFINE: 'DEFINE';
INTENT: 'INTENT';
DESCRIPTION: 'DESCRIPTION';
REQUIRES: 'REQUIRES';
ACTIONS: 'ACTIONS';
ON_FAILURE: 'ON_FAILURE';
WITH: 'WITH';

// Jobs and Triggers
JOB: 'JOB';
TRIGGER: 'TRIGGER';
SCHEDULE: 'SCHEDULE';
TIMEZONE: 'TIMEZONE';
ENABLED: 'ENABLED';
ON_KW: 'ON';
INDEX: 'INDEX';
WHEN: 'WHEN';
EVERY: 'EVERY';
RUNS: 'RUNS';
SHOW: 'SHOW';
JOBS: 'JOBS';
TRIGGERS: 'TRIGGERS';
ALTER: 'ALTER';
ENABLE: 'ENABLE';
DISABLE: 'DISABLE';
SECOND: 'SECOND';
SECONDS: 'SECONDS';
MINUTE: 'MINUTE';
MINUTES: 'MINUTES';
HOUR: 'HOUR';
HOURS: 'HOURS';

// Async Execution (Pipe-Driven)
ON_DONE: 'ON_DONE';
ON_FAIL: 'ON_FAIL';
TRACK: 'TRACK';
AS: 'AS';
TIMEOUT: 'TIMEOUT';
EXECUTION: 'EXECUTION';
STATUS: 'STATUS';
CANCEL: 'CANCEL';
RETRY: 'RETRY';
WAIT: 'WAIT';
PARALLEL: 'PARALLEL';
ON_ALL_DONE: 'ON_ALL_DONE';
ON_ANY_FAIL: 'ON_ANY_FAIL';
START_WITH: 'START WITH';
DO: 'DO';

// Print rules
PRINT: 'PRINT';
DEBUG: 'DEBUG';
INFO: 'INFO';
WARN: 'WARN';
ERROR: 'ERROR';

// Keywords
ELSEIF: 'ELSEIF';
ELSE: 'ELSE';
IF: 'IF';
THEN: 'THEN';
END: 'END';
BEGIN: 'BEGIN';
EXECUTE: 'EXECUTE';
DECLARE: 'DECLARE';
VAR: 'VAR';
CONST: 'CONST';
SET: 'SET';
FOR: 'FOR';
NULL: [Nn][Uu][Ll][Ll];



WHILE: 'WHILE';
LOOP: 'LOOP';
ENDLOOP: 'END LOOP';
TRY: 'TRY';
CATCH: 'CATCH';
FINALLY: 'FINALLY';
THROW: 'THROW';
ENDTRY: 'END TRY';
FUNCTION: 'FUNCTION';
RETURN: 'RETURN';
BREAK: 'BREAK';
CONTINUE: 'CONTINUE';
SWITCH: 'SWITCH';
CASE: 'CASE';
DEFAULT: 'DEFAULT';
END_SWITCH: 'END SWITCH';
PERSIST: 'PERSIST';
INTO: 'INTO';
CURSOR: 'CURSOR';

// Data Types
INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
DATE_TYPE: 'DATE';
NUMBER_TYPE: 'NUMBER';
DOCUMENT_TYPE: 'DOCUMENT';
ARRAY_TYPE: 'ARRAY';
BOOLEAN_TYPE: 'BOOLEAN';

// Operators
PLUS: '+';
MINUS: '-';
MULTIPLY: '*';
DIVIDE: '/';
MODULO: '%';
GREATER_THAN: '>';
LESS_THAN: '<';
NOT_EQUAL: '!=';
GREATER_EQUAL: '>=';
LESS_EQUAL: '<=';
OR: 'OR';
AND: 'AND';
NOT: 'NOT';
BANG: '!';
IS: 'IS';
    EQ: '==';             // equality operator
    ASSIGN: '=';          // assignment operator

// Range Operator
DOT_DOT: '..';

// Other Symbols
PIPE: '|';
DOT: '.';
QUESTION: '?';
NULLCOALESCE: '??';
SAFENAV: '?.';
ARROW: '=>';
LPAREN: '(';
RPAREN: ')';
COMMA: ',';
COLON: ':';
SEMICOLON: ';';
AT: '@';
LBRACKET: '[';
RBRACKET: ']';
LBRACE: '{';
RBRACE: '}';

// Literals
BOOLEAN: [Tt][Rr][Uu][Ee] | [Ff][Aa][Ll][Ss][Ee];
FLOAT: [0-9]+ '.' [0-9]+;
INT: [0-9]+;
STRING
    : ('\'' ( ~('\'' | '\\') | '\\' . )* '\''
    | '"' ( ~('"' | '\\') | '\\' . )* '"')
    ;
// Identifier
ID: [a-zA-Z_][a-zA-Z_0-9]*;

// Comments and Whitespace
COMMENT
    : ( '--' ~[\r\n]* | '/*' .*? '*/' ) -> channel(HIDDEN)
    ;

WS : [ \t\r\n]+ -> channel(HIDDEN);

// --- Lexer rules for String built‑in functions ---
LENGTH: 'LENGTH';
SUBSTR: 'SUBSTR';
UPPER: 'UPPER';
LOWER: 'LOWER';
TRIM: 'TRIM';
LTRIM: 'LTRIM';
RTRIM: 'RTRIM';
REPLACE: 'REPLACE';
INSTR: 'INSTR';
LPAD: 'LPAD';
RPAD: 'RPAD';
SPLIT: 'SPLIT';
CONCAT: '||';
REGEXP_REPLACE: 'REGEXP_REPLACE';
REGEXP_SUBSTR: 'REGEXP_SUBSTR';
REVERSE: 'REVERSE';
INITCAP: 'INITCAP';
LIKE: 'LIKE';

// --- Lexer Rules for Numeric Built‑In Functions ---

ABS: 'ABS';
CEIL: 'CEIL';
FLOOR: 'FLOOR';
ROUND: 'ROUND';
POWER: 'POWER';
SQRT: 'SQRT';
LOG: 'LOG';
EXP: 'EXP';
MOD: 'MOD';
SIGN: 'SIGN';
TRUNC: 'TRUNC';

// Lexer tokens for built‑in date functions:
CURRENT_DATE: 'CURRENT_DATE';
CURRENT_TIMESTAMP: 'CURRENT_TIMESTAMP';
DATE_ADD: 'DATE_ADD';
DATE_SUB: 'DATE_SUB';
EXTRACT_YEAR: 'EXTRACT_YEAR';
EXTRACT_MONTH: 'EXTRACT_MONTH';
EXTRACT_DAY: 'EXTRACT_DAY';
DATEDIFF: 'DATEDIFF';

// Lexer tokens for built‑in array functions:
ARRAY_LENGTH: 'ARRAY_LENGTH';
ARRAY_APPEND: 'ARRAY_APPEND';
ARRAY_PREPEND: 'ARRAY_PREPEND';
ARRAY_REMOVE: 'ARRAY_REMOVE';
ARRAY_CONTAINS: 'ARRAY_CONTAINS';
ARRAY_DISTINCT: 'ARRAY_DISTINCT';

// --- Lexer Rules for Document built‑in functions ---
DOCUMENT_KEYS: 'DOCUMENT_KEYS';
DOCUMENT_VALUES: 'DOCUMENT_VALUES';
DOCUMENT_GET: 'DOCUMENT_GET';
DOCUMENT_MERGE: 'DOCUMENT_MERGE';
DOCUMENT_REMOVE: 'DOCUMENT_REMOVE';
DOCUMENT_CONTAINS: 'DOCUMENT_CONTAINS';

// Datasource
ESQL_QUERY: 'ESQL_QUERY';
INDEX_DOCUMENT: 'INDEX_DOCUMENT';

// ES|QL Augmentation
PROCESS: 'PROCESS';
BATCH: 'BATCH';
FROM: 'FROM';

// =======================
// Parser Rules
// =======================

program
    : create_procedure_statement
    | delete_procedure_statement
    | call_procedure_statement
    | define_intent_statement
    | job_statement
    | trigger_statement
    ;

procedure
    : PROCEDURE ID LPAREN (parameter_list)? RPAREN BEGIN statement+ END PROCEDURE
    ;

create_procedure_statement
    : CREATE procedure
    ;

delete_procedure_statement
    : DELETE PROCEDURE ID SEMICOLON
    ;

statement
    : throw_statement
    | print_statement
    | execute_statement
    | declare_statement
    | var_statement
    | const_statement
    | assignment_statement
    | if_statement
    | loop_statement
    | try_catch_statement
    | function_definition
    | function_call_statement
    | async_procedure_statement
    | execution_control_statement
    | parallel_statement
    | call_procedure_statement
    | intent_statement
    | return_statement
    | break_statement
    | continue_statement
    | switch_statement
    | esql_into_statement
    | esql_process_statement
    | expression_statement
    | SEMICOLON
    ;



call_procedure_statement
    : CALL ID LPAREN (argument_list)? RPAREN
    ;

// =======================
// Async Execution (Pipe-Driven)
// =======================

// Async procedure call with pipe continuations
// Example: analyze_logs() | ON_DONE process(@result) | ON_FAIL handle(@error) | TRACK AS 'daily';
async_procedure_statement
    : ID LPAREN (argument_list)? RPAREN pipe_continuation+ SEMICOLON
    ;

pipe_continuation
    : PIPE ON_DONE continuation_handler      # onDoneContinuation
    | PIPE ON_FAIL continuation_handler      # onFailContinuation
    | PIPE FINALLY continuation_handler      # finallyContinuation
    | PIPE TRACK AS STRING                   # trackAsContinuation
    | PIPE TIMEOUT INT                       # timeoutContinuation
    ;

continuation_handler
    : ID LPAREN continuation_arg_list? RPAREN     // Handler call: process(@result)
    | lambda_continuation                          // Inline handler: (@result) => { ... }
    ;

continuation_arg_list
    : continuation_arg (COMMA continuation_arg)*
    ;

continuation_arg
    : AT ID           // @result, @error, @progress
    | expression      // Regular expressions
    ;

lambda_continuation
    : LPAREN continuation_arg_list? RPAREN ARROW LBRACE statement+ RBRACE
    ;

// Execution control: EXECUTION('name') | STATUS;
execution_control_statement
    : EXECUTION LPAREN STRING RPAREN PIPE execution_operation SEMICOLON
    ;

execution_operation
    : STATUS                                           # statusOperation
    | CANCEL                                           # cancelOperation
    | RETRY                                            # retryOperation
    | WAIT (TIMEOUT INT)?                              # waitOperation
    ;

// Parallel execution: PARALLEL [proc1(), proc2()] | ON_ALL_DONE merge(@results);
parallel_statement
    : PARALLEL LBRACKET parallel_procedure_list RBRACKET parallel_continuation+ SEMICOLON
    ;

parallel_procedure_list
    : parallel_procedure_call (COMMA parallel_procedure_call)*
    ;

parallel_procedure_call
    : ID LPAREN (argument_list)? RPAREN
    ;

parallel_continuation
    : PIPE ON_ALL_DONE continuation_handler   # onAllDoneContinuation
    | PIPE ON_ANY_FAIL continuation_handler   # onAnyFailContinuation
    | PIPE TRACK AS STRING                    # parallelTrackAsContinuation
    | PIPE TIMEOUT INT                        # parallelTimeoutContinuation
    ;

print_statement
    : PRINT expression (COMMA severity)? SEMICOLON
    ;

break_statement
    : BREAK SEMICOLON
    ;

continue_statement
    : CONTINUE SEMICOLON
    ;

switch_statement
    : SWITCH expression case_clause+ default_clause? END_SWITCH
    ;

case_clause
    : CASE expression ':' statement*
    ;

default_clause
    : DEFAULT ':' statement*
    ;

return_statement
    : RETURN expression SEMICOLON
    ;

expression_statement
    : expression SEMICOLON
    ;

execute_statement
    : EXECUTE variable_assignment LPAREN esql_query_content RPAREN (persist_clause)? SEMICOLON
    ;

variable_assignment
    : ID ASSIGN
    ;

esql_query_content
    : ( . )*?  // Match any content non-greedily
    ;

// =======================
// ES|QL Augmentation (PLANNED)
// =======================
// Note: INTO and PROCESS WITH syntax is planned for a future release.
// Currently, use ESQL_QUERY() function and CURSOR for ES|QL integration.
//
// Planned syntax:
// - FROM logs-* INTO my_results;
// - FROM logs-* | WHERE level = 'ERROR' INTO 'destination-index';
// - FROM logs-* PROCESS WITH my_procedure;
// - FROM logs-* PROCESS WITH my_procedure BATCH 50;
//
// Current supported patterns:
// - DECLARE result ARRAY = ESQL_QUERY('FROM logs-* | WHERE level = "ERROR"');
// - DECLARE logs CURSOR FOR FROM logs-* | WHERE level = 'ERROR';

// Placeholder rules for future implementation
esql_into_statement
    : 'ESQL_INTO_PLACEHOLDER' SEMICOLON
    ;

esql_process_statement
    : 'ESQL_PROCESS_PLACEHOLDER' SEMICOLON
    ;

declare_statement
    : DECLARE variable_declaration_list SEMICOLON
    | DECLARE ID CURSOR FOR cursor_query SEMICOLON
    ;

var_statement
    : VAR var_declaration_list SEMICOLON
    ;

var_declaration_list
    : var_declaration (COMMA var_declaration)*
    ;

var_declaration
    : ID ASSIGN expression
    ;

const_statement
    : CONST const_declaration_list SEMICOLON
    ;

const_declaration_list
    : const_declaration (COMMA const_declaration)*
    ;

const_declaration
    : ID datatype? ASSIGN expression
    ;

cursor_query
    : cursor_query_content
    ;

cursor_query_content
    : (~SEMICOLON)+  // Match everything until semicolon
    ;

variable_declaration_list
    : variable_declaration (COMMA variable_declaration)*
    ;

variable_declaration
    : ID datatype (ASSIGN expression)?
    ;

assignment_statement
    : SET varRef ASSIGN expression SEMICOLON
    ;

if_statement
    : IF condition THEN then_block+=statement+
        (elseif_block)*
        (ELSE else_block+=statement+)?
      END IF
      ;

elseif_block
    : ELSEIF condition THEN statement+
      ;

condition
    : expression
      ;

loop_statement
    : for_range_loop
    | for_array_loop
    | for_esql_loop
    | while_loop
    ;

for_range_loop
    : FOR ID IN range_loop_expression LOOP statement+ ENDLOOP
    ;

for_array_loop
    : FOR ID IN array_loop_expression LOOP statement+ ENDLOOP
    ;

for_esql_loop
    : FOR ID IN LPAREN inline_esql_query RPAREN LOOP statement+ ENDLOOP
    ;

inline_esql_query
    : inline_esql_content
    ;

inline_esql_content
    : (~(LPAREN | RPAREN) | LPAREN inline_esql_content? RPAREN)+  // Match balanced parentheses, including empty ()
    ;

while_loop
    : WHILE condition LOOP statement+ ENDLOOP
    ;

range_loop_expression
    : expression DOT_DOT expression
    ;

array_loop_expression
    : expression
    ;

try_catch_statement
    : TRY statement+ (CATCH statement+)? (FINALLY statement+)? ENDTRY
    ;

throw_statement
    : THROW STRING SEMICOLON
    ;

function_definition
    : FUNCTION ID LPAREN (parameter_list)? RPAREN BEGIN statement+ END FUNCTION
    ;

function_call_statement
    : function_call SEMICOLON
    ;

function_call
    : ID LPAREN (argument_list)? RPAREN
    ;

parameter_list
    : parameter (COMMA parameter)*
    ;

parameter
    : (IN | OUT | INOUT)? ID datatype
    ;

argument_list
    : expression (COMMA expression)*
    ;

expression
    : ternaryExpression (CONCAT ternaryExpression)*
    ;

ternaryExpression
    : nullCoalesceExpression (QUESTION nullCoalesceExpression COLON nullCoalesceExpression)?
    ;

nullCoalesceExpression
    : logicalOrExpression (NULLCOALESCE logicalOrExpression)*
    ;

logicalOrExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQ    | NOT_EQUAL) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LESS_THAN | GREATER_THAN | LESS_EQUAL | GREATER_EQUAL) additiveExpression)*  # comparisonExpr
    | additiveExpression IS NULL                             # isNullExpr
    | additiveExpression IS NOT NULL                         # isNotNullExpr
    | additiveExpression IN LPAREN expressionList RPAREN     # inListExpr
    | additiveExpression NOT IN LPAREN expressionList RPAREN # notInListExpr
    | additiveExpression IN additiveExpression               # inArrayExpr
    | additiveExpression NOT IN additiveExpression           # notInArrayExpr
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpr ((MULTIPLY | DIVIDE | MODULO) unaryExpr)*
    ;

unaryExpr
    : '-' unaryExpr
    | NOT unaryExpr
    | BANG unaryExpr
    | primaryExpression
    ;

arrayLiteral
    : '[' expressionList? ']'
    ;

expressionList
    : expression (COMMA expression)*
    ;

documentLiteral
    : '{' (documentField (',' documentField)*)? '}'
    ;
documentField
    : STRING ':' expression
    ;

pairList
    : pair (COMMA pair)*
    ;

pair
    : (ID | STRING) COLON expression
    ;

primaryExpression
    : simplePrimaryExpression accessExpression*
    ;

accessExpression
    : bracketExpression
    | safeNavExpression
    ;

bracketExpression
    : '[' expression ']'
    ;

safeNavExpression
    : SAFENAV ID
    ;

simplePrimaryExpression
    : LPAREN expression RPAREN
    | lambdaExpression
    | call_procedure_statement
    | function_call
    | INT
    | FLOAT
    | STRING
    | BOOLEAN
    | arrayLiteral
    | documentLiteral
    | ID
    | NULL
    ;

// Lambda expressions: (x) => x * 2  or  (a, b) => a + b
lambdaExpression
    : LPAREN lambdaParamList? RPAREN ARROW expression
    | ID ARROW expression   // Single parameter without parens
    ;

lambdaParamList
    : ID (COMMA ID)*
    ;

varRef
    : ID bracketExpression*
    ;

datatype
    : INT_TYPE
    | FLOAT_TYPE
    | STRING_TYPE
    | DATE_TYPE
    | NUMBER_TYPE
    | DOCUMENT_TYPE
    | BOOLEAN_TYPE
    | array_datatype
    ;

array_datatype
    : ARRAY_TYPE ('OF' (NUMBER_TYPE | STRING_TYPE | DOCUMENT_TYPE | DATE_TYPE | BOOLEAN_TYPE | ARRAY_TYPE ))?
    ;

persist_clause
    : PERSIST INTO ID
    ;

severity
    : DEBUG
    | INFO
    | WARN
    | ERROR
    ;

// =======================
// Intent Rules
// =======================

// DEFINE INTENT name(params) DESCRIPTION 'text' REQUIRES conditions ACTIONS statements ON_FAILURE statements END INTENT
define_intent_statement
    : DEFINE INTENT ID LPAREN (parameter_list)? RPAREN
      (DESCRIPTION STRING)?
      (requires_clause)?
      actions_clause
      (on_failure_clause)?
      END INTENT
    ;

requires_clause
    : REQUIRES requires_condition (COMMA requires_condition)*
    ;

requires_condition
    : expression
    ;

actions_clause
    : ACTIONS statement+
    ;

on_failure_clause
    : ON_FAILURE statement+
    ;

// Intent invocation: INTENT name(args); or INTENT name WITH param=value, param=value;
intent_statement
    : INTENT ID LPAREN (argument_list)? RPAREN SEMICOLON                    # intentCallWithArgs
    | INTENT ID (WITH intent_named_args)? SEMICOLON                          # intentCallWithNamedArgs
    ;

intent_named_args
    : intent_named_arg (COMMA intent_named_arg)*
    ;

intent_named_arg
    : ID ASSIGN expression
    ;

// =======================
// Job Rules
// =======================

job_statement
    : create_job_statement
    | alter_job_statement
    | drop_job_statement
    | show_jobs_statement
    ;

// CREATE JOB name SCHEDULE 'cron' [TIMEZONE 'tz'] [ENABLED true|false] [DESCRIPTION 'desc'] AS BEGIN ... END JOB
create_job_statement
    : CREATE JOB ID
      SCHEDULE STRING
      (TIMEZONE STRING)?
      (ENABLED BOOLEAN)?
      (DESCRIPTION STRING)?
      AS BEGIN statement+ END JOB
    ;

// ALTER JOB name ENABLE|DISABLE or ALTER JOB name SCHEDULE 'cron'
alter_job_statement
    : ALTER JOB ID (ENABLE | DISABLE)                    # alterJobEnableDisable
    | ALTER JOB ID SCHEDULE STRING                       # alterJobSchedule
    ;

// DROP JOB name
drop_job_statement
    : DROP JOB ID
    ;

// SHOW JOBS | SHOW JOB name | SHOW JOB RUNS FOR name
show_jobs_statement
    : SHOW JOBS                                          # showAllJobs
    | SHOW JOB ID                                        # showJobDetail
    | SHOW JOB RUNS FOR ID                               # showJobRuns
    ;

// =======================
// Trigger Rules
// =======================

trigger_statement
    : create_trigger_statement
    | alter_trigger_statement
    | drop_trigger_statement
    | show_triggers_statement
    ;

// CREATE TRIGGER name ON INDEX 'pattern' [WHEN condition] [EVERY interval] [ENABLED bool] [DESCRIPTION 'desc'] AS BEGIN ... END TRIGGER
create_trigger_statement
    : CREATE TRIGGER ID
      ON_KW INDEX STRING
      (WHEN expression)?
      (EVERY interval_expression)?
      (ENABLED BOOLEAN)?
      (DESCRIPTION STRING)?
      AS BEGIN statement+ END TRIGGER
    ;

// Interval: 5 SECONDS, 1 MINUTE, etc.
interval_expression
    : INT (SECOND | SECONDS | MINUTE | MINUTES | HOUR | HOURS)
    ;

// ALTER TRIGGER name ENABLE|DISABLE or ALTER TRIGGER name EVERY interval
alter_trigger_statement
    : ALTER TRIGGER ID (ENABLE | DISABLE)                # alterTriggerEnableDisable
    | ALTER TRIGGER ID EVERY interval_expression         # alterTriggerInterval
    ;

// DROP TRIGGER name
drop_trigger_statement
    : DROP TRIGGER ID
    ;

// SHOW TRIGGERS | SHOW TRIGGER name | SHOW TRIGGER RUNS FOR name
show_triggers_statement
    : SHOW TRIGGERS                                      # showAllTriggers
    | SHOW TRIGGER ID                                    # showTriggerDetail
    | SHOW TRIGGER RUNS FOR ID                           # showTriggerRuns
    ;
