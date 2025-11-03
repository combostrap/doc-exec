# DocExec


## About

`doc-exec` is a tool that can:
* parse documentation files, 
* extract code blocks, 
* execute them, 
* and update the documentation with the execution result.

It's similar to executable documentation tools like Jupyter notebooks but for general
documentation formats.

## Example

### With the doc-exec cli


```bash
doc-exec \
  --doc-path /path/to/the/doc/directory \
  --file-path /path/to/the/file/directory \
  run \
  relative/glob/path/to/the/doc
```

See the [DocExecutorCli Class for all options](src/main/java/com/combostrap/docExec/DocExecutorCli.java)

### Embedded as a library

```java
// The documentation file to execute
final Path path = Paths.get("./src/test/resources/docTest/fileTest.txt");

// Execution
List<DocExecutorResult> docTestRuns = DocExecutor
  .create("run-name")
  // where to find the files to inject
  .setBaseFileDirectory(Paths.get("./src/test/resources"))
  .setShellCommandExecuteViaMainClass("cat", CommandCat.class)
  .build()      
  .run(path);
```

See the [DocExecutor Class for all options](src/main/java/com/combostrap/docExec/DocExecutor.java)


## Doc-Exec Execution

It will:
* scan for [unit node](#unit-node-syntax)
* capture the file, code and console node
* replace the file content if present
* execute the code block if present and replace the console block (if present) with the result

## Unit Node Syntax

A unit may have:
* one or more file block (to replace the content of a file, generally used in the code)
* zero or one code block (the code to execute)
* zero or one console block (to get the content of the code execution)

```xml
<unit envHOME="Whatever">
    <file lang path/to/File>
    </file>
    <code bash>
       echo $HOME
    </code>
    <console>
        Whatever
    </console>
</unit>
```

## Lang
### Java

[Java](docs/lang-java.md)

### Shell Command (Dos, Bash)

[Shell](docs/lang-shell.md)

## Features

### Inject result of code execution

It will replace the content of a console block with the result of a code execution.

### File replacement

It will replace the content of the file inside the file block
```xml
<unit>
    <file lang path/to/File>
    </file>
</unit>
```

The paths are relative to the base file directory

## Installation

### Brew

For Linux/Windows WSL and macOS, with [brew](https://brew.sh/)

```bash
brew install combostrap/tap/doc-exec
```

### Docker

With [docker releases](https://github.com/ComboStrap/doc-exec/pkgs/container/doc-exec)

```bash
docker run \
  --rm \
  -v $(pwd):/workspace \
  ghcr.io/combostrap/doc-exec-alpine:latest \
  doc-exec --version
```
