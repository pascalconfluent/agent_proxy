package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(value = """
            {
               "properties":{
                  "uri":{
                     "connect.index":0,
                     "type":"string"
                  }
               },
               "required":[
                  "uri"
               ],
               "title":"Record",
               "type":"object"
            }""", refs = {})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceRequest {
    @JsonProperty(value = "uri", required = true)
    private String uri;
}