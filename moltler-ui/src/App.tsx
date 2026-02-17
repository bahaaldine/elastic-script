import { useState } from 'react'
import { QueryClient, QueryClientProvider, useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Zap, RefreshCw, Moon, Sun, Code, Boxes, FunctionSquare } from 'lucide-react'
import { SkillsTable } from '@/components/skills/SkillsTable'
import { SkillEditor } from '@/components/skills/SkillEditor'
import { SkillDetail } from '@/components/skills/SkillDetail'
import { Button } from '@/components/ui/button'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  fetchSkills,
  fetchProcedures,
  fetchFunctions,
  saveProcedure,
  deleteProcedure,
  executeCode,
  type Skill,
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
  const [createType, setCreateType] = useState<'PROCEDURE' | 'FUNCTION' | 'SKILL'>('PROCEDURE')
  const [activeTab, setActiveTab] = useState('skills')
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark')
    }
    return false
  })

  // Fetch all data
  const { data: skills = [], isLoading: skillsLoading, refetch: refetchSkills } = useQuery({
    queryKey: ['skills'],
    queryFn: fetchSkills,
  })

  const { data: procedures = [], isLoading: proceduresLoading, refetch: refetchProcedures } = useQuery({
    queryKey: ['procedures'],
    queryFn: fetchProcedures,
  })

  const { data: functions = [], isLoading: functionsLoading, refetch: refetchFunctions } = useQuery({
    queryKey: ['functions'],
    queryFn: fetchFunctions,
  })

  const isLoading = skillsLoading || proceduresLoading || functionsLoading

  const refetchAll = () => {
    refetchSkills()
    refetchProcedures()
    refetchFunctions()
  }

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: (skill: Skill) => saveProcedure(skill.body || ''),
    onSuccess: () => {
      queryClientHook.invalidateQueries({ queryKey: ['skills'] })
      queryClientHook.invalidateQueries({ queryKey: ['procedures'] })
      queryClientHook.invalidateQueries({ queryKey: ['functions'] })
    },
  })

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (name: string) => deleteProcedure(name),
    onSuccess: () => {
      queryClientHook.invalidateQueries({ queryKey: ['skills'] })
      queryClientHook.invalidateQueries({ queryKey: ['procedures'] })
      queryClientHook.invalidateQueries({ queryKey: ['functions'] })
      setSelectedSkill(null)
    },
  })

  const toggleTheme = () => {
    const newDark = !isDark
    setIsDark(newDark)
    document.documentElement.classList.toggle('dark', newDark)
  }

  const handleDelete = async (skill: Skill) => {
    const typeLabel = skill.type === 'SKILL' ? 'skill' : skill.type === 'FUNCTION' ? 'function' : 'procedure'
    if (confirm(`Are you sure you want to delete the ${typeLabel} "${skill.name}"?`)) {
      await deleteMutation.mutateAsync(skill.name)
    }
  }

  const handleCreate = (type: 'PROCEDURE' | 'FUNCTION' | 'SKILL') => {
    setCreateType(type)
    setIsCreating(true)
  }

  // Get the active list based on tab
  const getActiveList = () => {
    switch (activeTab) {
      case 'skills':
        return skills
      case 'procedures':
        return procedures
      case 'functions':
        return functions
      default:
        return skills
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
                onClick={refetchAll}
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
      <main className="container mx-auto px-4 py-6">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-4">
          <TabsList className="grid w-full max-w-md grid-cols-3">
            <TabsTrigger value="skills" className="flex items-center gap-2">
              <Boxes className="h-4 w-4" />
              Skills ({skills.length})
            </TabsTrigger>
            <TabsTrigger value="procedures" className="flex items-center gap-2">
              <Code className="h-4 w-4" />
              Procedures ({procedures.length})
            </TabsTrigger>
            <TabsTrigger value="functions" className="flex items-center gap-2">
              <FunctionSquare className="h-4 w-4" />
              Functions ({functions.length})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="skills" className="space-y-4">
            <SkillsTable
              skills={skills}
              isLoading={skillsLoading}
              onSelect={(skill) => setSelectedSkill(skill)}
              onEdit={(skill) => setEditingSkill(skill)}
              onDelete={handleDelete}
              onExecute={(skill) => setEditingSkill(skill)}
              onCreate={() => handleCreate('SKILL')}
              createLabel="Create Skill"
            />
          </TabsContent>

          <TabsContent value="procedures" className="space-y-4">
            <SkillsTable
              skills={procedures}
              isLoading={proceduresLoading}
              onSelect={(skill) => setSelectedSkill(skill)}
              onEdit={(skill) => setEditingSkill(skill)}
              onDelete={handleDelete}
              onExecute={(skill) => setEditingSkill(skill)}
              onCreate={() => handleCreate('PROCEDURE')}
              createLabel="Create Procedure"
            />
          </TabsContent>

          <TabsContent value="functions" className="space-y-4">
            <SkillsTable
              skills={functions}
              isLoading={functionsLoading}
              onSelect={(skill) => setSelectedSkill(skill)}
              onEdit={(skill) => setEditingSkill(skill)}
              onDelete={handleDelete}
              onExecute={(skill) => setEditingSkill(skill)}
              onCreate={() => handleCreate('FUNCTION')}
              createLabel="Create Function"
            />
          </TabsContent>
        </Tabs>
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
        createType={createType}
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
