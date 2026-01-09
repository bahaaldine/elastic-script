# Functions

!!! note "Coming Soon"
    Detailed documentation for these functions is being written.
    
    In the meantime, use `ESCRIPT_FUNCTIONS()` to discover available functions.

```sql
DECLARE functions ARRAY = ESCRIPT_FUNCTIONS();
FOR func IN functions LOOP
    PRINT func.name || ': ' || func.description;
END LOOP;
```
