"""
PL|ESQL Jupyter Kernel
======================
A custom Jupyter kernel for elastic-script (PL|ESQL).

Sends code to Elasticsearch's _escript endpoint and displays results.
"""

import json
import logging
import sys
import requests
from ipykernel.kernelbase import Kernel

# Setup logging to stdout so it appears in Jupyter console
logging.basicConfig(
    level=logging.DEBUG,
    format='[PL|ESQL] %(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger('plesql_kernel')


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
        except requests.exceptions.Timeout:
            logger.error("Request timed out")
            self._send_error("Timeout: Procedure took too long to execute")
        except requests.exceptions.HTTPError as e:
            logger.error(f"HTTP error: {e.response.status_code} - {e.response.text[:200]}")
            try:
                error_detail = e.response.json()
                error_msg = error_detail.get("error", {}).get("reason", str(e))
            except:
                error_msg = str(e)
            self._send_error(f"Elasticsearch Error: {error_msg}")
        except Exception as e:
            logger.exception(f"Unexpected error: {e}")
            self._send_error(f"Error: {str(e)}")
        
        return self._success_response()
    
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



