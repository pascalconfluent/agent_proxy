package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
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
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlobResourceResponse extends ResourceResponse {
    @JsonProperty(value = "blob", required = true)
    private String blob;

    public BlobResourceResponse(String uri, String mimeType, String blob) {
        super(ResponseType.BLOB, uri, mimeType);
        this.blob = blob;
    }
}