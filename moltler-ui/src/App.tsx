import { useState } from 'react'
import { QueryClient, QueryClientProvider, useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Zap, RefreshCw, Moon, Sun } from 'lucide-react'
import { SkillsTable } from '@/components/skills/SkillsTable'
import { SkillEditor } from '@/components/skills/SkillEditor'
import { SkillDetail } from '@/components/skills/SkillDetail'
import { Button } from '@/components/ui/button'
import {
  fetchSkills,
  saveProcedure,
  deleteProcedure,
  executeCode,
  type Skill,
  type ExecutionResult,
} from '@/api/skills'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

function SkillsManager() {
  const queryClientHook = useQueryClient()
  const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null)
  const [editingSkill, setEditingSkill] = useState<Skill | null>(null)
  const [isCreating, setIsCreating] = useState(false)
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark')
    }
    return false
  })

  // Fetch skills
  const { data: skills = [], isLoading, refetch } = useQuery({
    queryKey: ['skills'],
    queryFn: fetchSkills,
  })

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: (skill: Skill) => saveProcedure(skill.body || ''),
    onSuccess: () => {
      queryClientHook.invalidateQueries({ queryKey: ['skills'] })
    },
  })

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (name: string) => deleteProcedure(name),
    onSuccess: () => {
      queryClientHook.invalidateQueries({ queryKey: ['skills'] })
      setSelectedSkill(null)
    },
  })

  const toggleTheme = () => {
    const newDark = !isDark
    setIsDark(newDark)
    document.documentElement.classList.toggle('dark', newDark)
  }

  const handleDelete = async (skill: Skill) => {
    if (confirm(`Are you sure you want to delete "${skill.name}"?`)) {
      await deleteMutation.mutateAsync(skill.name)
    }
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary text-primary-foreground">
                <Zap className="h-6 w-6" />
              </div>
              <div>
                <h1 className="text-xl font-bold">Moltler Skills Manager</h1>
                <p className="text-sm text-muted-foreground">
                  Create, manage, and run elastic-script skills
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="icon"
                onClick={() => refetch()}
                disabled={isLoading}
              >
                <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              </Button>
              <Button
                variant="outline"
                size="icon"
                onClick={toggleTheme}
              >
                {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-8">
        <SkillsTable
          skills={skills}
          isLoading={isLoading}
          onSelect={(skill) => setSelectedSkill(skill)}
          onEdit={(skill) => setEditingSkill(skill)}
          onDelete={handleDelete}
          onExecute={(skill) => {
            setEditingSkill(skill)
          }}
          onCreate={() => setIsCreating(true)}
        />
      </main>

      {/* Skill Detail Flyout */}
      <SkillDetail
        skill={selectedSkill}
        isOpen={!!selectedSkill && !editingSkill}
        onClose={() => setSelectedSkill(null)}
        onEdit={() => {
          setEditingSkill(selectedSkill)
        }}
        onDelete={() => selectedSkill && handleDelete(selectedSkill)}
        onExecute={() => {
          setEditingSkill(selectedSkill)
        }}
      />

      {/* Skill Editor Flyout */}
      <SkillEditor
        skill={editingSkill}
        isOpen={!!editingSkill || isCreating}
        isNew={isCreating}
        onClose={() => {
          setEditingSkill(null)
          setIsCreating(false)
        }}
        onSave={async (skill) => {
          await saveMutation.mutateAsync(skill)
        }}
        onExecute={executeCode}
      />
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SkillsManager />
    </QueryClientProvider>
  )
}

export default App
