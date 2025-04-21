package io.confluent.pas.agent.exemple;

/**
 * Interface for an assistant that can chat with a user.
 */
public interface Assistant {
    String chat(String userMessage);
}
