#!/usr/bin/env python3
"""
Download Instruction File from Awesome Copilot

This script downloads a specific instruction file from the awesome-copilot repository
and saves it to the local .github/instructions directory.
"""

import sys
from pathlib import Path
from urllib.request import urlopen
from urllib.error import URLError

# Constants
AWESOME_COPILOT_INSTRUCTIONS_BASE = "https://raw.githubusercontent.com/github/awesome-copilot/main/instructions/"
LOCAL_INSTRUCTIONS_DIR = ".github/instructions"
REPO_ROOT = Path(__file__).parent.parent.parent


def download_instruction(filename: str) -> bool:
    """
    Download an instruction file from awesome-copilot.
    
    Args:
        filename: The instruction filename (e.g., 'java.instructions.md')
        
    Returns:
        True if successful, False otherwise
    """
    # Ensure filename ends with .instructions.md
    if not filename.endswith(".instructions.md"):
        filename = f"{filename}.instructions.md"
    
    # Construct URLs
    url = f"{AWESOME_COPILOT_INSTRUCTIONS_BASE}{filename}"
    local_path = REPO_ROOT / LOCAL_INSTRUCTIONS_DIR / filename
    
    # Ensure local directory exists
    local_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Download the file
    try:
        print(f"Downloading {filename} from awesome-copilot...", file=sys.stderr)
        print(f"URL: {url}", file=sys.stderr)
        
        with urlopen(url) as response:
            content = response.read().decode('utf-8')
        
        # Save to local file
        local_path.write_text(content)
        
        print(f"✅ Successfully downloaded {filename}", file=sys.stderr)
        print(f"📁 Saved to: {local_path}", file=sys.stderr)
        
        return True
        
    except URLError as e:
        print(f"❌ Error downloading {filename}: {e}", file=sys.stderr)
        print(f"URL attempted: {url}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}", file=sys.stderr)
        return False


def download_multiple(filenames: list) -> int:
    """
    Download multiple instruction files.
    
    Args:
        filenames: List of instruction filenames
        
    Returns:
        Number of files successfully downloaded
    """
    success_count = 0
    
    for filename in filenames:
        if download_instruction(filename):
            success_count += 1
        print()  # Blank line between downloads
    
    return success_count


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print("Usage: python download-instruction.py <filename1> [filename2] [...]", file=sys.stderr)
        print("\nExamples:", file=sys.stderr)
        print("  python download-instruction.py java.instructions.md", file=sys.stderr)
        print("  python download-instruction.py java quarkus docker", file=sys.stderr)
        return 1
    
    filenames = sys.argv[1:]
    
    print(f"Downloading {len(filenames)} instruction file(s)...\n", file=sys.stderr)
    
    success_count = download_multiple(filenames)
    
    print(f"\n{'=' * 50}", file=sys.stderr)
    print(f"Downloaded {success_count}/{len(filenames)} files successfully", file=sys.stderr)
    
    if success_count > 0:
        print("\nFiles saved to:", file=sys.stderr)
        for filename in filenames:
            if not filename.endswith(".instructions.md"):
                filename = f"{filename}.instructions.md"
            local_path = REPO_ROOT / LOCAL_INSTRUCTIONS_DIR / filename
            if local_path.exists():
                print(f"  - {local_path}", file=sys.stderr)
    
    return 0 if success_count == len(filenames) else 1


if __name__ == "__main__":
    sys.exit(main())
