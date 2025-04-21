package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Schema(value = """
            {
               "properties":{
                  "registrationType":{
                     "connect.index":5,
                     "type":"string"
                  },
                  "correlationIdFieldName":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "description":{
                     "connect.index":1,
                     "type":"string"
                  },
                  "name":{
                     "connect.index":0,
                     "type":"string"
                  },
                  "requestTopicName":{
                     "connect.index":2,
                      "type":"string"
                  },
                  "responseTopicName":{
                     "connect.index":3,
                     "type":"string"
                  },
                  "mimeType":{
                     "connect.index":6,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "url":{
                     "connect.index":7,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  }
               },
               "required":[
                  "name",
                  "description",
                  "registrationType",
                  "requestTopicName",
                  "responseTopicName"
               ],
               "additionalProperties":false,
               "title":"Record",
               "type":"object"
            }""", refs = {})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "registrationType", defaultImpl = Registration.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourceRegistration.class, name = Registration.RESOURCE),
        @JsonSubTypes.Type(value = Registration.class, name = Registration.TOOL)})
@Builder
public class Registration {
    public final static String TOOL = "tool";
    public final static String RESOURCE = "resource";
    public final static String CORRELATION_ID_FIELD_NAME = "correlationId";

    @JsonProperty(value = "registrationType", required = true, defaultValue = TOOL)
    private String registrationType;
    @JsonProperty(value = "name", required = true)
    private String name;
    @JsonProperty(value = "description", required = true)
    private String description;
    @JsonProperty(value = "requestTopicName", required = true)
    private String requestTopicName;
    @JsonProperty(value = "responseTopicName", required = true)
    private String responseTopicName;
    @JsonProperty(value = "correlationIdFieldName", defaultValue = CORRELATION_ID_FIELD_NAME)
    private String correlationIdFieldName;

    public Registration(String name, String description, String requestTopicName, String responseTopicName) {
        this(TOOL, name, description, requestTopicName, responseTopicName, CORRELATION_ID_FIELD_NAME);
    }

    public Registration(String name, String description, String requestTopicName, String responseTopicName, String correlationIdFieldName) {
        this(TOOL, name, description, requestTopicName, responseTopicName, correlationIdFieldName);
    }

    @JsonIgnore
    public boolean isResource() {
        return StringUtils.equals(registrationType, RESOURCE);
    }
}