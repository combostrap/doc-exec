package com.combostrap.docExec.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.nio.file.Path;

public class PathSerializer extends StdSerializer<Path> {

    public PathSerializer() {
        super(Path.class);
    }

    @Override
    public void serialize(Path path, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (path == null) {
            gen.writeNull();
            return;
        }
        boolean isLocal = path.getFileSystem().provider().getScheme().equals("file");
        if(isLocal) {
            /**
             * No scheme, print the path (relative or absolute)
             * {@link Path#toUri()} makes it an absolute path,
             * and we don't want that when reporting
             */
            gen.writeString(path.toString());
            return;
        }
        gen.writeString(path.toUri().toString());
    }
}
