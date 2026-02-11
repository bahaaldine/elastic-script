# Salesforce Connector

Connect to Salesforce to query accounts, opportunities, cases, and more.

## Setup

### Create Connector

```sql
CREATE CONNECTOR salesforce_prod
TYPE 'salesforce'
CONFIG {
    "instance_url": "https://mycompany.my.salesforce.com",
    "client_id": "{{secrets.sf_client_id}}",
    "client_secret": "{{secrets.sf_client_secret}}",
    "username": "{{secrets.sf_username}}",
    "password": "{{secrets.sf_password}}",
    "security_token": "{{secrets.sf_security_token}}"
};
```

### OAuth Flow (Recommended)

```sql
CREATE CONNECTOR salesforce_prod
TYPE 'salesforce'
CONFIG {
    "instance_url": "https://mycompany.my.salesforce.com",
    "client_id": "{{secrets.sf_client_id}}",
    "client_secret": "{{secrets.sf_client_secret}}",
    "refresh_token": "{{secrets.sf_refresh_token}}"
};
```

---

## Available Entities

### Accounts (`accounts`)

```sql
SELECT Id, Name, Type, Industry, AnnualRevenue, Website
FROM salesforce_prod.accounts
WHERE Type = 'Customer'
ORDER BY AnnualRevenue DESC
LIMIT 100;
```

### Opportunities (`opportunities`)

```sql
SELECT Id, Name, StageName, Amount, CloseDate, Probability
FROM salesforce_prod.opportunities
WHERE StageName NOT IN ('Closed Won', 'Closed Lost')
  AND CloseDate <= NOW() + INTERVAL '30d'
ORDER BY Amount DESC;
```

### Cases (`cases`)

```sql
SELECT CaseNumber, Subject, Status, Priority, CreatedDate
FROM salesforce_prod.cases
WHERE Status = 'Open'
  AND Priority = 'High'
ORDER BY CreatedDate DESC;
```

### Contacts (`contacts`)

```sql
SELECT Id, Name, Email, Phone, AccountId
FROM salesforce_prod.contacts
WHERE Account.Type = 'Customer';
```

### Leads (`leads`)

```sql
SELECT Id, Name, Email, Company, Status, LeadSource
FROM salesforce_prod.leads
WHERE Status = 'New'
ORDER BY CreatedDate DESC;
```

---

## SOQL Queries

Use native SOQL for complex queries:

```sql
SELECT * FROM salesforce_prod
WHERE SOQL = "SELECT Id, Name, (SELECT Id, Subject FROM Cases) 
              FROM Account WHERE Type = 'Customer'";
```

---

## Actions

### Create Record

```sql
CONNECTOR_EXEC salesforce_prod.cases.create({
    "Subject": "Support request from Moltler",
    "Description": "Automated case creation",
    "Priority": "Medium",
    "Status": "New"
});
```

### Update Record

```sql
CONNECTOR_EXEC salesforce_prod.cases.update(
    record_id => '5001234567',
    data => {
        "Status": "In Progress",
        "Priority": "High"
    }
);
```

### Query with API

```sql
CONNECTOR_EXEC salesforce_prod.query(
    soql => "SELECT Id, Name FROM Account LIMIT 10"
);
```

---

## Example Skills

### Skill: Escalate High-Value Cases

```sql
CREATE SKILL escalate_high_value_cases
VERSION '1.0.0'
DESCRIPTION 'Escalates cases from high-value customers'
PARAMETERS (
    connector STRING,
    revenue_threshold NUMBER DEFAULT 1000000
)
RETURNS NUMBER
BEGIN
    DECLARE cases CURSOR FOR
        SELECT c.Id, c.Subject, c.Priority, a.AnnualRevenue
        FROM connector.cases c
        JOIN connector.accounts a ON c.AccountId = a.Id
        WHERE c.Status = 'Open'
          AND c.Priority != 'Critical'
          AND a.AnnualRevenue > revenue_threshold;
    
    DECLARE count = 0;
    OPEN cases;
    FOR case_rec IN cases LOOP
        CONNECTOR_EXEC connector.cases.update(
            record_id => case_rec.Id,
            data => {"Priority": "Critical"}
        );
        SET count = count + 1;
    END LOOP;
    CLOSE cases;
    
    RETURN count;
END SKILL;
```

---

## What's Next?

<div class="grid cards" markdown>

-   :material-face-agent:{ .lg .middle } __Zendesk Connector__

    Connect to Zendesk.

    [:octicons-arrow-right-24: Zendesk](zendesk.md)

-   :material-hammer-wrench:{ .lg .middle } __Build Your Own__

    Create custom connectors.

    [:octicons-arrow-right-24: Building Connectors](building-connectors.md)

</div>
