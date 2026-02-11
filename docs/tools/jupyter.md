# Jupyter Integration

Use Moltler skills and connectors in Jupyter notebooks.

## Installation

```bash
pip install moltler[jupyter]
```

## Setup

### Register Kernel

```bash
python -m moltler.jupyter install
```

This installs the `elastic-script` kernel for Jupyter.

### Start Notebook

```bash
jupyter notebook
```

Select "elastic-script" as the kernel.

---

## Magic Commands

### Cell Magics

```python
%%escript
-- Execute elastic-script code
CREATE SKILL hello
VERSION '1.0.0'
BEGIN
    RETURN 'Hello from Jupyter!';
END SKILL;
```

```python
%%escript
CALL hello();
```

### Line Magics

```python
# Execute a single statement
%escript SHOW SKILLS;

# Call a skill
%escript CALL check_health('api-gateway')
```

---

## Python Integration

### Use Moltler Client

```python
from moltler import Moltler

client = Moltler()

# List skills
skills = client.skills.list()
for skill in skills:
    print(f"{skill.name}: {skill.description}")
```

### Pass Variables Between Python and elastic-script

```python
# Python variable
service_name = "api-gateway"
threshold = 0.95

# Pass to elastic-script
%escript -v service_name threshold
CALL check_slo($service_name, $threshold);
```

### Get Results in Python

```python
%%escript --output result
CALL analyze_logs(time_range => '24h');
```

```python
# Result is now a Python variable
print(f"Found {result['total_patterns']} patterns")

import pandas as pd
df = pd.DataFrame(result['patterns'])
df.head()
```

---

## Visualization

### Built-in Visualizations

```python
%%escript --chart line
FROM metrics-*
| WHERE @timestamp > NOW() - INTERVAL '1h'
| STATS cpu = AVG(system.cpu.pct) BY bucket = DATE_TRUNC('minute', @timestamp)
| SORT bucket;
```

### With Matplotlib

```python
%%escript --output data
FROM metrics-*
| WHERE @timestamp > NOW() - INTERVAL '1h'
| STATS cpu = AVG(system.cpu.pct), memory = AVG(system.memory.pct) 
  BY minute = DATE_TRUNC('minute', @timestamp)
| SORT minute;
```

```python
import matplotlib.pyplot as plt
import pandas as pd

df = pd.DataFrame(data)
df['minute'] = pd.to_datetime(df['minute'])

fig, ax = plt.subplots(figsize=(12, 6))
ax.plot(df['minute'], df['cpu'], label='CPU')
ax.plot(df['minute'], df['memory'], label='Memory')
ax.legend()
ax.set_xlabel('Time')
ax.set_ylabel('Usage %')
plt.show()
```

### With Plotly

```python
import plotly.express as px

df = pd.DataFrame(data)
fig = px.line(df, x='minute', y=['cpu', 'memory'], 
              title='Resource Usage Over Time')
fig.show()
```

---

## Interactive Widgets

```python
import ipywidgets as widgets
from IPython.display import display

service_dropdown = widgets.Dropdown(
    options=['api', 'web', 'worker'],
    value='api',
    description='Service:'
)

def check_service(service):
    result = client.skills.call("check_health", service=service)
    print(f"Status: {result['status']}")

widgets.interact(check_service, service=service_dropdown);
```

---

## Example Notebooks

### Observability Dashboard

```python
# Cell 1: Setup
from moltler import Moltler
import pandas as pd
import plotly.express as px

client = Moltler()
```

```python
# Cell 2: Check all services
%%escript --output slo_status
CALL check_service_slos();
```

```python
# Cell 3: Visualize
df = pd.DataFrame(slo_status['services'])

fig = px.bar(df, x='service', y='availability', 
             color='availability_met',
             title='Service Availability')
fig.add_hline(y=99.9, line_dash="dash", 
              annotation_text="SLO Target")
fig.show()
```

### Incident Investigation

```python
# Cell 1: Get incident context
incident_id = "INC-123"
```

```python
# Cell 2: Query logs around incident
%%escript --output logs -v incident_id
FROM logs-*
| WHERE incident.id == $incident_id
| SORT @timestamp
| LIMIT 100;
```

```python
# Cell 3: Analyze with AI
%%escript --output analysis -v logs
DECLARE log_text = '';
FOR log IN $logs LOOP
    SET log_text = log_text || log.message || '\n';
END LOOP;

RETURN LLM_ANALYZE(log_text, 
    question => 'What caused this incident?');
```

```python
# Cell 4: Display analysis
from IPython.display import Markdown
display(Markdown(analysis['summary']))
```

---

## Best Practices

### 1. Use Meaningful Cell Names

```python
# %% [markdown]
# ## Service Health Check
```

```python
# %% Check all services
%%escript --output health
CALL check_service_slos();
```

### 2. Cache Expensive Queries

```python
from functools import lru_cache

@lru_cache(maxsize=100)
def get_metrics(service, hours):
    return client.skills.call("get_metrics", 
                              service=service, 
                              hours=hours)
```

### 3. Handle Errors Gracefully

```python
try:
    result = client.skills.call("risky_operation")
except Exception as e:
    print(f"Operation failed: {e}")
    # Show fallback data
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-console:{ .lg .middle } __CLI Reference__

    Command-line interface.

    [:octicons-arrow-right-24: CLI](cli.md)

-   :material-microsoft-visual-studio-code:{ .lg .middle } __VSCode Extension__

    IDE integration.

    [:octicons-arrow-right-24: VSCode](vscode.md)

</div>
