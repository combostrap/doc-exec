package com.combostrap.docExec.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.logging.Level;

public class LogLevelSerializer extends StdSerializer<Level> {

    public LogLevelSerializer() {
        super(Level.class);
    }

    @Override
    public void serialize(Level level, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (level == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(level.getName().toLowerCase());
    }
}
