# The windows Jpackage resource directory

## Ref

To see a list of file that can be customized for windows.

Check the [ref](https://docs.oracle.com/en/java/javase/14/jpackage/override-jpackage-resources.html#GUID-405708DC-0243-49FC-84D9-B2A7F0A011A9__GUID-D0DA890C-5D6A-48A5-9380-FD18DB66F33D)

## List

* [Main WiX source file, main.wxs](main.wxs) to add the installation directory in the PATH so that the user does not need to do it.

## How to get the files

* Run Jreleaser on Windows
```bash
.\contrib\script\jreleaser-de.ps1 assemble --debug --select-current-platform --assembler=jpackage
```
* Grab the Jpackage command in the JReleaser `trace.log` file and add the `--temp` option with a directory 
```bash
jpackage ... --temp "C:\tmp\jpack\"
```
* Check the directory `C:\tmp\jpack\config`

