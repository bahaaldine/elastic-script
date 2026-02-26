CREATE SKILL create_snapshot_backup
VERSION '1.0.0'
DESCRIPTION 'Create a snapshot backup of specified indices'
AUTHOR 'Moltler'
TAGS ['snapshot', 'backup', 'disaster-recovery']
(
    repository IN STRING,
    snapshot_name IN STRING,
    indices IN STRING DEFAULT '*',
    wait_for_completion IN BOOLEAN DEFAULT true
)
RETURNS DOCUMENT
BEGIN
    DECLARE result DOCUMENT;
    
    -- Create snapshot
    SET result = ES_CREATE_SNAPSHOT(repository, snapshot_name, indices, wait_for_completion);
    
    RETURN {
        'repository': repository,
        'snapshot': snapshot_name,
        'indices': indices,
        'state': result['state'],
        'success': result['state'] = 'SUCCESS' OR result['accepted'] = true
    };
END SKILL;
