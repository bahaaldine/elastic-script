import React, { useEffect, useState, useCallback } from 'react';
import type { HttpSetup, NotificationsStart } from '@kbn/core/public';
import {
  EuiBasicTable,
  EuiBasicTableColumn,
  EuiButton,
  EuiButtonIcon,
  EuiEmptyPrompt,
  EuiFieldSearch,
  EuiFlexGroup,
  EuiFlexItem,
  EuiHealth,
  EuiLink,
  EuiLoadingSpinner,
  EuiSpacer,
  EuiBadge,
  EuiToolTip,
} from '@elastic/eui';
import type { SkillDefinition } from '../../common';

interface SkillsListProps {
  http: HttpSetup;
  notifications: NotificationsStart;
  onSelectSkill: (name: string) => void;
}

export const SkillsList: React.FC<SkillsListProps> = ({
  http,
  notifications,
  onSelectSkill,
}) => {
  const [skills, setSkills] = useState<SkillDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [runningSkill, setRunningSkill] = useState<string | null>(null);

  const fetchSkills = useCallback(async () => {
    setLoading(true);
    try {
      const response = await http.get<{ skills: SkillDefinition[]; count: number }>(
        '/api/moltler/skills'
      );
      setSkills(response.skills);
    } catch (error: any) {
      notifications.toasts.addError(error, {
        title: 'Failed to fetch skills',
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
      const response = await http.post<{ success: boolean; result: any }>(
        `/api/moltler/skills/${skillName}/run`,
        {
          body: JSON.stringify({ parameters: {} }),
        }
      );
      if (response.success) {
        notifications.toasts.addSuccess({
          title: `Skill '${skillName}' executed`,
          text: JSON.stringify(response.result, null, 2).substring(0, 200),
        });
      }
    } catch (error: any) {
      notifications.toasts.addError(error, {
        title: `Failed to run skill '${skillName}'`,
      });
    } finally {
      setRunningSkill(null);
    }
  };

  const filteredSkills = skills.filter(
    (skill) =>
      skill.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      skill.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      skill.tags?.some((tag) => tag.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const columns: Array<EuiBasicTableColumn<SkillDefinition>> = [
    {
      field: 'name',
      name: 'Name',
      sortable: true,
      render: (name: string) => (
        <EuiLink onClick={() => onSelectSkill(name)}>
          <strong>{name}</strong>
        </EuiLink>
      ),
    },
    {
      field: 'description',
      name: 'Description',
      truncateText: true,
      width: '35%',
    },
    {
      field: 'version',
      name: 'Version',
      sortable: true,
      width: '80px',
      render: (version: string) => (
        <EuiBadge color="hollow">{version || '1.0'}</EuiBadge>
      ),
    },
    {
      field: 'returnType',
      name: 'Returns',
      width: '100px',
      render: (returnType: string) => (
        <EuiBadge color="primary">{returnType || 'ANY'}</EuiBadge>
      ),
    },
    {
      field: 'parameters',
      name: 'Params',
      width: '80px',
      render: (params: any[]) => (
        <EuiBadge>{params?.length || 0}</EuiBadge>
      ),
    },
    {
      field: 'author',
      name: 'Author',
      sortable: true,
      width: '120px',
    },
    {
      name: 'Actions',
      width: '100px',
      actions: [
        {
          name: 'Run',
          description: 'Run this skill',
          icon: 'play',
          type: 'icon',
          onClick: (skill: SkillDefinition) => runSkill(skill.name),
          isPrimary: true,
        },
        {
          name: 'View',
          description: 'View skill details',
          icon: 'eye',
          type: 'icon',
          onClick: (skill: SkillDefinition) => onSelectSkill(skill.name),
        },
      ],
    },
  ];

  if (loading) {
    return (
      <EuiFlexGroup justifyContent="center" alignItems="center" style={{ minHeight: 200 }}>
        <EuiFlexItem grow={false}>
          <EuiLoadingSpinner size="xl" />
        </EuiFlexItem>
      </EuiFlexGroup>
    );
  }

  if (skills.length === 0) {
    return (
      <EuiEmptyPrompt
        iconType="logstashInput"
        title={<h2>No skills found</h2>}
        body={
          <p>
            Create your first skill using the Moltler CLI:
            <br />
            <code>moltler demo</code>
          </p>
        }
        actions={
          <EuiButton onClick={fetchSkills} iconType="refresh">
            Refresh
          </EuiButton>
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
            aria-label="Search skills"
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
        loading={runningSkill !== null}
      />

      <EuiSpacer size="m" />

      <EuiFlexGroup justifyContent="flexEnd">
        <EuiFlexItem grow={false}>
          <span style={{ color: '#69707D' }}>
            Showing {filteredSkills.length} of {skills.length} skills
          </span>
        </EuiFlexItem>
      </EuiFlexGroup>
    </>
  );
};
