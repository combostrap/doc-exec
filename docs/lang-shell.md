Execution Options :
* via shell binary 
* via `Java Exec` (ie parsing and passing the args to Java Exec)



## Shell supported:
The doc must have a unit with the following format.
### Bash

The code is executed with `bash -c` (default)

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

### Dos

```xml
<unit envHOME="Whatever">
    <file lang path/to/File>
    </file>
    <code dos>
        echo ^
          %HOME%
    </code>
    <console>
        Whatever
    </console>
</unit>
```

