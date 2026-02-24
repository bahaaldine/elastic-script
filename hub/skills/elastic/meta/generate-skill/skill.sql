CREATE SKILL generate_skill
  VERSION '1.0.0'
  DESCRIPTION 'Generate a new skill from a natural language description using Elasticsearch Inference API. Describe what you want and get working skill code.'
  AUTHOR 'elastic'
  TAGS ['meta', 'ai', 'generation', 'inference', 'nlp']
  (
    description STRING DESCRIPTION 'Natural language description of what you want the skill to do',
    skill_name STRING DESCRIPTION 'Name for the generated skill (auto-generated if not provided)' DEFAULT NULL,
    model_id STRING DESCRIPTION 'Inference endpoint to use' DEFAULT '.elser-2-elasticsearch'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE prompt STRING;
  DECLARE generated_code STRING;
  DECLARE skill_template STRING;
  DECLARE suggested_name STRING;
  
  -- Generate a skill name if not provided
  IF skill_name IS NULL THEN
    SET suggested_name = 'custom_skill_' || SUBSTR(MD5(description), 1, 6);
  ELSE
    SET suggested_name = skill_name;
  END IF;
  
  -- Build context about available capabilities
  SET skill_template = '
CREATE SKILL ' || suggested_name || '
  VERSION ''1.0.0''
  DESCRIPTION ''[Generated skill description]''
  AUTHOR ''user''
  TAGS [''custom'', ''generated'']
  (
    -- Add parameters here with types: STRING, INTEGER, BOOLEAN, ARRAY, DOCUMENT
    -- Example: param_name STRING DESCRIPTION ''description'' DEFAULT ''value''
  )
  RETURNS ARRAY  -- or DOCUMENT, STRING, INTEGER, BOOLEAN
BEGIN
  DECLARE result ARRAY;
  
  -- Your implementation here
  -- Use ESQL_QUERY() for Elasticsearch queries:
  -- SET result = ESQL_QUERY(''FROM index-* | WHERE condition | LIMIT 10'');
  
  -- Use control flow:
  -- IF condition THEN ... END IF;
  -- FOR i IN 1..10 LOOP ... END LOOP;
  
  -- Call other skills:
  -- SET data = RUN SKILL other_skill(param);
  
  RETURN result;
END SKILL;
';

  -- Build the generation prompt
  SET prompt = 'You are a Moltler skill code generator. Generate a complete, valid skill.sql file.

TASK: ' || description || '

TEMPLATE TO FOLLOW:
' || skill_template || '

AVAILABLE FUNCTIONS:
- ESQL_QUERY(query_string) - Execute ES|QL query, returns ARRAY
- ARRAY_LENGTH(arr), ARRAY_APPEND(arr, item), ARRAY_FILTER(arr, condition)
- DOCUMENT_GET(doc, key), DOCUMENT_KEYS(doc), DOCUMENT_MERGE(doc1, doc2)
- LENGTH(str), SUBSTR(str, start, len), UPPER(str), LOWER(str), REPLACE(str, old, new)
- CURRENT_TIMESTAMP(), DATE_ADD(date, interval), DATE_DIFF(unit, date1, date2)
- HTTP_GET(url), HTTP_POST(url, body) - External HTTP calls
- INFERENCE(endpoint, task_type, input) - Call inference API
- RUN SKILL skill_name(params) - Call another skill
- PRINT message - Output for debugging

COMMON ESQL PATTERNS:
- FROM logs-* | WHERE log.level == "ERROR" | LIMIT 10
- FROM metrics-* | STATS avg_cpu = AVG(cpu.usage) BY host.name
- FROM apm-* | WHERE transaction.duration.us > 1000000 | SORT @timestamp DESC

RULES:
1. Always include VERSION, DESCRIPTION, AUTHOR, TAGS
2. Use meaningful parameter names with DESCRIPTION and DEFAULT values
3. Declare all variables with DECLARE before use
4. End the skill with END SKILL;
5. Use single quotes for strings in SQL, escape with double single quotes

Generate ONLY the skill code, no explanations:';

  -- Call Elasticsearch Inference API to generate the skill
  DECLARE inference_result DOCUMENT;
  SET inference_result = INFERENCE_CHAT(
    model_id,
    prompt,
    {
      'temperature': 0.2,
      'max_tokens': 2000
    }
  );
  
  -- Extract generated code
  SET generated_code = inference_result.content;
  
  -- Basic validation
  DECLARE is_valid BOOLEAN;
  DECLARE validation_errors ARRAY;
  SET validation_errors = [];
  SET is_valid = TRUE;
  
  IF generated_code NOT LIKE '%CREATE SKILL%' THEN
    SET is_valid = FALSE;
    SET validation_errors = ARRAY_APPEND(validation_errors, 'Missing CREATE SKILL');
  END IF;
  
  IF generated_code NOT LIKE '%END SKILL%' THEN
    SET is_valid = FALSE;
    SET validation_errors = ARRAY_APPEND(validation_errors, 'Missing END SKILL');
  END IF;
  
  IF generated_code NOT LIKE '%BEGIN%' THEN
    SET is_valid = FALSE;
    SET validation_errors = ARRAY_APPEND(validation_errors, 'Missing BEGIN');
  END IF;
  
  IF generated_code NOT LIKE '%RETURNS%' THEN
    SET is_valid = FALSE;
    SET validation_errors = ARRAY_APPEND(validation_errors, 'Missing RETURNS');
  END IF;
  
  RETURN {
    'status': CASE WHEN is_valid THEN 'success' ELSE 'needs_review' END,
    'skill_name': suggested_name,
    'description': description,
    'generated_code': generated_code,
    'is_valid': is_valid,
    'validation_errors': validation_errors,
    'next_steps': CASE 
      WHEN is_valid THEN [
        'Review the generated code below',
        'Copy and run it to install the skill',
        'Test with: RUN SKILL ' || suggested_name || '()'
      ]
      ELSE [
        'The generated code may have issues',
        'Review and fix the validation errors',
        'Try regenerating with a clearer description'
      ]
    END,
    'install_command': 'POST /_escript { "query": "<paste generated code here>" }'
  };
END SKILL;
