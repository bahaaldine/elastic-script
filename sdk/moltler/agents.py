"""
Moltler Agents Management.

Provides APIs for creating, managing, and triggering autonomous agents.
"""

from typing import List, Optional, Dict, Any
from dataclasses import dataclass, field
from enum import Enum

from .exceptions import AgentNotFoundError, ExecutionError


class ExecutionMode(Enum):
    """Agent execution modes."""
    AUTONOMOUS = "autonomous"
    SUPERVISED = "supervised"
    HUMAN_APPROVAL = "human_approval"
    DRY_RUN = "dry_run"


class TriggerType(Enum):
    """Agent trigger types."""
    MANUAL = "manual"
    SCHEDULE = "schedule"
    ALERT = "alert"
    WEBHOOK = "webhook"


@dataclass
class AgentTrigger:
    """A trigger definition for an agent."""
    type: str
    schedule: str = None
    alert_name: str = None
    condition: str = None
    webhook_id: str = None


@dataclass
class AgentSkillRef:
    """Reference to a skill used by an agent."""
    name: str
    version: str = None
    mode: str = None  # approval, forbidden, etc.


@dataclass
class AgentExecutionStep:
    """A step in an agent execution."""
    phase: str  # observe, orient, decide, act
    timestamp: int
    data: Dict[str, Any] = field(default_factory=dict)


@dataclass
class AgentExecutionResult:
    """Result of an agent execution."""
    execution_id: str
    agent: str
    status: str
    execution_mode: str
    started_at: int = None
    completed_at: int = None
    steps: List[AgentExecutionStep] = field(default_factory=list)
    result: Dict[str, Any] = field(default_factory=dict)
    error: str = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "AgentExecutionResult":
        """Create from a dictionary."""
        steps = []
        if "steps" in data:
            for s in data["steps"]:
                steps.append(AgentExecutionStep(
                    phase=s.get("phase"),
                    timestamp=s.get("timestamp"),
                    data=s.get("data", {}),
                ))
        
        return cls(
            execution_id=data.get("execution_id"),
            agent=data.get("agent"),
            status=data.get("status"),
            execution_mode=data.get("execution_mode"),
            started_at=data.get("started_at"),
            completed_at=data.get("completed_at"),
            steps=steps,
            result=data.get("result", {}),
            error=data.get("error"),
        )


@dataclass
class Agent:
    """Represents a Moltler agent."""
    name: str
    goal: str
    skills: List[AgentSkillRef] = field(default_factory=list)
    execution_mode: str = "human_approval"
    triggers: List[AgentTrigger] = field(default_factory=list)
    model: str = None
    config: Dict[str, Any] = field(default_factory=dict)
    enabled: bool = True
    status: str = "created"
    created_at: int = None
    updated_at: int = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "Agent":
        """Create an Agent from a dictionary."""
        skills = []
        if "skills" in data:
            for s in data["skills"]:
                skills.append(AgentSkillRef(
                    name=s.get("name"),
                    version=s.get("version"),
                    mode=s.get("mode"),
                ))
        
        triggers = []
        if "triggers" in data:
            for t in data["triggers"]:
                triggers.append(AgentTrigger(
                    type=t.get("type"),
                    schedule=t.get("schedule"),
                    alert_name=t.get("alert"),
                    condition=t.get("condition"),
                    webhook_id=t.get("webhook_id"),
                ))
        
        return cls(
            name=data.get("name"),
            goal=data.get("goal"),
            skills=skills,
            execution_mode=data.get("execution_mode", "human_approval"),
            triggers=triggers,
            model=data.get("model"),
            config=data.get("config", {}),
            enabled=data.get("enabled", True),
            status=data.get("status", "created"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )


class AgentBuilder:
    """
    Builder for creating agents programmatically.
    
    Example:
        >>> builder = AgentBuilder("incident_responder")
        >>> builder.goal("Automatically respond to production incidents")
        >>> builder.skill("diagnose_issue", mode="approval")
        >>> builder.skill("notify_team")
        >>> builder.execution_mode("supervised")
        >>> builder.trigger_on_schedule("every 5 minutes")
        >>> builder.trigger_on_alert("HighCPUUsage")
        >>> agent = builder.build()
    """
    
    def __init__(self, name: str):
        self._name = name
        self._goal = ""
        self._skills = []
        self._execution_mode = "human_approval"
        self._triggers = []
        self._model = None
        self._config = {}
    
    def goal(self, goal: str) -> "AgentBuilder":
        """Set the agent's goal."""
        self._goal = goal
        return self
    
    def skill(
        self,
        name: str,
        version: str = None,
        mode: str = None
    ) -> "AgentBuilder":
        """Add a skill to the agent."""
        self._skills.append(AgentSkillRef(
            name=name,
            version=version,
            mode=mode,
        ))
        return self
    
    def execution_mode(self, mode: str) -> "AgentBuilder":
        """Set the execution mode."""
        self._execution_mode = mode
        return self
    
    def model(self, model: str) -> "AgentBuilder":
        """Set the AI model to use."""
        self._model = model
        return self
    
    def config(self, **kwargs) -> "AgentBuilder":
        """Set configuration options."""
        self._config.update(kwargs)
        return self
    
    def trigger_on_schedule(self, schedule: str) -> "AgentBuilder":
        """Add a schedule trigger."""
        self._triggers.append(AgentTrigger(type="schedule", schedule=schedule))
        return self
    
    def trigger_on_alert(self, alert_name: str, condition: str = None) -> "AgentBuilder":
        """Add an alert trigger."""
        self._triggers.append(AgentTrigger(
            type="alert",
            alert_name=alert_name,
            condition=condition,
        ))
        return self
    
    def trigger_on_webhook(self, webhook_id: str) -> "AgentBuilder":
        """Add a webhook trigger."""
        self._triggers.append(AgentTrigger(type="webhook", webhook_id=webhook_id))
        return self
    
    def build(self) -> Agent:
        """Build the Agent object."""
        return Agent(
            name=self._name,
            goal=self._goal,
            skills=self._skills,
            execution_mode=self._execution_mode,
            triggers=self._triggers,
            model=self._model,
            config=self._config,
        )
    
    def to_statement(self) -> str:
        """Generate the CREATE AGENT statement."""
        stmt = f"CREATE AGENT {self._name}\n"
        stmt += f"    GOAL '{_escape(self._goal)}'\n"
        
        # Skills
        skill_strs = []
        for s in self._skills:
            ss = s.name
            if s.version:
                ss += f"@{s.version}"
            if s.mode:
                ss += f"[{s.mode}]"
            skill_strs.append(ss)
        stmt += f"    SKILLS [{', '.join(skill_strs)}]\n"
        
        # Execution mode
        mode_map = {
            "autonomous": "AUTONOMOUS",
            "supervised": "SUPERVISED",
            "human_approval": "HUMAN_APPROVAL",
            "dry_run": "DRY_RUN",
        }
        stmt += f"    EXECUTION {mode_map.get(self._execution_mode, 'HUMAN_APPROVAL')}\n"
        
        # Triggers
        if self._triggers:
            trigger_strs = []
            for t in self._triggers:
                if t.type == "schedule":
                    trigger_strs.append(f"SCHEDULE '{t.schedule}'")
                elif t.type == "alert":
                    if t.condition:
                        trigger_strs.append(f"ALERT {t.condition}")
                    else:
                        trigger_strs.append(f"ALERT '{t.alert_name}'")
                elif t.type == "webhook":
                    trigger_strs.append(f"WEBHOOK '{t.webhook_id}'")
            stmt += f"    ON {', '.join(trigger_strs)}\n"
        
        # Model
        if self._model:
            stmt += f"    MODEL '{self._model}'\n"
        
        # Config
        if self._config:
            config_str = _dict_to_doc(self._config)
            stmt += f"    CONFIG {config_str}\n"
        
        stmt += "BEGIN\n"
        stmt += "    -- Agent body\n"
        stmt += "END AGENT;"
        
        return stmt


class AgentManager:
    """
    Manager for agent operations.
    
    Accessed via `client.agents`.
    """
    
    def __init__(self, client):
        self._client = client
    
    def list(self) -> List[Agent]:
        """
        List all configured agents.
        
        Returns:
            List of Agent objects
        """
        result = self._client.execute("SHOW AGENTS")
        if not result.success:
            return []
        
        agents = []
        data = result.data
        if isinstance(data, dict) and "agents" in data:
            for agent_data in data["agents"]:
                agents.append(Agent.from_dict(agent_data))
        
        return agents
    
    def get(self, name: str) -> Agent:
        """
        Get an agent by name.
        
        Args:
            name: Agent name
            
        Returns:
            Agent object
            
        Raises:
            AgentNotFoundError: If agent not found
        """
        result = self._client.execute(f"SHOW AGENT {name}")
        if not result.success:
            raise AgentNotFoundError(name)
        
        return Agent.from_dict(result.data)
    
    def create(self, agent: Agent) -> Agent:
        """
        Create a new agent.
        
        Args:
            agent: Agent object or AgentBuilder result
            
        Returns:
            Created Agent object
        """
        if isinstance(agent, AgentBuilder):
            stmt = agent.to_statement()
        else:
            builder = AgentBuilder(agent.name)
            builder.goal(agent.goal)
            for skill in agent.skills:
                builder.skill(skill.name, skill.version, skill.mode)
            builder.execution_mode(agent.execution_mode)
            for trigger in agent.triggers:
                if trigger.type == "schedule":
                    builder.trigger_on_schedule(trigger.schedule)
                elif trigger.type == "alert":
                    builder.trigger_on_alert(trigger.alert_name, trigger.condition)
                elif trigger.type == "webhook":
                    builder.trigger_on_webhook(trigger.webhook_id)
            if agent.model:
                builder.model(agent.model)
            if agent.config:
                builder.config(**agent.config)
            stmt = builder.to_statement()
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to create agent", statement=stmt)
        
        return self.get(agent.name if isinstance(agent, Agent) else agent._name)
    
    def create_from_builder(self, builder: AgentBuilder) -> Agent:
        """
        Create an agent from an AgentBuilder.
        
        Args:
            builder: AgentBuilder instance
            
        Returns:
            Created Agent object
        """
        stmt = builder.to_statement()
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to create agent", statement=stmt)
        
        return self.get(builder._name)
    
    def drop(self, name: str) -> bool:
        """
        Drop an agent.
        
        Args:
            name: Agent name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"DROP AGENT {name}")
        return result.success
    
    def trigger(
        self,
        name: str,
        context: Dict[str, Any] = None
    ) -> AgentExecutionResult:
        """
        Manually trigger an agent execution.
        
        Args:
            name: Agent name
            context: Optional context to pass to the agent
            
        Returns:
            AgentExecutionResult with execution details
        """
        stmt = f"TRIGGER AGENT {name}"
        if context:
            stmt += f" WITH {_dict_to_doc(context)}"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Trigger failed", statement=stmt)
        
        return AgentExecutionResult.from_dict(result.data if isinstance(result.data, dict) else {
            "execution_id": result.data,
            "agent": name,
            "status": "triggered",
        })
    
    def enable(self, name: str) -> bool:
        """
        Enable an agent.
        
        Args:
            name: Agent name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"ENABLE AGENT {name}")
        return result.success
    
    def disable(self, name: str) -> bool:
        """
        Disable an agent.
        
        Args:
            name: Agent name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"DISABLE AGENT {name}")
        return result.success
    
    def get_history(self, name: str, limit: int = 10) -> List[AgentExecutionResult]:
        """
        Get execution history for an agent.
        
        Args:
            name: Agent name
            limit: Number of records to return
            
        Returns:
            List of AgentExecutionResult objects
        """
        result = self._client.execute(f"SHOW AGENT {name} HISTORY")
        if not result.success:
            return []
        
        history = []
        data = result.data
        if isinstance(data, dict) and "executions" in data:
            for exec_data in data["executions"]:
                history.append(AgentExecutionResult.from_dict(exec_data))
        
        return history
    
    def get_execution(self, name: str, execution_id: str) -> AgentExecutionResult:
        """
        Get details of a specific execution.
        
        Args:
            name: Agent name
            execution_id: Execution ID
            
        Returns:
            AgentExecutionResult with full details
        """
        result = self._client.execute(f"SHOW AGENT {name} EXECUTION '{execution_id}'")
        if not result.success:
            raise ExecutionError(result.error or "Execution not found")
        
        return AgentExecutionResult.from_dict(result.data)
    
    def set_execution_mode(self, name: str, mode: str) -> bool:
        """
        Change an agent's execution mode.
        
        Args:
            name: Agent name
            mode: New execution mode (autonomous, supervised, human_approval, dry_run)
            
        Returns:
            True if successful
        """
        mode_map = {
            "autonomous": "AUTONOMOUS",
            "supervised": "SUPERVISED",
            "human_approval": "HUMAN_APPROVAL",
            "dry_run": "DRY_RUN",
        }
        mode_str = mode_map.get(mode.lower(), mode.upper())
        result = self._client.execute(f"ALTER AGENT {name} EXECUTION {mode_str}")
        return result.success


def _escape(s: str) -> str:
    """Escape single quotes in a string."""
    return s.replace("'", "''")


def _dict_to_doc(d: Dict[str, Any]) -> str:
    """Convert a dictionary to a document literal string."""
    pairs = []
    for k, v in d.items():
        if isinstance(v, str):
            pairs.append(f'"{k}": "{v}"')
        elif isinstance(v, bool):
            pairs.append(f'"{k}": {"true" if v else "false"}')
        elif v is None:
            pairs.append(f'"{k}": null')
        else:
            pairs.append(f'"{k}": {v}')
    return "{" + ", ".join(pairs) + "}"
