CREATE SKILL install_integration
  VERSION '1.0.0'
  DESCRIPTION 'Install a Fleet integration package'
  AUTHOR 'elastic'
  TAGS ['kibana', 'fleet', 'integrations', 'install']
  (
    package_name STRING DESCRIPTION 'Name of the integration package (e.g., nginx, aws)',
    version STRING DESCRIPTION 'Version to install (optional, latest if not specified)' DEFAULT NULL
  )
  RETURNS DOCUMENT
BEGIN
  DECLARE result DOCUMENT;
  
  IF version IS NOT NULL THEN
    SET result = PACKAGE_INSTALL(package_name, version);
  ELSE
    SET result = PACKAGE_INSTALL(package_name, '');
  END IF;
  
  IF result.success = TRUE THEN
    RETURN {
      'status': 'installed',
      'package': package_name,
      'version': result.data.items[0].version,
      'message': 'Integration installed successfully'
    };
  ELSE
    RETURN {
      'status': 'error',
      'error': result.error
    };
  END IF;
END SKILL;
