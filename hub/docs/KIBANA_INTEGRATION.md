# Kibana API Integration

This document describes the comprehensive Kibana API integration for elastic-script, providing over 150 built-in functions across 15 API categories.

## Overview

The Kibana integration enables automation and scripting of Kibana management tasks directly from elastic-script. All functions communicate with Kibana's REST API and support both API key and basic authentication.

## Configuration

### Environment Variables

```bash
# Kibana URL (defaults to http://localhost:5601)
export KIBANA_URL=http://localhost:5601

# Authentication - Option 1: API Key
export KIBANA_API_KEY=your-api-key

# Authentication - Option 2: Basic Auth
export ELASTIC_USER=elastic-admin
export ELASTIC_PASSWORD=elastic-password
```

## Function Categories

### 1. Core Kibana Functions (KibanaFunctions)

| Function | Description |
|----------|-------------|
| `KIBANA_REQUEST(method, path, body, kibana_url)` | Make a generic Kibana API request |
| `KIBANA_STATUS(kibana_url)` | Get Kibana status |
| `KIBANA_FEATURES(kibana_url)` | Get available Kibana features |

### 2. Alerting Functions (AlertingFunctions)

| Function | Description |
|----------|-------------|
| `ALERT_RULE_LIST(filter, per_page, page)` | List all alert rules |
| `ALERT_RULE_GET(rule_id)` | Get a specific alert rule |
| `ALERT_RULE_CREATE(config)` | Create a new alert rule |
| `ALERT_RULE_UPDATE(rule_id, config)` | Update an alert rule |
| `ALERT_RULE_DELETE(rule_id)` | Delete an alert rule |
| `ALERT_RULE_ENABLE(rule_id)` | Enable an alert rule |
| `ALERT_RULE_DISABLE(rule_id)` | Disable an alert rule |
| `ALERT_RULE_MUTE(rule_id)` | Mute all alerts for a rule |
| `ALERT_RULE_UNMUTE(rule_id)` | Unmute all alerts for a rule |
| `ALERT_FIND(query)` | Search for alerts |
| `ALERT_STATUS_SET(rule_id, alert_id, action)` | Set alert status |
| `ALERT_RULE_TYPES()` | Get available rule types |

### 3. Cases Functions (CaseFunctions)

| Function | Description |
|----------|-------------|
| `CASE_LIST(status, tags, per_page)` | List cases |
| `CASE_GET(case_id)` | Get a specific case |
| `CASE_CREATE(title, description, tags, severity, connector)` | Create a new case |
| `CASE_UPDATE(case_id, version, updates)` | Update a case |
| `CASE_DELETE(case_ids)` | Delete cases |
| `CASE_COMMENT_ADD(case_id, comment, owner)` | Add a comment |
| `CASE_COMMENT_LIST(case_id)` | List comments |
| `CASE_ALERTS(case_id)` | Get alerts attached to a case |
| `CASE_PUSH(case_id, connector_id)` | Push case to external system |
| `CASE_TAGS()` | Get all case tags |

### 4. Connectors Functions (ConnectorFunctions)

| Function | Description |
|----------|-------------|
| `CONNECTOR_LIST()` | List all connectors |
| `CONNECTOR_GET(connector_id)` | Get a specific connector |
| `CONNECTOR_CREATE(connector_type, name, config, secrets)` | Create a connector |
| `CONNECTOR_UPDATE(connector_id, name, config, secrets)` | Update a connector |
| `CONNECTOR_DELETE(connector_id)` | Delete a connector |
| `CONNECTOR_EXECUTE(connector_id, params)` | Execute a connector action |
| `CONNECTOR_TYPES()` | Get available connector types |

### 5. Dashboard Functions (DashboardFunctions)

| Function | Description |
|----------|-------------|
| `DASHBOARD_LIST(search, per_page)` | List dashboards |
| `DASHBOARD_GET(dashboard_id)` | Get a specific dashboard |
| `DASHBOARD_CREATE(title, panels, options, id)` | Create a dashboard |
| `DASHBOARD_UPDATE(dashboard_id, attributes)` | Update a dashboard |
| `DASHBOARD_DELETE(dashboard_id)` | Delete a dashboard |
| `DASHBOARD_DUPLICATE(dashboard_id, new_title)` | Duplicate a dashboard |
| `VISUALIZATION_LIST(search, per_page)` | List visualizations |
| `VISUALIZATION_CREATE(title, vis_type, vis_state, references)` | Create a visualization |
| `LENS_LIST(search, per_page)` | List Lens visualizations |
| `LENS_CREATE(title, state, references)` | Create a Lens visualization |

### 6. Data View Functions (DataViewFunctions)

| Function | Description |
|----------|-------------|
| `DATA_VIEW_LIST()` | List all data views |
| `DATA_VIEW_GET(view_id)` | Get a specific data view |
| `DATA_VIEW_CREATE(title, name, time_field, id)` | Create a data view |
| `DATA_VIEW_UPDATE(view_id, updates)` | Update a data view |
| `DATA_VIEW_DELETE(view_id)` | Delete a data view |
| `DATA_VIEW_REFRESH(view_id)` | Refresh data view fields |
| `DATA_VIEW_FIELDS(pattern)` | Get fields for a pattern |
| `DATA_VIEW_SET_DEFAULT(view_id)` | Set default data view |
| `DATA_VIEW_RUNTIME_FIELD_CREATE(view_id, name, type, script)` | Create runtime field |
| `DATA_VIEW_RUNTIME_FIELD_DELETE(view_id, field_name)` | Delete runtime field |

### 7. Detection Functions (DetectionFunctions)

| Function | Description |
|----------|-------------|
| `DETECTION_RULE_LIST(per_page, page, filter)` | List detection rules |
| `DETECTION_RULE_GET(rule_id)` | Get a detection rule |
| `DETECTION_RULE_CREATE(rule_config)` | Create a detection rule |
| `DETECTION_RULE_UPDATE(rule_id, updates)` | Update a detection rule |
| `DETECTION_RULE_DELETE(rule_id)` | Delete a detection rule |
| `DETECTION_RULE_ENABLE(rule_ids)` | Enable detection rules |
| `DETECTION_RULE_DISABLE(rule_ids)` | Disable detection rules |
| `DETECTION_RULE_BULK_ACTION(action, rule_ids, query)` | Bulk rule action |
| `SIGNAL_QUERY(query)` | Query security signals |
| `SIGNAL_STATUS_UPDATE(signal_ids, status)` | Update signal status |
| `EXCEPTION_LIST_CREATE(list_id, name, description, type)` | Create exception list |
| `EXCEPTION_LIST_GET(list_id)` | Get exception list |
| `EXCEPTION_ITEM_ADD(list_id, name, entries, description)` | Add exception item |
| `TIMELINE_LIST(page_size)` | List security timelines |
| `TIMELINE_GET(timeline_id)` | Get a timeline |
| `TIMELINE_CREATE(timeline)` | Create a timeline |
| `PREBUILT_RULES_INSTALL()` | Install prebuilt rules |
| `PREBUILT_RULES_STATUS()` | Get prebuilt rules status |

### 8. Entity Store Functions (EntityStoreFunctions)

| Function | Description |
|----------|-------------|
| `ENTITY_STORE_INIT(entity_types)` | Initialize entity store |
| `ENTITY_STORE_STATUS()` | Get entity store status |
| `ENTITY_STORE_DELETE(data_view_id)` | Delete entity store |
| `ENTITY_LIST(entity_type, page, per_page)` | List entities |
| `ENTITY_GET(entity_type, entity_id)` | Get an entity |
| `ASSET_CRITICALITY_LIST(page, per_page)` | List asset criticality |
| `ASSET_CRITICALITY_SET(id_field, id_value, criticality_level)` | Set criticality |
| `ASSET_CRITICALITY_DELETE(id_field, id_value)` | Delete criticality |
| `RISK_SCORE_LIST(entity_type, page, per_page)` | List risk scores |
| `RISK_SCORE_PREVIEW(config)` | Preview risk scores |

### 9. Fleet Functions (FleetFunctions)

| Function | Description |
|----------|-------------|
| `AGENT_LIST(per_page, page, kuery)` | List agents |
| `AGENT_GET(agent_id)` | Get an agent |
| `AGENT_UNENROLL(agent_id, revoke)` | Unenroll an agent |
| `AGENT_REASSIGN(agent_id, policy_id)` | Reassign agent to policy |
| `AGENT_UPGRADE(agent_id, version)` | Upgrade an agent |
| `AGENT_BULK_UPGRADE(agents, version)` | Bulk upgrade agents |
| `AGENT_ACTION(agent_id, action)` | Send action to agent |
| `AGENT_POLICY_LIST(per_page, page)` | List agent policies |
| `AGENT_POLICY_GET(policy_id)` | Get agent policy |
| `AGENT_POLICY_CREATE(name, namespace, description)` | Create policy |
| `AGENT_POLICY_UPDATE(policy_id, updates)` | Update policy |
| `AGENT_POLICY_DELETE(policy_id)` | Delete policy |
| `AGENT_POLICY_COPY(policy_id, new_name)` | Copy policy |
| `PACKAGE_POLICY_LIST(per_page, kuery)` | List package policies |
| `PACKAGE_POLICY_CREATE(config)` | Create package policy |
| `PACKAGE_POLICY_UPDATE(policy_id, config)` | Update package policy |
| `PACKAGE_POLICY_DELETE(policy_ids)` | Delete package policies |
| `PACKAGE_LIST(category)` | List integrations |
| `PACKAGE_INSTALL(package_name, version)` | Install integration |
| `PACKAGE_UNINSTALL(package_name, version)` | Uninstall integration |
| `ENROLLMENT_TOKEN_LIST(policy_id)` | List enrollment tokens |
| `ENROLLMENT_TOKEN_CREATE(policy_id, name)` | Create enrollment token |

### 10. ML Functions (MlFunctions)

| Function | Description |
|----------|-------------|
| `ML_ANOMALY_JOB_LIST()` | List anomaly detection jobs |
| `ML_ANOMALY_JOB_GET(job_id)` | Get an anomaly job |
| `ML_ANOMALY_JOB_CREATE(job_id, config)` | Create anomaly job |
| `ML_ANOMALY_JOB_DELETE(job_id)` | Delete anomaly job |
| `ML_ANOMALY_JOB_OPEN(job_id)` | Open anomaly job |
| `ML_ANOMALY_JOB_CLOSE(job_id)` | Close anomaly job |
| `ML_ANOMALY_JOB_START(job_id, start, end)` | Start datafeed |
| `ML_ANOMALY_JOB_STOP(job_id)` | Stop datafeed |
| `ML_ANOMALY_JOB_RESULTS(job_id, result_type, size)` | Get job results |
| `ML_DFA_JOB_LIST()` | List data frame analytics jobs |
| `ML_DFA_JOB_CREATE(job_id, config)` | Create DFA job |
| `ML_DFA_JOB_START(job_id)` | Start DFA job |
| `ML_DFA_JOB_STOP(job_id)` | Stop DFA job |
| `ML_TRAINED_MODEL_LIST()` | List trained models |
| `ML_TRAINED_MODEL_GET(model_id)` | Get a trained model |
| `ML_TRAINED_MODEL_DELETE(model_id)` | Delete trained model |
| `ML_TRAINED_MODEL_DEPLOY(model_id)` | Deploy model |
| `ML_TRAINED_MODEL_UNDEPLOY(model_id)` | Undeploy model |

### 11. Saved Object Functions (SavedObjectFunctions)

| Function | Description |
|----------|-------------|
| `SAVED_OBJECT_FIND(type, search, per_page)` | Find saved objects |
| `SAVED_OBJECT_GET(type, id)` | Get a saved object |
| `SAVED_OBJECT_CREATE(type, id, attributes, references)` | Create saved object |
| `SAVED_OBJECT_UPDATE(type, id, attributes)` | Update saved object |
| `SAVED_OBJECT_DELETE(type, id)` | Delete saved object |
| `SAVED_OBJECT_BULK_GET(objects)` | Bulk get saved objects |
| `SAVED_OBJECT_EXPORT(types, objects)` | Export saved objects |
| `SAVED_OBJECT_IMPORT(objects, overwrite)` | Import saved objects |

### 12. SLO Functions (SloFunctions)

| Function | Description |
|----------|-------------|
| `SLO_LIST(name, page, per_page)` | List SLOs |
| `SLO_GET(slo_id)` | Get an SLO |
| `SLO_CREATE(config)` | Create an SLO |
| `SLO_UPDATE(slo_id, updates)` | Update an SLO |
| `SLO_DELETE(slo_id)` | Delete an SLO |
| `SLO_ENABLE(slo_id)` | Enable an SLO |
| `SLO_DISABLE(slo_id)` | Disable an SLO |
| `SLO_RESET(slo_id)` | Reset an SLO |
| `SLO_BURN_RATES(slo_id)` | Get SLO burn rates |

### 13. Space Functions (SpaceFunctions)

| Function | Description |
|----------|-------------|
| `SPACE_LIST()` | List all spaces |
| `SPACE_GET(space_id)` | Get a space |
| `SPACE_CREATE(id, name, description, color, initials, disabled_features)` | Create a space |
| `SPACE_UPDATE(space_id, updates)` | Update a space |
| `SPACE_DELETE(space_id)` | Delete a space |
| `SPACE_COPY_OBJECTS(source_space, dest_spaces, objects, include_references, overwrite)` | Copy objects between spaces |

### 14. Synthetics Functions (SyntheticsFunctions)

| Function | Description |
|----------|-------------|
| `SYNTHETICS_MONITOR_LIST(page, per_page, tags)` | List monitors |
| `SYNTHETICS_MONITOR_GET(monitor_id)` | Get a monitor |
| `SYNTHETICS_MONITOR_CREATE(config)` | Create a monitor |
| `SYNTHETICS_MONITOR_UPDATE(monitor_id, config)` | Update a monitor |
| `SYNTHETICS_MONITOR_DELETE(monitor_ids)` | Delete monitors |
| `SYNTHETICS_MONITOR_ENABLE(monitor_id)` | Enable a monitor |
| `SYNTHETICS_MONITOR_DISABLE(monitor_id)` | Disable a monitor |
| `SYNTHETICS_PRIVATE_LOCATION_LIST()` | List private locations |
| `SYNTHETICS_PRIVATE_LOCATION_CREATE(label, agent_policy_id, tags)` | Create private location |
| `SYNTHETICS_PRIVATE_LOCATION_DELETE(location_id)` | Delete private location |
| `SYNTHETICS_PARAMS(action, params)` | Manage synthetics parameters |

### 15. APM Functions (ApmFunctions)

| Function | Description |
|----------|-------------|
| `APM_SERVICE_LIST(start, end, environment)` | List APM services |
| `APM_SERVICE_GET(service_name, start, end)` | Get service details |
| `APM_TRANSACTIONS(service_name, start, end, transaction_type)` | Get transactions |
| `APM_ERRORS(service_name, start, end)` | Get service errors |
| `APM_SERVICE_MAP(start, end, environment)` | Get service map |
| `APM_AGENT_CONFIG_LIST()` | List agent configurations |
| `APM_AGENT_CONFIG_CREATE(service_name, settings, environment)` | Create agent config |
| `APM_SOURCEMAP_UPLOAD(service_name, service_version, bundle_filepath, sourcemap)` | Upload sourcemap |
| `APM_ANNOTATION_CREATE(service_name, message, timestamp, tags)` | Create annotation |

## Usage Examples

### Create an Alert Rule

```sql
DECLARE rule_config DOCUMENT;
DECLARE result DOCUMENT;

SET rule_config = {
  'name': 'High CPU Alert',
  'rule_type_id': '.es-query',
  'consumer': 'alerts',
  'schedule': {'interval': '5m'},
  'params': {
    'esQuery': '{"query": {"range": {"system.cpu.total.pct": {"gt": 0.9}}}}',
    'index': ['metrics-*'],
    'timeField': '@timestamp',
    'threshold': [0],
    'thresholdComparator': '>'
  },
  'actions': []
};

SET result = ALERT_RULE_CREATE(rule_config);
PRINT result;
```

### Create a Case

```sql
DECLARE result DOCUMENT;

SET result = CASE_CREATE(
  'Suspicious Activity Detected',
  'User account showing signs of compromise',
  ['security', 'high-priority'],
  'high',
  NULL
);

PRINT result;
```

### Install a Fleet Integration

```sql
DECLARE result DOCUMENT;

SET result = PACKAGE_INSTALL('nginx', '');

IF result.success = TRUE THEN
  PRINT 'Integration installed: ' || result.data.items[0].version;
ELSE
  PRINT 'Failed: ' || result.error;
END IF;
```

### Create an SLO

```sql
DECLARE slo_config DOCUMENT;
DECLARE result DOCUMENT;

SET slo_config = {
  'name': 'API Availability SLO',
  'description': '99.9% availability for API endpoints',
  'indicator': {
    'type': 'sli.kql.custom',
    'params': {
      'index': 'logs-*',
      'good': 'http.response.status_code < 500',
      'total': '*'
    }
  },
  'budgetingMethod': 'occurrences',
  'objective': {
    'target': 0.999
  },
  'timeWindow': {
    'duration': '30d',
    'type': 'rolling'
  }
};

SET result = SLO_CREATE(slo_config);
PRINT result;
```

### Deploy a Trained Model

```sql
DECLARE result DOCUMENT;

SET result = ML_TRAINED_MODEL_DEPLOY('.elser_model_2');

IF result.success = TRUE THEN
  PRINT 'Model deployment started';
ELSE
  PRINT 'Deployment failed: ' || result.error;
END IF;
```

## Skills Available

The following Moltler skills wrap these functions for easier use:

| Category | Skills |
|----------|--------|
| Alerting | `list_alert_rules`, `create_alert_rule`, `enable_alert_rule`, `disable_alert_rule` |
| Cases | `list_cases`, `create_case`, `add_case_comment` |
| Dashboards | `list_dashboards`, `create_dashboard` |
| Data Views | `list_data_views`, `create_data_view` |
| Detections | `list_detection_rules`, `install_prebuilt_rules` |
| Entity Store | `entity_store_status` |
| Fleet | `list_fleet_agents`, `upgrade_fleet_agent`, `install_integration` |
| ML | `list_ml_anomaly_jobs`, `deploy_trained_model` |
| SLO | `list_slos`, `create_slo` |
| Spaces | `list_spaces`, `create_space` |
| Connectors | `list_connectors`, `execute_connector` |
| APM | `list_apm_services` |
| Synthetics | `list_synthetics_monitors`, `create_synthetics_monitor` |
| Saved Objects | `export_saved_objects` |

## Total Function Count

- **150+ built-in functions** across 15 categories
- **30+ ready-to-use skills** in MoltlerHub
- Full coverage of major Kibana APIs

## API Reference

For complete Kibana API documentation, see:
- [Kibana API Documentation](https://www.elastic.co/docs/api/doc/kibana/)
- [Elastic Security API](https://www.elastic.co/guide/en/security/current/security-apis.html)
- [Fleet API](https://www.elastic.co/guide/en/fleet/current/fleet-apis.html)

---

*Last updated: January 22, 2026*
