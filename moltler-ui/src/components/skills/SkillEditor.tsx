import { useState, useEffect } from 'react'
import Editor from '@monaco-editor/react'
import { Save, Play, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet'
import type { Skill } from '@/api/skills'

interface ExecutionResult {
  success: boolean;
  result?: unknown;
  output?: string;
  error?: string;
}

interface SkillEditorProps {
  skill: Skill | null
  isOpen: boolean
  onClose: () => void
  onSave: (skill: Skill) => Promise<void>
  onExecute: (code: string) => Promise<ExecutionResult>
  isNew?: boolean
  createType?: 'PROCEDURE' | 'FUNCTION' | 'SKILL'
}

// Register elastic-script language for Monaco
const registerElasticScriptLanguage = (monaco: typeof import('monaco-editor')) => {
  // Register the language
  monaco.languages.register({ id: 'elasticscript' })

  // Define tokens
  monaco.languages.setMonarchTokensProvider('elasticscript', {
    ignoreCase: true,
    keywords: [
      'CREATE', 'PROCEDURE', 'FUNCTION', 'SKILL', 'BEGIN', 'END', 'DECLARE', 'VAR', 'CONST',
      'VERSION', 'DESCRIPTION', 'AUTHOR', 'TAGS', 'RETURNS', 'REQUIRES', 'DEFAULT',
      'IF', 'THEN', 'ELSE', 'ELSEIF', 'ENDIF', 'FOR', 'LOOP', 'WHILE', 'DO',
      'IN', 'OUT', 'INOUT', 'RETURN', 'CALL', 'PRINT', 'SET',
      'TRY', 'CATCH', 'FINALLY', 'THROW', 'EXCEPTION',
      'ON_DONE', 'ON_FAIL', 'TRACK', 'TIMEOUT', 'PARALLEL', 'EXECUTION',
      'STATUS', 'CANCEL', 'RETRY', 'WAIT', 'CURSOR', 'FETCH', 'INTO', 'FROM',
      'TRUE', 'FALSE', 'NULL', 'AND', 'OR', 'NOT', 'AS', 'IS',
      'DROP', 'SHOW', 'SKILLS', 'PACK',
    ],
    builtinFunctions: [
      'LENGTH', 'SUBSTR', 'UPPER', 'LOWER', 'TRIM', 'REPLACE', 'CONCAT',
      'ABS', 'CEIL', 'FLOOR', 'ROUND', 'SQRT', 'POWER',
      'ARRAY_LENGTH', 'ARRAY_APPEND', 'ARRAY_CONTAINS', 'ARRAY_MAP', 'ARRAY_FILTER',
      'DOCUMENT_GET', 'DOCUMENT_KEYS', 'DOCUMENT_MERGE',
      'ESQL_QUERY', 'INDEX_DOCUMENT', 'GET_DOCUMENT', 'UPDATE_DOCUMENT',
      'LLM_COMPLETE', 'LLM_CHAT', 'LLM_EMBED', 'INFERENCE',
      'HTTP_GET', 'HTTP_POST', 'WEBHOOK',
      'CURRENT_DATE', 'CURRENT_TIMESTAMP', 'DATE_ADD', 'DATE_DIFF',
    ],
    operators: [
      '=', '>', '<', '!', '~', '?', ':', '==', '<=', '>=', '!=',
      '&&', '||', '++', '--', '+', '-', '*', '/', '&', '|', '^', '%',
      '<<', '>>', '>>>', '+=', '-=', '*=', '/=', '&=', '|=', '^=',
      '%=', '<<=', '>>=', '>>>=', '??', '?.', '..',
    ],
    symbols: /[=><!~?:&|+\-*\/\^%]+/,
    tokenizer: {
      root: [
        // Comments
        [/--.*$/, 'comment'],
        [/\/\*/, 'comment', '@comment'],

        // Strings
        [/"([^"\\]|\\.)*$/, 'string.invalid'],
        [/'([^'\\]|\\.)*$/, 'string.invalid'],
        [/"/, 'string', '@string_double'],
        [/'/, 'string', '@string_single'],

        // Numbers
        [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
        [/\d+/, 'number'],

        // Keywords and identifiers
        [/[a-zA-Z_]\w*/, {
          cases: {
            '@keywords': 'keyword',
            '@builtinFunctions': 'predefined',
            '@default': 'identifier'
          }
        }],

        // Operators
        [/@symbols/, {
          cases: {
            '@operators': 'operator',
            '@default': ''
          }
        }],

        // Delimiters
        [/[{}()\[\]]/, '@brackets'],
        [/[;,.]/, 'delimiter'],
      ],
      comment: [
        [/[^\/*]+/, 'comment'],
        [/\*\//, 'comment', '@pop'],
        [/[\/*]/, 'comment']
      ],
      string_double: [
        [/[^\\"]+/, 'string'],
        [/\\./, 'string.escape'],
        [/"/, 'string', '@pop']
      ],
      string_single: [
        [/[^\\']+/, 'string'],
        [/\\./, 'string.escape'],
        [/'/, 'string', '@pop']
      ],
    },
  })

  // Define autocomplete
  monaco.languages.registerCompletionItemProvider('elasticscript', {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn
      }

      const keywords = [
        'CREATE PROCEDURE', 'CREATE FUNCTION', 'CREATE SKILL', 
        'BEGIN', 'END PROCEDURE', 'END FUNCTION', 'END SKILL',
        'VERSION', 'DESCRIPTION', 'AUTHOR', 'TAGS', 'RETURNS', 'REQUIRES', 'DEFAULT',
        'DECLARE', 'VAR', 'CONST',
        'IF', 'THEN', 'ELSE', 'ELSEIF', 'ENDIF', 'FOR', 'LOOP', 'END LOOP',
        'WHILE', 'DO', 'RETURN', 'CALL', 'PRINT', 'SET',
        'TRY', 'CATCH', 'FINALLY', 'ON_DONE', 'ON_FAIL', 'TRACK',
        'DROP PROCEDURE', 'DROP FUNCTION', 'DROP SKILL', 'SHOW SKILLS',
      ]

      const functions = [
        'LENGTH', 'SUBSTR', 'UPPER', 'LOWER', 'TRIM', 'REPLACE', 'CONCAT',
        'ESQL_QUERY', 'LLM_COMPLETE', 'LLM_CHAT', 'HTTP_GET', 'HTTP_POST',
        'ARRAY_LENGTH', 'ARRAY_MAP', 'ARRAY_FILTER', 'DOCUMENT_GET',
      ]

      const suggestions = [
        ...keywords.map(k => ({
          label: k,
          kind: monaco.languages.CompletionItemKind.Keyword,
          insertText: k,
          range,
        })),
        ...functions.map(f => ({
          label: f,
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: f + '($0)',
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          range,
        })),
      ]

      return { suggestions }
    }
  })
}

const TEMPLATES = {
  PROCEDURE: `CREATE PROCEDURE my_procedure()
BEGIN
  -- Your code here
  PRINT 'Hello from Moltler!';
END PROCEDURE;`,
  FUNCTION: `CREATE FUNCTION my_function(value NUMBER)
RETURNS NUMBER
AS
BEGIN
  -- Your code here
  RETURN value * 2;
END FUNCTION;`,
  SKILL: `CREATE SKILL my_skill
  VERSION '1.0'
  DESCRIPTION 'A brief description of what this skill does'
  AUTHOR 'Your Name'
  TAGS ['demo', 'example']
  (param1 STRING DEFAULT 'default_value')
  RETURNS STRING
BEGIN
  -- Your skill implementation
  -- Skills can call procedures: CALL my_procedure();
  -- Or run ES|QL queries: SET result = ESQL_QUERY('FROM logs-* | LIMIT 10');
  RETURN 'Hello from ' || param1;
END SKILL;`,
}

export function SkillEditor({
  skill,
  isOpen,
  onClose,
  onSave,
  onExecute,
  isNew = false,
  createType = 'PROCEDURE',
}: SkillEditorProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [code, setCode] = useState('')
  const [output, setOutput] = useState<string | null>(null)
  const [isExecuting, setIsExecuting] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (skill) {
      setName(skill.name)
      setDescription(skill.description || '')
      setCode(skill.body || '')
    } else if (isNew) {
      setName('')
      setDescription('')
      setCode(TEMPLATES[createType] || TEMPLATES.PROCEDURE)
    }
    setOutput(null)
    setError(null)
  }, [skill, isNew, createType])

  const detectType = (code: string): 'PROCEDURE' | 'FUNCTION' | 'SKILL' => {
    const upper = code.toUpperCase()
    if (upper.includes('CREATE SKILL')) return 'SKILL'
    if (upper.includes('CREATE FUNCTION')) return 'FUNCTION'
    return 'PROCEDURE'
  }

  const handleSave = async () => {
    setIsSaving(true)
    setError(null)
    try {
      await onSave({
        name,
        type: detectType(code),
        description,
        body: code,
      })
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setIsSaving(false)
    }
  }

  // Extract the name from a CREATE statement
  const extractName = (code: string): string | null => {
    // Match CREATE SKILL/PROCEDURE/FUNCTION name
    const match = code.match(/CREATE\s+(SKILL|PROCEDURE|FUNCTION)\s+(\w+)/i)
    return match ? match[2] : null
  }

  // Generate invocation code based on the definition
  const generateInvocation = (code: string): string | null => {
    const upper = code.toUpperCase()
    const extractedName = extractName(code)
    
    if (!extractedName) return null
    
    if (upper.includes('CREATE SKILL')) {
      // For skills, use RUN SKILL which looks up the skill and executes its procedure
      return `RUN SKILL ${extractedName}`
    } else if (upper.includes('CREATE PROCEDURE')) {
      return `CALL ${extractedName}()`
    } else if (upper.includes('CREATE FUNCTION')) {
      // Functions need to be called in an expression context
      return `SELECT ${extractedName}()`
    }
    
    return null
  }

  const handleExecute = async () => {
    setIsExecuting(true)
    setError(null)
    setOutput(null)
    try {
      // Check if this is a definition (CREATE SKILL/PROCEDURE/FUNCTION)
      // If so, invoke it instead of recreating it
      const invocation = generateInvocation(code)
      const codeToExecute = invocation || code
      
      const result = await onExecute(codeToExecute)
      if (result.success) {
        // Format the output to show both message and result
        let outputText = ''
        
        // If we generated an invocation, show what we ran
        if (invocation) {
          outputText += `Invoked: ${invocation}\n\n`
        }
        
        if (result.output) {
          outputText += result.output
        }
        if (result.result !== undefined && result.result !== null) {
          const resultStr = typeof result.result === 'object' 
            ? JSON.stringify(result.result, null, 2) 
            : String(result.result)
          if (outputText && !outputText.endsWith('\n\n')) outputText += '\n\n'
          outputText += `Result:\n${resultStr}`
        }
        setOutput(outputText || 'Executed successfully')
      } else {
        setError(result.error || 'Execution failed')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Execution failed')
    } finally {
      setIsExecuting(false)
    }
  }

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent side="right" className="w-full sm:max-w-3xl flex flex-col">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            {isNew ? 'Create New Skill' : 'Edit Skill'}
            {skill && skill.type && (
              <Badge variant={skill.type === 'PROCEDURE' ? 'default' : skill.type === 'SKILL' ? 'success' : 'secondary'}>
                {skill.type}
              </Badge>
            )}
          </SheetTitle>
          <SheetDescription>
            {isNew
              ? 'Create a new procedure or function'
              : `Editing ${skill?.name}`}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 flex flex-col gap-4 overflow-hidden py-4">
          {/* Name and Description */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Name</label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="skill_name"
                disabled={!isNew}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Description</label>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What does this skill do?"
              />
            </div>
          </div>

          {/* Code Editor */}
          <div className="flex-1 border rounded-md overflow-hidden">
            <Editor
              height="100%"
              defaultLanguage="elasticscript"
              value={code}
              onChange={(value) => setCode(value || '')}
              theme="vs-dark"
              beforeMount={registerElasticScriptLanguage}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                tabSize: 2,
                wordWrap: 'on',
              }}
            />
          </div>

          {/* Output / Error */}
          {(output || error) && (
            <div className={`p-3 rounded-md text-sm font-mono max-h-48 overflow-auto ${
              error
                ? 'bg-red-100 text-red-900 border border-red-300 dark:bg-red-950 dark:text-red-200 dark:border-red-800'
                : 'bg-emerald-100 text-emerald-900 border border-emerald-300 dark:bg-emerald-950 dark:text-emerald-200 dark:border-emerald-800'
            }`}>
              <div className="flex items-start justify-between gap-2">
                <pre className="whitespace-pre-wrap flex-1 overflow-auto">{error || output}</pre>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6 shrink-0 hover:bg-black/10 dark:hover:bg-white/10"
                  onClick={() => {
                    setOutput(null)
                    setError(null)
                  }}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </div>

        <SheetFooter className="flex-row justify-between sm:justify-between">
          <Button
            variant="outline"
            onClick={handleExecute}
            disabled={isExecuting || !code.trim()}
          >
            <Play className="mr-2 h-4 w-4" />
            {isExecuting 
              ? 'Running...' 
              : generateInvocation(code) 
                ? 'Invoke' 
                : 'Run'
            }
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button onClick={handleSave} disabled={isSaving || !name.trim() || !code.trim()}>
              <Save className="mr-2 h-4 w-4" />
              {isSaving ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}
