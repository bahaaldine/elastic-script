CREATE SKILL cluster_health_check
VERSION '1.0.0'
DESCRIPTION 'Perform a comprehensive cluster health check'
AUTHOR 'Moltler'
TAGS ['cluster', 'health', 'monitoring', 'ops']
(
    index_pattern IN STRING DEFAULT NULL
)
RETURNS DOCUMENT
BEGIN
    DECLARE health DOCUMENT;
    DECLARE stats DOCUMENT;
    DECLARE result DOCUMENT;
    
    -- Get cluster health
    SET health = ES_CLUSTER_HEALTH(index_pattern);
    
    -- Get cluster stats
    SET stats = ES_CLUSTER_STATS();
    
    -- Build comprehensive report
    SET result = {
        'cluster_name': health['cluster_name'],
        'status': health['status'],
        'nodes': {
            'total': health['number_of_nodes'],
            'data': health['number_of_data_nodes']
        },
        'shards': {
            'active': health['active_shards'],
            'primary': health['active_primary_shards'],
            'relocating': health['relocating_shards'],
            'initializing': health['initializing_shards'],
            'unassigned': health['unassigned_shards']
        },
        'active_shards_percent': health['active_shards_percent'],
        'indices': stats['indices'],
        'is_healthy': health['status'] = 'green'
    };
    
    RETURN result;
END SKILL;
