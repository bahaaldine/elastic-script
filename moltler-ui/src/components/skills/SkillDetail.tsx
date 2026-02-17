import Editor from '@monaco-editor/react'
import { Play, Pencil, Trash2, Copy, Check } from 'lucide-react'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet'
import type { Skill } from '@/api/skills'

interface SkillDetailProps {
  skill: Skill | null
  isOpen: boolean
  onClose: () => void
  onEdit: () => void
  onDelete: () => void
  onExecute: () => void
}

export function SkillDetail({
  skill,
  isOpen,
  onClose,
  onEdit,
  onDelete,
  onExecute,
}: SkillDetailProps) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    if (skill?.body) {
      await navigator.clipboard.writeText(skill.body)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  if (!skill) return null

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent side="right" className="w-full sm:max-w-2xl flex flex-col">
        <SheetHeader>
          <div className="flex items-center justify-between">
            <SheetTitle className="flex items-center gap-2">
              {skill.name}
              <Badge variant={skill.type === 'PROCEDURE' ? 'default' : 'secondary'}>
                {skill.type}
              </Badge>
            </SheetTitle>
          </div>
          <SheetDescription>
            {skill.description || 'No description'}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 flex flex-col gap-4 overflow-hidden py-4">
          {/* Parameters */}
          {skill.parameters && skill.parameters.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-sm font-medium">Parameters</h4>
              <div className="flex flex-wrap gap-2">
                {skill.parameters.map((param, idx) => (
                  <Badge key={idx} variant="outline" className="font-mono">
                    {param.mode && <span className="text-muted-foreground mr-1">{param.mode}</span>}
                    {param.name}: {param.type}
                    {param.default !== undefined && <span className="text-muted-foreground ml-1">= {String(param.default)}</span>}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {/* Code Preview */}
          <div className="flex-1 flex flex-col space-y-2 overflow-hidden">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-medium">Implementation</h4>
              <Button variant="ghost" size="sm" onClick={handleCopy}>
                {copied ? (
                  <>
                    <Check className="mr-2 h-4 w-4 text-green-500" />
                    Copied!
                  </>
                ) : (
                  <>
                    <Copy className="mr-2 h-4 w-4" />
                    Copy
                  </>
                )}
              </Button>
            </div>
            <div className="flex-1 border rounded-md overflow-hidden min-h-[300px]">
              <Editor
                height="100%"
                defaultLanguage="sql"
                value={skill.body}
                theme="vs-dark"
                options={{
                  readOnly: true,
                  minimap: { enabled: false },
                  fontSize: 13,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  automaticLayout: true,
                  wordWrap: 'on',
                }}
              />
            </div>
          </div>

          {/* Metadata */}
          {(skill.created_at || skill.updated_at) && (
            <div className="text-xs text-muted-foreground space-y-1">
              {skill.created_at && (
                <p>Created: {new Date(skill.created_at).toLocaleString()}</p>
              )}
              {skill.updated_at && (
                <p>Updated: {new Date(skill.updated_at).toLocaleString()}</p>
              )}
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center justify-between pt-4 border-t">
          <Button
            variant="destructive"
            size="sm"
            onClick={onDelete}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            Delete
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onExecute}>
              <Play className="mr-2 h-4 w-4" />
              Execute
            </Button>
            <Button onClick={onEdit}>
              <Pencil className="mr-2 h-4 w-4" />
              Edit
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}
