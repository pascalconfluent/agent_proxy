package io.confluent.pas.agent.proxy.registration.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkItems {

    private String correlationId;
    private List<WorkItem> workItems = new ArrayList<>();

}
