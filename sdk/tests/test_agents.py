"""
Tests for Moltler agents module.
"""

import pytest
from moltler.agents import (
    Agent, AgentBuilder, AgentTrigger, AgentSkillRef,
    AgentExecutionResult, AgentExecutionStep
)


class TestAgentBuilder:
    """Tests for AgentBuilder."""
    
    def test_basic_builder(self):
        """Test basic agent building."""
        builder = AgentBuilder("test_agent")
        builder.goal("Do something")
        builder.skill("skill1")
        
        agent = builder.build()
        
        assert agent.name == "test_agent"
        assert agent.goal == "Do something"
        assert len(agent.skills) == 1
    
    def test_builder_with_skills(self):
        """Test building agent with multiple skills."""
        builder = AgentBuilder("multi_skill")
        builder.goal("Complex task")
        builder.skill("skill1", version="1.0", mode="approval")
        builder.skill("skill2")
        
        agent = builder.build()
        
        assert len(agent.skills) == 2
        assert agent.skills[0].version == "1.0"
        assert agent.skills[0].mode == "approval"
    
    def test_builder_with_triggers(self):
        """Test building agent with triggers."""
        builder = AgentBuilder("triggered")
        builder.goal("Respond to events")
        builder.trigger_on_schedule("every 5 minutes")
        builder.trigger_on_alert("HighCPU")
        builder.trigger_on_webhook("webhook-123")
        
        agent = builder.build()
        
        assert len(agent.triggers) == 3
        assert agent.triggers[0].type == "schedule"
        assert agent.triggers[0].schedule == "every 5 minutes"
        assert agent.triggers[1].type == "alert"
        assert agent.triggers[2].type == "webhook"
    
    def test_to_statement(self):
        """Test generating CREATE AGENT statement."""
        builder = AgentBuilder("stmt_agent")
        builder.goal("Test goal")
        builder.skill("my_skill")
        builder.execution_mode("supervised")
        
        stmt = builder.to_statement()
        
        assert "CREATE AGENT stmt_agent" in stmt
        assert "GOAL 'Test goal'" in stmt
        assert "SKILLS [my_skill]" in stmt
        assert "EXECUTION SUPERVISED" in stmt


class TestAgent:
    """Tests for Agent class."""
    
    def test_from_dict(self):
        """Test creating agent from dictionary."""
        data = {
            "name": "dict_agent",
            "goal": "From dict",
            "skills": [
                {"name": "skill1", "version": "1.0"}
            ],
            "execution_mode": "autonomous",
            "triggers": [
                {"type": "schedule", "schedule": "daily"}
            ],
            "enabled": True
        }
        
        agent = Agent.from_dict(data)
        
        assert agent.name == "dict_agent"
        assert agent.goal == "From dict"
        assert len(agent.skills) == 1
        assert agent.skills[0].version == "1.0"
        assert len(agent.triggers) == 1


class TestAgentExecutionResult:
    """Tests for AgentExecutionResult."""
    
    def test_from_dict(self):
        """Test creating execution result from dictionary."""
        data = {
            "execution_id": "exec-123",
            "agent": "my_agent",
            "status": "completed",
            "execution_mode": "supervised",
            "steps": [
                {"phase": "observe", "timestamp": 1000, "data": {}},
                {"phase": "act", "timestamp": 1100, "data": {"action": "execute"}}
            ]
        }
        
        result = AgentExecutionResult.from_dict(data)
        
        assert result.execution_id == "exec-123"
        assert result.status == "completed"
        assert len(result.steps) == 2
        assert result.steps[1].phase == "act"
