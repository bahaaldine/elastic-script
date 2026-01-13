/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.features.NodeFeature;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.actions.RestCreateProcedureAction;
import org.elasticsearch.xpack.escript.actions.RestGetProcedureAction;
import org.elasticsearch.xpack.escript.actions.RestRunEScriptAction;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.actions.ElasticScriptAction;
import org.elasticsearch.xpack.escript.actions.TransportElasticScriptAction;
import org.elasticsearch.xpack.escript.scheduling.LeaderElectionService;
import org.elasticsearch.xpack.escript.scheduling.JobSchedulerService;
import org.elasticsearch.xpack.escript.scheduling.TriggerPollingService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ElasticScriptPlugin extends Plugin implements ActionPlugin {
    private ThreadPool threadPool;
    private ElasticScriptExecutor elasticScriptExecutor;
    
    // Scheduling services
    private LeaderElectionService leaderElectionService;
    private JobSchedulerService jobSchedulerService;
    private TriggerPollingService triggerPollingService;

    private static final Logger LOGGER = LogManager.getLogger(ElasticScriptPlugin.class);

    @Override
    public Collection<?> createComponents(PluginServices services) {
        this.threadPool = services.threadPool();
        Client client = services.client();
        ClusterService clusterService = services.clusterService();

        // Initialize ElasticScriptExecutor
        elasticScriptExecutor = new ElasticScriptExecutor(threadPool, client);

        // Initialize scheduling services
        LOGGER.info("Initializing elastic-script scheduling services");
        
        leaderElectionService = new LeaderElectionService(client, threadPool, clusterService);
        jobSchedulerService = new JobSchedulerService(client, threadPool, elasticScriptExecutor, leaderElectionService);
        triggerPollingService = new TriggerPollingService(client, threadPool, elasticScriptExecutor, leaderElectionService);
        
        // Start the services (they will wait for leader election)
        leaderElectionService.start();
        jobSchedulerService.start();
        triggerPollingService.start();
        
        LOGGER.info("Elastic-script scheduling services initialized");

        // Return lifecycle components for proper shutdown
        List<Object> components = new ArrayList<>();
        components.add(leaderElectionService);
        components.add(jobSchedulerService);
        components.add(triggerPollingService);
        return components;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        NamedWriteableRegistry namedWriteableRegistry,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster,
        Predicate<NodeFeature> clusterSupportsFeature
    ) {
        return List.of(
            new RestRunEScriptAction(elasticScriptExecutor),
            new RestGetProcedureAction(elasticScriptExecutor)
        );
    }

    @Override
    public List<ActionHandler> getActions() {
        return List.of(
            new ActionHandler(ElasticScriptAction.INSTANCE, TransportElasticScriptAction.class)
        );
    }
}
