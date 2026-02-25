#!/usr/bin/env npx ts-node
/**
 * Generate skills data from the hub/skills directory
 * Run: npx ts-node scripts/generate-skills.ts
 */

import * as fs from 'fs';
import * as path from 'path';

interface Skill {
  name: string;
  displayName: string;
  description: string;
  version: string;
  author: string;
  category: string;
  tags: string[];
  parameters: Parameter[];
  returns: string;
  sourceUrl: string;
}

interface Parameter {
  name: string;
  type: string;
  description: string;
  default?: string;
  required: boolean;
}

const HUB_BASE_PATH = path.join(__dirname, '../../hub/skills');
const OUTPUT_PATH = path.join(__dirname, '../src/data/skills.json');

// Skill sources with their base URLs
// Multi-category sources contain category subdirectories (elastic/observability/skill/)
// Single-category sources have skills directly under them (sfdc/skill/)
const SKILL_SOURCES = [
  { path: 'elastic', baseUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/elastic', isSingleCategory: false },
  { path: 'sfdc', baseUrl: 'https://github.com/bahaaldine/moltler/tree/main/hub/skills/sfdc', isSingleCategory: true, category: 'sfdc' },
];

function parseSkillSql(content: string): Partial<Skill> {
  const skill: Partial<Skill> = {};
  
  // Extract skill name
  const nameMatch = content.match(/CREATE\s+SKILL\s+(\w+)/i);
  if (nameMatch) {
    skill.name = nameMatch[1];
  }
  
  // Extract version
  const versionMatch = content.match(/VERSION\s+['"]([^'"]+)['"]/i);
  if (versionMatch) {
    skill.version = versionMatch[1];
  }
  
  // Extract description
  const descMatch = content.match(/DESCRIPTION\s+['"]([^'"]+)['"]/i);
  if (descMatch) {
    skill.description = descMatch[1];
  }
  
  // Extract author
  const authorMatch = content.match(/AUTHOR\s+['"]([^'"]+)['"]/i);
  if (authorMatch) {
    skill.author = authorMatch[1];
  }
  
  // Extract tags
  const tagsMatch = content.match(/TAGS\s+\[([^\]]+)\]/i);
  if (tagsMatch) {
    skill.tags = tagsMatch[1]
      .split(',')
      .map(t => t.trim().replace(/['"]/g, ''))
      .filter(t => t.length > 0);
  }
  
  // Extract return type
  const returnsMatch = content.match(/RETURNS\s+(\w+)/i);
  if (returnsMatch) {
    skill.returns = returnsMatch[1];
  }
  
  // Extract parameters (simplified)
  const paramsMatch = content.match(/\(\s*\n?([\s\S]*?)\)\s*\n?\s*RETURNS/i);
  if (paramsMatch) {
    const paramsBlock = paramsMatch[1];
    const paramLines = paramsBlock.split('\n').filter(l => l.trim().length > 0);
    skill.parameters = [];
    
    for (const line of paramLines) {
      const paramMatch = line.match(/(\w+)\s+(STRING|INT|NUMBER|BOOLEAN|ARRAY|DOCUMENT)(?:\s+DEFAULT\s+([^\s,]+))?(?:\s+DESCRIPTION\s+['"]([^'"]+)['"])?/i);
      if (paramMatch) {
        skill.parameters.push({
          name: paramMatch[1],
          type: paramMatch[2].toUpperCase(),
          default: paramMatch[3]?.replace(/['"]/g, ''),
          description: paramMatch[4] || '',
          required: !paramMatch[3],
        });
      }
    }
  }
  
  return skill;
}

function parseSkillYaml(content: string): Partial<Skill> {
  const skill: Partial<Skill> = {};
  
  const lines = content.split('\n');
  for (const line of lines) {
    const match = line.match(/^(\w+):\s*(.*)$/);
    if (match) {
      const [, key, value] = match;
      const cleanValue = value.replace(/^['"]|['"]$/g, '');
      
      switch (key) {
        case 'name':
          skill.name = cleanValue;
          break;
        case 'version':
          skill.version = cleanValue;
          break;
        case 'description':
          skill.description = cleanValue;
          break;
        case 'author':
          skill.author = cleanValue;
          break;
        case 'category':
          skill.category = cleanValue;
          break;
      }
    }
    
    // Parse tags array
    if (line.trim().startsWith('- ') && lines[lines.indexOf(line) - 1]?.includes('tags:')) {
      if (!skill.tags) skill.tags = [];
      skill.tags.push(line.trim().slice(2));
    }
  }
  
  return skill;
}

function toDisplayName(name: string): string {
  return name
    .replace(/-/g, ' ')
    .replace(/\b\w/g, l => l.toUpperCase());
}

function scanSkillDirectory(skillPath: string, baseUrl: string): Skill | null {
  if (!fs.statSync(skillPath).isDirectory()) return null;
  
  const skillDir = path.basename(skillPath);
  const category = path.basename(path.dirname(skillPath));
  
  let skill: Skill = {
    name: skillDir,
    displayName: toDisplayName(skillDir),
    description: `${toDisplayName(skillDir)} skill for ${category}`,
    version: '1.0.0',
    author: 'moltler',
    category: category,
    tags: [category],
    parameters: [],
    returns: 'ARRAY',
    sourceUrl: `${baseUrl}/${category}/${skillDir}`,
  };
  
  // Try to parse skill.sql
  const skillSqlPath = path.join(skillPath, 'skill.sql');
  if (fs.existsSync(skillSqlPath)) {
    const content = fs.readFileSync(skillSqlPath, 'utf-8');
    const parsed = parseSkillSql(content);
    skill = { ...skill, ...parsed };
  }
  
  // Try to parse skill.yaml
  const skillYamlPath = path.join(skillPath, 'skill.yaml');
  if (fs.existsSync(skillYamlPath)) {
    const content = fs.readFileSync(skillYamlPath, 'utf-8');
    const parsed = parseSkillYaml(content);
    skill = { ...skill, ...parsed };
  }
  
  // Ensure category is set
  skill.category = category;
  
  return skill;
}

function generateSkillsData(): Skill[] {
  const skills: Skill[] = [];
  
  for (const source of SKILL_SOURCES) {
    const sourcePath = path.join(HUB_BASE_PATH, source.path);
    
    if (!fs.existsSync(sourcePath)) {
      console.log(`Source path not found: ${sourcePath}, skipping...`);
      continue;
    }
    
    console.log(`Scanning ${source.path}...`);
    
    if (source.isSingleCategory) {
      // Single-category source: skills are directly under the source path
      const skillDirs = fs.readdirSync(sourcePath);
      
      for (const skillDir of skillDirs) {
        const skillPath = path.join(sourcePath, skillDir);
        if (!fs.statSync(skillPath).isDirectory()) continue;
        
        const skill = scanSkillDirectory(skillPath, source.baseUrl);
        if (skill) {
          skill.category = source.category || source.path;
          skill.sourceUrl = `${source.baseUrl}/${skillDir}`;
          skills.push(skill);
        }
      }
    } else {
      // Multi-category source: categories are subdirectories
      const categories = fs.readdirSync(sourcePath);
      
      for (const category of categories) {
        const categoryPath = path.join(sourcePath, category);
        if (!fs.statSync(categoryPath).isDirectory()) continue;
        
        const skillDirs = fs.readdirSync(categoryPath);
        
        for (const skillDir of skillDirs) {
          const skillPath = path.join(categoryPath, skillDir);
          const skill = scanSkillDirectory(skillPath, source.baseUrl);
          if (skill) {
            skills.push(skill);
          }
        }
      }
    }
  }
  
  return skills;
}

// Main
const skills = generateSkillsData();
console.log(`Generated ${skills.length} skills`);

// Ensure output directory exists
const outputDir = path.dirname(OUTPUT_PATH);
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Write JSON
fs.writeFileSync(OUTPUT_PATH, JSON.stringify(skills, null, 2));
console.log(`Written to ${OUTPUT_PATH}`);

// Also generate TypeScript file for type safety
const tsOutput = `// Auto-generated - do not edit
import type { Skill } from '@/lib/skills';

export const SKILLS: Skill[] = ${JSON.stringify(skills, null, 2)};

export const SKILL_COUNT = ${skills.length};
`;

fs.writeFileSync(path.join(outputDir, 'skills.ts'), tsOutput);
console.log(`Written TypeScript to ${path.join(outputDir, 'skills.ts')}`);
