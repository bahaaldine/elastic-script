/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.functions.kibana;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.AlertingFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.CaseFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.ConnectorFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.DashboardFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.DataViewFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.DetectionFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.EntityStoreFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.FleetFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.KibanaFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.MlFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.SavedObjectFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.SloFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.SpaceFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.SyntheticsFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.kibana.ApmFunctions;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for Kibana function registration.
 */
public class KibanaFunctionsTests extends ESTestCase {

    private ExecutionContext context;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
    }

    public void testKibanaCoreFunctionsRegistration() {
        KibanaFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("KIBANA_REQUEST"));
        assertNotNull(context.getFunction("KIBANA_STATUS"));
        assertNotNull(context.getFunction("KIBANA_FEATURES"));
    }

    public void testAlertingFunctionsRegistration() {
        AlertingFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("ALERT_RULE_LIST"));
        assertNotNull(context.getFunction("ALERT_RULE_GET"));
        assertNotNull(context.getFunction("ALERT_RULE_CREATE"));
        assertNotNull(context.getFunction("ALERT_RULE_UPDATE"));
        assertNotNull(context.getFunction("ALERT_RULE_DELETE"));
        assertNotNull(context.getFunction("ALERT_RULE_ENABLE"));
        assertNotNull(context.getFunction("ALERT_RULE_DISABLE"));
        assertNotNull(context.getFunction("ALERT_RULE_MUTE"));
        assertNotNull(context.getFunction("ALERT_RULE_UNMUTE"));
        assertNotNull(context.getFunction("ALERT_FIND"));
        assertNotNull(context.getFunction("ALERT_STATUS_SET"));
        assertNotNull(context.getFunction("ALERT_RULE_TYPES"));
    }

    public void testCaseFunctionsRegistration() {
        CaseFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("CASE_LIST"));
        assertNotNull(context.getFunction("CASE_GET"));
        assertNotNull(context.getFunction("CASE_CREATE"));
        assertNotNull(context.getFunction("CASE_UPDATE"));
        assertNotNull(context.getFunction("CASE_DELETE"));
        assertNotNull(context.getFunction("CASE_COMMENT_ADD"));
        assertNotNull(context.getFunction("CASE_COMMENT_LIST"));
        assertNotNull(context.getFunction("CASE_ALERTS"));
        assertNotNull(context.getFunction("CASE_PUSH"));
        assertNotNull(context.getFunction("CASE_TAGS"));
    }

    public void testConnectorFunctionsRegistration() {
        ConnectorFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("CONNECTOR_LIST"));
        assertNotNull(context.getFunction("CONNECTOR_GET"));
        assertNotNull(context.getFunction("CONNECTOR_CREATE"));
        assertNotNull(context.getFunction("CONNECTOR_UPDATE"));
        assertNotNull(context.getFunction("CONNECTOR_DELETE"));
        assertNotNull(context.getFunction("CONNECTOR_EXECUTE"));
        assertNotNull(context.getFunction("CONNECTOR_TYPES"));
    }

    public void testDashboardFunctionsRegistration() {
        DashboardFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("DASHBOARD_LIST"));
        assertNotNull(context.getFunction("DASHBOARD_GET"));
        assertNotNull(context.getFunction("DASHBOARD_CREATE"));
        assertNotNull(context.getFunction("DASHBOARD_UPDATE"));
        assertNotNull(context.getFunction("DASHBOARD_DELETE"));
        assertNotNull(context.getFunction("DASHBOARD_DUPLICATE"));
        assertNotNull(context.getFunction("VISUALIZATION_LIST"));
        assertNotNull(context.getFunction("VISUALIZATION_CREATE"));
        assertNotNull(context.getFunction("LENS_LIST"));
        assertNotNull(context.getFunction("LENS_CREATE"));
    }

    public void testDataViewFunctionsRegistration() {
        DataViewFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("DATA_VIEW_LIST"));
        assertNotNull(context.getFunction("DATA_VIEW_GET"));
        assertNotNull(context.getFunction("DATA_VIEW_CREATE"));
        assertNotNull(context.getFunction("DATA_VIEW_UPDATE"));
        assertNotNull(context.getFunction("DATA_VIEW_DELETE"));
        assertNotNull(context.getFunction("DATA_VIEW_REFRESH"));
        assertNotNull(context.getFunction("DATA_VIEW_FIELDS"));
        assertNotNull(context.getFunction("DATA_VIEW_SET_DEFAULT"));
        assertNotNull(context.getFunction("DATA_VIEW_RUNTIME_FIELD_CREATE"));
        assertNotNull(context.getFunction("DATA_VIEW_RUNTIME_FIELD_DELETE"));
    }

    public void testDetectionFunctionsRegistration() {
        DetectionFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("DETECTION_RULE_LIST"));
        assertNotNull(context.getFunction("DETECTION_RULE_GET"));
        assertNotNull(context.getFunction("DETECTION_RULE_CREATE"));
        assertNotNull(context.getFunction("DETECTION_RULE_UPDATE"));
        assertNotNull(context.getFunction("DETECTION_RULE_DELETE"));
        assertNotNull(context.getFunction("DETECTION_RULE_ENABLE"));
        assertNotNull(context.getFunction("DETECTION_RULE_DISABLE"));
        assertNotNull(context.getFunction("DETECTION_RULE_BULK_ACTION"));
        assertNotNull(context.getFunction("SIGNAL_QUERY"));
        assertNotNull(context.getFunction("SIGNAL_STATUS_UPDATE"));
        assertNotNull(context.getFunction("EXCEPTION_LIST_CREATE"));
        assertNotNull(context.getFunction("EXCEPTION_LIST_GET"));
        assertNotNull(context.getFunction("EXCEPTION_ITEM_ADD"));
        assertNotNull(context.getFunction("TIMELINE_LIST"));
        assertNotNull(context.getFunction("TIMELINE_GET"));
        assertNotNull(context.getFunction("TIMELINE_CREATE"));
        assertNotNull(context.getFunction("PREBUILT_RULES_INSTALL"));
        assertNotNull(context.getFunction("PREBUILT_RULES_STATUS"));
    }

    public void testEntityStoreFunctionsRegistration() {
        EntityStoreFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("ENTITY_STORE_INIT"));
        assertNotNull(context.getFunction("ENTITY_STORE_STATUS"));
        assertNotNull(context.getFunction("ENTITY_STORE_DELETE"));
        assertNotNull(context.getFunction("ENTITY_LIST"));
        assertNotNull(context.getFunction("ENTITY_GET"));
        assertNotNull(context.getFunction("ASSET_CRITICALITY_LIST"));
        assertNotNull(context.getFunction("ASSET_CRITICALITY_SET"));
        assertNotNull(context.getFunction("ASSET_CRITICALITY_DELETE"));
        assertNotNull(context.getFunction("RISK_SCORE_LIST"));
        assertNotNull(context.getFunction("RISK_SCORE_PREVIEW"));
    }

    public void testFleetFunctionsRegistration() {
        FleetFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("AGENT_LIST"));
        assertNotNull(context.getFunction("AGENT_GET"));
        assertNotNull(context.getFunction("AGENT_UNENROLL"));
        assertNotNull(context.getFunction("AGENT_REASSIGN"));
        assertNotNull(context.getFunction("AGENT_UPGRADE"));
        assertNotNull(context.getFunction("AGENT_BULK_UPGRADE"));
        assertNotNull(context.getFunction("AGENT_ACTION"));
        assertNotNull(context.getFunction("AGENT_POLICY_LIST"));
        assertNotNull(context.getFunction("AGENT_POLICY_GET"));
        assertNotNull(context.getFunction("AGENT_POLICY_CREATE"));
        assertNotNull(context.getFunction("AGENT_POLICY_UPDATE"));
        assertNotNull(context.getFunction("AGENT_POLICY_DELETE"));
        assertNotNull(context.getFunction("AGENT_POLICY_COPY"));
        assertNotNull(context.getFunction("PACKAGE_POLICY_LIST"));
        assertNotNull(context.getFunction("PACKAGE_POLICY_CREATE"));
        assertNotNull(context.getFunction("PACKAGE_POLICY_UPDATE"));
        assertNotNull(context.getFunction("PACKAGE_POLICY_DELETE"));
        assertNotNull(context.getFunction("PACKAGE_LIST"));
        assertNotNull(context.getFunction("PACKAGE_INSTALL"));
        assertNotNull(context.getFunction("PACKAGE_UNINSTALL"));
        assertNotNull(context.getFunction("ENROLLMENT_TOKEN_LIST"));
        assertNotNull(context.getFunction("ENROLLMENT_TOKEN_CREATE"));
    }

    public void testMlFunctionsRegistration() {
        MlFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_LIST"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_GET"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_CREATE"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_DELETE"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_OPEN"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_CLOSE"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_START"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_STOP"));
        assertNotNull(context.getFunction("ML_ANOMALY_JOB_RESULTS"));
        assertNotNull(context.getFunction("ML_DFA_JOB_LIST"));
        assertNotNull(context.getFunction("ML_DFA_JOB_CREATE"));
        assertNotNull(context.getFunction("ML_DFA_JOB_START"));
        assertNotNull(context.getFunction("ML_DFA_JOB_STOP"));
        assertNotNull(context.getFunction("ML_TRAINED_MODEL_LIST"));
        assertNotNull(context.getFunction("ML_TRAINED_MODEL_GET"));
        assertNotNull(context.getFunction("ML_TRAINED_MODEL_DELETE"));
        assertNotNull(context.getFunction("ML_TRAINED_MODEL_DEPLOY"));
        assertNotNull(context.getFunction("ML_TRAINED_MODEL_UNDEPLOY"));
    }

    public void testSavedObjectFunctionsRegistration() {
        SavedObjectFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("SAVED_OBJECT_FIND"));
        assertNotNull(context.getFunction("SAVED_OBJECT_GET"));
        assertNotNull(context.getFunction("SAVED_OBJECT_CREATE"));
        assertNotNull(context.getFunction("SAVED_OBJECT_UPDATE"));
        assertNotNull(context.getFunction("SAVED_OBJECT_DELETE"));
        assertNotNull(context.getFunction("SAVED_OBJECT_BULK_GET"));
        assertNotNull(context.getFunction("SAVED_OBJECT_EXPORT"));
        assertNotNull(context.getFunction("SAVED_OBJECT_IMPORT"));
    }

    public void testSloFunctionsRegistration() {
        SloFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("SLO_LIST"));
        assertNotNull(context.getFunction("SLO_GET"));
        assertNotNull(context.getFunction("SLO_CREATE"));
        assertNotNull(context.getFunction("SLO_UPDATE"));
        assertNotNull(context.getFunction("SLO_DELETE"));
        assertNotNull(context.getFunction("SLO_ENABLE"));
        assertNotNull(context.getFunction("SLO_DISABLE"));
        assertNotNull(context.getFunction("SLO_RESET"));
        assertNotNull(context.getFunction("SLO_BURN_RATES"));
    }

    public void testSpaceFunctionsRegistration() {
        SpaceFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("SPACE_LIST"));
        assertNotNull(context.getFunction("SPACE_GET"));
        assertNotNull(context.getFunction("SPACE_CREATE"));
        assertNotNull(context.getFunction("SPACE_UPDATE"));
        assertNotNull(context.getFunction("SPACE_DELETE"));
        assertNotNull(context.getFunction("SPACE_COPY_OBJECTS"));
    }

    public void testSyntheticsFunctionsRegistration() {
        SyntheticsFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_LIST"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_GET"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_CREATE"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_UPDATE"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_DELETE"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_ENABLE"));
        assertNotNull(context.getFunction("SYNTHETICS_MONITOR_DISABLE"));
        assertNotNull(context.getFunction("SYNTHETICS_PRIVATE_LOCATION_LIST"));
        assertNotNull(context.getFunction("SYNTHETICS_PRIVATE_LOCATION_CREATE"));
        assertNotNull(context.getFunction("SYNTHETICS_PRIVATE_LOCATION_DELETE"));
        assertNotNull(context.getFunction("SYNTHETICS_PARAMS"));
    }

    public void testApmFunctionsRegistration() {
        ApmFunctions.registerAll(context);
        
        assertNotNull(context.getFunction("APM_SERVICE_LIST"));
        assertNotNull(context.getFunction("APM_SERVICE_GET"));
        assertNotNull(context.getFunction("APM_TRANSACTIONS"));
        assertNotNull(context.getFunction("APM_ERRORS"));
        assertNotNull(context.getFunction("APM_SERVICE_MAP"));
        assertNotNull(context.getFunction("APM_AGENT_CONFIG_LIST"));
        assertNotNull(context.getFunction("APM_AGENT_CONFIG_CREATE"));
        assertNotNull(context.getFunction("APM_SOURCEMAP_UPLOAD"));
        assertNotNull(context.getFunction("APM_ANNOTATION_CREATE"));
    }

    public void testAllFunctionCount() {
        // Register all function classes
        KibanaFunctions.registerAll(context);
        AlertingFunctions.registerAll(context);
        CaseFunctions.registerAll(context);
        ConnectorFunctions.registerAll(context);
        DashboardFunctions.registerAll(context);
        DataViewFunctions.registerAll(context);
        DetectionFunctions.registerAll(context);
        EntityStoreFunctions.registerAll(context);
        FleetFunctions.registerAll(context);
        MlFunctions.registerAll(context);
        SavedObjectFunctions.registerAll(context);
        SloFunctions.registerAll(context);
        SpaceFunctions.registerAll(context);
        SyntheticsFunctions.registerAll(context);
        ApmFunctions.registerAll(context);
        
        int totalFunctions = context.getAllFunctions().size();
        
        // We expect 150+ Kibana functions
        assertTrue("Expected at least 150 Kibana functions, got " + totalFunctions, totalFunctions >= 150);
    }
}
