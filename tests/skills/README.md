# Moltler Skill Testing Framework

Comprehensive testing infrastructure for Moltler skills. This framework ensures every skill works correctly before being published to the Hub.

## Quick Start

```bash
# Run all skill tests
./tests/skills/run_tests.sh

# Test specific category
./tests/skills/run_tests.sh --category security

# Test specific skill
./tests/skills/run_tests.sh --skill hunt_ioc

# Generate test cases for all skills
./tests/skills/run_tests.sh --generate
```

## Prerequisites

1. **Elasticsearch with elastic-script plugin**
   ```bash
   ./scripts/quick-start.sh
   ```

2. **Python 3.8+** with dependencies
   ```bash
   pip install -r tests/skills/requirements.txt
   ```

## Test Framework Architecture

```
tests/skills/
├── framework/               # Core testing framework
│   ├── __init__.py
│   ├── es_client.py        # Elasticsearch client wrapper
│   ├── skill_tester.py     # Skill test runner
│   ├── fixtures.py         # Test data fixtures
│   └── reporter.py         # Test result reporters
├── fixtures/               # Custom fixture files
├── results/                # Generated test reports
│   ├── test_results.json   # JSON report
│   ├── test_results.md     # Markdown report
│   ├── test_results.xml    # JUnit XML for CI
│   └── badge.json          # Badge data
├── run_tests.sh            # Shell wrapper
├── run_skill_tests.py      # Python test runner
├── generate_test_cases.py  # Test case generator
└── requirements.txt
```

## Test Phases

Each skill goes through 4 test phases:

1. **Syntax Validation** - Validates skill SQL structure
   - Has CREATE SKILL statement
   - Has BEGIN/END SKILL blocks
   - Valid return type
   - Parameters properly defined

2. **Installation** - Installs skill into Elasticsearch
   - Creates the skill procedure
   - No syntax errors from ES

3. **Execution** - Runs skill with test parameters
   - Executes without runtime errors
   - Handles missing data gracefully

4. **Return Validation** - Validates return value
   - Correct type (ARRAY, DOCUMENT, etc.)
   - Contains expected fields
   - Meets result count bounds

## Test Fixtures

The framework creates these test indices:

| Index | Documents | Description |
|-------|-----------|-------------|
| `logs-test` | 200 | Application logs with errors |
| `metrics-test` | 200 | System metrics (CPU, memory) |
| `security-test` | 150 | Security events with threats |
| `apm-test` | 100 | APM transaction traces |
| `content-test` | 50 | Search content documents |

## Creating Test Cases

Each skill can have a `tests.yaml` file:

```yaml
skill: hunt_ioc
category: security
tests:
  - name: default_execution
    description: Test hunting for a known threat IP
    parameters:
      ioc: "192.168.1.100"
      ioc_type: "ip"
    fixtures_required:
      - security-test
    expected_result_type: array
    expected_min_results: 0

  - name: with_domain
    description: Test hunting for a domain IOC
    parameters:
      ioc: "malware.example.com"
      ioc_type: "domain"
    expected_status: success
```

### Test Case Fields

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Test case name |
| `description` | string | What the test validates |
| `parameters` | object | Parameters to pass to skill |
| `fixtures_required` | array | Required fixture indices |
| `expected_status` | string | "success" or "error" |
| `expected_result_type` | string | "array" or "object" |
| `expected_min_results` | int | Minimum results expected |
| `expected_max_results` | int | Maximum results expected |
| `expected_fields` | array | Fields that must exist |
| `skip` | bool | Skip this test |
| `skip_reason` | string | Why test is skipped |

## Generating Test Cases

Auto-generate test cases for all skills:

```bash
# Generate for all skills (won't overwrite existing)
./tests/skills/run_tests.sh --generate

# Overwrite existing test cases
python tests/skills/generate_test_cases.py --overwrite

# Generate for specific category
python tests/skills/generate_test_cases.py --category security
```

## CI Integration

The framework integrates with GitHub Actions:

```yaml
# .github/workflows/skill-tests.yml
- Runs on push to hub/skills/**
- Validates all skill syntax
- Runs execution tests (when ES available)
- Generates JUnit XML for test reporting
- Updates badge on success
```

### Running in CI

```bash
# Syntax validation only (no ES needed)
python run_skill_tests.py --skip-fixtures

# Full test with ES
python run_skill_tests.py --es-url http://elasticsearch:9200
```

## Test Reports

After running tests, find reports in `tests/skills/results/`:

- **test_results.json** - Machine-readable results
- **test_results.md** - Human-readable markdown
- **test_results.xml** - JUnit XML for CI systems
- **badge.json** - Shields.io badge data

## Adding Tests for New Skills

1. Create your skill in `hub/skills/elastic/{category}/{skill-name}/skill.sql`

2. Generate test case:
   ```bash
   ./tests/skills/run_tests.sh --generate
   ```

3. Customize `tests.yaml` if needed

4. Run tests:
   ```bash
   ./tests/skills/run_tests.sh --skill your_skill_name
   ```

5. Ensure tests pass before committing

## Skills Requiring External Services

Some skills require external services and are marked as skipped:

- `send-slack-message` - Requires Slack credentials
- `send-email` - Requires email server
- `trigger-pagerduty` - Requires PagerDuty API key
- `invoke-aws-lambda` - Requires AWS credentials

These skills have `skip: true` in their test cases with an explanation.

## Troubleshooting

### Elasticsearch not available
```bash
# Start Elasticsearch with plugin
./scripts/quick-start.sh
```

### Missing dependencies
```bash
pip install -r tests/skills/requirements.txt
```

### Tests timing out
```bash
# Increase timeout or run specific skill
./tests/skills/run_tests.sh --skill problematic_skill --verbose
```

### Fixture issues
```bash
# Recreate fixtures
./tests/skills/run_tests.sh --teardown
./tests/skills/run_tests.sh --setup-fixtures
```

## Contributing

1. All new skills MUST have passing tests
2. Run full test suite before submitting PR
3. Skills with external dependencies should have `skip: true` with reason
4. Update test cases when modifying skill behavior
