"""
Moltler Skills Management.

Provides APIs for creating, managing, and executing skills.
"""

from typing import List, Optional, Dict, Any
from dataclasses import dataclass, field

from .exceptions import SkillNotFoundError, ExecutionError, TestFailedError


@dataclass
class SkillParameter:
    """A skill parameter definition."""
    name: str
    datatype: str
    mode: str = "IN"  # IN, OUT, INOUT
    default: Any = None
    description: str = None


@dataclass
class Skill:
    """Represents a Moltler skill."""
    name: str
    version: str = "1.0.0"
    description: str = None
    author: str = None
    tags: List[str] = field(default_factory=list)
    dependencies: List[str] = field(default_factory=list)
    parameters: List[SkillParameter] = field(default_factory=list)
    returns: str = None
    body: str = None
    created_at: int = None
    updated_at: int = None
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "Skill":
        """Create a Skill from a dictionary."""
        params = []
        if "parameters" in data:
            for p in data["parameters"]:
                params.append(SkillParameter(
                    name=p.get("name"),
                    datatype=p.get("datatype", "STRING"),
                    mode=p.get("mode", "IN"),
                    default=p.get("default"),
                    description=p.get("description"),
                ))
        
        return cls(
            name=data.get("name"),
            version=data.get("version", "1.0.0"),
            description=data.get("description"),
            author=data.get("author"),
            tags=data.get("tags", []),
            dependencies=data.get("dependencies", []),
            parameters=params,
            returns=data.get("returns"),
            body=data.get("body"),
            created_at=data.get("created_at"),
            updated_at=data.get("updated_at"),
        )


class SkillBuilder:
    """
    Builder for creating skills programmatically.
    
    Example:
        >>> builder = SkillBuilder("analyze_logs")
        >>> builder.version("1.0.0")
        >>> builder.description("Analyze logs for anomalies")
        >>> builder.author("DevOps Team")
        >>> builder.parameter("index", "STRING", description="Index pattern")
        >>> builder.parameter("threshold", "NUMBER", default=0.9)
        >>> builder.body('''
        ...     DECLARE results DOCUMENT;
        ...     SET results = ESQL_QUERY('FROM ' || index || ' | STATS count=COUNT(*)');
        ...     RETURN results;
        ... ''')
        >>> skill = builder.build()
    """
    
    def __init__(self, name: str):
        self._name = name
        self._version = "1.0.0"
        self._description = None
        self._author = None
        self._tags = []
        self._dependencies = []
        self._parameters = []
        self._returns = None
        self._body = None
    
    def version(self, version: str) -> "SkillBuilder":
        """Set skill version."""
        self._version = version
        return self
    
    def description(self, description: str) -> "SkillBuilder":
        """Set skill description."""
        self._description = description
        return self
    
    def author(self, author: str) -> "SkillBuilder":
        """Set skill author."""
        self._author = author
        return self
    
    def tag(self, *tags: str) -> "SkillBuilder":
        """Add tags to the skill."""
        self._tags.extend(tags)
        return self
    
    def requires(self, *dependencies: str) -> "SkillBuilder":
        """Add skill dependencies."""
        self._dependencies.extend(dependencies)
        return self
    
    def parameter(
        self,
        name: str,
        datatype: str = "STRING",
        mode: str = "IN",
        default: Any = None,
        description: str = None
    ) -> "SkillBuilder":
        """Add a parameter to the skill."""
        self._parameters.append(SkillParameter(
            name=name,
            datatype=datatype,
            mode=mode,
            default=default,
            description=description,
        ))
        return self
    
    def returns(self, datatype: str) -> "SkillBuilder":
        """Set the return type."""
        self._returns = datatype
        return self
    
    def body(self, body: str) -> "SkillBuilder":
        """Set the skill body (implementation)."""
        self._body = body
        return self
    
    def build(self) -> Skill:
        """Build the Skill object."""
        return Skill(
            name=self._name,
            version=self._version,
            description=self._description,
            author=self._author,
            tags=self._tags,
            dependencies=self._dependencies,
            parameters=self._parameters,
            returns=self._returns,
            body=self._body,
        )
    
    def to_statement(self) -> str:
        """Generate the CREATE SKILL statement."""
        stmt = f"CREATE SKILL {self._name}"
        stmt += f" VERSION '{self._version}'"
        
        if self._description:
            stmt += f" DESCRIPTION '{_escape(self._description)}'"
        
        if self._author:
            stmt += f" AUTHOR '{_escape(self._author)}'"
        
        if self._tags:
            stmt += f" TAGS [{', '.join(repr(t) for t in self._tags)}]"
        
        if self._dependencies:
            stmt += f" REQUIRES [{', '.join(repr(d) for d in self._dependencies)}]"
        
        if self._parameters:
            param_strs = []
            for p in self._parameters:
                ps = f"{p.name} {p.mode} {p.datatype}"
                if p.default is not None:
                    ps += f" DEFAULT {repr(p.default)}"
                param_strs.append(ps)
            stmt += f" ({', '.join(param_strs)})"
        
        if self._returns:
            stmt += f" RETURNS {self._returns}"
        
        stmt += " BEGIN\n"
        stmt += self._body or "    -- Skill body\n"
        stmt += "\nEND SKILL;"
        
        return stmt


class SkillManager:
    """
    Manager for skill operations.
    
    Accessed via `client.skills`.
    """
    
    def __init__(self, client):
        self._client = client
    
    def list(self) -> List[Skill]:
        """
        List all available skills.
        
        Returns:
            List of Skill objects
        """
        result = self._client.execute("SHOW SKILLS")
        if not result.success:
            return []
        
        skills = []
        data = result.data
        if isinstance(data, dict) and "skills" in data:
            for skill_data in data["skills"]:
                skills.append(Skill.from_dict(skill_data))
        
        return skills
    
    def get(self, name: str) -> Skill:
        """
        Get a skill by name.
        
        Args:
            name: Skill name
            
        Returns:
            Skill object
            
        Raises:
            SkillNotFoundError: If skill not found
        """
        result = self._client.execute(f"SHOW SKILL {name}")
        if not result.success:
            raise SkillNotFoundError(name)
        
        return Skill.from_dict(result.data)
    
    def create(self, skill: Skill) -> Skill:
        """
        Create a new skill.
        
        Args:
            skill: Skill object or SkillBuilder result
            
        Returns:
            Created Skill object
        """
        if isinstance(skill, SkillBuilder):
            stmt = skill.to_statement()
        else:
            builder = SkillBuilder(skill.name)
            builder.version(skill.version)
            if skill.description:
                builder.description(skill.description)
            if skill.author:
                builder.author(skill.author)
            for tag in skill.tags:
                builder.tag(tag)
            for dep in skill.dependencies:
                builder.requires(dep)
            for param in skill.parameters:
                builder.parameter(
                    param.name, param.datatype, param.mode,
                    param.default, param.description
                )
            if skill.returns:
                builder.returns(skill.returns)
            if skill.body:
                builder.body(skill.body)
            stmt = builder.to_statement()
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to create skill", statement=stmt)
        
        return self.get(skill.name if isinstance(skill, Skill) else skill._name)
    
    def create_from_builder(self, builder: SkillBuilder) -> Skill:
        """
        Create a skill from a SkillBuilder.
        
        Args:
            builder: SkillBuilder instance
            
        Returns:
            Created Skill object
        """
        stmt = builder.to_statement()
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to create skill", statement=stmt)
        
        return self.get(builder._name)
    
    def drop(self, name: str) -> bool:
        """
        Drop a skill.
        
        Args:
            name: Skill name
            
        Returns:
            True if successful
        """
        result = self._client.execute(f"DROP SKILL {name}")
        return result.success
    
    def execute(self, name: str, **kwargs) -> Any:
        """
        Execute a skill with given parameters.
        
        Args:
            name: Skill name
            **kwargs: Skill parameters
            
        Returns:
            Skill execution result
        """
        # Build CALL statement
        if kwargs:
            params = ", ".join(f"{k} => {repr(v)}" for k, v in kwargs.items())
            stmt = f"CALL {name}({params})"
        else:
            stmt = f"CALL {name}()"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Skill execution failed", statement=stmt)
        
        return result.data
    
    def test(
        self,
        name: str,
        params: Dict[str, Any] = None,
        expect: Any = None
    ) -> Dict[str, Any]:
        """
        Test a skill with given parameters.
        
        Args:
            name: Skill name
            params: Test parameters
            expect: Expected result (optional)
            
        Returns:
            Test result dictionary
            
        Raises:
            TestFailedError: If test fails and expect is provided
        """
        stmt = f"TEST SKILL {name}"
        
        if params:
            param_str = ", ".join(f"{k} = {repr(v)}" for k, v in params.items())
            stmt += f" WITH {param_str}"
        
        if expect is not None:
            stmt += f" EXPECT {repr(expect)}"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Test failed", statement=stmt)
        
        test_data = result.data
        
        if expect is not None and isinstance(test_data, dict):
            if not test_data.get("passed", False):
                raise TestFailedError(
                    name,
                    expected=expect,
                    actual=test_data.get("actual_result")
                )
        
        return test_data
    
    def generate(self, goal: str, name: str = None) -> Skill:
        """
        Generate a skill using AI based on a goal description.
        
        Args:
            goal: Natural language description of what the skill should do
            name: Optional name for the skill
            
        Returns:
            Generated Skill object
        """
        stmt = f"GENERATE SKILL FROM '{_escape(goal)}'"
        if name:
            stmt += f" AS {name}"
        
        result = self._client.execute(stmt)
        if not result.success:
            raise ExecutionError(result.error or "Failed to generate skill", statement=stmt)
        
        # Parse the generated skill
        if isinstance(result.data, dict):
            generated_name = result.data.get("name") or name
            if generated_name:
                return self.get(generated_name)
        
        return result.data


def _escape(s: str) -> str:
    """Escape single quotes in a string."""
    return s.replace("'", "''")
