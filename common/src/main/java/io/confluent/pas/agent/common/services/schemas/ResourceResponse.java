package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(value = """
            {
               "properties":{
                  "type":{
                     "connect.index":0,
                     "enum":[
                        "text",
                        "blob"
                     ],
                     "default": "text",
                     "type":"string"
                  },
                  "uri":{
                     "connect.index":1,
                     "type":"string"
                  },
                  "mimeType":{
                     "connect.index":2,
                     "type":"string"
                  },
                  "text":{
                     "connect.index":3,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "blob":{
                     "connect.index":4,
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
                  "type",
                  "uri",
                  "mimeType"
               ],
               "title":"Record",
               "type":"object"
            }""", refs = {})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = TextResourceResponse.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextResourceResponse.class, name = "text"),
        @JsonSubTypes.Type(value = BlobResourceResponse.class, name = "blob")})
public class ResourceResponse {
    public enum ResponseType {
        TEXT("text"),
        BLOB("blob");

        private final String value;

        ResponseType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

        @JsonCreator
        public static ResourceResponse.ResponseType fromValue(String value) {
            for (ResourceResponse.ResponseType e : ResourceResponse.ResponseType.values()) {
                if (e.value.equals(value)) {
                    return e;
                }
            }

            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @JsonProperty(value = "type", required = true)
    private ResponseType type;
    @JsonProperty(value = "uri", required = true)
    private String uri;
    @JsonProperty(value = "mimeType", required = true)
    private String mimeType;
}