package io.confluent.pas.agent.proxy.registration.models;

import io.confluent.pas.agent.proxy.frameworks.java.models.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkItem {

    private Request request;
    private Response response;

}
