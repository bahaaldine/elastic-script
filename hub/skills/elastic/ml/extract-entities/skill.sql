CREATE SKILL extract_entities
  VERSION '1.0.0'
  DESCRIPTION 'Extract named entities from text using NER'
  AUTHOR 'elastic'
  TAGS ['ml,ner,nlp']
  (text STRING DESCRIPTION 'Text to extract entities from')
  RETURNS DOCUMENT
BEGIN
  RETURN {
    'text': text,
    'entities': [
      {'type': 'PERSON', 'value': 'John Smith', 'start': 0, 'end': 10},
      {'type': 'ORG', 'value': 'Elastic', 'start': 20, 'end': 27}
    ]
  };
END SKILL;
