package org.example.atuo_attend_backend.prototype.penpot;

import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Penpot RPC 客户端单元测试：验证 auto 模式下 404 回退、多基址与网络失败重试（不依赖真实 Penpot 进程）。
 */
@ExtendWith(MockitoExtension.class)
class PenpotRpcClientTest {

    @Mock
    private RestTemplate restTemplate;

    private PenpotProperties props;

    @BeforeEach
    void setUp() {
        props = new PenpotProperties();
        props.setEnabled(true);
        props.setRpcPathStyle("auto");
        props.setBaseUrl("http://primary:8080");
        props.setApiBaseUrl("");
        props.setBackendDirectUri("http://direct:6060");
        props.setClientHeader("penpot-backend");
    }

    @Test
    void commandNoAuth_fallbackFromNewPathToLegacyOnSameBase() {
        PenpotRpcClient client = new PenpotRpcClient(props, restTemplate);
        String newUrl = "http://primary:8080/api/main/methods/prepare-register-profile";
        String legacyUrl = "http://primary:8080/api/rpc/command/prepare-register-profile";
        when(restTemplate.exchange(eq(newUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", null, null, StandardCharsets.UTF_8));
        when(restTemplate.exchange(eq(legacyUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"token\":\"tok1\"}", HttpStatus.OK));

        var node = client.commandNoAuth("prepare-register-profile", samplePrepareBody());
        assertEquals("tok1", node.get("token").asText());
    }

    @Test
    void commandNoAuth_triesSecondBaseWhenFirstBaseReturns404OnBothPaths() {
        PenpotRpcClient client = new PenpotRpcClient(props, restTemplate);
        String pNew = "http://primary:8080/api/main/methods/prepare-register-profile";
        String pLeg = "http://primary:8080/api/rpc/command/prepare-register-profile";
        String dNew = "http://direct:6060/api/main/methods/prepare-register-profile";
        when(restTemplate.exchange(eq(pNew), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", null, null, StandardCharsets.UTF_8));
        when(restTemplate.exchange(eq(pLeg), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", null, null, StandardCharsets.UTF_8));
        when(restTemplate.exchange(eq(dNew), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"token\":\"from-direct\"}", HttpStatus.OK));

        var node = client.commandNoAuth("prepare-register-profile", samplePrepareBody());
        assertEquals("from-direct", node.get("token").asText());
    }

    @Test
    void commandNoAuth_primaryConnectionFailure_thenSucceedsOnDirectBase() {
        PenpotRpcClient client = new PenpotRpcClient(props, restTemplate);
        String pNew = "http://primary:8080/api/main/methods/prepare-register-profile";
        String pLeg = "http://primary:8080/api/rpc/command/prepare-register-profile";
        String dNew = "http://direct:6060/api/main/methods/prepare-register-profile";
        when(restTemplate.exchange(eq(pNew), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        when(restTemplate.exchange(eq(pLeg), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
        when(restTemplate.exchange(eq(dNew), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"token\":\"net-ok\"}", HttpStatus.OK));

        var node = client.commandNoAuth("prepare-register-profile", samplePrepareBody());
        assertEquals("net-ok", node.get("token").asText());
    }

    @Test
    void loginFetchAuthCookie_triesDirectBaseAfterPrimaryFails() {
        PenpotRpcClient client = new PenpotRpcClient(props, restTemplate);
        AtomicInteger n = new AtomicInteger();
        when(restTemplate.exchange(
                ArgumentMatchers.contains("login-with-password"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenAnswer(inv -> {
                    int i = n.incrementAndGet();
                    if (i <= 2) {
                        throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", null, null, StandardCharsets.UTF_8);
                    }
                    return ResponseEntity.ok()
                            .header("Set-Cookie", "auth-token=abc123; Path=/; HttpOnly")
                            .body("{}");
                });

        String cookie = client.loginFetchAuthCookie("u@test.local", "secret");
        assertEquals("auth-token=abc123", cookie);
        verify(restTemplate, atLeastOnce()).exchange(
                ArgumentMatchers.contains("login-with-password"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void commandNoAuth_allPaths404_throwsWithHint() {
        props.setBackendDirectUri("");
        PenpotRpcClient client = new PenpotRpcClient(props, restTemplate);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", null, null, StandardCharsets.UTF_8));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.commandNoAuth("prepare-register-profile", samplePrepareBody()));
        assertTrue(ex.getMessage().contains("HTTP 404"));
        assertTrue(ex.getMessage().contains("PENPOT_INTERNAL_URI") || ex.getMessage().contains("penpot"));
    }

    private static Map<String, Object> samplePrepareBody() {
        Map<String, Object> m = new HashMap<>();
        m.put("fullname", "T");
        m.put("email", "e@test.local");
        m.put("password", "p");
        m.put("create-welcome-file", false);
        m.put("accept-newsletter-updates", false);
        return m;
    }
}
