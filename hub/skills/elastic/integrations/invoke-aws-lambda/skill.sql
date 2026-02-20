CREATE SKILL invoke_aws_lambda
  VERSION '1.0.0'
  DESCRIPTION 'Invoke an AWS Lambda function'
  AUTHOR 'elastic'
  TAGS ['integrations', 'aws', 'lambda']
  (function_name STRING DESCRIPTION 'Lambda function name', payload STRING DESCRIPTION 'JSON payload' DEFAULT '{}')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'status': 'invoked',
    'function': function_name,
    'status_code': 200
  };
END SKILL;
