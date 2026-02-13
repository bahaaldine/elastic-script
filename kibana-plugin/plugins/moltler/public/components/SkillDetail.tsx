import React, { useEffect, useState, useCallback } from 'react';
import type { HttpSetup, NotificationsStart } from '@kbn/core/public';
import {
  EuiButton,
  EuiButtonEmpty,
  EuiCallOut,
  EuiCodeBlock,
  EuiDescriptionList,
  EuiDescriptionListTitle,
  EuiDescriptionListDescription,
  EuiFlexGroup,
  EuiFlexItem,
  EuiLoadingSpinner,
  EuiPanel,
  EuiSpacer,
  EuiTitle,
  EuiBadge,
  EuiText,
  EuiHorizontalRule,
} from '@elastic/eui';
import type { SkillDefinition } from '../../common';

interface SkillDetailProps {
  http: HttpSetup;
  notifications: NotificationsStart;
  skillName: string;
  onBack: () => void;
}

export const SkillDetail: React.FC<SkillDetailProps> = ({
  http,
  notifications,
  skillName,
  onBack,
}) => {
  const [skill, setSkill] = useState<SkillDefinition | null>(null);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchSkill = useCallback(async () => {
    setLoading(true);
    try {
      const response = await http.get<{ skill: SkillDefinition }>(
        `/api/moltler/skills/${skillName}`
      );
      setSkill(response.skill);
    } catch (err: any) {
      notifications.toasts.addError(err, {
        title: `Failed to fetch skill '${skillName}'`,
      });
    } finally {
      setLoading(false);
    }
  }, [http, notifications, skillName]);

  useEffect(() => {
    fetchSkill();
  }, [fetchSkill]);

  const runSkill = async () => {
    setRunning(true);
    setResult(null);
    setError(null);
    try {
      const response = await http.post<{ success: boolean; result: any }>(
        `/api/moltler/skills/${skillName}/run`,
        {
          body: JSON.stringify({ parameters: {} }),
        }
      );
      if (response.success) {
        setResult(response.result);
        notifications.toasts.addSuccess(`Skill '${skillName}' executed successfully`);
      }
    } catch (err: any) {
      setError(err.message);
      notifications.toasts.addError(err, {
        title: `Failed to run skill '${skillName}'`,
      });
    } finally {
      setRunning(false);
    }
  };

  if (loading) {
    return (
      <EuiFlexGroup justifyContent="center" alignItems="center" style={{ minHeight: 200 }}>
        <EuiFlexItem grow={false}>
          <EuiLoadingSpinner size="xl" />
        </EuiFlexItem>
      </EuiFlexGroup>
    );
  }

  if (!skill) {
    return (
      <EuiCallOut title="Skill not found" color="danger" iconType="alert">
        <p>Could not load skill: {skillName}</p>
        <EuiButton onClick={onBack}>Back to Skills</EuiButton>
      </EuiCallOut>
    );
  }

  return (
    <>
      <EuiFlexGroup alignItems="center">
        <EuiFlexItem grow={false}>
          <EuiButtonEmpty iconType="arrowLeft" onClick={onBack}>
            Back to Skills
          </EuiButtonEmpty>
        </EuiFlexItem>
      </EuiFlexGroup>

      <EuiSpacer size="m" />

      <EuiPanel>
        <EuiFlexGroup justifyContent="spaceBetween" alignItems="center">
          <EuiFlexItem>
            <EuiTitle size="l">
              <h1>{skill.name}</h1>
            </EuiTitle>
            <EuiSpacer size="s" />
            <EuiText color="subdued">
              <p>{skill.description || 'No description provided'}</p>
            </EuiText>
          </EuiFlexItem>
          <EuiFlexItem grow={false}>
            <EuiButton
              fill
              iconType="play"
              onClick={runSkill}
              isLoading={running}
              disabled={running}
            >
              Run Skill
            </EuiButton>
          </EuiFlexItem>
        </EuiFlexGroup>

        <EuiHorizontalRule />

        <EuiFlexGroup>
          <EuiFlexItem>
            <EuiDescriptionList>
              <EuiDescriptionListTitle>Version</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                <EuiBadge color="hollow">{skill.version || '1.0'}</EuiBadge>
              </EuiDescriptionListDescription>

              <EuiDescriptionListTitle>Author</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                {skill.author || 'Unknown'}
              </EuiDescriptionListDescription>

              <EuiDescriptionListTitle>Return Type</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                <EuiBadge color="primary">{skill.returnType || 'ANY'}</EuiBadge>
              </EuiDescriptionListDescription>
            </EuiDescriptionList>
          </EuiFlexItem>

          <EuiFlexItem>
            <EuiDescriptionList>
              <EuiDescriptionListTitle>Parameters</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                {skill.parameters?.length ? (
                  skill.parameters.map((param, idx) => (
                    <EuiBadge key={idx} color="default" style={{ marginRight: 4 }}>
                      {param.name}: {param.type}
                    </EuiBadge>
                  ))
                ) : (
                  <span>None</span>
                )}
              </EuiDescriptionListDescription>

              <EuiDescriptionListTitle>Tags</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                {skill.tags?.length ? (
                  skill.tags.map((tag, idx) => (
                    <EuiBadge key={idx} color="hollow" style={{ marginRight: 4 }}>
                      {tag}
                    </EuiBadge>
                  ))
                ) : (
                  <span>None</span>
                )}
              </EuiDescriptionListDescription>

              <EuiDescriptionListTitle>Procedure</EuiDescriptionListTitle>
              <EuiDescriptionListDescription>
                {skill.procedureName || skill.name}
              </EuiDescriptionListDescription>
            </EuiDescriptionList>
          </EuiFlexItem>
        </EuiFlexGroup>
      </EuiPanel>

      <EuiSpacer size="l" />

      <EuiPanel>
        <EuiTitle size="s">
          <h3>Skill Definition</h3>
        </EuiTitle>
        <EuiSpacer size="m" />
        <EuiCodeBlock language="sql" paddingSize="m" isCopyable>
          {skill.body || `-- Skill body not available\nRUN SKILL ${skill.name}`}
        </EuiCodeBlock>
      </EuiPanel>

      {(result || error) && (
        <>
          <EuiSpacer size="l" />
          <EuiPanel>
            <EuiTitle size="s">
              <h3>Execution Result</h3>
            </EuiTitle>
            <EuiSpacer size="m" />
            {error ? (
              <EuiCallOut title="Execution failed" color="danger" iconType="alert">
                {error}
              </EuiCallOut>
            ) : (
              <EuiCodeBlock language="json" paddingSize="m" isCopyable>
                {JSON.stringify(result, null, 2)}
              </EuiCodeBlock>
            )}
          </EuiPanel>
        </>
      )}
    </>
  );
};
