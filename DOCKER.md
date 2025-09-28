# Docker Build & Usage

## Building the Docker Image

The project includes a convenient build script that handles both the Java compilation and Docker image creation:

```bash
./build-docker.sh
```

This script will:
1. Build the Java application using Maven
2. Create the Docker image with all required dependencies
3. Test the image to ensure it works correctly

## Manual Build Process

If you prefer to build manually:

```bash
# Build the Java application
mvn clean install

# Build the Docker image
docker build -t structurizr-confluence:latest .
```

## Required System Libraries

The Docker image includes all necessary libraries for Playwright functionality:

### Core Playwright Dependencies
- Browser libraries: `libnss3`, `libnspr4`, `libatk-bridge2.0-0`
- Display libraries: `libdrm2`, `libxss1`, `libxtst6`, `libxrandr2`
- Audio: `libasound2t64`
- Graphics: `libpangocairo-1.0-0`, `libatk1.0-0`, `libcairo-gobject2`
- UI libraries: `libgtk-3-0`, `libgdk-pixbuf2.0-0`, `libxcomposite1`

### Additional Required Libraries
- GStreamer: `libgstreamer1.0-0`, `libgstreamer-plugins-base1.0-0`, `libgstreamer-gl1.0-0`, `libgstreamer-plugins-bad1.0-0`, `gstreamer1.0-libav`
- Media libraries: `libatomic1`, `libxslt1.1`, `libwoff1`, `libvpx9`, `libopus0`, `libwebpdemux2`
- System libraries: `libevent-2.1-7t64`, `libharfbuzz-icu0`, `libenchant-2-2`, `libsecret-1-0`, `libhyphen0`, `libmanette-0.2-0`, `libflite1`

## Usage Examples

```bash
# Show help
docker run --rm structurizr-confluence:latest --help

# Export a workspace
docker run --rm \
  -e CONFLUENCE_URL=https://your-domain.atlassian.net \
  -e CONFLUENCE_USER=your-email@domain.com \
  -e CONFLUENCE_TOKEN=your-api-token \
  -e CONFLUENCE_SPACE_KEY=YOUR_SPACE \
  structurizr-confluence:latest export --workspace-id 123
```

## Image Optimization

The Docker image follows best practices for minimal size:
- Uses `--no-install-recommends` to avoid unnecessary packages
- Single RUN command to minimize layers
- Proper cleanup of apt cache and temporary files
- Efficient build context with `.dockerignore`

Final image size: ~1.05GB (includes Java runtime and all Playwright dependencies)