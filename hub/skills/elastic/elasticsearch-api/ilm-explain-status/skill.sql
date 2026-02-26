CREATE SKILL ilm_explain_status
VERSION '1.0.0'
DESCRIPTION 'Explain ILM lifecycle status for indices'
AUTHOR 'Moltler'
TAGS ['ilm', 'lifecycle', 'monitoring']
(
    index_pattern IN STRING
)
RETURNS DOCUMENT
BEGIN
    DECLARE status DOCUMENT;
    DECLARE ilm_status DOCUMENT;
    
    -- Get ILM status for indices
    SET status = ES_ILM_EXPLAIN(index_pattern);
    
    -- Get overall ILM system status
    SET ilm_status = ES_ILM_STATUS();
    
    RETURN {
        'operation_mode': ilm_status['operation_mode'],
        'indices': status
    };
END SKILL;
