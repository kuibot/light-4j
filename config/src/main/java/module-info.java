open module com.networknt.config {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires encoder;
    requires slf4j.api;
    requires snakeyaml;

    exports com.networknt.config;
}