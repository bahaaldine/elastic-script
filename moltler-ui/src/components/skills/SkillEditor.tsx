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
}

// Register elastic-script language for Monaco
const registerElasticScriptLanguage = (monaco: typeof import('monaco-editor')) => {
  // Register the language
  monaco.languages.register({ id: 'elasticscript' })

  // Define tokens
  monaco.languages.setMonarchTokensProvider('elasticscript', {
    ignoreCase: true,
    keywords: [
      'CREATE', 'PROCEDURE', 'FUNCTION', 'BEGIN', 'END', 'DECLARE', 'VAR', 'CONST',
      'IF', 'THEN', 'ELSE', 'ELSEIF', 'ENDIF', 'FOR', 'LOOP', 'WHILE', 'DO',
      'IN', 'OUT', 'INOUT', 'RETURN', 'CALL', 'PRINT', 'SET',
      'TRY', 'CATCH', 'FINALLY', 'THROW', 'EXCEPTION',
      'ON_DONE', 'ON_FAIL', 'TRACK', 'TIMEOUT', 'PARALLEL', 'EXECUTION',
      'STATUS', 'CANCEL', 'RETRY', 'WAIT', 'CURSOR', 'FETCH', 'INTO', 'FROM',
      'TRUE', 'FALSE', 'NULL', 'AND', 'OR', 'NOT', 'AS', 'IS',
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
        'CREATE PROCEDURE', 'BEGIN', 'END PROCEDURE', 'DECLARE', 'VAR', 'CONST',
        'IF', 'THEN', 'ELSE', 'ELSEIF', 'ENDIF', 'FOR', 'LOOP', 'END LOOP',
        'WHILE', 'DO', 'RETURN', 'CALL', 'PRINT', 'SET',
        'TRY', 'CATCH', 'FINALLY', 'ON_DONE', 'ON_FAIL', 'TRACK',
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

export function SkillEditor({
  skill,
  isOpen,
  onClose,
  onSave,
  onExecute,
  isNew = false,
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
      setCode(`CREATE PROCEDURE my_skill()
BEGIN
  -- Your code here
  PRINT 'Hello from Moltler!';
END PROCEDURE;`)
    }
    setOutput(null)
    setError(null)
  }, [skill, isNew])

  const handleSave = async () => {
    setIsSaving(true)
    setError(null)
    try {
      await onSave({
        name,
        type: code.toUpperCase().includes('CREATE FUNCTION') ? 'FUNCTION' : 'PROCEDURE',
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

  const handleExecute = async () => {
    setIsExecuting(true)
    setError(null)
    setOutput(null)
    try {
      const result = await onExecute(code)
      if (result.success) {
        setOutput(result.output || 'Executed successfully')
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
            {skill && (
              <Badge variant={skill.type === 'PROCEDURE' ? 'default' : 'secondary'}>
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
            <div className={`p-3 rounded-md text-sm font-mono ${
              error
                ? 'bg-destructive/10 text-destructive border border-destructive/20'
                : 'bg-green-50 text-green-800 border border-green-200 dark:bg-green-900/20 dark:text-green-200 dark:border-green-800'
            }`}>
              <div className="flex items-start justify-between">
                <pre className="whitespace-pre-wrap">{error || output}</pre>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6 shrink-0"
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
            {isExecuting ? 'Running...' : 'Run'}
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
