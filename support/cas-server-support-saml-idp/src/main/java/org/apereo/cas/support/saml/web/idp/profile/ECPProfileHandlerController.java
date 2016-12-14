package org.apereo.cas.support.saml.web.idp.profile;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.apache.commons.lang3.tuple.Pair;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationResult;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.SamlIdPConstants;
import org.apereo.cas.support.saml.SamlIdPUtils;
import org.apereo.cas.support.saml.SamlUtils;
import org.apereo.cas.support.saml.services.idp.metadata.cache.SamlRegisteredServiceCachingMetadataResolver;
import org.apereo.cas.support.saml.web.idp.profile.builders.SamlProfileObjectBuilder;
import org.apereo.cas.support.saml.web.idp.profile.builders.response.BaseSamlProfileSamlResponseBuilder;
import org.apereo.cas.support.saml.web.idp.profile.builders.enc.SamlObjectSigner;
import org.apereo.cas.util.DateTimeUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.AssertionImpl;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.impl.SAMLSOAPDecoderBodyHandler;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPSOAP11Decoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.soap.messaging.context.SOAP11Context;
import org.opensaml.soap.soap11.Envelope;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.BasicAuthExtractor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * This is {@link ECPProfileHandlerController}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
public class ECPProfileHandlerController extends AbstractSamlProfileHandlerController {
    /**
     * Instantiates a new ecp saml profile handler controller.
     *
     * @param samlObjectSigner                             the saml object signer
     * @param parserPool                                   the parser pool
     * @param servicesManager                              the services manager
     * @param webApplicationServiceFactory                 the web application service factory
     * @param samlRegisteredServiceCachingMetadataResolver the saml registered service caching metadata resolver
     * @param configBean                                   the config bean
     * @param responseBuilder                              the response builder
     * @param authenticationContextClassMappings           the authentication context class mappings
     * @param serverPrefix                                 the server prefix
     * @param serverName                                   the server name
     * @param authenticationContextRequestParameter        the authentication context request parameter
     * @param loginUrl                                     the login url
     * @param logoutUrl                                    the logout url
     * @param forceSignedLogoutRequests                    the force signed logout requests
     * @param singleLogoutCallbacksDisabled                the single logout callbacks disabled
     */
    public ECPProfileHandlerController(final SamlObjectSigner samlObjectSigner,
                                       final ParserPool parserPool,
                                       final AuthenticationSystemSupport authenticationSystemSupport,
                                       final ServicesManager servicesManager,
                                       final ServiceFactory<WebApplicationService> webApplicationServiceFactory,
                                       final SamlRegisteredServiceCachingMetadataResolver samlRegisteredServiceCachingMetadataResolver,
                                       final OpenSamlConfigBean configBean,
                                       final SamlProfileObjectBuilder<org.opensaml.saml.saml2.ecp.Response> responseBuilder,
                                       final Map<String, String> authenticationContextClassMappings,
                                       final String serverPrefix,
                                       final String serverName,
                                       final String authenticationContextRequestParameter,
                                       final String loginUrl,
                                       final String logoutUrl,
                                       final boolean forceSignedLogoutRequests,
                                       final boolean singleLogoutCallbacksDisabled) {
        super(samlObjectSigner, parserPool, authenticationSystemSupport,
                servicesManager, webApplicationServiceFactory,
                samlRegisteredServiceCachingMetadataResolver,
                configBean, responseBuilder, authenticationContextClassMappings,
                serverPrefix, serverName,
                authenticationContextRequestParameter, loginUrl, logoutUrl,
                forceSignedLogoutRequests, singleLogoutCallbacksDisabled);
    }

    /**
     * Handle ecp request.
     *
     * @param response the response
     * @param request  the request
     * @throws Exception the exception
     */
    @PostMapping(path = SamlIdPConstants.ENDPOINT_SAML2_IDP_ECP_PROFILE_SSO, consumes = MediaType.TEXT_XML_VALUE)
    public void handleEcpRequest(final HttpServletResponse response,
                                 final HttpServletRequest request) throws Exception {
        final MessageContext soapContext = decodeSoapRequest(request);
        final Credential credential = extractBasicAuthenticationCredential(request, response);

        if (credential == null) {
            logger.error("Credentials could not be extracted from the SAML ECP request");
            return;
        }
        if (soapContext == null) {
            logger.error("SAML ECP request could not be determined from the authentication request");
            return;
        }
        final Envelope envelope = soapContext.getSubcontext(SOAP11Context.class).getEnvelope();
        SamlUtils.logSamlObject(configBean, envelope);

        final AuthnRequest authnRequest = (AuthnRequest) soapContext.getMessage();
        final Pair<AuthnRequest, MessageContext> authenticationContext = Pair.of(authnRequest, soapContext);
        verifySamlAuthenticationRequest(authenticationContext, request);
        final Authentication authentication = authenticateEcpRequest(credential, authenticationContext);
        buildSamlResponse(response, request, authenticationContext, buildEcpCasAssertion(authentication));
    }

    /**
     * Authenticate ecp request.
     *
     * @param credential   the credential
     * @param authnRequest the authn request
     * @return the authentication
     */
    protected Authentication authenticateEcpRequest(final Credential credential, final Pair<AuthnRequest, MessageContext> authnRequest) {
        final Service service = webApplicationServiceFactory.createService(SamlIdPUtils.getIssuerFromSamlRequest(authnRequest.getKey()));
        final AuthenticationResult authenticationResult =
                authenticationSystemSupport.handleAndFinalizeSingleAuthenticationTransaction(service, credential);
        return authenticationResult.getAuthentication();
    }

    /**
     * Build ecp cas assertion assertion.
     *
     * @param authentication the authentication
     * @return the assertion
     */
    protected Assertion buildEcpCasAssertion(final Authentication authentication) {
        final AttributePrincipal principal = new AttributePrincipalImpl(authentication.getPrincipal().getId(),
                authentication.getPrincipal().getAttributes());
        return new AssertionImpl(principal, DateTimeUtils.dateOf(authentication.getAuthenticationDate()),
                null, DateTimeUtils.dateOf(authentication.getAuthenticationDate()),
                authentication.getAttributes());
    }

    /**
     * Decode soap 11 context.
     *
     * @param request the request
     * @return the soap 11 context
     * @throws Exception the exception
     */
    protected MessageContext decodeSoapRequest(final HttpServletRequest request) {
        try {
            final HTTPSOAP11Decoder decoder = new HTTPSOAP11Decoder();
            decoder.setParserPool(parserPool);
            decoder.setHttpServletRequest(request);
            decoder.setBindingDescriptor(new BindingDescriptor());
            decoder.setBodyHandler(new SAMLSOAPDecoderBodyHandler());
            decoder.initialize();
            decoder.decode();
            return decoder.getMessageContext();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private Credential extractBasicAuthenticationCredential(final HttpServletRequest request,
                                                            final HttpServletResponse response) {
        try {
            final BasicAuthExtractor extractor = new BasicAuthExtractor(this.getClass().getSimpleName());
            final WebContext webContext = new J2EContext(request, response);
            final UsernamePasswordCredentials credentials = extractor.extract(webContext);
            if (credentials != null) {
                logger.debug("Received basic authentication ECP request from credentials {} ", credentials);
                return new UsernamePasswordCredential(credentials.getUsername(), credentials.getPassword());
            }
        } catch (final Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }
}
