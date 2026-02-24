CREATE SKILL validate_skill_syntax
  VERSION '1.0.0'
  DESCRIPTION 'Validate the syntax of a skill definition before installation. Checks for common errors and returns detailed feedback.'
  AUTHOR 'elastic'
  TAGS ['meta', 'validation', 'development']
  (
    skill_code STRING DESCRIPTION 'The complete skill code to validate (CREATE SKILL ... END SKILL)'
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE errors ARRAY;
  DECLARE warnings ARRAY;
  DECLARE is_valid BOOLEAN;
  DECLARE skill_name STRING;
  
  SET errors = [];
  SET warnings = [];
  SET is_valid = TRUE;
  
  -- Check required clauses
  IF skill_code NOT LIKE '%CREATE SKILL%' THEN
    SET is_valid = FALSE;
    SET errors = ARRAY_APPEND(errors, {
      'code': 'MISSING_CREATE',
      'message': 'Missing CREATE SKILL statement',
      'hint': 'Start with: CREATE SKILL skill_name'
    });
  END IF;
  
  IF skill_code NOT LIKE '%END SKILL%' THEN
    SET is_valid = FALSE;
    SET errors = ARRAY_APPEND(errors, {
      'code': 'MISSING_END',
      'message': 'Missing END SKILL statement',
      'hint': 'End with: END SKILL;'
    });
  END IF;
  
  IF skill_code NOT LIKE '%BEGIN%' THEN
    SET is_valid = FALSE;
    SET errors = ARRAY_APPEND(errors, {
      'code': 'MISSING_BEGIN',
      'message': 'Missing BEGIN statement',
      'hint': 'Add BEGIN before the skill body'
    });
  END IF;
  
  IF skill_code NOT LIKE '%RETURNS%' THEN
    SET is_valid = FALSE;
    SET errors = ARRAY_APPEND(errors, {
      'code': 'MISSING_RETURNS',
      'message': 'Missing RETURNS clause',
      'hint': 'Add: RETURNS ARRAY (or DOCUMENT, STRING, INTEGER, BOOLEAN)'
    });
  END IF;
  
  IF skill_code NOT LIKE '%VERSION%' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'MISSING_VERSION',
      'message': 'Missing VERSION clause',
      'hint': 'Add: VERSION ''1.0.0'''
    });
  END IF;
  
  IF skill_code NOT LIKE '%DESCRIPTION%' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'MISSING_DESCRIPTION',
      'message': 'Missing DESCRIPTION clause',
      'hint': 'Add: DESCRIPTION ''What this skill does'''
    });
  END IF;
  
  IF skill_code NOT LIKE '%AUTHOR%' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'MISSING_AUTHOR',
      'message': 'Missing AUTHOR clause',
      'hint': 'Add: AUTHOR ''your_name'''
    });
  END IF;
  
  IF skill_code NOT LIKE '%TAGS%' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'MISSING_TAGS',
      'message': 'Missing TAGS clause',
      'hint': 'Add: TAGS [''category'', ''type'']'
    });
  END IF;
  
  -- Check for RETURN statement in body
  IF skill_code NOT LIKE '%RETURN %' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'MISSING_RETURN',
      'message': 'No RETURN statement found in body',
      'hint': 'Add: RETURN result; before END SKILL'
    });
  END IF;
  
  -- Check for common syntax issues
  IF skill_code LIKE '%""%' THEN
    SET warnings = ARRAY_APPEND(warnings, {
      'code': 'POSSIBLE_QUOTE_ISSUE',
      'message': 'Found double quotes - use single quotes for strings in skill code',
      'hint': 'Replace "text" with ''text'''
    });
  END IF;
  
  -- Try to extract skill name
  DECLARE name_start INTEGER;
  DECLARE name_end INTEGER;
  SET name_start = INSTR(skill_code, 'CREATE SKILL ') + 13;
  IF name_start > 13 THEN
    SET name_end = INSTR(SUBSTR(skill_code, name_start), ' ');
    IF name_end > 0 THEN
      SET skill_name = TRIM(SUBSTR(skill_code, name_start, name_end - 1));
    ELSE
      SET skill_name = 'unknown';
    END IF;
  ELSE
    SET skill_name = 'unknown';
  END IF;
  
  RETURN {
    'valid': is_valid,
    'skill_name': skill_name,
    'errors': errors,
    'error_count': ARRAY_LENGTH(errors),
    'warnings': warnings,
    'warning_count': ARRAY_LENGTH(warnings),
    'summary': CASE 
      WHEN is_valid AND ARRAY_LENGTH(warnings) = 0 THEN 'Valid skill with no warnings'
      WHEN is_valid THEN 'Valid skill with ' || ARRAY_LENGTH(warnings) || ' warning(s)'
      ELSE 'Invalid skill with ' || ARRAY_LENGTH(errors) || ' error(s)'
    END,
    'next_step': CASE 
      WHEN is_valid THEN 'Install with: POST /_escript {"query": "<skill_code>"}'
      ELSE 'Fix the errors above and re-validate'
    END
  };
END SKILL;
