#!/bin/bash
set -e

GRAALVM_VERSION="25.0.1"
GRAALVM_BUILD="8.1"
GRAALVM_DIR="/opt/graalvm"

echo "Installing GraalVM JDK ${GRAALVM_VERSION}+${GRAALVM_BUILD}..."

# Download GraalVM for Linux (amd64)
DOWNLOAD_URL="https://download.oracle.com/graalvm/25/latest/graalvm-jdk-${GRAALVM_VERSION}_linux-x64_bin.tar.gz"

# Alternative: Use GitHub releases if Oracle URL doesn't work
# DOWNLOAD_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAALVM_VERSION}/graalvm-community-jdk-${GRAALVM_VERSION}_linux-x64_bin.tar.gz"

echo "Downloading from: ${DOWNLOAD_URL}"

# Create temp directory and download
TEMP_DIR=$(mktemp -d)
cd "${TEMP_DIR}"

curl -fsSL -o graalvm.tar.gz "${DOWNLOAD_URL}" || {
    echo "Oracle download failed, trying GraalVM Community Edition..."
    DOWNLOAD_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-25.0.1/graalvm-community-jdk-25.0.1_linux-x64_bin.tar.gz"
    curl -fsSL -o graalvm.tar.gz "${DOWNLOAD_URL}"
}

# Extract and install
sudo mkdir -p "${GRAALVM_DIR}"
sudo tar -xzf graalvm.tar.gz -C "${GRAALVM_DIR}" --strip-components=1

# Cleanup
cd -
rm -rf "${TEMP_DIR}"

# Verify installation
echo "GraalVM installation complete!"
"${GRAALVM_DIR}/bin/java" -version

# Update alternatives (optional, for system-wide default)
sudo update-alternatives --install /usr/bin/java java "${GRAALVM_DIR}/bin/java" 1
sudo update-alternatives --install /usr/bin/javac javac "${GRAALVM_DIR}/bin/javac" 1

echo "GraalVM JDK ${GRAALVM_VERSION} setup complete!"
