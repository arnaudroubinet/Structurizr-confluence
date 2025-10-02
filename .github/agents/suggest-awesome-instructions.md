---
mode: 'agent'
description: 'Suggest relevant GitHub Copilot instruction files from the awesome-copilot repository based on current repository context and chat history, avoiding duplicates with existing instructions in this repository.'
tools: ['edit', 'search', 'runCommands', 'runTasks', 'think', 'changes', 'testFailure', 'openSimpleBrowser', 'fetch', 'githubRepo', 'todos', 'search']
---
# Suggest Awesome GitHub Copilot Instructions

Analyze current repository context and suggest relevant copilot-instruction files from the [GitHub awesome-copilot repository](https://github.com/github/awesome-copilot/blob/main/README.instructions.md) that are not already available in this repository.

## Process

1. **Fetch Available Instructions**: Extract instruction list and descriptions from [awesome-copilot README.instructions.md](https://github.com/github/awesome-copilot/blob/main/README.instructions.md). Must use `#fetch` tool.
2. **Scan Local Instructions**: Discover existing instruction files in `.github/instructions/` folder
3. **Extract Descriptions**: Read front matter from local instruction files to get descriptions and `applyTo` patterns
4. **Analyze Context**: Review chat history, repository files, and current project needs
5. **Compare Existing**: Check against instructions already available in this repository
6. **Match Relevance**: Compare available instructions against identified patterns and requirements
7. **Present Options**: Display relevant instructions with descriptions, rationale, and availability status
8. **Validate**: Ensure suggested instructions would add value not already covered by existing instructions
9. **Output**: Provide structured table with suggestions, descriptions, and links to both awesome-copilot instructions and similar local instructions
   **AWAIT** user request to proceed with installation of specific instructions. DO NOT INSTALL UNLESS DIRECTED TO DO SO.
10. **Download Assets**: For requested instructions, automatically download and install individual instructions to `.github/instructions/` folder. Do NOT adjust content of the files.  Use `#todos` tool to track progress. Prioritize use of `#fetch` tool to download assets, but may use `curl` using `#runInTerminal` tool to ensure all content is retrieved.

## Context Analysis Criteria

🔍 **Repository Patterns**:
- Programming languages used (.cs, .js, .py, .ts, .java, etc.)
- Framework indicators (ASP.NET, React, Azure, Next.js, Quarkus, Spring Boot, etc.)
- Project types (web apps, APIs, libraries, tools, CLI applications)
- Development workflow requirements (testing, CI/CD, deployment)
- Build tools (Maven, Gradle, npm, etc.)
- Infrastructure as Code (Docker, Kubernetes, Terraform, etc.)

🗨️ **Chat History Context**:
- Recent discussions and pain points
- Technology-specific questions
- Coding standards discussions
- Development workflow requirements

## Repository Context for This Project

Based on the `.github/copilot-instructions.md` file and repository structure:

**Language & Framework**:
- Java 21 (Primary language)
- Quarkus 3.15.1 (Framework for CLI and REST clients)
- Maven (Build tool)
- Picocli (CLI framework)

**Key Technologies**:
- Structurizr (Architecture documentation)
- Confluence Cloud (Target platform)
- Atlassian Document Format (ADF)
- Playwright (Browser automation for diagrams)
- Docker (Containerization)

**Architecture**:
- CLI application
- REST client integrations
- Document processing pipeline
- Export/conversion workflows

**Development Practices**:
- CI/CD with GitHub Actions
- Docker builds
- Semantic versioning
- Release automation

## Output Format

Display analysis results in structured table comparing awesome-copilot instructions with existing repository instructions:

| Awesome-Copilot Instruction | Description | Already Installed | Similar Local Instruction | Suggestion Rationale |
|------------------------------|-------------|-------------------|---------------------------|---------------------|
| [java.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/java.instructions.md) | Guidelines for building Java base applications | ❌ No | None | Would enhance Java development with established patterns for Java 21 features |
| [quarkus.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/quarkus.instructions.md) | Quarkus development standards and instructions | ❌ No | None | Project uses Quarkus 3.15.1 - would benefit from framework-specific patterns |
| [containerization-docker-best-practices.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/containerization-docker-best-practices.instructions.md) | Comprehensive Docker best practices | ❌ No | None | Project has extensive Docker usage - would improve Dockerfile optimization |

## Local Instructions Discovery Process

1. List all `*.instructions.md` files in the `.github/instructions/` directory
2. For each discovered file, read front matter to extract `description` and `applyTo` patterns
3. Build comprehensive inventory of existing instructions with their applicable file patterns
4. Use this inventory to avoid suggesting duplicates

## File Structure Requirements

Based on GitHub documentation, copilot-instructions files should be:
- **Repository-wide instructions**: `.github/copilot-instructions.md` (applies to entire repository) ✅ Already exists
- **Path-specific instructions**: `.github/instructions/NAME.instructions.md` (applies to specific file patterns via `applyTo` frontmatter)
- **Community instructions**: `instructions/NAME.instructions.md` (for sharing and distribution)

## Front Matter Structure

Instructions files in awesome-copilot use this front matter format:
```markdown
---
description: 'Brief description of what this instruction provides'
applyTo: '**/*.js,**/*.ts' # Optional: glob patterns for file matching
---
```

## Requirements

- Use `githubRepo` tool to get content from awesome-copilot repository
- Scan local file system for existing instructions in `.github/instructions/` directory
- Read YAML front matter from local instruction files to extract descriptions and `applyTo` patterns
- Compare against existing instructions in this repository to avoid duplicates
- Focus on gaps in current instruction library coverage
- Validate that suggested instructions align with repository's purpose and standards
- Provide clear rationale for each suggestion
- Include links to both awesome-copilot instructions and similar local instructions
- Consider technology stack compatibility and project-specific needs
- Don't provide any additional information or context beyond the table and the analysis

## Icons Reference

- ✅ Already installed in repo
- ❌ Not installed in repo

## Relevant Instructions for This Repository

Based on the repository analysis, the following types of instructions would be most relevant:

### High Priority
1. **Java Development** - Core language (Java 21)
2. **Quarkus** - Primary framework (3.15.1)
3. **Maven** - Build tool
4. **Docker/Containerization** - Extensive Docker usage
5. **GitHub Actions CI/CD** - Automated workflows

### Medium Priority
6. **Markdown** - Documentation standards
7. **Security/OWASP** - Secure coding practices
8. **Performance Optimization** - Application performance
9. **Testing** - JUnit and integration tests

### Low Priority (Context-Specific)
10. **Java Upgrade Guides** - If planning version upgrades
11. **DevOps Core Principles** - General best practices
12. **Accessibility** - If UI components are added

## Download Process

When user requests specific instructions:

1. Use `#fetch` tool to download from:
   - Base URL: `https://raw.githubusercontent.com/github/awesome-copilot/main/instructions/`
   - Filename: `{instruction-name}.instructions.md`
   - Example: `https://raw.githubusercontent.com/github/awesome-copilot/main/instructions/java.instructions.md`

2. Save to: `.github/instructions/{instruction-name}.instructions.md`

3. Verify download success and file integrity

4. Report completion with file location

5. Do NOT modify the content of downloaded files

## Example Workflow

**User Request**: "Suggest relevant instructions for this repository"

**Agent Response**:
1. Fetch awesome-copilot README.instructions.md
2. Parse available instructions
3. Scan `.github/instructions/` directory
4. Analyze repository context (Java, Quarkus, Maven, Docker)
5. Generate comparison table
6. Present top 5-10 most relevant suggestions
7. Wait for user selection

**User Request**: "Install java.instructions.md and quarkus.instructions.md"

**Agent Response**:
1. Create todos for each file
2. Download java.instructions.md from awesome-copilot
3. Save to `.github/instructions/java.instructions.md`
4. Download quarkus.instructions.md from awesome-copilot
5. Save to `.github/instructions/quarkus.instructions.md`
6. Report completion

## Notes

- This agent does NOT automatically install instructions
- Always wait for explicit user approval before downloading
- Preserve original content from awesome-copilot without modifications
- Track progress using `#todos` tool
- Provide clear rationale for each suggestion
- Consider existing `.github/copilot-instructions.md` when suggesting additions
