#!/usr/bin/env pwsh

function Find-GitRoot {
    $dir = Get-Location
    while ($dir.Path -ne $dir.Root.FullName) {
        if (Test-Path (Join-Path $dir.Path ".git")) {
            return $dir.Path
        }
        $dir = $dir.Parent
    }
    Write-Error "No .git directory found in any parent directory."
    return $null
}

$PROJECT_ROOT = Find-GitRoot
if (-not $PROJECT_ROOT) {
    exit 1
}

# Be sure to generate only for the current platform
$env:JRELEASER_PROJECT_VERSION = yq --exit-status '.project.version' "$PROJECT_ROOT/pom.xml"

# $env:JRELEASER_GITHUB_TOKEN = pass github/docker-registry
# for the docker upload
# $env:JRELEASER_DOCKER_GHCR_IO_PASSWORD = $env:JRELEASER_GITHUB_TOKEN
# Same as maven (Default to out)
$env:JRELEASER_OUTPUT_DIRECTORY = "target"
# search the .git directory recursively
$env:JRELEASER_GIT_ROOT_SEARCH = "true"
# execution directory so that we can execute it from anywhere
$env:JRELEASER_BASEDIR = $PROJECT_ROOT

# Not needed to be good with agent running locally?
$env:JRELEASER_GPG_PASSPHRASE = "yolo"

$env:JRELEASER_GITHUB_TOKEN = "yolo"

# No env for config file, we add it at the command line so that we can do
# https://jreleaser.org/guide/latest/tools/jreleaser-cli.html#_environment_variables
# Extract command
# Why because config file needs to be set after at the cli
$COMMAND = $args[0]
if ($COMMAND -eq "assemble")
{

    $remainingArgs = $args[1..($args.Length - 1)]

    $JRELEASER_CONFIG_FILE = Join-Path $env:JRELEASER_BASEDIR "jreleaser.yml"
    if (-not (Test-Path $JRELEASER_CONFIG_FILE))
    {
        Write-Error "Config file does not exist: $JRELEASER_CONFIG_FILE"
        exit 1
    }

    # $jreleaserPath = Join-Path $HOME ".sdkman/candidates/jreleaser/$JRELEASER_VERSION/bin/jreleaser"
    & jreleaser $COMMAND --config-file="$JRELEASER_CONFIG_FILE" @remainingArgs

    return
}

jreleaser @args