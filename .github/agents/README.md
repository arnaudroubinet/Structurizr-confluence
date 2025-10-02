# Awesome Copilot Instructions Tools

This directory contains tools for suggesting and downloading GitHub Copilot instruction files from the [awesome-copilot repository](https://github.com/github/awesome-copilot).

## Overview

These tools help you discover and install relevant Copilot instruction files that match your project's technology stack and development needs.

## Tools

### 1. `suggest-instructions.py`

Analyzes your repository context and suggests relevant instruction files from awesome-copilot.

**Usage:**
```bash
python .github/agents/suggest-instructions.py
```

**What it does:**
- Analyzes the repository to detect languages, frameworks, and tools
- Fetches available instructions from awesome-copilot
- Scans local `.github/instructions/` for already installed instructions
- Generates a ranked list of suggestions with relevance scores
- Displays results in a formatted table

**Example Output:**
```
## Repository Context Analysis

**Languages:** Java
**Frameworks:** Quarkus
**Build Tools:** Maven
**Technologies:** Docker, GitHub Actions, JUnit

## Top Suggestions for This Repository

| Awesome-Copilot Instruction | Description | Already Installed | Similar Local Instruction | Suggestion Rationale |
|------------------------------|-------------|-------------------|---------------------------|---------------------|
| [Java Development](https://github.com/...) | Guidelines for building Java base applications | ✅ Yes | java.instructions.md | Highly relevant - Core technology used in this project |
| [Quarkus](https://github.com/...) | Quarkus development standards | ❌ No | None | Highly relevant - Core technology used in this project |
...
```

### 2. `download-instruction.py`

Downloads specific instruction files from awesome-copilot and saves them to `.github/instructions/`.

**Usage:**
```bash
# Download a single instruction
python .github/agents/download-instruction.py java.instructions.md

# Download multiple instructions
python .github/agents/download-instruction.py java quarkus docker

# You can omit the .instructions.md suffix
python .github/agents/download-instruction.py java
```

**What it does:**
- Downloads instruction files from awesome-copilot repository
- Saves them to `.github/instructions/` directory
- Preserves original content without modifications
- Reports download status and file locations

**Example Output:**
```
Downloading 1 instruction file(s)...

Downloading java.instructions.md from awesome-copilot...
✅ Successfully downloaded java.instructions.md
📁 Saved to: .github/instructions/java.instructions.md

==================================================
Downloaded 1/1 files successfully
```

### 3. `suggest-awesome-instructions.md`

The agent file that defines the workflow and requirements for suggesting instructions. This file is used by GitHub Copilot agents and contains:
- Process description
- Context analysis criteria
- Output format specifications
- Download workflow

## Workflow

### 1. Discover Relevant Instructions

Run the suggestion tool to see what's available:

```bash
python .github/agents/suggest-instructions.py
```

Review the output to identify instructions that would benefit your project.

### 2. Download Selected Instructions

Choose the instructions you want and download them:

```bash
# Download high-priority instructions
python .github/agents/download-instruction.py \
  java.instructions.md \
  quarkus.instructions.md \
  containerization-docker-best-practices.instructions.md
```

### 3. Verify Installation

The instruction files are saved to `.github/instructions/` and will automatically be used by GitHub Copilot when working on relevant files.

You can verify installation by running the suggest script again:

```bash
python .github/agents/suggest-instructions.py
```

Installed instructions will show ✅ Yes in the "Already Installed" column.

## Repository Context Detection

The `suggest-instructions.py` script automatically detects:

- **Languages**: Java, Python, TypeScript, etc. (by file extensions)
- **Frameworks**: Quarkus, Spring Boot, React, etc. (from pom.xml, package.json)
- **Build Tools**: Maven, Gradle, npm, etc. (from build files)
- **Technologies**: Docker, GitHub Actions, Kubernetes, etc. (from Dockerfile, workflows)

This analysis drives the relevance scoring and helps prioritize suggestions.

## Relevance Scoring

Instructions are scored based on how well they match your project:

- **100+ points**: Core technologies (Java, Quarkus for this project)
- **70-99 points**: Important tools (Docker, GitHub Actions)
- **40-69 points**: Quality improvements (Security, Performance)
- **20-39 points**: Moderate relevance
- **0-19 points**: General applicability

## Installed Instructions

Current instructions in this repository:

- ✅ `java.instructions.md` - Java development guidelines
- ✅ `quarkus.instructions.md` - Quarkus framework standards
- ✅ `containerization-docker-best-practices.instructions.md` - Docker best practices

## Adding Custom Instructions

You can also add your own custom instruction files to `.github/instructions/`. They should follow this format:

```markdown
---
description: 'Brief description of the instruction'
applyTo: '**/*.java,**/*.xml'  # Optional: file patterns
---

# Your Instruction Title

Your instruction content here...
```

## Troubleshooting

### Script won't run
Ensure Python 3 is installed:
```bash
python3 --version
```

### Download fails
Check your internet connection and verify the instruction filename is correct. You can browse available instructions at:
https://github.com/github/awesome-copilot/tree/main/instructions

### Instructions not applying
Make sure files are saved in `.github/instructions/` (not `instructions/`) and have the `.instructions.md` extension.

## Resources

- [awesome-copilot repository](https://github.com/github/awesome-copilot)
- [awesome-copilot instructions list](https://github.com/github/awesome-copilot/blob/main/README.instructions.md)
- [GitHub Copilot documentation](https://docs.github.com/en/copilot)

## Contributing

To suggest improvements to these tools or report issues, please open an issue or pull request in this repository.
