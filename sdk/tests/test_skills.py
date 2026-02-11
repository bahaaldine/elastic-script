"""
Tests for Moltler skills module.
"""

import pytest
from moltler.skills import Skill, SkillBuilder, SkillParameter


class TestSkillBuilder:
    """Tests for the SkillBuilder class."""
    
    def test_basic_builder(self):
        """Test basic skill building."""
        builder = SkillBuilder("test_skill")
        builder.version("1.0.0")
        builder.description("A test skill")
        
        skill = builder.build()
        
        assert skill.name == "test_skill"
        assert skill.version == "1.0.0"
        assert skill.description == "A test skill"
    
    def test_builder_with_parameters(self):
        """Test building skill with parameters."""
        builder = SkillBuilder("param_skill")
        builder.parameter("input", "STRING", mode="IN")
        builder.parameter("output", "NUMBER", mode="OUT", default=0)
        builder.returns("DOCUMENT")
        
        skill = builder.build()
        
        assert len(skill.parameters) == 2
        assert skill.parameters[0].name == "input"
        assert skill.parameters[0].datatype == "STRING"
        assert skill.parameters[1].default == 0
        assert skill.returns == "DOCUMENT"
    
    def test_builder_with_metadata(self):
        """Test building skill with metadata."""
        builder = SkillBuilder("meta_skill")
        builder.author("Test Author")
        builder.tag("tag1", "tag2")
        builder.requires("dep1", "dep2")
        
        skill = builder.build()
        
        assert skill.author == "Test Author"
        assert skill.tags == ["tag1", "tag2"]
        assert skill.dependencies == ["dep1", "dep2"]
    
    def test_to_statement_basic(self):
        """Test generating CREATE SKILL statement."""
        builder = SkillBuilder("simple_skill")
        builder.version("1.0.0")
        builder.body("PRINT 'Hello';")
        
        stmt = builder.to_statement()
        
        assert "CREATE SKILL simple_skill" in stmt
        assert "VERSION '1.0.0'" in stmt
        assert "BEGIN" in stmt
        assert "PRINT 'Hello';" in stmt
        assert "END SKILL;" in stmt
    
    def test_to_statement_full(self):
        """Test generating full CREATE SKILL statement."""
        builder = SkillBuilder("full_skill")
        builder.version("2.0.0")
        builder.description("Full skill example")
        builder.author("Developer")
        builder.tag("test")
        builder.requires("other_skill")
        builder.parameter("x", "NUMBER", mode="IN", default=10)
        builder.returns("STRING")
        builder.body("RETURN 'result';")
        
        stmt = builder.to_statement()
        
        assert "CREATE SKILL full_skill" in stmt
        assert "VERSION '2.0.0'" in stmt
        assert "DESCRIPTION 'Full skill example'" in stmt
        assert "AUTHOR 'Developer'" in stmt
        assert "TAGS ['test']" in stmt
        assert "REQUIRES ['other_skill']" in stmt
        assert "x IN NUMBER" in stmt
        assert "RETURNS STRING" in stmt
    
    def test_chaining(self):
        """Test builder method chaining."""
        skill = (SkillBuilder("chain_skill")
            .version("1.0.0")
            .description("Chained")
            .author("Author")
            .tag("a", "b")
            .parameter("p", "STRING")
            .returns("DOCUMENT")
            .body("RETURN {};")
            .build())
        
        assert skill.name == "chain_skill"
        assert skill.version == "1.0.0"
        assert len(skill.tags) == 2
        assert len(skill.parameters) == 1


class TestSkill:
    """Tests for the Skill class."""
    
    def test_from_dict(self):
        """Test creating skill from dictionary."""
        data = {
            "name": "dict_skill",
            "version": "1.0.0",
            "description": "From dict",
            "author": "Author",
            "tags": ["a", "b"],
            "dependencies": ["dep1"],
            "parameters": [
                {"name": "p1", "datatype": "STRING", "mode": "IN"}
            ],
            "returns": "DOCUMENT"
        }
        
        skill = Skill.from_dict(data)
        
        assert skill.name == "dict_skill"
        assert skill.version == "1.0.0"
        assert len(skill.tags) == 2
        assert len(skill.parameters) == 1
        assert skill.parameters[0].name == "p1"
    
    def test_from_dict_minimal(self):
        """Test creating skill from minimal dictionary."""
        data = {"name": "minimal"}
        
        skill = Skill.from_dict(data)
        
        assert skill.name == "minimal"
        assert skill.version == "1.0.0"  # default
        assert skill.tags == []
        assert skill.parameters == []
