"""
PL|ESQL Jupyter Kernel
======================
A custom Jupyter kernel for elastic-script (PL|ESQL).

Sends code to Elasticsearch's _escript endpoint and displays results.
Automatically sends traces to OTEL Collector for observability.
"""

import json
import logging
import sys
import time
import uuid
import re
import requests
from ipykernel.kernelbase import Kernel

# Setup logging to stdout so it appears in Jupyter console
logging.basicConfig(
    level=logging.DEBUG,
    format='[PL|ESQL] %(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger('plesql_kernel')


# =============================================================================
# OpenTelemetry Tracing (lightweight, no SDK dependency)
# =============================================================================

class OTLPTracer:
    """Lightweight OTLP tracer that sends traces directly via HTTP."""
    
    def __init__(self, endpoint="http://localhost:4318", service_name="elastic-script"):
        self.endpoint = f"{endpoint}/v1/traces"
        self.service_name = service_name
        self.enabled = True
    
    def _generate_trace_id(self):
        """Generate a 32-character hex trace ID."""
        return uuid.uuid4().hex
    
    def _generate_span_id(self):
        """Generate a 16-character hex span ID."""
        return uuid.uuid4().hex[:16]
    
    def send_span(self, name, duration_ms, attributes=None, status_ok=True):
        """Send a single span to the OTEL Collector."""
        if not self.enabled:
            return
        
        try:
            trace_id = self._generate_trace_id()
            span_id = self._generate_span_id()
            end_time_ns = int(time.time() * 1e9)
            start_time_ns = end_time_ns - int(duration_ms * 1e6)
            
            # Build attributes following OTEL attribute value types
            span_attributes = []
            if attributes:
                for key, value in attributes.items():
                    if isinstance(value, bool):  # Check bool first (bool is subclass of int)
                        span_attributes.append({"key": key, "value": {"boolValue": value}})
                    elif isinstance(value, int):
                        span_attributes.append({"key": key, "value": {"intValue": str(value)}})
                    elif isinstance(value, float):
                        span_attributes.append({"key": key, "value": {"doubleValue": value}})
                    elif isinstance(value, str):
                        span_attributes.append({"key": key, "value": {"stringValue": value}})
                    elif isinstance(value, list):
                        # Array of strings
                        span_attributes.append({"key": key, "value": {"arrayValue": {"values": [{"stringValue": str(v)} for v in value]}}})
            
            payload = {
                "resourceSpans": [{
                    "resource": {
                        "attributes": [
                            {"key": "service.name", "value": {"stringValue": self.service_name}},
                            {"key": "service.version", "value": {"stringValue": "1.0.0"}}
                        ]
                    },
                    "scopeSpans": [{
                        "scope": {"name": "plesql.kernel", "version": "1.0"},
                        "spans": [{
                            "traceId": trace_id,
                            "spanId": span_id,
                            "name": name,
                            "kind": 1,  # INTERNAL
                            "startTimeUnixNano": str(start_time_ns),
                            "endTimeUnixNano": str(end_time_ns),
                            "attributes": span_attributes,
                            "status": {"code": 1 if status_ok else 2}  # 1=OK, 2=ERROR
                        }]
                    }]
                }]
            }
            
            response = requests.post(
                self.endpoint,
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=2
            )
            
            if response.status_code == 200:
                logger.debug(f"Sent trace: {name} (trace_id={trace_id[:8]}...)")
            else:
                logger.debug(f"Failed to send trace: {response.status_code}")
                
        except Exception as e:
            # Don't fail execution if tracing fails
            logger.debug(f"Tracing error (non-fatal): {e}")


# Global tracer instance
_tracer = OTLPTracer()


class PlesqlKernel(Kernel):
    implementation = 'plesql'
    implementation_version = '1.0'
    language = 'plesql'
    language_version = '1.0'
    language_info = {
        'name': 'plesql',
        'mimetype': 'text/x-sql',
        'file_extension': '.plesql',
        'codemirror_mode': 'sql',
    }
    banner = "elastic-script Kernel - Run PL|ESQL procedures in Elasticsearch"

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.es_endpoint = "http://localhost:9200/_escript"
        # Default credentials for dev builds (./gradlew :run)
        self.es_auth = ("elastic-admin", "elastic-password")
        logger.info(f"PL|ESQL Kernel initialized. Endpoint: {self.es_endpoint}")
    
    def do_execute(self, code, silent, store_history=True, user_expressions=None, allow_stdin=False):
        """Execute the code by sending it to Elasticsearch."""
        
        logger.info(f"Executing cell (silent={silent}):")
        logger.debug(f"Code:\n{code[:200]}..." if len(code) > 200 else f"Code:\n{code}")
        
        # Skip empty cells only
        code = code.strip()
        if not code:
            logger.info("Skipping empty cell")
            return self._success_response()
        
        # Check if cell is ONLY comments (all lines start with --)
        lines = [line.strip() for line in code.split('\n') if line.strip()]
        if all(line.startswith('--') for line in lines):
            logger.info("Skipping comment-only cell")
            return self._success_response()
        
        headers = {"Content-Type": "application/json"}
        payload = {"query": code}
        
        logger.info(f"Sending request to {self.es_endpoint}")
        
        # Extract procedure/function name for tracing
        span_name = self._extract_span_name(code)
        start_time = time.time()
        status_ok = True
        error_msg = None
        
        try:
            response = requests.post(
                self.es_endpoint, 
                headers=headers, 
                data=json.dumps(payload),
                auth=self.es_auth,
                timeout=300  # 5 minute timeout for long-running procedures
            )
            logger.info(f"Response status: {response.status_code}")
            logger.debug(f"Response body: {response.text[:500]}")
            response.raise_for_status()
            json_resp = response.json()
            result = json_resp.get("result", json_resp)
            output = json_resp.get("output", [])  # PRINT statements (when available)
            
            logger.info(f"Got result: {type(result)}, output lines: {len(output) if output else 0}")
            
            if not silent:
                # Display PRINT output first (if any)
                if output and isinstance(output, list) and len(output) > 0:
                    self._display_output(output)
                
                # Display the result
                self._display_result(result)
            else:
                logger.info("Silent mode, not displaying result")
                
        except requests.exceptions.ConnectionError as e:
            logger.error(f"Connection error: {e}")
            self._send_error("Connection Error: Cannot connect to Elasticsearch at localhost:9200\n"
                           "Make sure Elasticsearch is running: ./scripts/quick-start.sh --start")
            status_ok = False
            error_msg = "Connection error"
        except requests.exceptions.Timeout:
            logger.error("Request timed out")
            self._send_error("Timeout: Procedure took too long to execute")
            status_ok = False
            error_msg = "Timeout"
        except requests.exceptions.HTTPError as e:
            logger.error(f"HTTP error: {e.response.status_code} - {e.response.text[:200]}")
            try:
                error_detail = e.response.json()
                error_msg = error_detail.get("error", {}).get("reason", str(e))
            except:
                error_msg = str(e)
            self._send_error(f"Elasticsearch Error: {error_msg}")
            status_ok = False
        except Exception as e:
            logger.exception(f"Unexpected error: {e}")
            self._send_error(f"Error: {str(e)}")
            status_ok = False
            error_msg = str(e)
        
        # Send trace to OTEL Collector
        duration_ms = (time.time() - start_time) * 1000
        
        # Build comprehensive trace attributes following OTEL semantic conventions
        trace_attributes = {
            # OTEL DB semantic conventions
            "db.system": "elasticsearch",
            "db.operation": "execute",
            "db.statement": code[:500] if len(code) <= 500 else code[:497] + "...",
            
            # elastic-script specific attributes
            "escript.statement.type": span_name.split()[0] if ' ' in span_name else span_name,
            "escript.statement.name": span_name,
            "escript.execution.cell_number": self.execution_count,
            "escript.execution.duration_ms": round(duration_ms, 2),
            "escript.execution.status": "ok" if status_ok else "error",
            
            # Code metrics
            "escript.code.length": len(code),
            "escript.code.lines": code.count('\n') + 1,
        }
        
        if error_msg:
            trace_attributes["error.type"] = "ExecutionError"
            trace_attributes["error.message"] = error_msg[:500]
        
        _tracer.send_span(
            name=f"escript: {span_name}",
            duration_ms=duration_ms,
            attributes=trace_attributes,
            status_ok=status_ok
        )
        
        return self._success_response()
    
    def _extract_span_name(self, code):
        """Extract a meaningful name from the code for the span."""
        code_upper = code.upper().strip()
        
        # Try to extract procedure name from CALL
        call_match = re.search(r'CALL\s+(\w+)', code_upper)
        if call_match:
            return f"CALL {call_match.group(1)}"
        
        # Try to extract procedure name from CREATE PROCEDURE
        create_proc_match = re.search(r'CREATE\s+(?:OR\s+REPLACE\s+)?PROCEDURE\s+(\w+)', code_upper)
        if create_proc_match:
            return f"CREATE PROCEDURE {create_proc_match.group(1)}"
        
        # Try to extract function name from CREATE FUNCTION
        create_func_match = re.search(r'CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+(\w+)', code_upper)
        if create_func_match:
            return f"CREATE FUNCTION {create_func_match.group(1)}"
        
        # Try to detect statement types with patterns
        patterns = [
            (r'^DECLARE\s+(\w+)', 'DECLARE'),
            (r'^SET\s+(\w+)', 'SET'),
            (r'^PRINT\b', 'PRINT'),
            (r'^IF\b', 'IF'),
            (r'^FOR\b', 'FOR'),
            (r'^WHILE\b', 'WHILE'),
            (r'^TRY\b', 'TRY'),
            (r'^BEGIN\b', 'BEGIN'),
            (r'^DROP\s+PROCEDURE\s+(\w+)', 'DROP PROCEDURE'),
            (r'^DROP\s+FUNCTION\s+(\w+)', 'DROP FUNCTION'),
            (r'^DEFINE\s+INTENT\s+(\w+)', 'DEFINE INTENT'),
            (r'^ASYNC\b', 'ASYNC'),
            (r'^PARALLEL\b', 'PARALLEL'),
            (r'^EXECUTION\b', 'EXECUTION'),
        ]
        
        for pattern, name in patterns:
            match = re.search(pattern, code_upper)
            if match:
                if match.lastindex:  # Has a capture group
                    return f"{name} {match.group(1)}"
                return name
        
        # Check for function calls
        if 'ESQL_QUERY' in code_upper:
            return "ESQL_QUERY"
        if 'LLM_' in code_upper:
            llm_match = re.search(r'(LLM_\w+)', code_upper)
            if llm_match:
                return llm_match.group(1)
        if 'INFERENCE' in code_upper:
            return "INFERENCE"
        
        # Fallback: first 30 chars
        return code[:30].replace('\n', ' ').strip()
    
    def _display_result(self, result):
        """Display the result in the notebook."""
        
        # Handle list of dicts (table data)
        if isinstance(result, list) and result and isinstance(result[0], dict):
            try:
                import pandas as pd
                df = pd.DataFrame(result)
                self.send_response(self.iopub_socket, 'execute_result', {
                    'execution_count': self.execution_count,
                    'data': {
                        'text/plain': str(df),
                        'text/html': df.to_html(index=False, classes='table table-striped')
                    },
                    'metadata': {}
                })
            except ImportError:
                # Fallback if pandas not available
                self._send_stream(json.dumps(result, indent=2))
        
        # Handle dict (document)
        elif isinstance(result, dict):
            self.send_response(self.iopub_socket, 'execute_result', {
                'execution_count': self.execution_count,
                'data': {
                    'text/plain': json.dumps(result, indent=2),
                    'application/json': result
                },
                'metadata': {}
            })
        
        # Handle simple values
        else:
            self._send_stream(str(result))
    
    def _display_output(self, output):
        """Display PRINT statement output."""
        if output:
            # Format PRINT output with a distinct prefix
            formatted = "\n".join([f"📝 {line}" for line in output])
            self.send_response(self.iopub_socket, 'stream', {
                'name': 'stdout',
                'text': formatted + '\n'
            })
    
    def _send_stream(self, text):
        """Send text to stdout stream."""
        self.send_response(self.iopub_socket, 'stream', {
            'name': 'stdout',
            'text': text + '\n'
        })
    
    def _send_error(self, text):
        """Send error to stderr stream."""
        self.send_response(self.iopub_socket, 'stream', {
            'name': 'stderr', 
            'text': text + '\n'
        })
    
    def _success_response(self):
        """Return a success execution response."""
        return {
            'status': 'ok',
            'execution_count': self.execution_count,
            'payload': [],
            'user_expressions': {},
        }
    
    def do_complete(self, code, cursor_pos):
        """Basic code completion."""
        # Simple keyword completion
        keywords = [
            'CREATE', 'PROCEDURE', 'BEGIN', 'END', 'DECLARE', 'SET', 'IF', 'THEN',
            'ELSE', 'ELSEIF', 'FOR', 'WHILE', 'LOOP', 'END LOOP', 'RETURN', 'PRINT',
            'CALL', 'TRY', 'CATCH', 'FINALLY', 'END TRY', 'THROW', 'BREAK', 'CONTINUE',
            'INTENT', 'DEFINE', 'REQUIRES', 'ACTIONS', 'ON_FAILURE', 'END INTENT',
            'ON_DONE', 'ON_FAIL', 'FINALLY', 'TRACK AS', 'TIMEOUT', 'EXECUTION',
            'STATUS', 'CANCEL', 'RETRY', 'WAIT', 'PARALLEL', 'ON_ALL_DONE', 'ON_ANY_FAIL',
            'STRING', 'NUMBER', 'BOOLEAN', 'DOCUMENT', 'ARRAY', 'DATE',
            'CURSOR', 'FROM', 'WHERE', 'LIMIT', 'SORT', 'KEEP',
            'SWITCH', 'CASE', 'DEFAULT', 'END SWITCH',
            'VAR', 'CONST', 'IN', 'NOT IN', 'IS NULL', 'IS NOT NULL',
            'AND', 'OR', 'NOT', 'TRUE', 'FALSE', 'NULL'
        ]
        
        # Find the word being typed
        word_start = cursor_pos
        while word_start > 0 and code[word_start - 1].isalnum():
            word_start -= 1
        
        prefix = code[word_start:cursor_pos].upper()
        matches = [kw for kw in keywords if kw.startswith(prefix)]
        
        return {
            'status': 'ok',
            'matches': matches,
            'cursor_start': word_start,
            'cursor_end': cursor_pos,
            'metadata': {}
        }


if __name__ == "__main__":
    from ipykernel.kernelapp import IPKernelApp
    IPKernelApp.launch_instance(kernel_class=PlesqlKernel)



