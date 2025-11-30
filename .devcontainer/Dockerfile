FROM mcr.microsoft.com/devcontainers/base:ubuntu

ARG GRAALVM_VERSION=25.0.1

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Download and install GraalVM
RUN mkdir -p /opt/graalvm \
    && curl -fsSL "https://download.oracle.com/graalvm/25/latest/graalvm-jdk-${GRAALVM_VERSION}_linux-x64_bin.tar.gz" \
    | tar -xz -C /opt/graalvm --strip-components=1 \
    || (curl -fsSL "https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAALVM_VERSION}/graalvm-community-jdk-${GRAALVM_VERSION}_linux-x64_bin.tar.gz" \
    | tar -xz -C /opt/graalvm --strip-components=1)

# Set environment variables
ENV JAVA_HOME=/opt/graalvm
ENV GRAALVM_HOME=/opt/graalvm
ENV PATH="${GRAALVM_HOME}/bin:${PATH}"

# Install Maven
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    | tar -xz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Verify installation
RUN java -version && mvn -version
