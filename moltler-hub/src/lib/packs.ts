export interface SkillPack {
  id: string;
  name: string;
  version: string;
  description: string;
  author: string;
  category: 'observability' | 'security' | 'search' | 'automation';
  skills: string[];
  installCount?: number;
  icon: string;
}

export const SKILL_PACKS: SkillPack[] = [
  {
    id: 'incident-response',
    name: 'Incident Response',
    version: '1.0.0',
    description: 'Complete incident response automation - from detection to postmortem',
    author: 'sre-team',
    category: 'observability',
    skills: [
      'detect_incident@1.0.0',
      'triage_incident@1.0.0',
      'notify_oncall@2.0.0',
      'create_postmortem@1.0.0',
      'track_resolution@1.0.0'
    ],
    installCount: 1243,
    icon: '🚨'
  },
  {
    id: 'threat-hunting',
    name: 'Threat Hunting',
    version: '2.1.0',
    description: 'Proactive threat detection with IOC hunting, user behavior analysis, and alert correlation',
    author: 'security-team',
    category: 'security',
    skills: [
      'hunt_ioc@2.0.0',
      'get_risky_users@1.5.0',
      'detect_anomalies@1.0.0',
      'correlate_alerts@1.2.0',
      'enrich_with_threat_intel@1.0.0'
    ],
    installCount: 892,
    icon: '🎯'
  },
  {
    id: 'log-analysis',
    name: 'Log Analysis',
    version: '1.2.0',
    description: 'Comprehensive log analysis - errors, patterns, correlations, and trends',
    author: 'platform-team',
    category: 'observability',
    skills: [
      'get_recent_errors@1.0.0',
      'count_logs_by_level@1.0.0',
      'correlate_logs@1.1.0',
      'detect_log_patterns@1.0.0',
      'get_error_trends@1.0.0'
    ],
    installCount: 2156,
    icon: '📋'
  },
  {
    id: 'performance-monitoring',
    name: 'Performance Monitoring',
    version: '1.0.0',
    description: 'APM and performance analysis - slow transactions, latency, throughput',
    author: 'observability-team',
    category: 'observability',
    skills: [
      'get_slow_transactions@1.0.0',
      'get_service_latency@1.0.0',
      'get_throughput_stats@1.0.0',
      'detect_performance_regression@1.0.0',
      'compare_deployments@1.0.0'
    ],
    installCount: 1567,
    icon: '⚡'
  },
  {
    id: 'semantic-search-suite',
    name: 'Semantic Search Suite',
    version: '1.0.0',
    description: 'AI-powered search with embeddings, reranking, and hybrid search',
    author: 'search-team',
    category: 'search',
    skills: [
      'semantic_search@1.0.0',
      'hybrid_search@1.0.0',
      'rerank_results@1.0.0',
      'generate_embeddings@1.0.0',
      'search_with_filters@1.0.0'
    ],
    installCount: 734,
    icon: '🧠'
  },
  {
    id: 'compliance-audit',
    name: 'Compliance Audit',
    version: '1.1.0',
    description: 'Security compliance checks - access reviews, policy violations, audit trails',
    author: 'compliance-team',
    category: 'security',
    skills: [
      'get_failed_logins@1.0.0',
      'audit_user_access@1.0.0',
      'detect_policy_violations@1.0.0',
      'generate_compliance_report@1.0.0',
      'track_privileged_actions@1.0.0'
    ],
    installCount: 456,
    icon: '📜'
  },
  {
    id: 'data-quality',
    name: 'Data Quality',
    version: '1.0.0',
    description: 'Data validation, deduplication, and quality monitoring',
    author: 'data-team',
    category: 'automation',
    skills: [
      'detect_duplicates@1.0.0',
      'validate_schema@1.0.0',
      'check_data_freshness@1.0.0',
      'find_missing_fields@1.0.0',
      'calculate_quality_score@1.0.0'
    ],
    installCount: 321,
    icon: '✅'
  },
  {
    id: 'alerting-automation',
    name: 'Alerting Automation',
    version: '1.0.0',
    description: 'Smart alerting with deduplication, escalation, and auto-remediation',
    author: 'sre-team',
    category: 'automation',
    skills: [
      'dedupe_alerts@1.0.0',
      'escalate_alert@1.0.0',
      'auto_acknowledge@1.0.0',
      'route_to_team@1.0.0',
      'trigger_runbook@1.0.0'
    ],
    installCount: 678,
    icon: '🔔'
  }
];

export const PACK_CATEGORIES = [
  { id: 'observability', name: 'Observability', icon: '📊', color: 'purple' },
  { id: 'security', name: 'Security', icon: '🛡️', color: 'red' },
  { id: 'search', name: 'Search', icon: '🔍', color: 'blue' },
  { id: 'automation', name: 'Automation', icon: '⚙️', color: 'green' }
];
