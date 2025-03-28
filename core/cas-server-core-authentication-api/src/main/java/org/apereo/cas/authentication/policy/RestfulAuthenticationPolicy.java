package org.apereo.cas.authentication.policy;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationPolicyExecutionResult;
import org.apereo.cas.authentication.exceptions.AccountDisabledException;
import org.apereo.cas.authentication.exceptions.AccountPasswordMustChangeException;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.model.core.authentication.RestAuthenticationPolicyProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.http.HttpExecutionRequest;
import org.apereo.cas.util.http.HttpUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import java.io.Serial;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;

/**
 * This is {@link RestfulAuthenticationPolicy}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@NoArgsConstructor(force = true)
@EqualsAndHashCode(callSuper = true, exclude = "properties")
@Setter
@Getter
@AllArgsConstructor
@Accessors(chain = true)
public class RestfulAuthenticationPolicy extends BaseAuthenticationPolicy {
    @Serial
    private static final long serialVersionUID = -7688729533538097898L;

    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(false).build().toObjectMapper();

    private final RestAuthenticationPolicyProperties properties;

    private static Exception handleResponseStatusCode(final HttpStatus statusCode, final Principal principal) {
        if (statusCode == HttpStatus.FORBIDDEN || statusCode == HttpStatus.METHOD_NOT_ALLOWED) {
            return new AccountDisabledException("Could not authenticate forbidden account for " + principal.getId());
        }
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            return new FailedLoginException("Could not authenticate account for " + principal.getId());
        }
        if (statusCode == HttpStatus.NOT_FOUND) {
            return new AccountNotFoundException("Could not locate account for " + principal.getId());
        }
        if (statusCode == HttpStatus.LOCKED) {
            return new AccountLockedException("Could not authenticate locked account for " + principal.getId());
        }
        if (statusCode == HttpStatus.PRECONDITION_FAILED) {
            return new AccountExpiredException("Could not authenticate expired account for " + principal.getId());
        }
        if (statusCode == HttpStatus.PRECONDITION_REQUIRED) {
            return new AccountPasswordMustChangeException("Account password must change for " + principal.getId());
        }
        return new FailedLoginException("Rest endpoint returned an unknown status code " + statusCode);
    }

    @Override
    public AuthenticationPolicyExecutionResult isSatisfiedBy(final Authentication authentication,
                                                             final Set<AuthenticationHandler> authenticationHandlers,
                                                             final ConfigurableApplicationContext applicationContext,
                                                             final Map<String, ? extends Serializable> context) throws Exception {
        HttpResponse response = null;
        val principal = authentication.getPrincipal();
        try {
            val entity = MAPPER.writeValueAsString(principal);
            val headers = CollectionUtils.<String, String>wrap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            headers.putAll(properties.getHeaders());
            val exec = HttpExecutionRequest.builder()
                .url(properties.getUrl())
                .basicAuthPassword(properties.getBasicAuthUsername())
                .basicAuthUsername(properties.getBasicAuthPassword())
                .method(HttpMethod.POST)
                .entity(entity)
                .headers(headers)
                .build();
            response = HttpUtils.execute(exec);
            val statusCode = HttpStatus.valueOf(response.getCode());
            if (statusCode != HttpStatus.OK) {
                val ex = handleResponseStatusCode(statusCode, principal);
                throw new GeneralSecurityException(ex);
            }
            return AuthenticationPolicyExecutionResult.success();
        } finally {
            HttpUtils.close(response);
        }
    }
}
