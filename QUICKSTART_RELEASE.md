# Quick Start: Creating Release v1.1.0

## ⚡ Fastest Method (After PR Merge)

Once this PR is merged, go to:
https://github.com/arnaudroubinet/Structurizr-confluence/actions/workflows/create-release-tag.yml

Then:
1. Click **"Run workflow"**
2. Enter version: `v1.1.0`
3. Select target: `main`
4. Click **"Run workflow"** button

The release will be created automatically! 🎉

## 📋 What This PR Provides

This PR adds complete release infrastructure:

### 1. Automated Release Workflow
`.github/workflows/create-release-tag.yml` - Manually trigger releases from GitHub Actions

### 2. Release Script  
`create-release.sh` - Run locally to create and push releases

### 3. Comprehensive Documentation
- `RELEASING.md` - Complete guide to the release process
- `RELEASE_v1.1.0.md` - Specific instructions for v1.1.0

## 🎯 Next Steps After Merge

1. **Merge this PR** to main branch
2. **Run the workflow** from the Actions tab (link above)
3. **Verify the release** at https://github.com/arnaudroubinet/Structurizr-confluence/releases/tag/v1.1.0

That's it! The workflow will:
- ✅ Create the v1.1.0 tag
- ✅ Trigger the release workflow
- ✅ Build and publish the JAR
- ✅ Build and push the Docker image

## 🔍 What Changed Since v1.0.20

- Fix diagram resolution for view keys containing dashes (#27)

## 📦 Release Outputs

After the workflow completes, you'll have:
- GitHub Release with attached JAR file
- Docker image: `ghcr.io/arnaudroubinet/structurizr-confluence:1.1.0`
- Docker image: `ghcr.io/arnaudroubinet/structurizr-confluence:latest`

## 🛠️ Alternative: Manual Release

If you prefer manual control, after merging:

```bash
git checkout main
git pull origin main
./create-release.sh
```

Or see `RELEASING.md` for other methods.
