/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.sanitizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Sanitizer configuration class. The new config class will be backward compatible
 * to the old version of the config file generated by the light-codegen.
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "sanitizer", configName = "sanitizer", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class SanitizerConfig {
    public static final String ENABLED = "enabled";
    public static final String BODY_ENABLED = "bodyEnabled";
    public static final String HEADER_ENABLED = "headerEnabled";
    public static Logger logger = LoggerFactory.getLogger(SanitizerConfig.class);

    public static final String CONFIG_NAME = "sanitizer";

    // the default encoder value for the old version of config file.
    public static final String DEFAULT_ENCODER = "javascript-source";
    public static final String BODY_ENCODER = "bodyEncoder";
    public static final String HEADER_ENCODER = "headerEncoder";
    public static final String BODY_ATTRIBUTES_TO_ENCODE = "bodyAttributesToEncode";
    public static final String BODY_ATTRIBUTES_TO_IGNORE = "bodyAttributesToIgnore";
    public static final String HEADER_ATTRIBUTES_TO_ENCODE = "headerAttributesToEncode";
    public static final String HEADER_ATTRIBUTES_TO_IGNORE = "headerAttributesToIgnore";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            description = "indicate if sanitizer is enabled or not"
    )
    private boolean enabled;

    @BooleanField(
            configFieldName = BODY_ENABLED,
            externalizedKeyName = BODY_ENABLED,
            externalized = true,
            defaultValue = "true",
            description = "if it is enabled, the body needs to be sanitized"
    )
    private boolean bodyEnabled;

    @StringField(
            configFieldName = BODY_ENCODER,
            externalizedKeyName = BODY_ENCODER,
            externalized = true,
            defaultValue = "javascript-source",
            description = "the encoder for the body. javascript, javascript-attribute, javascript-block or javascript-source\n" +
                    "There are other encoders that you can choose depending on your requirement. Please refer to site\n" +
                    "https://github.com/OWASP/owasp-java-encoder/blob/main/core/src/main/java/org/owasp/encoder/Encoders.java"
    )
    private String bodyEncoder;

    @ArrayField(
            configFieldName = BODY_ATTRIBUTES_TO_ENCODE,
            externalizedKeyName = BODY_ATTRIBUTES_TO_ENCODE,
            externalized = true,
            description = "pick up a list of keys to encode the values to limit the scope to only selected keys. You can\n" +
                    "choose this option if you want to only encode certain fields in the body. When this option is\n" +
                    "selected, you can not use the bodyAttributesToIgnore list.",
            items = String.class
    )
    private List<String> bodyAttributesToEncode;

    @ArrayField(
            configFieldName = BODY_ATTRIBUTES_TO_IGNORE,
            externalizedKeyName = BODY_ATTRIBUTES_TO_IGNORE,
            externalized = true,
            description = "pick up a list of keys to ignore the values encoding to skip some of the values so that these\n" +
                    "values won't be encoded. You can choose this option if you want to encode everything except\n" +
                    "several values with a list of the keys. When this option is selected, you can not use the\n" +
                    "bodyAttributesToEncode list.",
            items = String.class
    )
    private List<String> bodyAttributesToIgnore;

    @BooleanField(
            configFieldName = HEADER_ENABLED,
            externalizedKeyName = HEADER_ENABLED,
            externalized = true,
            defaultValue = "true",
            description = "if it is enabled, the header needs to be sanitized"
    )
    private boolean headerEnabled;

    @BooleanField(
            configFieldName = HEADER_ENCODER,
            externalizedKeyName =  HEADER_ENCODER,
            externalized = true,
            defaultValue = "true",
            description = "the encoder for the header. javascript, javascript-attribute, javascript-block or javascript-source\n" +
                    "There are other encoders that you can choose depending on your requirement. Please refer to site\n" +
                    "https://github.com/OWASP/owasp-java-encoder/blob/main/core/src/main/java/org/owasp/encoder/Encoders.java"
    )
    private String headerEncoder;

    @ArrayField(
            configFieldName = HEADER_ATTRIBUTES_TO_ENCODE,
            externalizedKeyName = HEADER_ATTRIBUTES_TO_ENCODE,
            externalized = true,
            description = "pick up a list of keys to encode the values to limit the scope to only selected keys. You can\n" +
                    "choose this option if you want to only encode certain values in the headers. When this option is\n" +
                    "selected, you can not use the headerAttributesToIgnore list.",
            items = String.class
    )
    private List<String> headerAttributesToEncode;

    @ArrayField(
            configFieldName = HEADER_ATTRIBUTES_TO_IGNORE,
            externalizedKeyName = HEADER_ATTRIBUTES_TO_IGNORE,
            externalized = true,
            description = "pick up a list of keys to ignore the values encoding to skip some of the values so that these\n" +
                    "values won't be encoded. You can choose this option if you want to encode everything except\n" +
                    "several values with a list of the keys. When this option is selected, you can not use the\n" +
                    "headerAttributesToEncode list.",
            items = String.class
    )
    private List<String> headerAttributesToIgnore;


    private final Map<String, Object> mappedConfig;

    private SanitizerConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigList();
        setConfigData();
    }

    public static SanitizerConfig load() {
        return new SanitizerConfig(CONFIG_NAME);
    }

    @Deprecated
    public static SanitizerConfig load(String configName) {
        return new SanitizerConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBodyEnabled() {
        return bodyEnabled;
    }

    public void setBodyEnabled(boolean bodyEnabled) {
        this.bodyEnabled = bodyEnabled;
    }

    public String getBodyEncoder() {
        return bodyEncoder;
    }

    public void setBodyEncoder(String bodyEncoder) {
        this.bodyEncoder = bodyEncoder;
    }

    public List<String> getBodyAttributesToIgnore() {
        return bodyAttributesToIgnore;
    }

    public void setBodyAttributesToIgnore(List<String> bodyAttributesToIgnore) {
        this.bodyAttributesToIgnore = bodyAttributesToIgnore;
    }

    public List<String> getBodyAttributesToEncode() {
        return bodyAttributesToEncode;
    }

    public void setBodyAttributesToEncode(List<String> bodyAttributesToEncode) {
        this.bodyAttributesToEncode = bodyAttributesToEncode;
    }

    public boolean isHeaderEnabled() {
        return headerEnabled;
    }

    public void setHeaderEnabled(boolean headerEnabled) {
        this.headerEnabled = headerEnabled;
    }

    public String getHeaderEncoder() {
        return headerEncoder;
    }

    public void setHeaderEncoder(String headerEncoder) {
        this.headerEncoder = headerEncoder;
    }

    public List<String> getHeaderAttributesToIgnore() {
        return headerAttributesToIgnore;
    }

    public void setHeaderAttributesToIgnore(List<String> headerAttributesToIgnore) {
        this.headerAttributesToIgnore = headerAttributesToIgnore;
    }

    public List<String> getHeaderAttributesToEncode() {
        return headerAttributesToEncode;
    }

    public void setHeaderAttributesToEncode(List<String> headerAttributesToEncode) {
        this.headerAttributesToEncode = headerAttributesToEncode;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null) {
            if(object instanceof String) {
                enabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                enabled = (Boolean) object;
            } else {
                throw new ConfigException("enabled must be a boolean value.");
            }
        }

        object = mappedConfig.get(BODY_ENABLED);
        if(object != null) {
            if(object instanceof String) {
                bodyEnabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                bodyEnabled = (Boolean) object;
            } else {
                object = mappedConfig.get("sanitizeBody");
                if(object != null) {
                    if(object instanceof String) {
                        bodyEnabled = Boolean.parseBoolean((String)object);
                    } else if (object instanceof Boolean) {
                        bodyEnabled = (Boolean) object;
                    } else {
                        throw new ConfigException("sanitizeBody must be a boolean value.");
                    }
                }
            }
        }
        object = mappedConfig.get(HEADER_ENABLED);
        if(object != null) {
            if(object instanceof String) {
                headerEnabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                headerEnabled = (Boolean) object;
            } else {
                object = mappedConfig.get("sanitizeHeader");
                if(object != null) {
                    if(object instanceof String) {
                        headerEnabled = Boolean.parseBoolean((String)object);
                    } else if (object instanceof Boolean) {
                        headerEnabled = (Boolean) object;
                    } else {
                        throw new ConfigException("sanitizeHeader must be a boolean value.");
                    }
                }
            }
        }
        object = mappedConfig.get(BODY_ENCODER);
        if(object != null ) {
            bodyEncoder = (String)object;
        } else {
            bodyEncoder = DEFAULT_ENCODER;
        }

        object = mappedConfig.get(HEADER_ENCODER);
        if(object != null ) {
            headerEncoder = (String)object;
        } else {
            headerEncoder = DEFAULT_ENCODER;
        }

    }

    private void setConfigList() {

        if(mappedConfig.get(BODY_ATTRIBUTES_TO_ENCODE) != null) {
            Object object = mappedConfig.get(BODY_ATTRIBUTES_TO_ENCODE);
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("bodyAttributesToEncode = " + s);
                if(s.startsWith("[")) {
                    // this is a JSON string, and we need to parse it.
                    try {
                        bodyAttributesToEncode = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the bodyAttributesToEncode json with a list of strings.");
                    }
                } else {
                    // this is a comma separated string.
                    bodyAttributesToEncode = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                bodyAttributesToEncode = (List<String>)object;
            } else {
                throw new ConfigException("bodyAttributesToEncode list is missing or wrong type.");
            }
        }

        if(mappedConfig.get(BODY_ATTRIBUTES_TO_IGNORE) != null) {
            Object object = mappedConfig.get(BODY_ATTRIBUTES_TO_IGNORE);
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("bodyAttributesToIgnore = " + s);
                if(s.startsWith("[")) {
                    // this is a JSON string, and we need to parse it.
                    try {
                        bodyAttributesToIgnore = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the bodyAttributesToIgnore json with a list of strings.");
                    }
                } else {
                    // this is a comma separated string.
                    bodyAttributesToIgnore = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                bodyAttributesToIgnore = (List<String>)object;
            } else {
                throw new ConfigException("bodyAttributesToIgnore list is missing or wrong type.");
            }
        }

        if(mappedConfig.get(HEADER_ATTRIBUTES_TO_ENCODE) != null) {
            Object object = mappedConfig.get(HEADER_ATTRIBUTES_TO_ENCODE);
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("headerAttributesToEncode = " + s);
                if(s.startsWith("[")) {
                    // this is a JSON string, and we need to parse it.
                    try {
                        headerAttributesToEncode = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the headerAttributesToEncode json with a list of strings.");
                    }
                } else {
                    // this is a comma separated string.
                    headerAttributesToEncode = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                headerAttributesToEncode = (List<String>)object;
            } else {
                throw new ConfigException("headerAttributesToEncode list is missing or wrong type.");
            }
        }

        if(mappedConfig.get(HEADER_ATTRIBUTES_TO_IGNORE) != null) {
            Object object = mappedConfig.get(HEADER_ATTRIBUTES_TO_IGNORE);
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("headerAttributesToIgnore = " + s);
                if(s.startsWith("[")) {
                    // this is a JSON string, and we need to parse it.
                    try {
                        headerAttributesToIgnore = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the headerAttributesToIgnore json with a list of strings.");
                    }
                } else {
                    // this is a comma separated string.
                    headerAttributesToIgnore = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                headerAttributesToIgnore = (List<String>)object;
            } else {
                throw new ConfigException("headerAttributesToIgnore list is missing or wrong type.");
            }
        }

    }

}
