package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * A key for a registration.
 */
@Schema(value = """
            {
               "properties": {
                 "name": {
                   "connect.index": 0,
                   "type": "string"
                 }
               },
               "required": [
                 "name"
               ],
               "title": "Record",
               "type": "object"
             }
            """,
        refs = {})
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@AllArgsConstructor()
@NoArgsConstructor()
public class RegistrationKey implements Comparable<RegistrationKey> {

    @JsonProperty(value = "name", required = true)
    private String name;

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof RegistrationKey key && StringUtils.equals(name, key.getName());
    }

    @Override
    public int compareTo(@NotNull RegistrationKey o) {
        return StringUtils.compare(name, o.getName());
    }
}