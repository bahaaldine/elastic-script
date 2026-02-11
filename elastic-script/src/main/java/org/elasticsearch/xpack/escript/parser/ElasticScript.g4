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
PROCEDURES: 'PROCEDURES';
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

// Bulk Operations
FORALL: 'FORALL';
BULK_KW: 'BULK';
COLLECT: 'COLLECT';
SAVE_KW: 'SAVE';
EXCEPTIONS: 'EXCEPTIONS';

// Packages
PACKAGE: 'PACKAGE';
BODY: 'BODY';
END_PACKAGE: 'END PACKAGE';
PRIVATE: 'PRIVATE';
PUBLIC: 'PUBLIC';

// Permissions (GRANT/REVOKE)
GRANT: 'GRANT';
REVOKE: 'REVOKE';
EXECUTE: 'EXECUTE';
PRIVILEGES: 'PRIVILEGES';
ALL_PRIVILEGES: 'ALL PRIVILEGES';
TO: 'TO';
OF: 'OF';
ROLE: 'ROLE';
USER: 'USER';
PERMISSIONS: 'PERMISSIONS';

// Profiler
PROFILE: 'PROFILE';
PROFILES: 'PROFILES';
CLEAR: 'CLEAR';
ANALYZE: 'ANALYZE';

// Applications (Intelligent Data Applications)
APPLICATION: 'APPLICATION';
APPLICATIONS: 'APPLICATIONS';
INSTALL: 'INSTALL';
EXTEND: 'EXTEND';
SKILL: 'SKILL';
SKILLS: 'SKILLS';
INTENTS: 'INTENTS';
SOURCE: 'SOURCE';
SOURCES: 'SOURCES';
VERSION: 'VERSION';
CONFIG: 'CONFIG';
PAUSE: 'PAUSE';
RESUME: 'RESUME';
HISTORY: 'HISTORY';
ADD: 'ADD';
REMOVE: 'REMOVE';
MODIFY: 'MODIFY';
END_APPLICATION: 'END APPLICATION';
END_SKILL: 'END SKILL';
GENERATE: 'GENERATE';
EXAMPLES: 'EXAMPLES';
MODEL: 'MODEL';

// Connectors (External Data Sources)
CONNECTOR: 'CONNECTOR';
CONNECTORS: 'CONNECTORS';
SYNC: 'SYNC';
INCREMENTAL: 'INCREMENTAL';
OPTIONS: 'OPTIONS';
TEST_KW: 'TEST';
CONNECTOR_EXEC: 'CONNECTOR_EXEC';
EXEC: 'EXEC';
ORDER: 'ORDER';
BY: 'BY';
ASC: 'ASC';
DESC: 'DESC';

// Agent Runtime
AGENT: 'AGENT';
AGENTS: 'AGENTS';
GOAL: 'GOAL';
APPROVAL: 'APPROVAL';
AUTONOMOUS: 'AUTONOMOUS';
SUPERVISED: 'SUPERVISED';
DRY_RUN: 'DRY_RUN';
HUMAN_APPROVAL: 'HUMAN_APPROVAL';
END_AGENT: 'END AGENT';
ALERT: 'ALERT';

// Skill Metadata
AUTHOR: 'AUTHOR';
TAGS: 'TAGS';
LICENSE: 'LICENSE';
DEPRECATES: 'DEPRECATES';
PACK: 'PACK';
PACKS: 'PACKS';
END_PACK: 'END PACK';
EXPECT: 'EXPECT';

// User-Defined Types
TYPE: 'TYPE';
TYPES: 'TYPES';
RECORD: 'RECORD';
END_TYPE: 'END TYPE';

// AUTHID (Definer/Invoker Rights)
AUTHID: 'AUTHID';
DEFINER: 'DEFINER';
INVOKER: 'CURRENT_USER';

// First-Class Commands (Elasticsearch Operations)
SEARCH: 'SEARCH';
REFRESH: 'REFRESH';
QUERY: 'QUERY';
MAPPINGS: 'MAPPINGS';
SETTINGS: 'SETTINGS';
WHERE_CMD: 'WHERE';
ESQL_PROCESS_PLACEHOLDER: 'ESQL_PROCESS_PLACEHOLDER';
ESQL_INTO_PLACEHOLDER: 'ESQL_INTO_PLACEHOLDER';

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
IMMEDIATE: 'IMMEDIATE';
USING: 'USING';
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
RAISE: 'RAISE';
CODE: 'CODE';
ENDTRY: 'END TRY';
FUNCTION: 'FUNCTION';
FUNCTIONS: 'FUNCTIONS';
RETURNS: 'RETURNS';
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
OPEN_CURSOR: 'OPEN';
CLOSE_CURSOR: 'CLOSE';
FETCH: 'FETCH';
LIMIT: 'LIMIT';
NOTFOUND: '%NOTFOUND';
ROWCOUNT: '%ROWCOUNT';

// Data Types
INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
DATE_TYPE: 'DATE';
NUMBER_TYPE: 'NUMBER';
DOCUMENT_TYPE: 'DOCUMENT';
ARRAY_TYPE: 'ARRAY';
MAP_TYPE: 'MAP';
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

// ES|QL Augmentation (must be before ID to avoid matching as identifier)
PROCESS: 'PROCESS';
BATCH: 'BATCH';
FROM: 'FROM';

// Literals
BOOLEAN: [Tt][Rr][Uu][Ee] | [Ff][Aa][Ll][Ss][Ee];
FLOAT: [0-9]+ '.' [0-9]+;
INT: [0-9]+;
STRING
    : ('\'' ( ~('\'' | '\\') | '\\' . )* '\''
    | '"' ( ~('"' | '\\') | '\\' . )* '"')
    ;
// Identifier (must be AFTER all keyword tokens)
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

// =======================
// Parser Rules
// =======================

program
    : create_procedure_statement
    | delete_procedure_statement
    | call_procedure_statement
    | show_procedures_statement
    | create_function_statement
    | delete_function_statement
    | show_functions_statement
    | define_intent_statement
    | job_statement
    | trigger_statement
    | package_statement
    | permission_statement
    | profile_statement
    | type_statement
    | application_statement
    | skill_statement
    | connector_statement
    | agent_statement
    ;

procedure
    : PROCEDURE ID LPAREN (parameter_list)? RPAREN authid_clause? BEGIN statement+ END PROCEDURE
    ;

// AUTHID clause specifies the privileges under which a stored procedure executes
// AUTHID DEFINER - execute with definer's privileges (default)
// AUTHID CURRENT_USER - execute with invoker's privileges
authid_clause
    : AUTHID (DEFINER | INVOKER)
    ;

create_procedure_statement
    : CREATE procedure
    ;

delete_procedure_statement
    : DELETE PROCEDURE ID SEMICOLON
    ;

show_procedures_statement
    : SHOW PROCEDURES                                    # showAllProcedures
    | SHOW PROCEDURE ID                                  # showProcedureDetail
    ;

// =======================
// Stored Function Statements
// =======================
// CREATE FUNCTION name(params) RETURNS type AS BEGIN ... END FUNCTION
// Example: CREATE FUNCTION calculate_score(x NUMBER, y NUMBER) RETURNS NUMBER AS
//          BEGIN RETURN x * 10 + y; END FUNCTION

create_function_statement
    : CREATE FUNCTION ID LPAREN (parameter_list)? RPAREN RETURNS return_type authid_clause? AS BEGIN statement+ END FUNCTION
    ;

delete_function_statement
    : DELETE FUNCTION ID SEMICOLON
    ;

show_functions_statement
    : SHOW FUNCTIONS                                     # showAllFunctions
    | SHOW FUNCTION ID                                   # showFunctionDetail
    ;

return_type
    : datatype
    ;

statement
    : throw_statement
    | print_statement
    | execute_statement
    | execute_immediate_statement
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
    | open_cursor_statement
    | close_cursor_statement
    | fetch_cursor_statement
    | forall_statement
    | bulk_collect_statement
    | esql_into_statement
    | esql_process_statement
    | index_command
    | delete_command
    | search_command
    | refresh_command
    | create_index_command
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
    : CASE expression COLON statement*
    ;

default_clause
    : DEFAULT COLON statement*
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

// =======================
// EXECUTE IMMEDIATE Statement
// =======================
// Executes a dynamically built ES|QL query string at runtime.
//
// Syntax:
//   EXECUTE IMMEDIATE query_expression [INTO var1, var2, ...] [USING bind1, bind2, ...];
//
// Examples:
//   EXECUTE IMMEDIATE 'FROM logs-* | LIMIT 10';
//   EXECUTE IMMEDIATE query_var INTO result;
//   EXECUTE IMMEDIATE 'FROM ' || index_name || ' | WHERE status = :1' USING 'error';
//
execute_immediate_statement
    : EXECUTE IMMEDIATE expression (INTO id_list)? (USING expression_list)? SEMICOLON
    ;

id_list
    : ID (COMMA ID)*
    ;

expression_list
    : expression (COMMA expression)*
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
    : ESQL_INTO_PLACEHOLDER SEMICOLON
    ;

esql_process_statement
    : ESQL_PROCESS_PLACEHOLDER SEMICOLON
    ;

// =======================
// First-Class Commands (Elasticsearch Operations)
// =======================
// These are language-level commands for core Elasticsearch operations,
// replacing function calls with cleaner, more intuitive syntax.

// INDEX document INTO 'index-name';
// INDEX { 'field': 'value' } INTO 'my-index';
// INDEX my_doc INTO 'my-index';
index_command
    : INDEX expression INTO index_target SEMICOLON
    ;

index_target
    : STRING    // Literal index name: 'my-index'
    | ID        // Variable containing index name
    ;

// DELETE FROM 'index-name' WHERE _id == 'doc-id';
// DELETE FROM 'my-index' WHERE condition;
delete_command
    : DELETE FROM index_target WHERE_CMD expression SEMICOLON
    ;

// SEARCH 'index-name' QUERY { ... };
// SEARCH 'logs-*' QUERY { "match": { "level": "ERROR" } };
// SEARCH index_var QUERY query_doc;
search_command
    : SEARCH index_target QUERY expression SEMICOLON
    ;

// REFRESH 'index-name';
// REFRESH my_index_var;
refresh_command
    : REFRESH index_target SEMICOLON
    ;

// CREATE INDEX 'name' WITH { mappings: {...}, settings: {...} };
// CREATE INDEX 'my-index' WITH MAPPINGS { ... };
// CREATE INDEX 'my-index' WITH SETTINGS { ... } MAPPINGS { ... };
create_index_command
    : CREATE INDEX index_target (WITH create_index_options)? SEMICOLON
    ;

create_index_options
    : MAPPINGS expression                                    // CREATE INDEX 'x' WITH MAPPINGS {...}
    | SETTINGS expression                                    // CREATE INDEX 'x' WITH SETTINGS {...}
    | SETTINGS expression MAPPINGS expression                // CREATE INDEX 'x' WITH SETTINGS {...} MAPPINGS {...}
    | expression                                             // CREATE INDEX 'x' WITH {...} (full body)
    ;

declare_statement
    : DECLARE ID esql_binding_type FROM esql_binding_query SEMICOLON   // ES|QL binding (must be first due to ARRAY/STRING/etc overlap with datatype)
    | DECLARE ID CURSOR FOR cursor_query SEMICOLON                     // Cursor declaration
    | DECLARE variable_declaration_list SEMICOLON                      // Regular variable declaration
    ;

// Type-aware ES|QL binding: DECLARE x TYPE FROM <esql>
esql_binding_type
    : ARRAY_TYPE       // Multiple rows captured as array
    | DOCUMENT_TYPE    // Single row as document
    | NUMBER_TYPE      // Single row, single column as number
    | STRING_TYPE      // Single row, single column as string
    | DATE_TYPE        // Single row, single column as date
    | BOOLEAN_TYPE     // Single row, single column as boolean
    ;

esql_binding_query
    : esql_binding_content
    ;

esql_binding_content
    : (~SEMICOLON)+  // Match everything until semicolon
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

// Cursor operations
// OPEN cursor_name;
open_cursor_statement
    : OPEN_CURSOR ID SEMICOLON
    ;

// CLOSE cursor_name;
close_cursor_statement
    : CLOSE_CURSOR ID SEMICOLON
    ;

// FETCH cursor_name INTO variable;
// FETCH cursor_name LIMIT n INTO array_variable;
fetch_cursor_statement
    : FETCH ID INTO ID SEMICOLON                          // Fetch single row
    | FETCH ID LIMIT expression INTO ID SEMICOLON         // Fetch multiple rows
    ;

// Cursor attributes: cursor_name%NOTFOUND, cursor_name%ROWCOUNT
// These are handled in expression evaluation as special identifiers

// =======================
// Bulk Operations
// =======================

// FORALL element IN collection statement [SAVE EXCEPTIONS]
// Executes statement for each element in collection with optional error continuation
forall_statement
    : FORALL ID IN expression forall_action (save_exceptions_clause)? SEMICOLON
    ;

forall_action
    : call_procedure_statement                               // FORALL x IN arr CALL process(x)
    | function_call                                          // FORALL x IN arr INDEX_DOCUMENT('idx', x)
    ;

save_exceptions_clause
    : SAVE_KW EXCEPTIONS
    ;

// BULK COLLECT INTO variable FROM esql_query
// Collects all results from ES|QL query into a single array
bulk_collect_statement
    : BULK_KW COLLECT INTO ID FROM esql_binding_query SEMICOLON
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

// TRY-CATCH-FINALLY with optional named exception types
// Examples:
//   TRY ... CATCH ... END TRY                          (catch all)
//   TRY ... CATCH http_error ... END TRY               (named exception)
//   TRY ... CATCH http_error ... CATCH ... END TRY     (named + catch-all)
try_catch_statement
    : TRY statement+ catch_block* (FINALLY statement+)? ENDTRY
    ;

catch_block
    : CATCH ID statement+           // Named exception: CATCH http_error
    | CATCH statement+              // Catch-all (no exception name)
    ;

// THROW/RAISE statement for throwing exceptions
// Examples:
//   THROW 'Error message';
//   THROW 'Not found' WITH CODE 'HTTP_404';
//   THROW error_message;  (expression)
//   RAISE 'Error';  (RAISE is alias for THROW)
throw_statement
    : (THROW | RAISE) expression (WITH CODE expression)? SEMICOLON
    ;

function_definition
    : FUNCTION ID LPAREN (parameter_list)? RPAREN BEGIN statement+ END FUNCTION
    ;

function_call_statement
    : function_call SEMICOLON
    ;

function_call
    : namespaced_function_call
    | simple_function_call
    ;

// Namespaced function call: ARRAY.MAP(...), STRING.UPPER(...), K8S.GET_PODS(...)
// namespace_id allows keywords like ARRAY, STRING, etc. to be used as namespaces
// method_name allows keywords like MAP to be used as method names
namespaced_function_call
    : namespace_id DOT method_name LPAREN (argument_list)? RPAREN
    ;

// Method name - allows ID or keywords that might be used as method names
method_name
    : ID
    | MAP_TYPE    // ARRAY.MAP(...) - MAP is a keyword but valid method name
    | ADD         // DATE.ADD(...) - ADD is a keyword but valid method name
    ;

// Namespace identifier - can be ID or type keywords used as namespace
namespace_id
    : ID
    | ARRAY_TYPE       // ARRAY.MAP(...)
    | STRING_TYPE      // STRING.UPPER(...)
    | NUMBER_TYPE      // NUMBER.FORMAT(...)
    | DATE_TYPE        // DATE.ADD(...)
    | DOCUMENT_TYPE    // DOCUMENT.KEYS(...)
    | MAP_TYPE         // MAP.GET(...), MAP.PUT(...)
    | BOOLEAN_TYPE     // BOOLEAN.PARSE(...)
    ;

// Simple function call: MY_FUNCTION(...)
simple_function_call
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
    : MINUS unaryExpr
    | NOT unaryExpr
    | BANG unaryExpr
    | primaryExpression
    ;

arrayLiteral
    : LBRACKET expressionList? RBRACKET
    ;

expressionList
    : expression (COMMA expression)*
    ;

documentLiteral
    : LBRACE (documentField (COMMA documentField)*)? RBRACE
    ;

documentField
    : STRING COLON expression
    ;

// MAP literal: MAP { 'key' => value, 'key2' => value2 }
// Also supports: MAP {} for empty map
mapLiteral
    : MAP_TYPE LBRACE (mapEntry (COMMA mapEntry)*)? RBRACE
    ;

mapEntry
    : expression ARROW expression
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
    : LBRACKET expression RBRACKET
    ;

safeNavExpression
    : SAFENAV ID
    ;

simplePrimaryExpression
    : LPAREN expression RPAREN
    | lambdaExpression
    | call_procedure_statement
    | function_call
    | cursorAttribute
    | INT
    | FLOAT
    | STRING
    | BOOLEAN
    | arrayLiteral
    | documentLiteral
    | mapLiteral
    | ID
    | NULL
    ;

// Cursor attributes: cursor_name%NOTFOUND, cursor_name%ROWCOUNT
cursorAttribute
    : ID NOTFOUND     // cursor%NOTFOUND - true when no more rows
    | ID ROWCOUNT     // cursor%ROWCOUNT - number of rows fetched
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
    | MAP_TYPE
    | array_datatype
    | map_datatype
    ;

array_datatype
    : ARRAY_TYPE (OF (NUMBER_TYPE | STRING_TYPE | DOCUMENT_TYPE | DATE_TYPE | BOOLEAN_TYPE | ARRAY_TYPE | MAP_TYPE ))?
    ;

// MAP type with optional key/value type specification
// Examples: MAP, MAP OF STRING, MAP OF STRING TO NUMBER
map_datatype
    : MAP_TYPE OF datatype (TO datatype)?
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

// =======================
// Package Rules
// =======================
// Packages group related procedures, functions, and variables
// Similar to PL/SQL packages with specification and body

package_statement
    : create_package_statement
    | create_package_body_statement
    | drop_package_statement
    | show_packages_statement
    ;

// CREATE PACKAGE name AS
//   PROCEDURE proc_name(params);  -- declaration only
//   FUNCTION func_name(params) RETURNS type;
//   variable_name TYPE := value;  -- package variable
// END PACKAGE;
create_package_statement
    : CREATE PACKAGE ID AS
      package_spec_item*
      END_PACKAGE
    ;

package_spec_item
    : package_procedure_spec
    | package_function_spec
    | package_variable_spec
    ;

// Procedure declaration (no body)
package_procedure_spec
    : (PUBLIC | PRIVATE)? PROCEDURE ID LPAREN (parameter_list)? RPAREN SEMICOLON
    ;

// Function declaration (no body)
package_function_spec
    : (PUBLIC | PRIVATE)? FUNCTION ID LPAREN (parameter_list)? RPAREN RETURNS datatype SEMICOLON
    ;

// Package variable
package_variable_spec
    : (PUBLIC | PRIVATE)? ID datatype (ASSIGN expression)? SEMICOLON
    ;

// CREATE PACKAGE BODY name AS
//   PROCEDURE proc_name(params) BEGIN ... END PROCEDURE;
//   FUNCTION func_name(params) RETURNS type BEGIN ... END FUNCTION;
// END PACKAGE;
create_package_body_statement
    : CREATE PACKAGE BODY ID AS
      package_body_item*
      END_PACKAGE
    ;

package_body_item
    : package_procedure_impl
    | package_function_impl
    ;

// Procedure implementation
package_procedure_impl
    : PROCEDURE ID LPAREN (parameter_list)? RPAREN BEGIN statement+ END PROCEDURE SEMICOLON
    ;

// Function implementation
package_function_impl
    : FUNCTION ID LPAREN (parameter_list)? RPAREN RETURNS datatype AS BEGIN statement+ END FUNCTION SEMICOLON
    ;

// DROP PACKAGE name
drop_package_statement
    : DROP PACKAGE ID
    ;

// SHOW PACKAGES | SHOW PACKAGE name
show_packages_statement
    : SHOW PACKAGE ID                                    # showPackageDetail
    ;

// =======================
// Permission Rules (GRANT/REVOKE)
// =======================
// Manage access to procedures, functions, and packages

permission_statement
    : grant_statement
    | revoke_statement
    | show_permissions_statement
    | create_role_statement
    | drop_role_statement
    | show_roles_statement
    ;

// GRANT privilege ON object_type object_name TO principal
// Examples:
//   GRANT EXECUTE ON PROCEDURE my_proc TO ROLE admin_role
//   GRANT EXECUTE ON FUNCTION my_func TO USER 'john'
//   GRANT ALL PRIVILEGES ON PACKAGE my_pkg TO ROLE operators
grant_statement
    : GRANT privilege_list ON_KW object_type ID TO principal
    ;

// REVOKE privilege FROM principal
// Examples:
//   REVOKE EXECUTE ON PROCEDURE my_proc FROM ROLE admin_role
revoke_statement
    : REVOKE privilege_list ON_KW object_type ID FROM principal
    ;

privilege_list
    : privilege (COMMA privilege)*
    | ALL_PRIVILEGES
    ;

privilege
    : EXECUTE                                            # executePrivilege
    ;

object_type
    : PROCEDURE
    | FUNCTION
    | PACKAGE
    | JOB
    | TRIGGER
    ;

principal
    : ROLE ID                                            # rolePrincipal
    | USER STRING                                        # userPrincipal
    ;

// CREATE ROLE role_name
create_role_statement
    : CREATE ROLE ID (DESCRIPTION STRING)?
    ;

// DROP ROLE role_name
drop_role_statement
    : DROP ROLE ID
    ;

// SHOW PERMISSIONS / SHOW PERMISSIONS FOR principal / SHOW ROLES
show_permissions_statement
    : SHOW PERMISSIONS                                   # showAllPermissions
    | SHOW PERMISSIONS FOR principal                     # showPrincipalPermissions
    ;

show_roles_statement
    : SHOW ROLE ID                                       # showRoleDetail
    ;

// =======================
// Profiler Rules
// =======================
// Performance profiling for procedures and functions

profile_statement
    : profile_exec_statement
    | show_profile_statement
    | clear_profile_statement
    | analyze_profile_statement
    ;

// PROFILE CALL procedure_name(args)
// Executes procedure and collects timing information
profile_exec_statement
    : PROFILE call_procedure_statement
    ;

// SHOW PROFILE / SHOW PROFILES / SHOW PROFILE FOR procedure_name
show_profile_statement
    : SHOW PROFILES                                      # showAllProfiles
    | SHOW PROFILE                                       # showLastProfile
    | SHOW PROFILE FOR ID                                # showProfileFor
    ;

// CLEAR PROFILES / CLEAR PROFILE FOR procedure_name
clear_profile_statement
    : CLEAR PROFILES                                     # clearAllProfiles
    | CLEAR PROFILE FOR ID                               # clearProfileFor
    ;

// ANALYZE PROFILE - get recommendations
analyze_profile_statement
    : ANALYZE PROFILE                                    # analyzeLastProfile
    | ANALYZE PROFILE FOR ID                             # analyzeProfileFor
    ;

// =======================
// User-Defined Type Rules
// =======================
// Custom record types for complex data structures

type_statement
    : create_type_statement
    | drop_type_statement
    | show_types_statement
    ;

// CREATE TYPE type_name AS RECORD (field1 TYPE, field2 TYPE, ...) END TYPE
// Example: CREATE TYPE address_type AS RECORD (street STRING, city STRING, zip STRING) END TYPE
create_type_statement
    : CREATE TYPE ID AS RECORD LPAREN type_field_list RPAREN END_TYPE
    ;

type_field_list
    : type_field (COMMA type_field)*
    ;

type_field
    : ID datatype
    ;

// DROP TYPE type_name
drop_type_statement
    : DROP TYPE ID
    ;

// SHOW TYPES / SHOW TYPE name
show_types_statement
    : SHOW TYPES                                         # showAllTypes
    | SHOW TYPE ID                                       # showTypeDetail
    ;

// =======================
// Application Rules (Intelligent Data Applications)
// =======================
// Applications bundle skills, intents, sources, jobs, and triggers
// into deployable, configurable units

application_statement
    : create_application_statement
    | install_application_statement
    | drop_application_statement
    | alter_application_statement
    | show_applications_statement
    | extend_application_statement
    | application_control_statement
    ;

// CREATE APPLICATION name
//   [DESCRIPTION 'desc']
//   [VERSION 'x.y.z']
//   [SOURCES (...)]
//   [SKILLS (...)]
//   [INTENTS (...)]
//   [JOBS (...)]
//   [TRIGGERS (...)]
// END APPLICATION
create_application_statement
    : CREATE APPLICATION ID
      (DESCRIPTION STRING)?
      (VERSION STRING)?
      application_section*
      END_APPLICATION
    ;

application_section
    : sources_section
    | skills_section
    | intents_section
    | jobs_section
    | triggers_section
    ;

// SOURCES (name FROM "index-pattern", ...)
sources_section
    : SOURCES LPAREN source_definition (COMMA source_definition)* RPAREN
    ;

source_definition
    : ID FROM STRING
    ;

// SKILLS (skill_name(params) RETURNS type DESCRIPTION 'desc' AS procedure_call, ...)
skills_section
    : SKILLS LPAREN skill_definition (COMMA skill_definition)* RPAREN
    ;

skill_definition
    : ID LPAREN (parameter_list)? RPAREN 
      RETURNS datatype 
      (DESCRIPTION STRING)?
      AS ID LPAREN (argument_list)? RPAREN
    ;

// INTENTS (pattern => skill_name, ...)
intents_section
    : INTENTS LPAREN intent_mapping (COMMA intent_mapping)* RPAREN
    ;

intent_mapping
    : STRING ARROW ID                     // "find churning customers" => detect_churn
    ;

// JOBS (job_name SCHEDULE 'cron' AS procedure(), ...)
jobs_section
    : JOBS LPAREN job_definition (COMMA job_definition)* RPAREN
    ;

job_definition
    : ID SCHEDULE STRING AS ID LPAREN (argument_list)? RPAREN
    ;

// TRIGGERS (ON condition DO procedure(), ...)
triggers_section
    : TRIGGERS LPAREN trigger_definition (COMMA trigger_definition)* RPAREN
    ;

trigger_definition
    : ON_KW ID (WHEN expression)? DO ID LPAREN (argument_list)? RPAREN
    ;

// INSTALL APPLICATION 'name' [CONFIG (key => value, ...)]
install_application_statement
    : INSTALL APPLICATION (STRING | ID)
      (CONFIG LPAREN config_item (COMMA config_item)* RPAREN)?
    ;

config_item
    : ID ARROW expression                 // orders_index => 'my-orders-*'
    ;

// DROP APPLICATION name
drop_application_statement
    : DROP APPLICATION ID
    ;

// ALTER APPLICATION name SET key = value, ...
// ALTER APPLICATION name ENABLE|DISABLE
alter_application_statement
    : ALTER APPLICATION ID SET config_item (COMMA config_item)*    # alterApplicationConfig
    | ALTER APPLICATION ID (ENABLE | DISABLE)                       # alterApplicationEnableDisable
    ;

// SHOW APPLICATIONS / SHOW APPLICATION name
show_applications_statement
    : SHOW APPLICATIONS                                  # showAllApplications
    | SHOW APPLICATION ID                                # showApplicationDetail
    | SHOW APPLICATION ID SKILLS                         # showApplicationSkills
    | SHOW APPLICATION ID INTENTS                        # showApplicationIntents
    | SHOW APPLICATION ID HISTORY                        # showApplicationHistory
    ;

// EXTEND APPLICATION name ADD|REMOVE|MODIFY ...
extend_application_statement
    : EXTEND APPLICATION ID ADD application_extension    # extendApplicationAdd
    | EXTEND APPLICATION ID REMOVE application_removal   # extendApplicationRemove
    ;

application_extension
    : SKILL skill_definition                             # addSkillExtension
    | INTENT intent_mapping                              # addIntentExtension
    | SOURCE source_definition                           # addSourceExtension
    ;

application_removal
    : SKILL ID                                           # removeSkillExtension
    | INTENT STRING                                      # removeIntentExtension
    | SOURCE ID                                          # removeSourceExtension
    ;

// APPLICATION 'name' | STATUS / PAUSE / RESUME / HISTORY
application_control_statement
    : APPLICATION (STRING | ID) PIPE application_control_operation
    ;

application_control_operation
    : STATUS                                             # appStatusOperation
    | PAUSE                                              # appPauseOperation
    | RESUME                                             # appResumeOperation
    | HISTORY (LIMIT INT)?                               # appHistoryOperation
    ;

// ============================================================================
// STANDALONE SKILL STATEMENTS
// ============================================================================

skill_statement
    : create_skill_statement
    | create_skill_pack_statement
    | drop_skill_statement
    | show_skills_statement
    | alter_skill_statement
    | generate_skill_statement
    | test_skill_statement
    ;

// CREATE SKILL name
//   VERSION 'semver'
//   [DESCRIPTION 'desc']
//   [AUTHOR 'author']
//   [TAGS ['tag1', 'tag2']]
//   [PARAMETERS (...)]
//   [RETURNS type]
//   BEGIN ... END SKILL
create_skill_statement
    : CREATE SKILL ID
      VERSION STRING
      (DESCRIPTION STRING)?
      (AUTHOR STRING)?
      (TAGS arrayLiteral)?
      (REQUIRES arrayLiteral)?
      (skill_parameters_clause)?
      (RETURNS datatype)?
      BEGIN statement+ END_SKILL
    ;

skill_parameters_clause
    : LPAREN skill_param_list RPAREN
    ;

skill_param_list
    : skill_param (COMMA skill_param)*
    ;

skill_param
    : ID datatype (DESCRIPTION STRING)? (DEFAULT expression)?
    ;

// CREATE SKILL PACK name VERSION 'semver' DESCRIPTION 'desc' SKILLS [...]
create_skill_pack_statement
    : CREATE SKILL PACK ID
      VERSION STRING
      (DESCRIPTION STRING)?
      (AUTHOR STRING)?
      (TAGS arrayLiteral)?
      SKILLS arrayLiteral
      END_PACK?
    ;

// DROP SKILL name [VERSION 'semver']
drop_skill_statement
    : DROP SKILL ID (VERSION STRING)?
    | DROP SKILL PACK ID
    ;

// SHOW SKILLS / SHOW SKILL name / SHOW SKILL name VERSIONS
show_skills_statement
    : SHOW SKILLS                                        # showAllSkills
    | SHOW SKILL ID                                      # showSkillDetail
    | SHOW SKILL ID VERSION STRING                       # showSkillVersion
    | SHOW SKILL PACK ID                                 # showSkillPackDetail
    | SHOW SKILL PACKS                                   # showAllSkillPacks
    ;

// ALTER SKILL name SET DESCRIPTION = 'new desc'
alter_skill_statement
    : ALTER SKILL ID SET skill_property EQ expression
    ;

skill_property
    : DESCRIPTION
    | AUTHOR
    ;

// TEST SKILL name WITH param = value, ... EXPECT expression
test_skill_statement
    : TEST_KW SKILL ID
      (WITH skill_test_args)?
      EXPECT expression
    ;

skill_test_args
    : skill_test_arg (COMMA skill_test_arg)*
    ;

skill_test_arg
    : ID ASSIGN expression
    ;

// GENERATE SKILL FROM 'natural language description'
// GENERATE SKILL FROM 'desc' WITH MODEL 'gpt-4'
generate_skill_statement
    : GENERATE SKILL FROM STRING (WITH MODEL STRING)? (SAVE_KW AS ID)?
    ;

// ============================================================================
// CONNECTOR STATEMENTS (External Data Sources)
// ============================================================================

connector_statement
    : create_connector_statement
    | drop_connector_statement
    | show_connectors_statement
    | test_connector_statement
    | sync_connector_statement
    | alter_connector_statement
    | exec_connector_statement
    | query_connector_statement
    ;

// CREATE CONNECTOR name TYPE 'type' CONFIG { ... } [OPTIONS { ... }]
// Example:
//   CREATE CONNECTOR github_org TYPE 'github' CONFIG { "token": "{{secrets.github_token}}", "org": "mycompany" };
create_connector_statement
    : CREATE CONNECTOR ID TYPE STRING CONFIG documentLiteral (OPTIONS documentLiteral)?
    ;

// DROP CONNECTOR name
drop_connector_statement
    : DROP CONNECTOR ID
    ;

// SHOW CONNECTORS / SHOW CONNECTOR name
show_connectors_statement
    : SHOW CONNECTORS                                    # showAllConnectors
    | SHOW CONNECTOR ID                                  # showConnectorDetail
    | SHOW CONNECTOR ID STATUS                           # showConnectorStatus
    ;

// TEST CONNECTOR name
test_connector_statement
    : TEST_KW CONNECTOR ID
    ;

// SYNC CONNECTOR name TO 'index-pattern' [INCREMENTAL ON field] [SCHEDULE 'cron']
// Example:
//   SYNC CONNECTOR github_org TO github-issues-* SCHEDULE '*/15 * * * *';
//   SYNC CONNECTOR github_org.issues TO github-issues-* INCREMENTAL ON updated_at;
sync_connector_statement
    : SYNC CONNECTOR connector_entity_ref TO STRING 
      (INCREMENTAL ON_KW ID)?
      (SCHEDULE STRING)?
    ;

connector_entity_ref
    : ID (DOT ID)?                                       // connector_name or connector_name.entity
    ;

// ALTER CONNECTOR name SET OPTIONS { ... }
alter_connector_statement
    : ALTER CONNECTOR ID SET OPTIONS documentLiteral     # alterConnectorOptions
    | ALTER CONNECTOR ID (ENABLE | DISABLE)              # alterConnectorEnableDisable
    ;

// EXEC connector_name.action(args) - Execute a connector action
// Example: EXEC github_org.get_pull_requests(repo='myrepo', state='open')
exec_connector_statement
    : EXEC connector_action_call
    ;

connector_action_call
    : ID DOT ID LPAREN connector_args? RPAREN
    ;

connector_args
    : connector_arg (COMMA connector_arg)*
    ;

connector_arg
    : ID ASSIGN expression                               // named argument
    | expression                                         // positional argument
    ;

// QUERY connector_name.entity WHERE condition - Query connector data
// Example: QUERY github_org.issues WHERE repo = 'myrepo' AND state = 'open'
query_connector_statement
    : QUERY ID DOT ID (WHERE_CMD expression)? (LIMIT INT)? (ORDER BY expression (ASC | DESC)?)?
    ;

// ============================================================================
// AGENT STATEMENTS (Autonomous Executors)
// ============================================================================

agent_statement
    : create_agent_statement
    | drop_agent_statement
    | show_agents_statement
    | alter_agent_statement
    | start_stop_agent_statement
    | trigger_agent_statement
    ;

// CREATE AGENT name
//   GOAL 'description'
//   SKILLS [skill1, skill2, ...]
//   [EXECUTION mode]
//   [TRIGGERS [...]]
//   [MODEL 'model']
//   [CONFIG { ... }]
//   BEGIN ... END AGENT
create_agent_statement
    : CREATE AGENT ID
      GOAL STRING
      SKILLS LBRACKET agent_skill_list RBRACKET
      (EXECUTION agent_execution_mode)?
      (TRIGGERS LBRACKET agent_trigger_list RBRACKET)?
      (MODEL STRING)?
      (CONFIG documentLiteral)?
      BEGIN statement+ END_AGENT
    ;

agent_skill_list
    : agent_skill_ref (COMMA agent_skill_ref)*
    ;

agent_skill_ref
    : ID (AT ID)?                                        // skill_name or skill_name@version
    | ID LBRACKET ID RBRACKET                            // skill_name[approval] or skill_name[forbidden]
    ;

agent_execution_mode
    : AUTONOMOUS
    | HUMAN_APPROVAL
    | SUPERVISED
    | DRY_RUN
    ;

agent_trigger_list
    : agent_trigger_def (COMMA agent_trigger_def)*
    ;

agent_trigger_def
    : ON_KW SCHEDULE STRING                              // ON SCHEDULE '*/5 * * * *'
    | ON_KW ALERT STRING                                 // ON ALERT 'high-cpu-usage'
    | ON_KW ALERT WHERE_CMD expression                   // ON ALERT WHERE severity = 'critical'
    | ON_KW ID                                           // ON MANUAL, ON EVENT 'x'
    ;

// DROP AGENT name
drop_agent_statement
    : DROP AGENT ID
    ;

// SHOW AGENTS / SHOW AGENT name / SHOW AGENT name EXECUTIONS / SHOW AGENT name METRICS
show_agents_statement
    : SHOW AGENTS                                        # showAllAgents
    | SHOW AGENT ID                                      # showAgentDetail
    | SHOW AGENT ID EXECUTION STRING                     # showAgentExecution
    | SHOW AGENT ID HISTORY                              # showAgentHistory
    ;

// ALTER AGENT name CONFIG { ... }
alter_agent_statement
    : ALTER AGENT ID SET CONFIG documentLiteral          # alterAgentConfig
    | ALTER AGENT ID SET EXECUTION agent_execution_mode  # alterAgentExecution
    ;

// START AGENT name / STOP AGENT name
start_stop_agent_statement
    : (ENABLE | DISABLE) AGENT ID                        # enableDisableAgent
    ;

// Manually trigger an agent: TRIGGER AGENT name WITH { context }
trigger_agent_statement
    : TRIGGER AGENT ID (WITH documentLiteral)?
    ;
