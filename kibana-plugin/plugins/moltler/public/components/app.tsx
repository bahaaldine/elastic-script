import React, { useState, useEffect, useCallback } from 'react';
import { i18n } from '@kbn/i18n';
import { I18nProvider } from '@kbn/i18n-react';
import { BrowserRouter as Router } from '@kbn/shared-ux-router';
import {
  EuiPageTemplate,
  EuiTitle,
  EuiTabs,
  EuiTab,
  EuiSpacer,
  EuiFlexGroup,
  EuiFlexItem,
  EuiFieldSearch,
  EuiBasicTable,
  EuiBasicTableColumn,
  EuiBadge,
  EuiButton,
  EuiButtonIcon,
  EuiEmptyPrompt,
  EuiLoadingSpinner,
  EuiCallOut,
  EuiText,
  EuiDescriptionList,
  EuiCodeBlock,
  EuiPanel,
  EuiIcon,
} from '@elastic/eui';
import type { CoreStart } from '@kbn/core/public';
import type { NavigationPublicPluginStart } from '@kbn/navigation-plugin/public';

import { PLUGIN_ID, PLUGIN_NAME, SkillDefinition } from '../../common';

interface MoltlerAppDeps {
  basename: string;
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  navigation: NavigationPublicPluginStart;
}

type TabId = 'skills' | 'connectors' | 'agents';

export const MoltlerApp = ({ basename, notifications, http, navigation }: MoltlerAppDeps) => {
  const [selectedTab, setSelectedTab] = useState<TabId>('skills');
  const [skills, setSkills] = useState<SkillDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSkill, setSelectedSkill] = useState<SkillDefinition | null>(null);
  const [runningSkill, setRunningSkill] = useState<string | null>(null);

  const fetchSkills = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await http.get<{ skills: SkillDefinition[]; count: number }>(
        '/api/moltler/skills'
      );
      setSkills(response.skills || []);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch skills');
      notifications.toasts.addError(err, {
        title: i18n.translate('moltler.fetchSkillsError', {
          defaultMessage: 'Failed to fetch skills',
        }),
      });
    } finally {
      setLoading(false);
    }
  }, [http, notifications]);

  useEffect(() => {
    fetchSkills();
  }, [fetchSkills]);

  const runSkill = async (skillName: string) => {
    setRunningSkill(skillName);
    try {
      const response = await http.post<{ success: boolean; result?: any; error?: string }>(
        `/api/moltler/skills/${skillName}/run`,
        { body: JSON.stringify({ parameters: {} }) }
      );

      if (response.success) {
        notifications.toasts.addSuccess({
          title: i18n.translate('moltler.skillRunSuccess', {
            defaultMessage: 'Skill executed successfully',
          }),
          text: `${skillName} completed in ${(response as any).executionTime}ms`,
        });
      } else {
        throw new Error(response.error || 'Unknown error');
      }
    } catch (err: any) {
      notifications.toasts.addError(err, {
        title: i18n.translate('moltler.skillRunError', {
          defaultMessage: 'Failed to run skill',
        }),
      });
    } finally {
      setRunningSkill(null);
    }
  };

  const filteredSkills = skills.filter(
    (skill) =>
      skill.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      skill.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      skill.tags?.some((tag) => tag.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const columns: Array<EuiBasicTableColumn<SkillDefinition>> = [
    {
      field: 'name',
      name: 'Name',
      sortable: true,
      render: (name: string, skill: SkillDefinition) => (
        <EuiButton
          size="s"
          color="text"
          onClick={() => setSelectedSkill(skill)}
          style={{ textDecoration: 'none' }}
        >
          {name}
        </EuiButton>
      ),
    },
    {
      field: 'description',
      name: 'Description',
      truncateText: true,
    },
    {
      field: 'version',
      name: 'Version',
      width: '80px',
    },
    {
      field: 'tags',
      name: 'Tags',
      render: (tags: string[]) => (
        <EuiFlexGroup gutterSize="xs" wrap responsive={false}>
          {(tags || []).slice(0, 3).map((tag) => (
            <EuiFlexItem grow={false} key={tag}>
              <EuiBadge color="hollow">{tag}</EuiBadge>
            </EuiFlexItem>
          ))}
          {(tags || []).length > 3 && (
            <EuiFlexItem grow={false}>
              <EuiBadge color="hollow">+{tags.length - 3}</EuiBadge>
            </EuiFlexItem>
          )}
        </EuiFlexGroup>
      ),
    },
    {
      name: 'Actions',
      width: '120px',
      actions: [
        {
          name: 'Run',
          description: 'Run this skill',
          icon: 'play',
          type: 'icon',
          onClick: (skill) => runSkill(skill.name),
          enabled: (skill) => runningSkill !== skill.name,
        },
        {
          name: 'View',
          description: 'View skill details',
          icon: 'eye',
          type: 'icon',
          onClick: (skill) => setSelectedSkill(skill),
        },
      ],
    },
  ];

  const renderSkillsList = () => {
    if (loading) {
      return (
        <EuiEmptyPrompt
          icon={<EuiLoadingSpinner size="xl" />}
          title={<h2>Loading skills...</h2>}
        />
      );
    }

    if (error) {
      return (
        <EuiCallOut title="Error loading skills" color="danger" iconType="error">
          <p>{error}</p>
          <EuiButton onClick={fetchSkills} size="s">
            Retry
          </EuiButton>
        </EuiCallOut>
      );
    }

    if (skills.length === 0) {
      return (
        <EuiEmptyPrompt
          iconType="documents"
          title={<h2>No skills found</h2>}
          body={
            <p>
              Create your first skill using the CLI:
              <br />
              <code>moltler demo</code>
            </p>
          }
        />
      );
    }

    return (
      <>
        <EuiFlexGroup>
          <EuiFlexItem>
            <EuiFieldSearch
              placeholder="Search skills..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              isClearable
              fullWidth
            />
          </EuiFlexItem>
          <EuiFlexItem grow={false}>
            <EuiButton iconType="refresh" onClick={fetchSkills}>
              Refresh
            </EuiButton>
          </EuiFlexItem>
        </EuiFlexGroup>

        <EuiSpacer size="m" />

        <EuiBasicTable
          items={filteredSkills}
          columns={columns}
          rowHeader="name"
          hasActions
        />
      </>
    );
  };

  const renderSkillDetail = () => {
    if (!selectedSkill) return null;

    const listItems = [
      {
        title: 'Version',
        description: selectedSkill.version || '1.0.0',
      },
      {
        title: 'Author',
        description: selectedSkill.author || 'Unknown',
      },
      {
        title: 'Procedure',
        description: selectedSkill.procedureName || selectedSkill.name,
      },
      {
        title: 'Tags',
        description: (
          <EuiFlexGroup gutterSize="xs" wrap>
            {(selectedSkill.tags || []).map((tag) => (
              <EuiFlexItem grow={false} key={tag}>
                <EuiBadge color="primary">{tag}</EuiBadge>
              </EuiFlexItem>
            ))}
          </EuiFlexGroup>
        ),
      },
    ];

    return (
      <>
        <EuiButton
          iconType="arrowLeft"
          onClick={() => setSelectedSkill(null)}
          size="s"
          color="text"
        >
          Back to skills
        </EuiButton>

        <EuiSpacer size="l" />

        <EuiFlexGroup>
          <EuiFlexItem>
            <EuiTitle size="l">
              <h1>{selectedSkill.name}</h1>
            </EuiTitle>
            <EuiText color="subdued">
              <p>{selectedSkill.description}</p>
            </EuiText>
          </EuiFlexItem>
          <EuiFlexItem grow={false}>
            <EuiButton
              fill
              iconType="play"
              onClick={() => runSkill(selectedSkill.name)}
              isLoading={runningSkill === selectedSkill.name}
            >
              Run Skill
            </EuiButton>
          </EuiFlexItem>
        </EuiFlexGroup>

        <EuiSpacer size="l" />

        <EuiPanel>
          <EuiDescriptionList listItems={listItems} type="column" />
        </EuiPanel>

        {selectedSkill.body && (
          <>
            <EuiSpacer size="l" />
            <EuiTitle size="s">
              <h3>Definition</h3>
            </EuiTitle>
            <EuiSpacer size="s" />
            <EuiCodeBlock language="sql" fontSize="m" paddingSize="m" isCopyable>
              {selectedSkill.body}
            </EuiCodeBlock>
          </>
        )}
      </>
    );
  };

  const tabs = [
    { id: 'skills' as TabId, name: 'Skills', disabled: false },
    { id: 'connectors' as TabId, name: 'Connectors', disabled: true },
    { id: 'agents' as TabId, name: 'Agents', disabled: true },
  ];

  const renderTabContent = () => {
    if (selectedSkill) {
      return renderSkillDetail();
    }

    switch (selectedTab) {
      case 'skills':
        return renderSkillsList();
      case 'connectors':
        return (
          <EuiEmptyPrompt
            iconType="package"
            title={<h2>Connectors</h2>}
            body={<p>Connector management coming soon.</p>}
          />
        );
      case 'agents':
        return (
          <EuiEmptyPrompt
            iconType="user"
            title={<h2>Agents</h2>}
            body={<p>Agent management coming soon.</p>}
          />
        );
      default:
        return null;
    }
  };

  return (
    <Router basename={basename}>
      <I18nProvider>
        <>
          <navigation.ui.TopNavMenu
            appName={PLUGIN_ID}
            showSearchBar={false}
            useDefaultBehaviors={true}
          />
          <EuiPageTemplate restrictWidth="1200px">
            <EuiPageTemplate.Header
              pageTitle={
                <EuiFlexGroup alignItems="center" gutterSize="m">
                  <EuiFlexItem grow={false}>
                    <EuiIcon type="logstashInput" size="xl" />
                  </EuiFlexItem>
                  <EuiFlexItem>{PLUGIN_NAME}</EuiFlexItem>
                </EuiFlexGroup>
              }
              description="Create, manage, and run AI skills powered by Elasticsearch"
            />
            <EuiPageTemplate.Section>
              {!selectedSkill && (
                <>
                  <EuiTabs>
                    {tabs.map((tab) => (
                      <EuiTab
                        key={tab.id}
                        onClick={() => {
                          setSelectedTab(tab.id);
                          setSelectedSkill(null);
                        }}
                        isSelected={selectedTab === tab.id}
                        disabled={tab.disabled}
                      >
                        {tab.name}
                      </EuiTab>
                    ))}
                  </EuiTabs>
                  <EuiSpacer size="l" />
                </>
              )}
              {renderTabContent()}
            </EuiPageTemplate.Section>
          </EuiPageTemplate>
        </>
      </I18nProvider>
    </Router>
  );
};
