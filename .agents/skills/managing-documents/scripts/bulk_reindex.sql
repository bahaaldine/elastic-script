-- Bulk Reindex with Progress Tracking
-- Usage: Set @source_index, @dest_index, and optionally @query before calling

DECLARE source_index STRING;
DECLARE dest_index STRING;
DECLARE query DOCUMENT;

SET source_index = COALESCE(@source_index, 'source-index');
SET dest_index = COALESCE(@dest_index, 'dest-index');
SET query = COALESCE(@query, {'match_all': {}});

-- Check source exists
IF NOT ES_INDEX_EXISTS(source_index) THEN
    THROW 'Source index does not exist: ' || source_index;
END IF;

-- Get document count
DECLARE source_count DOCUMENT;
SET source_count = ES_COUNT(source_index, {'query': query});
DECLARE total_docs NUMBER;
SET total_docs = source_count['count'];

PRINT 'Starting reindex of ' || total_docs || ' documents';
PRINT 'Source: ' || source_index;
PRINT 'Destination: ' || dest_index;

-- Start reindex
DECLARE result DOCUMENT;
SET result = ES_REINDEX(source_index, dest_index, query);

-- Check result
IF result['failures'] IS NOT NULL AND ARRAY_LENGTH(result['failures']) > 0 THEN
    PRINT 'WARNING: Reindex completed with ' || ARRAY_LENGTH(result['failures']) || ' failures';
    FOR failure IN result['failures'] LOOP
        PRINT '  Failed: ' || failure['id'] || ' - ' || failure['cause']['reason'];
    END LOOP;
ELSE
    PRINT 'Reindex completed successfully';
END IF;

PRINT 'Documents processed: ' || result['total'];
PRINT 'Documents created: ' || result['created'];
PRINT 'Documents updated: ' || result['updated'];
PRINT 'Time taken: ' || result['took'] || 'ms';

-- Refresh destination
ES_REFRESH_INDEX(dest_index);

-- Verify count
DECLARE dest_count DOCUMENT;
SET dest_count = ES_COUNT(dest_index);

PRINT 'Destination document count: ' || dest_count['count'];

RETURN {
    'source_index': source_index,
    'dest_index': dest_index,
    'documents_processed': result['total'],
    'documents_created': result['created'],
    'time_ms': result['took'],
    'success': ARRAY_LENGTH(COALESCE(result['failures'], [])) = 0
};
