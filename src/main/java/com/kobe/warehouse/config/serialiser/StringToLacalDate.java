package com.kobe.warehouse.config.serialiser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class StringToLacalDate extends StdDeserializer<LocalDate> {

    public StringToLacalDate() {
        this(null);
    }

    public StringToLacalDate(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (StringUtils.isNotEmpty(p.getText())) {
            try {
                return LocalDate.parse(p.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
