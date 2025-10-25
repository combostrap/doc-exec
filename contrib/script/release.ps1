# Windows script to create the windows installer with Jreleaser

# The trick is to
# * copy the Jlink image on Windows
# * run the JReleaser script
# * copy the resulting installer back in WSL

# Wix 3 (not 4)
# The Jdk 17 relies on Wix3 - ie on WiX compiler (Candle) and WiX linker (Light).
# This wix tools are no more present in Wix4
winget install -e --id WiXToolset.WiXToolset -v 3.14.1.8722

# Create the distributions
.\contrib\script\jreleaser-de.ps1 assemble --debug --select-current-platform

# Old Code
# for a full run from Windows
# Painfull
#
# Maven installation
# https://maven.apache.org/install.html
# winget install --id=Chocolatey.Chocolatey  -e
# choco install maven
#
# Package and download the SDK
# mvn '-Dmaven.test.skip=true' package -P release
# Note to only download one jdk, we need to use the jdks:setup-jdks
# but we need to set the download URL
