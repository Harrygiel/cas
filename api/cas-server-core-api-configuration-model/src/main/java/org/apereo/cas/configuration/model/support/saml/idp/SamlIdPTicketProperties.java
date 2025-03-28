package org.apereo.cas.configuration.model.support.saml.idp;

import org.apereo.cas.configuration.support.RequiresModule;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serial;
import java.io.Serializable;

/**
 * This is {@link SamlIdPTicketProperties}.
 *
 * @author Samuel Lyons
 * @since 6.0.0
 */
@RequiresModule(name = "cas-server-support-saml-idp")
@Getter
@Setter
@Accessors(chain = true)
public class SamlIdPTicketProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 6837089259390742073L;

    /**
     * name that should be given to the saml artifact cache storage name.
     */
    private String samlArtifactsCacheStorageName = "samlArtifactsCache";

    /**
     * The name that should be given to the saml attribute query cache storage name.
     */
    private String samlAttributeQueryCacheStorageName = "samlAttributeQueryCache";

    /**
     * Attribute query ticket properties.
     */
    @NestedConfigurationProperty
    private AttributeQueryTicketProperties attributeQuery = new AttributeQueryTicketProperties();
}
