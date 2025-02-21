package org.apereo.cas.multitenancy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serial;
import java.util.Set;

/**
 * This is {@link TenantCasAuthenticationProtocolPolicy}.
 *
 * @author Misagh Moayyed
 * @since 7.2.0
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class TenantCasAuthenticationProtocolPolicy implements TenantAuthenticationProtocolPolicy {
    @Serial
    private static final long serialVersionUID = -9012299259747093234L;

    private Set<String> supportedProtocols;
}
