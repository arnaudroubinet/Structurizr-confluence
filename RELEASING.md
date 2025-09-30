# Release Process

This document describes how to create a new release of Structurizr Confluence Exporter.

## Versioning

The project follows [Semantic Versioning](https://semver.org/):
- **MAJOR.MINOR.PATCH** (e.g., v1.1.0)
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

## Current Version

- **Latest Release**: v1.0.20
- **Next Version**: v1.1.0 (minor increment prepared)

## Release Methods

### Method 1: Using GitHub Actions Workflow (Recommended)

The easiest way to create a release is through the GitHub Actions workflow:

1. Go to [Actions → Create Release Tag](https://github.com/arnaudroubinet/Structurizr-confluence/actions/workflows/create-release-tag.yml)
2. Click "Run workflow"
3. Enter the version (e.g., `v1.1.0`)
4. Select target branch/commit (usually `main`)
5. Click "Run workflow"

The workflow will:
- ✅ Validate version format
- ✅ Check for duplicate tags
- ✅ Generate changelog
- ✅ Create and push the tag
- ✅ Trigger the release workflow automatically

### Method 2: Using the Release Script

Run the provided script from the repository root:

```bash
./create-release.sh
```

The script will:
- Fetch latest tags and changes
- Create an annotated tag with changelog
- Push the tag to GitHub
- Display next steps

### Method 3: Manual Git Commands

For manual control over the release process:

```bash
# Fetch latest changes
git fetch --all --tags
git checkout main
git pull origin main

# Create annotated tag
git tag -a v1.1.0 -m "Release version 1.1.0

Changes since v1.0.20:
- Fix diagram resolution for view keys containing dashes (#27)
"

# Push tag to trigger release
git push origin v1.1.0
```

### Method 4: Using GitHub CLI

```bash
# Create release directly with GitHub CLI
gh release create v1.1.0 \
  --title "Structurizr Confluence 1.1.0" \
  --notes "## Changes since v1.0.20

- Fix diagram resolution for view keys containing dashes (#27)" \
  --target main
```

## Automated Release Process

Once a tag matching `v*.*.*` is pushed, the `.github/workflows/release.yml` workflow automatically:

1. **Builds** the application with Maven
2. **Tests** the application (full test suite)
3. **Creates** a GitHub release
4. **Uploads** JAR artifact to the release
5. **Builds** and pushes Docker image to GHCR
   - Tags: `1.1.0` and `latest`

## Docker Images

Released Docker images are available at:
- `ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0`
- `ghcr.io/arnaudroubinet/structurizr-confluence:latest`

## Verification

After creating a release, verify:

1. ✅ Tag exists: https://github.com/arnaudroubinet/Structurizr-confluence/tags
2. ✅ Release published: https://github.com/arnaudroubinet/Structurizr-confluence/releases
3. ✅ Workflow succeeded: https://github.com/arnaudroubinet/Structurizr-confluence/actions
4. ✅ Docker image available: `docker pull ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0`
5. ✅ JAR artifact attached to release

## Rollback

If a release needs to be rolled back:

```bash
# Delete tag locally
git tag -d v1.1.0

# Delete tag from GitHub
git push --delete origin v1.1.0

# Delete release from GitHub
gh release delete v1.1.0 --yes

# Or delete via web interface:
# https://github.com/arnaudroubinet/Structurizr-confluence/releases
```

## Changelog Generation

For detailed changelog between versions:

```bash
# View all commits since last tag
git log v1.0.20..main --oneline

# View detailed changes
git log v1.0.20..main --format="%h %s" --no-merges
```

## Release Checklist

Before creating a release:

- [ ] All tests pass locally: `mvn clean test`
- [ ] Build succeeds: `mvn clean install`
- [ ] Docker image builds: `./build-docker.sh`
- [ ] Documentation is up-to-date
- [ ] CHANGELOG or release notes prepared
- [ ] Version number follows semantic versioning
- [ ] No uncommitted changes on main branch

After creating a release:

- [ ] GitHub Actions workflow completes successfully
- [ ] Release artifacts are available
- [ ] Docker image is accessible
- [ ] Release notes are accurate
- [ ] Tag points to correct commit

## Next Release

The next release after v1.1.0 should be:
- **v1.1.1** for bug fixes
- **v1.2.0** for new features
- **v2.0.0** for breaking changes
