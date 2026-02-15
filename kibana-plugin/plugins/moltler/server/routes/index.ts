import type { IRouter, Logger } from '@kbn/core/server';
import { schema } from '@kbn/config-schema';

export function defineRoutes(router: IRouter, logger: Logger) {
  // List all skills
  router.get(
    {
      path: '/api/moltler/skills',
      security: {
        authz: {
          enabled: false,
          reason: 'This route is open for all authenticated users',
        },
      },
      validate: false,
    },
    async (context, request, response) => {
      try {
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        
        // Check if skills index exists
        const indexExists = await esClient.indices.exists({ index: '.escript_skills' });
        
        if (!indexExists) {
          return response.ok({
            body: {
              skills: [],
              count: 0,
            },
          });
        }
        
        const result = await esClient.search({
          index: '.escript_skills',
          size: 100,
          query: { match_all: {} },
        });
        
        const skills = result.hits.hits.map((hit: any) => ({
          name: hit._id,
          ...hit._source,
        }));
        
        return response.ok({
          body: {
            skills,
            count: skills.length,
          },
        });
      } catch (error: any) {
        logger.error(`Failed to fetch skills: ${error.message}`);
        return response.customError({
          statusCode: 500,
          body: { message: error.message },
        });
      }
    }
  );

  // Get a specific skill
  router.get(
    {
      path: '/api/moltler/skills/{name}',
      security: {
        authz: {
          enabled: false,
          reason: 'This route is open for all authenticated users',
        },
      },
      validate: {
        params: schema.object({
          name: schema.string(),
        }),
      },
    },
    async (context, request, response) => {
      try {
        const { name } = request.params;
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        
        const result = await esClient.get({
          index: '.escript_skills',
          id: name,
        });
        
        return response.ok({
          body: {
            name: result._id,
            ...result._source,
          },
        });
      } catch (error: any) {
        if (error.meta?.statusCode === 404) {
          return response.notFound({
            body: { message: `Skill '${request.params.name}' not found` },
          });
        }
        logger.error(`Failed to fetch skill: ${error.message}`);
        return response.customError({
          statusCode: 500,
          body: { message: error.message },
        });
      }
    }
  );

  // Run a skill
  router.post(
    {
      path: '/api/moltler/skills/{name}/run',
      security: {
        authz: {
          enabled: false,
          reason: 'This route is open for all authenticated users',
        },
      },
      validate: {
        params: schema.object({
          name: schema.string(),
        }),
        body: schema.object({
          parameters: schema.maybe(schema.recordOf(schema.string(), schema.any())),
        }),
      },
    },
    async (context, request, response) => {
      try {
        const { name } = request.params;
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        
        const startTime = Date.now();
        
        // Execute the skill via _escript endpoint
        const result = await esClient.transport.request({
          method: 'POST',
          path: '/_escript',
          body: {
            query: `RUN SKILL ${name}`,
          },
        });
        
        const executionTime = Date.now() - startTime;
        
        return response.ok({
          body: {
            success: true,
            result,
            executionTime,
          },
        });
      } catch (error: any) {
        logger.error(`Failed to run skill: ${error.message}`);
        return response.customError({
          statusCode: 500,
          body: {
            success: false,
            error: error.message,
          },
        });
      }
    }
  );

  // List indices (for context awareness)
  router.get(
    {
      path: '/api/moltler/indices',
      security: {
        authz: {
          enabled: false,
          reason: 'This route is open for all authenticated users',
        },
      },
      validate: false,
    },
    async (context, request, response) => {
      try {
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        
        const result = await esClient.cat.indices({
          format: 'json',
          h: 'index,docs.count,store.size,health,status',
        });
        
        // Filter out system indices
        const indices = (result as any[]).filter(
          (idx: any) => !idx.index.startsWith('.')
        );
        
        return response.ok({
          body: { indices },
        });
      } catch (error: any) {
        logger.error(`Failed to fetch indices: ${error.message}`);
        return response.customError({
          statusCode: 500,
          body: { message: error.message },
        });
      }
    }
  );

  // Example endpoint (keeping for compatibility)
  router.get(
    {
      path: '/api/moltler/example',
      security: {
        authz: {
          enabled: false,
          reason: 'This route is open for all authenticated users',
        },
      },
      validate: false,
    },
    async (context, request, response) => {
      return response.ok({
        body: {
          time: new Date().toISOString(),
        },
      });
    }
  );
}
