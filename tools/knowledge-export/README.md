# Agent Knowledge Export

Use this tool to package the current woodcutting framework, route profiles, and Reason server notes into a single handoff bundle for another agent.

## Run

Double click:

`D:\Codex GPT\RSPS\KSBOT Script building\tools\knowledge-export\Launch-AgentKnowledgeExport.bat`

Or run:

```powershell
powershell -ExecutionPolicy Bypass -File "D:\Codex GPT\RSPS\KSBOT Script building\tools\knowledge-export\Export-AgentKnowledge.ps1"
```

## Output

- zip bundle: `D:\Codex GPT\RSPS\KSBOT Script building\handoff\knowledge\agent-knowledge-bundle-<timestamp>.zip`
- extracted brief: `D:\Codex GPT\RSPS\KSBOT Script building\handoff\knowledge\current-agent-brief\agent-brief.md`
