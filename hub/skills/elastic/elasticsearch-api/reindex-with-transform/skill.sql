CREATE SKILL reindex_with_transform
VERSION '1.0.0'
DESCRIPTION 'Reindex data from source to destination with optional query filter'
AUTHOR 'Moltler'
TAGS ['reindex', 'migration', 'data-management']
(
    source_index IN STRING,
    dest_index IN STRING,
    query_filter IN STRING DEFAULT '*'
)
RETURNS DOCUMENT
BEGIN
    DECLARE result DOCUMENT;
    
    -- Perform reindex operation
    SET result = ES_REINDEX(source_index, dest_index, null);
    
    -- Refresh destination index
    CALL ES_REFRESH_INDEX(dest_index);
    
    RETURN {
        'source': source_index,
        'destination': dest_index,
        'took_ms': result['took'],
        'total': result['total'],
        'created': result['created'],
        'updated': result['updated'],
        'failures': result['failures']
    };
END SKILL;
