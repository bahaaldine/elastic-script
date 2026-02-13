import type { IRouter, Logger } from '@kbn/core/server';
import { schema } from '@kbn/config-schema';

/**
 * Define server routes that proxy to Elasticsearch elastic-script endpoints
 */
export function defineRoutes(router: IRouter, logger: Logger) {
  // GET /api/moltler/skills - List all skills
  router.get(
    {
      path: '/api/moltler/skills',
      validate: false,
    },
    async (context, request, response) => {
      try {
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;

        // Query the .escript_skills index
        const result = await esClient.search({
          index: '.escript_skills',
          size: 1000,
          query: { match_all: {} },
          sort: [{ name: 'asc' }],
        });

        const skills = result.hits.hits.map((hit) => ({
          id: hit._id,
          ...hit._source as Record<string, unknown>,
        }));

        return response.ok({
          body: {
            skills,
            count: skills.length,
          },
        });
      } catch (error: any) {
        // If index doesn't exist, return empty list
        if (error.meta?.statusCode === 404) {
          return response.ok({
            body: {
              skills: [],
              count: 0,
            },
          });
        }
        logger.error(`Error fetching skills: ${error.message}`);
        return response.customError({
          statusCode: error.meta?.statusCode || 500,
          body: { message: error.message },
        });
      }
    }
  );

  // GET /api/moltler/skills/:name - Get a specific skill
  router.get(
    {
      path: '/api/moltler/skills/{name}',
      validate: {
        params: schema.object({
          name: schema.string(),
        }),
      },
    },
    async (context, request, response) => {
      try {
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        const { name } = request.params;

        const result = await esClient.get({
          index: '.escript_skills',
          id: name,
        });

        return response.ok({
          body: {
            skill: {
              id: result._id,
              ...result._source as Record<string, unknown>,
            },
          },
        });
      } catch (error: any) {
        if (error.meta?.statusCode === 404) {
          return response.notFound({
            body: { message: `Skill '${request.params.name}' not found` },
          });
        }
        logger.error(`Error fetching skill: ${error.message}`);
        return response.customError({
          statusCode: error.meta?.statusCode || 500,
          body: { message: error.message },
        });
      }
    }
  );

  // POST /api/moltler/skills/:name/run - Execute a skill
  router.post(
    {
      path: '/api/moltler/skills/{name}/run',
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
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        const { name } = request.params;

        // Execute the skill via _escript endpoint
        const result = await esClient.transport.request({
          method: 'POST',
          path: '/_escript',
          body: {
            query: `RUN SKILL ${name}`,
          },
        });

        return response.ok({
          body: {
            success: true,
            result,
          },
        });
      } catch (error: any) {
        logger.error(`Error running skill: ${error.message}`);
        return response.customError({
          statusCode: error.meta?.statusCode || 500,
          body: {
            success: false,
            message: error.message,
          },
        });
      }
    }
  );

  // GET /api/moltler/indices - List available indices for context awareness
  router.get(
    {
      path: '/api/moltler/indices',
      validate: {
        query: schema.object({
          pattern: schema.maybe(schema.string()),
        }),
      },
    },
    async (context, request, response) => {
      try {
        const esClient = (await context.core).elasticsearch.client.asCurrentUser;
        const pattern = request.query.pattern || '*';

        const result = await esClient.cat.indices({
          index: pattern,
          format: 'json',
          h: 'index,docs.count,store.size,health,status',
        });

        // Filter out system indices
        const indices = (result as any[]).filter(
          (idx) => !idx.index.startsWith('.') || idx.index.startsWith('.escript')
        );

        return response.ok({
          body: {
            indices,
            count: indices.length,
          },
        });
      } catch (error: any) {
        logger.error(`Error fetching indices: ${error.message}`);
        return response.customError({
          statusCode: error.meta?.statusCode || 500,
          body: { message: error.message },
        });
      }
    }
  );
}
