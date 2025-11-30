FROM mcr.microsoft.com/devcontainers/base:ubuntu

ARG JAVA_VERSION=25

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Detect architecture and download appropriate JDK
RUN ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "amd64" ]; then \
        JDK_ARCH="x64"; \
    elif [ "$ARCH" = "arm64" ]; then \
        JDK_ARCH="aarch64"; \
    else \
        echo "Unsupported architecture: $ARCH" && exit 1; \
    fi && \
    mkdir -p /opt/java && \
    curl -fsSL "https://download.oracle.com/java/${JAVA_VERSION}/latest/jdk-${JAVA_VERSION}_linux-${JDK_ARCH}_bin.tar.gz" \
    | tar -xz -C /opt/java --strip-components=1

# Set environment variables
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Maven using Apache archive (more reliable)
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    | tar -xz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Verify installation
RUN java -version && mvn -version
