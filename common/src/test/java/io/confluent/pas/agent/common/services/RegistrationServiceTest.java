package io.confluent.pas.agent.common.services;

import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.RegistrationKey;
import io.kcache.KafkaCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RegistrationServiceTest {

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private RegistrationServiceHandler.Handler<RegistrationKey, Registration> handler;

    @Mock
    private KafkaCache<RegistrationKey, Registration> registrationCache;

    private RegistrationService<RegistrationKey, Registration> registrationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        registrationService = new RegistrationService<>(registrationCache);
    }

    @Test
    public void testGetAllRegistrations() {
        List<Registration> registrations = List.of(new Registration());
        when(registrationCache.values()).thenReturn(registrations);

        List<Registration> result = registrationService.getAllRegistrations();

        assertEquals(registrations, result);
    }

    @Test
    public void testIsRegistered() {
        RegistrationKey key = new RegistrationKey();
        when(registrationCache.get(key)).thenReturn(new Registration());

        boolean result = registrationService.isRegistered(key);

        assertTrue(result);
    }

    @Test
    public void testRegister() {
        RegistrationKey key = new RegistrationKey();
        Registration registration = new Registration();

        registrationService.register(key, registration);

        verify(registrationCache, times(1)).put(key, registration);
    }

    @Test
    public void testUnregister() {
        RegistrationKey key = new RegistrationKey();

        registrationService.unregister(key);

        verify(registrationCache, times(1)).remove(key);
    }

    @Test
    public void testClose() throws IOException {
        registrationService.close();

        verify(registrationCache, times(1)).close();
    }
}