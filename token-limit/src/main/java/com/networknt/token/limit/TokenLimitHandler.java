package com.networknt.token.limit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.cache.CacheManager;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.http.CachedResponseEntity;
import com.networknt.http.ResponseEntity;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.CacheTask;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.server.handlers.ResponseCodeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This handler should be used on the oauth-kafka or a dedicated light-gateway instance for all OAuth 2.0
 * instances or providers.
 *
 * @author Steve Hu
 */
public class TokenLimitHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(TokenLimitHandler.class);
    // the cacheName in the cache.yml
    static final String TOKEN_LIMIT = "token-limit";
    static final String CLIENT_TOKEN = "client-token";
    static final String GRANT_TYPE = "grant_type";
    static final String CLIENT_CREDENTIALS = "client_credentials";
    static final String AUTHORIZATION_CODE = "authorization_code";
    static final String CLIENT_ID = "client_id";
    static final String CLIENT_SECRET = "client_secret";
    static final String SCOPE = "scope";
    static final String CODE = "code";
    static final String TOKEN_LIMIT_ERROR = "ERR10091";
    static final String BASIC_PREFIX = "BASIC";


    private volatile HttpHandler next;
    private final TokenLimitConfig config;
    private List<Pattern> patterns;

    CacheManager cacheManager = CacheManager.getInstance();

    public TokenLimitHandler() throws Exception{
        config = TokenLimitConfig.load();
        List<String> tokenPathTemplates = config.getTokenPathTemplates();
        if(tokenPathTemplates != null && !tokenPathTemplates.isEmpty()) {
            patterns = tokenPathTemplates.stream().map(Pattern::compile).collect(Collectors.toList());
        }
        logger.info("TokenLimitHandler constructed.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg token limit config
     * @throws Exception thrown when config is wrong.
     *
     */
    @Deprecated
    public TokenLimitHandler(TokenLimitConfig cfg) throws Exception{
        config = cfg;
        List<String> tokenPathTemplates = config.getTokenPathTemplates();
        if(tokenPathTemplates != null && !tokenPathTemplates.isEmpty()) {
            patterns = tokenPathTemplates.stream().map(Pattern::compile).collect(Collectors.toList());
        }
        logger.info("TokenLimitHandler constructed.");
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TokenLimitConfig.CONFIG_NAME, TokenLimitHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(TokenLimitConfig.CONFIG_NAME), null);

    }

    @Override
    public void reload() {
        config.reload();
        // after reload, we need to update the config in the module registry to ensure that server info returns the latest configuration.
        ModuleRegistry.registerModule(TokenLimitConfig.CONFIG_NAME, TokenLimitHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(TokenLimitConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) logger.info("TokenLimitHandler is reloaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("TokenLimitHandler.handleRequest starts.");
        String key = null;
        String grantType = null;
        String clientId = null;
        String clientSecret = null;
        String scope = null;
        // Check if x-forwarded-for exists, if not, get the client ip address from source address with port removed as the port is dynamic.
        String clientIpAddress = exchange.getRequestHeaders().contains(Headers.X_FORWARDED_FOR) ? exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_FOR) : exchange.getSourceAddress().getAddress().getHostAddress();
        if(logger.isTraceEnabled()) logger.trace("client address {}", clientIpAddress);

        // firstly, we need to identify if the request path ends with /token. If not, call next handler.
        String requestPath = exchange.getRequestPath();
        if(matchPath(requestPath) && cacheManager != null) {
            if(logger.isTraceEnabled()) logger.trace("request path {} matches with one of the {} patterns.", requestPath, config.getTokenPathTemplates().size());
            // this assumes that either BodyHandler(oauth-kafka) or RequestBodyInterceptor(light-gateway) is used in the chain.
            String requestBodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
            if (logger.isTraceEnabled()) logger.trace("requestBodyString = {}", requestBodyString);
            // grant_type=client_credentials&client_id=0oa6wgbkbcF27GoqA1d7&client_secret=GInDco_MGt6Fz0oHxmgk1LluEHb6qJ4RQY0MvH3Q&scope=lg.localoauth.corp
            // convert the string to a hashmap via a converter. We need to consider both x-www-urlencoded and json request body.
            // grant_type and scope will always be sent on body
            Map<String, String> bodyMap = convertStringToHashMap(requestBodyString);
            grantType = bodyMap.get(GRANT_TYPE);
            scope = (bodyMap.get(SCOPE) != null) ? bodyMap.get(SCOPE).replace(" ", "") : "";

            // For clientID and secrets lets check auth headers first
            String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (auth == null || auth.trim().length() == 0) {
                if(logger.isTraceEnabled()) logger.trace("No authorization header found. Will obtain credentials from body.");
                clientId = bodyMap.get(CLIENT_ID);
                clientSecret = bodyMap.get(CLIENT_SECRET);
            } else if (BASIC_PREFIX.equalsIgnoreCase(auth.substring(0, 5))) {
                // check if the client_id and client_secret are passed as headers.
                // TODO: move this to utility method. Same logic used on basic-auth handler
                String credentials = auth.substring(6);
                int pos = credentials.indexOf(':');
                if (pos == -1) {
                    credentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(credentials), UTF_8);
                }

                pos = credentials.indexOf(':');
                if (pos != -1) {
                    clientId = credentials.substring(0, pos);
                    clientSecret = credentials.substring(pos + 1);
                }
                if(logger.isTraceEnabled()) logger.trace("Credentials obtained from body as username = {}, password = {}", clientId, StringUtils.maskHalfString(clientSecret));
            } else {
                logger.error("Invalid/Unsupported authorization header {}", auth);
                ResponseCodeHandler.HANDLE_403.handleRequest(exchange);
                return;
            }

            // construct the key based on grant_type and client_id or code.
            if(grantType.equals(CLIENT_CREDENTIALS)) {
                key = clientId + ":" + clientIpAddress + ":" + scope;
                if(logger.isTraceEnabled()) logger.trace("client credentials key = {}", key);
            } else if(grantType.equals(AUTHORIZATION_CODE)) {
                String code = bodyMap.get(CODE);
                key = clientId  + ":" + code + ":" + clientIpAddress + ":" + scope;
                if(logger.isTraceEnabled()) logger.trace("authorization code key = {}", key);
            } else {
                // other grant_type, ignore it.
                if(logger.isTraceEnabled()) logger.trace("other grant type {}, ignore it", grantType);
            }

            // secondly, we need to identify if the ClientID is considered Legacy or not. If it is, bypass limit, cache and call next handler.
            List<String> legacyClient = config.getLegacyClient();
            if(legacyClient.contains(clientId)) {
                if(logger.isTraceEnabled()) logger.trace("client {} is configured as Legacy, bypass the token limit.", clientId);
                //  check if cache key exists in cache manager, if exists return cached token
                String expireKey = config.getExpireKey();
                CachedResponseEntity<String> cachedResponseEntity = expireKey.isEmpty() ? (CachedResponseEntity) cacheManager.get(CLIENT_TOKEN, key) : getUpdatedResponseEntity((CachedResponseEntity) cacheManager.get(CLIENT_TOKEN, key), expireKey);
                if (cachedResponseEntity != null) {
                    if(logger.isTraceEnabled()) logger.trace("legacy client cache key {} has token value, returning cached token.", key);
                    exchange.getResponseHeaders().putAll(cachedResponseEntity.getHeaders());
                    exchange.setStatusCode(cachedResponseEntity.getStatusCode().value());
                    exchange.getResponseSender().send(cachedResponseEntity.getBody());
                } else {
                    if(logger.isTraceEnabled()) logger.trace("legacy client cache key {} has NO token cached, calling next handler.", key);
                    exchange.putAttachment(AttachmentConstants.RESPONSE_CACHE, new CacheTask(CLIENT_TOKEN, key));
                    Handler.next(exchange, next);
                }
                return;
            }


            if(key != null) {
                // check if the key is in the cache manager.
                synchronized(this) {
                    Integer count = (Integer)cacheManager.get(TOKEN_LIMIT, key);
                    if(logger.isTraceEnabled()) logger.trace("count = {} and dupLimit = {}", count, config.duplicateLimit);
                    if(count != null) {
                        // check if the count is reached limit already.
                        if(count >= config.duplicateLimit) {
                            if(config.errorOnLimit) {
                                // return an error to the caller.
                                setExchangeStatus(exchange, TOKEN_LIMIT_ERROR);
                                return;
                            } else {
                                // log the error in the log.
                                logger.error("Too many token requests. Please cache the token on the client side.");
                            }
                        } else {
                            cacheManager.put(TOKEN_LIMIT, key, ++count);
                        }
                    } else {
                        // add count 1 into the cache.
                        cacheManager.put(TOKEN_LIMIT, key, 1);
                    }
                }
            }
        }
        Handler.next(exchange, next);
        if(logger.isDebugEnabled()) logger.debug("TokenLimitHandler.handleRequest ends.");
    }

    private CachedResponseEntity<String> getUpdatedResponseEntity(CachedResponseEntity<String> cachedResponseEntity, String expireKey) {
        if (cachedResponseEntity != null && cachedResponseEntity.getBody() != null) {
            // check if the response body is empty, if not, set the content type to application/json
            if (cachedResponseEntity.getBody().length() > 0) {
                ObjectMapper mapper = new ObjectMapper();
                long updatedExpireIn = 0L;

                try {
                    JsonNode jwtNode = mapper.readTree(cachedResponseEntity.getBody());
                    // update expire field with remaining time from cache.timestamp till now in seconds
                    updatedExpireIn = jwtNode.get(expireKey).asLong() - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - cachedResponseEntity.getTimestamp());
                    if(logger.isTraceEnabled()) logger.trace("Original expire field value = {} updated to value = {}", jwtNode.get(expireKey), updatedExpireIn);
                    ((ObjectNode)jwtNode).put(expireKey, updatedExpireIn);
                    return new CachedResponseEntity<String>(jwtNode.toString(), cachedResponseEntity.getHeaders(), cachedResponseEntity.getStatusCode(), cachedResponseEntity.getTimestamp());
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing response body and updating " + expireKey + " field. Will proceed with cached response.", e);
                }
            }
        }
        return cachedResponseEntity;
    }

    public Map<String, String> convertStringToHashMap(String input) {
        Map<String, String> map = new HashMap<>();

        String[] pairs = input.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                map.put(key, value);
            }
        }
        return map;
    }

    public boolean matchPath(String path) {
        // if there is no configured pattern, not matched.
        if(patterns == null || patterns.isEmpty()) return false;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
