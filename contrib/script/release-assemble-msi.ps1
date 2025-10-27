# Windows script to create the windows installer with Jreleaser on Windows
# You can also create from WSL. See `release-assemble-msi`

# The trick is to
# * clone the github project
# * copy the Jlink image from WSL to Windows
# * run the JReleaser script
# * copy the resulting installer back in WSL so that the release can continue (signining, ...)

##################
# Environment Set up
##################

# Clone
git clone https://github.com/ComboStrap/doc-exec.git
cd doc-exec

# Install tool
winget install jreleaser

# Maven installation to download the JDK for Jpackage
# https://maven.apache.org/install.html
winget install --id=Chocolatey.Chocolatey  -e
choco install maven

# Wix 3 (not 4)
# The Jdk 17 relies on Wix3 - ie on WiX compiler (Candle) and WiX linker (Light).
# This wix tools are no more present in Wix4
winget install -e --id WiXToolset.WiXToolset -v 3.14.1.8722

# Package the jar and download the SDK to get jpackage
mvn '-Dmaven.test.skip=true' package -P release
# Note to only download one jdk, we need to use the jdks:setup-jdks
# but we need to set the download URL

########################
# Installer generation
########################
# Pull
Set-Location .\code\doc-exec\
git pull

# Version parameter
$version = '1.0.0'

# Copy the jlink runtime image
$wslProjectDirectory="\\wsl.localhost\Debian\home\admin\code\doc-exec"
$jlinkRuntimeRelativeDir = "target\jreleaser\assemble\doc-exec-standalone\jlink\work-windows-x86_64\doc-exec-standalone-$version-windows-x86_64"
$null = New-Item -ItemType Directory -Path (Split-Path $jlinkRuntimeRelativeDir -Parent) -Force
Copy-Item "$wslProjectDirectory\$jlinkRuntimeRelativeDir" -Destination $jlinkRuntimeRelativeDir  -Recurse -Force

# Be sure to delete it if exists
$msiInstaller="target\jreleaser\assemble\doc-exec-installer\jpackage\doc-exec-$version-windows-x86_64.msi"
Remove-Item -Path $msiInstaller -Force -ErrorAction SilentlyContinue

# Create the distributions
.\contrib\script\jreleaser-de.ps1 assemble --debug --select-current-platform --assembler=jpackage

# Check if you can install it
#    msiexec /i "target\jreleaser\assemble\doc-exec-installer\jpackage\doc-exec-1.0.0-windows-x86_64.msi" /l*v "target\msi-logfile.txt"
# open a new powerhsell, then run it
#    doc-exec
# then uninstall it
#    msiexec /x "target\jreleaser\assemble\doc-exec-installer\jpackage\doc-exec-1.0.0-windows-x86_64.msi" /l*v "target\uninstall_log.txt"

# Copy it back
Copy-Item $msiInstaller -Destination "$wslProjectDirectory\$msiInstaller" -Force

