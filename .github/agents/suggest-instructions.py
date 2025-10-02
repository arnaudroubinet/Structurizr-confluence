#!/usr/bin/env python3
"""
Suggest Awesome GitHub Copilot Instructions

This script analyzes the repository context and suggests relevant instruction files
from the awesome-copilot repository that would benefit this project.
"""

import json
import os
import re
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from urllib.request import urlopen
from urllib.error import URLError

# Constants
AWESOME_COPILOT_README_URL = "https://raw.githubusercontent.com/github/awesome-copilot/main/README.instructions.md"
AWESOME_COPILOT_INSTRUCTIONS_BASE = "https://raw.githubusercontent.com/github/awesome-copilot/main/instructions/"
LOCAL_INSTRUCTIONS_DIR = ".github/instructions"
REPO_ROOT = Path(__file__).parent.parent.parent


class InstructionAnalyzer:
    """Analyzes repository and suggests relevant instructions."""

    def __init__(self):
        self.available_instructions: List[Dict[str, str]] = []
        self.local_instructions: List[Dict[str, str]] = []
        self.repo_context = self._analyze_repo_context()

    def _analyze_repo_context(self) -> Dict[str, any]:
        """Analyze the repository to understand its context."""
        context = {
            "languages": set(),
            "frameworks": set(),
            "build_tools": set(),
            "technologies": set(),
            "file_patterns": {},
        }

        # Check for Java files
        java_files = list(REPO_ROOT.glob("**/*.java"))
        if java_files:
            context["languages"].add("Java")
            
        # Check pom.xml for Maven and dependencies
        pom_file = REPO_ROOT / "pom.xml"
        if pom_file.exists():
            context["build_tools"].add("Maven")
            pom_content = pom_file.read_text()
            
            # Check for Quarkus
            if "quarkus" in pom_content.lower():
                context["frameworks"].add("Quarkus")
            
            # Check for Spring Boot
            if "spring-boot" in pom_content.lower():
                context["frameworks"].add("Spring Boot")
                
        # Check for Docker
        if (REPO_ROOT / "Dockerfile").exists():
            context["technologies"].add("Docker")
            
        # Check for GitHub Actions
        if (REPO_ROOT / ".github" / "workflows").exists():
            context["technologies"].add("GitHub Actions")
            
        # Check for test files
        test_files = list(REPO_ROOT.glob("**/test/**/*.java"))
        if test_files:
            context["technologies"].add("JUnit")

        return context

    def fetch_awesome_instructions(self) -> bool:
        """Fetch and parse the awesome-copilot instructions README."""
        try:
            with urlopen(AWESOME_COPILOT_README_URL) as response:
                content = response.read().decode('utf-8')
                
            # Parse markdown table
            # Pattern: | [Title](path) | Description |
            pattern = r'\| \[([^\]]+)\]\(instructions/([^\)]+)\)[^\|]*\| ([^\|]+) \|'
            matches = re.findall(pattern, content)
            
            for title, filename, description in matches:
                self.available_instructions.append({
                    "title": title.strip(),
                    "filename": filename.strip(),
                    "description": description.strip(),
                    "url": f"{AWESOME_COPILOT_INSTRUCTIONS_BASE}{filename.strip()}"
                })
            
            return True
        except (URLError, Exception) as e:
            print(f"Error fetching awesome-copilot instructions: {e}", file=sys.stderr)
            return False

    def scan_local_instructions(self) -> None:
        """Scan local .github/instructions directory for existing files."""
        instructions_path = REPO_ROOT / LOCAL_INSTRUCTIONS_DIR
        
        if not instructions_path.exists():
            return
            
        for file_path in instructions_path.glob("*.instructions.md"):
            content = file_path.read_text()
            
            # Extract front matter
            front_matter = {}
            if content.startswith("---"):
                parts = content.split("---", 2)
                if len(parts) >= 2:
                    # Parse YAML front matter (simple implementation)
                    for line in parts[1].split("\n"):
                        if ":" in line:
                            key, value = line.split(":", 1)
                            front_matter[key.strip()] = value.strip().strip("'\"")
            
            self.local_instructions.append({
                "filename": file_path.name,
                "description": front_matter.get("description", ""),
                "applyTo": front_matter.get("applyTo", ""),
                "path": str(file_path)
            })

    def is_instruction_installed(self, filename: str) -> bool:
        """Check if an instruction is already installed."""
        return any(local["filename"] == filename for local in self.local_instructions)

    def calculate_relevance_score(self, instruction: Dict[str, str]) -> int:
        """Calculate relevance score for an instruction based on repo context."""
        score = 0
        filename_lower = instruction["filename"].lower()
        description_lower = instruction["description"].lower()
        title_lower = instruction["title"].lower()
        
        # High priority matches
        if "java" in filename_lower or "java" in title_lower:
            if "Java" in self.repo_context["languages"]:
                score += 100
                
        if "quarkus" in filename_lower or "quarkus" in title_lower:
            if "Quarkus" in self.repo_context["frameworks"]:
                score += 100
                
        if "maven" in description_lower or "maven" in title_lower:
            if "Maven" in self.repo_context["build_tools"]:
                score += 80
                
        if "docker" in filename_lower or "containerization" in filename_lower:
            if "Docker" in self.repo_context["technologies"]:
                score += 80
                
        if "github-actions" in filename_lower or "ci-cd" in filename_lower:
            if "GitHub Actions" in self.repo_context["technologies"]:
                score += 70
                
        # Medium priority matches
        if "spring" in filename_lower:
            if "Spring Boot" in self.repo_context["frameworks"]:
                score += 90
                
        if "markdown" in filename_lower:
            score += 40
            
        if "security" in filename_lower or "owasp" in filename_lower:
            score += 50
            
        if "performance" in filename_lower:
            score += 40
            
        # Low priority general matches
        if "test" in filename_lower or "junit" in description_lower:
            if "JUnit" in self.repo_context["technologies"]:
                score += 30
                
        return score

    def generate_suggestions(self, top_n: int = 10) -> List[Dict[str, any]]:
        """Generate top N suggestions based on relevance."""
        suggestions = []
        
        for instruction in self.available_instructions:
            installed = self.is_instruction_installed(instruction["filename"])
            relevance = self.calculate_relevance_score(instruction)
            
            # Find similar local instructions
            similar_local = None
            for local in self.local_instructions:
                if local["filename"] == instruction["filename"]:
                    similar_local = local["filename"]
                    break
            
            if relevance > 0 or not installed:  # Include all non-installed
                suggestions.append({
                    "instruction": instruction,
                    "installed": installed,
                    "similar_local": similar_local,
                    "relevance_score": relevance,
                })
        
        # Sort by relevance score (descending)
        suggestions.sort(key=lambda x: x["relevance_score"], reverse=True)
        
        return suggestions[:top_n]

    def format_table(self, suggestions: List[Dict[str, any]]) -> str:
        """Format suggestions as a markdown table."""
        lines = [
            "| Awesome-Copilot Instruction | Description | Already Installed | Similar Local Instruction | Suggestion Rationale |",
            "|------------------------------|-------------|-------------------|---------------------------|---------------------|"
        ]
        
        for suggestion in suggestions:
            instruction = suggestion["instruction"]
            installed_icon = "✅ Yes" if suggestion["installed"] else "❌ No"
            similar = suggestion["similar_local"] if suggestion["similar_local"] else "None"
            
            # Generate rationale based on relevance score
            rationale = self._generate_rationale(instruction, suggestion["relevance_score"])
            
            # Format the row
            title_link = f"[{instruction['title']}]({instruction['url'].replace(AWESOME_COPILOT_INSTRUCTIONS_BASE, 'https://github.com/github/awesome-copilot/blob/main/instructions/')})"
            
            lines.append(
                f"| {title_link} | {instruction['description']} | {installed_icon} | {similar} | {rationale} |"
            )
        
        return "\n".join(lines)

    def _generate_rationale(self, instruction: Dict[str, str], score: int) -> str:
        """Generate a human-readable rationale for the suggestion."""
        if score >= 100:
            return "Highly relevant - Core technology used in this project"
        elif score >= 70:
            return "Very relevant - Important technology/tool in this project"
        elif score >= 40:
            return "Relevant - Would improve code quality and standards"
        elif score >= 20:
            return "Moderately relevant - Could be useful for this project"
        else:
            return "Available but not specifically matched to project needs"

    def print_analysis_summary(self) -> None:
        """Print a summary of the repository analysis."""
        print("## Repository Context Analysis\n")
        print(f"**Languages:** {', '.join(sorted(self.repo_context['languages'])) or 'None detected'}")
        print(f"**Frameworks:** {', '.join(sorted(self.repo_context['frameworks'])) or 'None detected'}")
        print(f"**Build Tools:** {', '.join(sorted(self.repo_context['build_tools'])) or 'None detected'}")
        print(f"**Technologies:** {', '.join(sorted(self.repo_context['technologies'])) or 'None detected'}")
        print(f"\n**Local Instructions Found:** {len(self.local_instructions)}")
        print(f"**Available Instructions:** {len(self.available_instructions)}")
        print()


def main():
    """Main entry point."""
    print("# Awesome Copilot Instructions Suggestions\n")
    
    analyzer = InstructionAnalyzer()
    
    # Fetch available instructions
    print("Fetching available instructions from awesome-copilot...", file=sys.stderr)
    if not analyzer.fetch_awesome_instructions():
        print("Failed to fetch instructions. Please check your internet connection.", file=sys.stderr)
        return 1
    
    # Scan local instructions
    print("Scanning local instructions...", file=sys.stderr)
    analyzer.scan_local_instructions()
    
    # Print analysis summary
    analyzer.print_analysis_summary()
    
    # Generate and display suggestions
    print("## Top Suggestions for This Repository\n")
    suggestions = analyzer.generate_suggestions(top_n=10)
    
    if not suggestions:
        print("No suggestions found. All relevant instructions may already be installed.")
        return 0
    
    table = analyzer.format_table(suggestions)
    print(table)
    
    print("\n## Next Steps\n")
    print("To install any of these instructions, use the following command:")
    print("```bash")
    print("python .github/agents/download-instruction.py <instruction-filename>")
    print("```")
    print("\nExample:")
    print("```bash")
    print("python .github/agents/download-instruction.py java.instructions.md")
    print("```")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
