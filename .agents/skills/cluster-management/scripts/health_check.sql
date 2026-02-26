-- Comprehensive Cluster Health Check
-- Usage: Execute via elastic-script to get full cluster health report

DECLARE health DOCUMENT;
DECLARE stats DOCUMENT;
DECLARE nodes_stats DOCUMENT;
DECLARE report DOCUMENT;

-- Get cluster health
SET health = ES_CLUSTER_HEALTH();

-- Get cluster stats
SET stats = ES_CLUSTER_STATS();

-- Get node stats
SET nodes_stats = ES_NODES_STATS('_all', 'fs,jvm,os');

-- Build report
SET report = {
    'timestamp': CURRENT_TIMESTAMP(),
    'cluster': {
        'name': health['cluster_name'],
        'status': health['status'],
        'is_healthy': health['status'] = 'green'
    },
    'nodes': {
        'total': health['number_of_nodes'],
        'data_nodes': health['number_of_data_nodes']
    },
    'shards': {
        'active': health['active_shards'],
        'primary': health['active_primary_shards'],
        'relocating': health['relocating_shards'],
        'initializing': health['initializing_shards'],
        'unassigned': health['unassigned_shards'],
        'active_percent': health['active_shards_percent_as_number']
    },
    'indices': {
        'count': stats['indices']['count'],
        'docs_count': stats['indices']['docs']['count'],
        'store_size_bytes': stats['indices']['store']['size_in_bytes']
    }
};

-- Add warnings
DECLARE warnings ARRAY;
SET warnings = [];

IF health['status'] = 'red' THEN
    SET warnings = ARRAY_APPEND(warnings, 'CRITICAL: Cluster status is RED - data loss possible');
ELSEIF health['status'] = 'yellow' THEN
    SET warnings = ARRAY_APPEND(warnings, 'WARNING: Cluster status is YELLOW - replicas unavailable');
END IF;

IF health['unassigned_shards'] > 0 THEN
    SET warnings = ARRAY_APPEND(warnings, 'Unassigned shards: ' || health['unassigned_shards']);
END IF;

IF health['relocating_shards'] > 0 THEN
    SET warnings = ARRAY_APPEND(warnings, 'Relocating shards: ' || health['relocating_shards']);
END IF;

SET report['warnings'] = warnings;

-- Print summary
PRINT '=== Cluster Health Report ===';
PRINT 'Cluster: ' || report['cluster']['name'];
PRINT 'Status: ' || report['cluster']['status'];
PRINT 'Nodes: ' || report['nodes']['total'] || ' total, ' || report['nodes']['data_nodes'] || ' data';
PRINT 'Shards: ' || report['shards']['active'] || ' active, ' || report['shards']['unassigned'] || ' unassigned';
PRINT 'Indices: ' || report['indices']['count'];

IF ARRAY_LENGTH(warnings) > 0 THEN
    PRINT '';
    PRINT '=== Warnings ===';
    FOR warning IN warnings LOOP
        PRINT '⚠️  ' || warning;
    END LOOP;
END IF;

RETURN report;
