package com.kobe.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static List<TvaEmbeded> transformTvaEmbeded(String content) {
        if (StringUtils.isNotEmpty(content)) {
            try {
                return new ObjectMapper().readValue(content, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                log.debug("{}",e);
                return Collections.emptyList();
            }

        }

        return Collections.emptyList();
    }
    public static String transformTvaEmbededToString(List<TvaEmbeded> tvaEmbededs) {
        if (tvaEmbededs.size()>0) {
            try {
                return new ObjectMapper().writeValueAsString(tvaEmbededs);
            } catch (JsonProcessingException e) {
                log.debug("{}",e);
                return null;
            }

        }

        return null;
    }
}
