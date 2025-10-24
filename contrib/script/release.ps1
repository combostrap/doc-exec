
# Maven installation
# https://maven.apache.org/install.html
winget install --id=Chocolatey.Chocolatey  -e
choco install maven


# Wix 3 (not 4)
winget install -e --id WiXToolset.WiXToolset -v 3.14.1.8722

# Package and download the SDK
mvn '-Dmaven.test.skip=true' package -P release
# Note to only download one jdk, we need to use the jdks:setup-jdks
# but we need to set the download URL

# Create the distributions
.\contrib\script\jreleaser-de.ps1 assemble --debug --select-current-platform

# Create the manifest files
.\contrib\script\jreleaser-de.ps1 prepare --debug --select-current-platform -p=winget

# Test
# winget install --manifest target/jreleaser/prepare/doc-exec-installer/winget/manifests/c/Combostrap/doc-exec/1.0.0