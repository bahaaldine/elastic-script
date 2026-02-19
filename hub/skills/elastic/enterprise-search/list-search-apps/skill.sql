CREATE SKILL list_search_apps
  VERSION '1.0.0'
  DESCRIPTION 'List all Enterprise Search applications'
  AUTHOR 'elastic'
  TAGS ['enterprise-search,apps,search']
  ()
  RETURNS ARRAY
BEGIN
  RETURN [
    {'name': 'company-docs', 'engine': 'elasticsearch', 'documents': 50000},
    {'name': 'product-catalog', 'engine': 'elasticsearch', 'documents': 10000}
  ];
END SKILL;
