# Using GitHub Copilot Instructions

This guide explains how to use and manage GitHub Copilot instruction files in this repository.

## What are Copilot Instructions?

GitHub Copilot instructions are markdown files that provide context-specific guidance to GitHub Copilot, helping it generate code that follows your project's standards and best practices.

## Repository-Wide vs Path-Specific Instructions

### Repository-Wide Instructions
- **File**: `.github/copilot-instructions.md`
- **Applies to**: All files in the repository
- **Use for**: General project conventions, architecture guidelines, development principles

### Path-Specific Instructions
- **Location**: `.github/instructions/*.instructions.md`
- **Applies to**: Specific file types (defined by `applyTo` front matter)
- **Use for**: Language-specific patterns, framework conventions, tool-specific best practices

## Installed Instructions

This repository currently has the following path-specific instructions:

| Instruction | Description | Applies To |
|-------------|-------------|------------|
| `java.instructions.md` | Java development best practices | `**/*.java` |
| `quarkus.instructions.md` | Quarkus framework patterns | Quarkus project files |
| `containerization-docker-best-practices.instructions.md` | Docker optimization and security | Dockerfile, docker-compose |

## Finding Relevant Instructions

### Automatic Suggestions

Run the suggestion tool to discover relevant instructions based on your project:

```bash
python .github/agents/suggest-instructions.py
```

**Example output:**
```
## Repository Context Analysis

**Languages:** Java
**Frameworks:** Quarkus
**Build Tools:** Maven
**Technologies:** Docker, GitHub Actions, JUnit

## Top Suggestions for This Repository

| Awesome-Copilot Instruction | Description | Already Installed | Suggestion Rationale |
|------------------------------|-------------|-------------------|---------------------|
| [Java Development](https://github.com/...) | Java best practices | ✅ Yes | Core technology used in this project |
| [GitHub Actions CI/CD](https://github.com/...) | CI/CD best practices | ❌ No | Important technology in this project |
...
```

### Browsing Available Instructions

You can browse all available instructions at:
- [awesome-copilot repository](https://github.com/github/awesome-copilot)
- [Instructions list](https://github.com/github/awesome-copilot/blob/main/README.instructions.md)

## Installing Instructions

### Using the Download Tool

Download one or more instructions:

```bash
# Single instruction
python .github/agents/download-instruction.py java.instructions.md

# Multiple instructions
python .github/agents/download-instruction.py \
  java.instructions.md \
  github-actions-ci-cd-best-practices.instructions.md \
  security-and-owasp.instructions.md
```

### Manual Installation

You can also manually download instructions:

1. Browse to the [instructions folder](https://github.com/github/awesome-copilot/tree/main/instructions)
2. Find the instruction you want
3. Download the raw file
4. Save it to `.github/instructions/` in your repository

## How Instructions Work

When you work on a file, GitHub Copilot:

1. Reads the repository-wide instructions from `.github/copilot-instructions.md`
2. Finds matching path-specific instructions based on the file you're editing
3. Uses this context to provide relevant suggestions

### Example

When editing a Java file (`src/main/java/MyClass.java`):

- Repository-wide instructions are always applied
- `java.instructions.md` is applied (matches `**/*.java`)
- `quarkus.instructions.md` may be applied (if relevant)

## Verifying Installation

### Check Installed Instructions

List all installed instructions:

```bash
ls -l .github/instructions/
```

### Verify with Suggestion Tool

Run the suggestion tool to see which instructions are marked as installed:

```bash
python .github/agents/suggest-instructions.py
```

Installed instructions will show ✅ in the "Already Installed" column.

## Creating Custom Instructions

You can create your own custom instructions for project-specific needs.

### Structure

```markdown
---
description: 'Brief description of the instruction'
applyTo: '**/*.java,**/*.xml'  # Optional: file patterns
---

# Your Instruction Title

## Section 1

Your instruction content...

## Section 2

More guidance...
```

### Best Practices for Custom Instructions

1. **Be specific**: Focus on your project's unique requirements
2. **Use examples**: Show concrete code examples
3. **Keep it focused**: One instruction per technology/domain
4. **Update regularly**: Review and update as practices evolve
5. **Use front matter**: Include `description` and `applyTo` fields

### Example Custom Instruction

```markdown
---
description: 'Structurizr workspace modeling patterns'
applyTo: '**/workspace/**/*.java'
---

# Structurizr Workspace Patterns

## Workspace Structure

When creating workspace models:
- Use descriptive names for elements
- Apply consistent tagging conventions
- Document relationships clearly

## Example

\`\`\`java
Workspace workspace = new Workspace("My System", "Description");
Model model = workspace.getModel();
SoftwareSystem system = model.addSoftwareSystem("Name", "Description");
system.addTags("Tag1", "Tag2");
\`\`\`
```

## Updating Instructions

### Updating from awesome-copilot

To update an instruction to the latest version:

```bash
# Re-download the instruction (will overwrite existing)
python .github/agents/download-instruction.py java.instructions.md
```

### Reviewing Changes

Before committing updated instructions:

```bash
# View changes
git diff .github/instructions/

# Review specific file
git diff .github/instructions/java.instructions.md
```

## Recommended Instructions for This Project

Based on the current project stack, consider installing:

### High Priority
- ✅ `java.instructions.md` - Already installed
- ✅ `quarkus.instructions.md` - Already installed
- ✅ `containerization-docker-best-practices.instructions.md` - Already installed
- 📦 `github-actions-ci-cd-best-practices.instructions.md` - For CI/CD improvements
- 📦 `security-and-owasp.instructions.md` - For security best practices

### Medium Priority
- 📦 `markdown.instructions.md` - For documentation standards
- 📦 `performance-optimization.instructions.md` - For optimization guidance
- 📦 `java-17-to-java-21-upgrade.instructions.md` - For Java 21 features

### Optional
- 📦 `playwright-typescript.instructions.md` - If working on Playwright tests
- 📦 `devops-core-principles.instructions.md` - For DevOps practices

## Troubleshooting

### Instructions Not Applying

1. **Check file location**: Instructions must be in `.github/instructions/`
2. **Verify file extension**: Files must end with `.instructions.md`
3. **Check front matter**: Ensure `applyTo` pattern matches your files
4. **Reload VS Code**: Sometimes Copilot needs a refresh

### Download Fails

1. **Check internet connection**: Ensure you can access GitHub
2. **Verify filename**: Check spelling and format
3. **Check URL**: Browse to the instruction on GitHub to verify it exists

### Permission Issues

Ensure the `.github/instructions/` directory is writable:

```bash
chmod 755 .github/instructions/
```

## Resources

- [GitHub Copilot Documentation](https://docs.github.com/en/copilot)
- [awesome-copilot Repository](https://github.com/github/awesome-copilot)
- [Project Copilot Instructions](.github/copilot-instructions.md)
- [Agents README](.github/agents/README.md)

## Questions?

If you have questions about using Copilot instructions:

1. Check the [Agents README](.github/agents/README.md)
2. Review the [awesome-copilot documentation](https://github.com/github/awesome-copilot)
3. Open a [GitHub Discussion](https://github.com/arnaudroubinet/Structurizr-confluence/discussions)
