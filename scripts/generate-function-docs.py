#!/usr/bin/env python3
"""
Generate function documentation from @FunctionSpec annotations.

This script scans Java source files for @FunctionSpec annotations and
generates markdown documentation organized by category.

Usage:
    python generate-function-docs.py [--output docs/functions]
"""

import os
import re
import sys
import argparse
from pathlib import Path
from collections import defaultdict

# Pattern to match @FunctionSpec annotations
FUNCTION_SPEC_PATTERN = re.compile(
    r'@FunctionSpec\s*\(\s*'
    r'name\s*=\s*"([^"]+)".*?'
    r'description\s*=\s*"([^"]+)".*?'
    r'parameters\s*=\s*\{([^}]*)\}.*?'
    r'returnType\s*=\s*@FunctionReturn\s*\(([^)]+)\).*?'
    r'examples\s*=\s*\{([^}]*)\}.*?'
    r'category\s*=\s*FunctionCategory\.(\w+)',
    re.DOTALL
)

# Pattern to match @FunctionParam annotations
PARAM_PATTERN = re.compile(
    r'@FunctionParam\s*\('
    r'name\s*=\s*"([^"]+)".*?'
    r'type\s*=\s*"([^"]+)".*?'
    r'description\s*=\s*"([^"]+)"'
    r'(?:.*?optional\s*=\s*(\w+))?.*?\)',
    re.DOTALL
)

# Pattern for return type
RETURN_TYPE_PATTERN = re.compile(
    r'type\s*=\s*"([^"]+)"(?:.*?description\s*=\s*"([^"]+)")?',
    re.DOTALL
)


def extract_functions_from_file(filepath):
    """Extract @FunctionSpec annotations from a Java file."""
    functions = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Warning: Could not read {filepath}: {e}", file=sys.stderr)
        return functions
    
    for match in FUNCTION_SPEC_PATTERN.finditer(content):
        name = match.group(1)
        description = match.group(2)
        params_str = match.group(3)
        return_str = match.group(4)
        examples_str = match.group(5)
        category = match.group(6)
        
        # Parse parameters
        params = []
        for param_match in PARAM_PATTERN.finditer(params_str):
            params.append({
                'name': param_match.group(1),
                'type': param_match.group(2),
                'description': param_match.group(3),
                'optional': param_match.group(4) == 'true' if param_match.group(4) else False
            })
        
        # Parse return type
        return_match = RETURN_TYPE_PATTERN.search(return_str)
        return_type = return_match.group(1) if return_match else "ANY"
        return_desc = return_match.group(2) if return_match and return_match.group(2) else ""
        
        # Parse examples
        examples = re.findall(r'"([^"]+)"', examples_str)
        
        functions.append({
            'name': name,
            'description': description,
            'parameters': params,
            'return_type': return_type,
            'return_description': return_desc,
            'examples': examples,
            'category': category,
            'source_file': os.path.basename(filepath)
        })
    
    return functions


def scan_directory(source_dir):
    """Scan directory for Java files and extract all function specs."""
    all_functions = []
    
    for root, dirs, files in os.walk(source_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                functions = extract_functions_from_file(filepath)
                all_functions.extend(functions)
    
    return all_functions


def generate_function_docs(func):
    """Generate markdown documentation for a single function."""
    md = []
    
    # Function signature
    params_sig = ', '.join([
        f"{p['name']} {p['type']}" + (" [optional]" if p.get('optional') else "")
        for p in func['parameters']
    ])
    md.append(f"### {func['name']}\n")
    md.append(f"```\n{func['name']}({params_sig}) -> {func['return_type']}\n```\n")
    
    # Description
    md.append(f"{func['description']}\n")
    
    # Parameters table
    if func['parameters']:
        md.append("**Parameters:**\n")
        md.append("| Name | Type | Description |")
        md.append("|------|------|-------------|")
        for p in func['parameters']:
            optional = " (optional)" if p.get('optional') else ""
            md.append(f"| `{p['name']}` | {p['type']}{optional} | {p['description']} |")
        md.append("")
    
    # Return type
    md.append(f"**Returns:** `{func['return_type']}`")
    if func.get('return_description'):
        md.append(f" - {func['return_description']}")
    md.append("\n")
    
    # Examples
    if func['examples']:
        md.append("**Examples:**\n")
        md.append("```sql")
        for ex in func['examples']:
            md.append(ex)
        md.append("```\n")
    
    return '\n'.join(md)


def generate_category_doc(category, functions):
    """Generate markdown for a category of functions."""
    md = []
    
    # Category header
    category_title = category.replace('_', ' ').title()
    md.append(f"# {category_title} Functions\n")
    
    # Quick reference table
    md.append("## Quick Reference\n")
    md.append("| Function | Description |")
    md.append("|----------|-------------|")
    for func in sorted(functions, key=lambda f: f['name']):
        md.append(f"| [`{func['name']}`](#{func['name'].lower().replace('_', '-')}) | {func['description'][:60]}{'...' if len(func['description']) > 60 else ''} |")
    md.append("\n---\n")
    
    # Detailed documentation
    md.append("## Function Details\n")
    for func in sorted(functions, key=lambda f: f['name']):
        md.append(generate_function_docs(func))
        md.append("---\n")
    
    return '\n'.join(md)


def generate_index(categories):
    """Generate an index page for all function categories."""
    md = []
    
    md.append("# Built-in Functions Reference\n")
    md.append("elastic-script provides a comprehensive library of built-in functions ")
    md.append("organized by category.\n")
    
    md.append("## Categories\n")
    
    total_functions = sum(len(funcs) for funcs in categories.values())
    md.append(f"Total: **{total_functions} functions** across **{len(categories)} categories**\n")
    
    md.append("| Category | Functions | Description |")
    md.append("|----------|-----------|-------------|")
    
    category_descriptions = {
        'STRING': 'String manipulation and formatting',
        'NUMBER': 'Mathematical and numeric operations',
        'ARRAY': 'Array/list operations and transformations',
        'DATE': 'Date and time operations',
        'DOCUMENT': 'Document/object manipulation',
        'ELASTICSEARCH': 'Elasticsearch data operations',
        'OPENAI': 'OpenAI LLM integration',
        'INFERENCE': 'Elasticsearch Inference API',
        'INTROSPECTION': 'Script introspection and metadata',
        'SLACK': 'Slack messaging integration',
        'AWS': 'AWS service integration',
        'KUBERNETES': 'Kubernetes cluster operations',
        'PAGERDUTY': 'PagerDuty incident management',
        'TERRAFORM': 'Terraform Cloud integration',
        'CICD': 'CI/CD pipeline integration',
        'S3': 'AWS S3 storage operations',
        'HTTP': 'Generic HTTP/webhook operations',
    }
    
    for category in sorted(categories.keys()):
        funcs = categories[category]
        desc = category_descriptions.get(category, '')
        category_link = category.lower()
        md.append(f"| [{category}]({category_link}.md) | {len(funcs)} | {desc} |")
    
    md.append("\n## Usage\n")
    md.append("Functions can be called from anywhere in elastic-script:\n")
    md.append("```sql")
    md.append("-- In expressions")
    md.append("DECLARE result STRING = UPPER('hello');")
    md.append("")
    md.append("-- In SET statements")
    md.append("SET total = ARRAY_LENGTH(items);")
    md.append("")
    md.append("-- In conditions")
    md.append("IF LENGTH(name) > 10 THEN")
    md.append("    PRINT 'Long name';")
    md.append("END IF;")
    md.append("```\n")
    
    md.append("## See Also\n")
    md.append("- [Variables & Types](../language/variables-types.md)\n")
    md.append("- [Control Flow](../language/control-flow.md)\n")
    md.append("- [User-Defined Functions](../language/functions.md)\n")
    
    return '\n'.join(md)


def main():
    parser = argparse.ArgumentParser(description='Generate function documentation from @FunctionSpec annotations')
    parser.add_argument('--source', default='elastic-script/src/main/java/org/elasticsearch/xpack/escript/functions',
                        help='Source directory to scan')
    parser.add_argument('--output', default='docs/functions',
                        help='Output directory for generated docs')
    args = parser.parse_args()
    
    # Get the project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    source_dir = project_root / args.source
    output_dir = project_root / args.output
    
    print(f"Scanning {source_dir}...")
    
    if not source_dir.exists():
        print(f"Error: Source directory not found: {source_dir}", file=sys.stderr)
        sys.exit(1)
    
    # Scan for functions
    all_functions = scan_directory(source_dir)
    print(f"Found {len(all_functions)} functions with @FunctionSpec annotations")
    
    if not all_functions:
        print("No functions found. Make sure @FunctionSpec annotations follow the expected format.")
        sys.exit(0)
    
    # Group by category
    categories = defaultdict(list)
    for func in all_functions:
        categories[func['category']].append(func)
    
    print(f"Categories: {', '.join(sorted(categories.keys()))}")
    
    # Create output directory
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Generate category docs
    for category, funcs in categories.items():
        doc_content = generate_category_doc(category, funcs)
        output_file = output_dir / f"{category.lower()}.md"
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(doc_content)
        print(f"  Generated {output_file} ({len(funcs)} functions)")
    
    # Generate index
    index_content = generate_index(categories)
    index_file = output_dir / "README.md"
    with open(index_file, 'w', encoding='utf-8') as f:
        f.write(index_content)
    print(f"  Generated {index_file}")
    
    print(f"\nDocumentation generated in {output_dir}")
    print(f"Total: {len(all_functions)} functions across {len(categories)} categories")


if __name__ == '__main__':
    main()
