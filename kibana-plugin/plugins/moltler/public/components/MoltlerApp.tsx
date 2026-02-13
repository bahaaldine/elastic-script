import React, { useState } from 'react';
import type { HttpSetup, NotificationsStart } from '@kbn/core/public';
import {
  EuiPage,
  EuiPageBody,
  EuiPageHeader,
  EuiPageSection,
  EuiTabs,
  EuiTab,
  EuiSpacer,
} from '@elastic/eui';
import { SkillsList } from './SkillsList';
import { SkillDetail } from './SkillDetail';

interface MoltlerAppProps {
  http: HttpSetup;
  notifications: NotificationsStart;
}

type TabId = 'skills' | 'connectors' | 'agents';

export const MoltlerApp: React.FC<MoltlerAppProps> = ({ http, notifications }) => {
  const [selectedTab, setSelectedTab] = useState<TabId>('skills');
  const [selectedSkill, setSelectedSkill] = useState<string | null>(null);

  const tabs: Array<{ id: TabId; name: string; disabled?: boolean }> = [
    { id: 'skills', name: 'Skills' },
    { id: 'connectors', name: 'Connectors', disabled: true },
    { id: 'agents', name: 'Agents', disabled: true },
  ];

  const renderTabContent = () => {
    switch (selectedTab) {
      case 'skills':
        if (selectedSkill) {
          return (
            <SkillDetail
              http={http}
              notifications={notifications}
              skillName={selectedSkill}
              onBack={() => setSelectedSkill(null)}
            />
          );
        }
        return (
          <SkillsList
            http={http}
            notifications={notifications}
            onSelectSkill={setSelectedSkill}
          />
        );
      case 'connectors':
        return <div>Connectors - Coming soon</div>;
      case 'agents':
        return <div>Agents - Coming soon</div>;
      default:
        return null;
    }
  };

  return (
    <EuiPage paddingSize="l">
      <EuiPageBody>
        <EuiPageHeader
          pageTitle="Moltler"
          description="Create, manage, and run AI skills powered by Elasticsearch"
          iconType="logstashInput"
        />

        <EuiSpacer size="l" />

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

        <EuiPageSection>{renderTabContent()}</EuiPageSection>
      </EuiPageBody>
    </EuiPage>
  );
};
