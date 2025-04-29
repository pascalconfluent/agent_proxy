package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.agent.common.utils.UriUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Setter
@Getter
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
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceRegistration extends Registration {
    private String mimeType;
    private String url;

    // TODO: Should not require to remove the leading slash
    public void setUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url cannot be blank");
        }

        this.url = url.startsWith("/") ? url.substring(1) : url;
    }

    @JsonIgnore
    public boolean isTemplate() {
        return UriUtils.isTemplate(url);
    }

    public ResourceRegistration(String name,
                                String description,
                                String requestTopicName,
                                String responseTopicName,
                                String mimeType,
                                String url) {
        super(RESOURCE, name, description, requestTopicName, responseTopicName, "N/A");
        this.mimeType = mimeType;
        setUrl(url);
    }
}

