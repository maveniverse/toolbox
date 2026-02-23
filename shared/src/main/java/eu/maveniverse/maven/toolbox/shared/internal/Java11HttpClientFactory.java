/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Java 11 {@link HttpClient} factory that creates pre-configured HTTP client based on Resolver {@link RemoteRepository}.
 */
public final class Java11HttpClientFactory {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10L);

    public static HttpClient buildHttpClient(RepositorySystemSession session, RemoteRepository repository) {
        return buildHttpClient(DEFAULT_TIMEOUT, session, repository);
    }

    private static HttpClient buildHttpClient(
            Duration timeout, RepositorySystemSession session, RemoteRepository repository) {

        HttpClient.Builder builder =
                HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER);

        HashMap<Authenticator.RequestorType, PasswordAuthentication> authentications = new HashMap<>();
        try (AuthenticationContext repoAuthContext = AuthenticationContext.forRepository(session, repository)) {
            if (repoAuthContext != null) {
                authentications.put(
                        Authenticator.RequestorType.SERVER,
                        new PasswordAuthentication(
                                repoAuthContext.get(AuthenticationContext.USERNAME),
                                repoAuthContext
                                        .get(AuthenticationContext.PASSWORD)
                                        .toCharArray()));
            }
        }
        if (repository.getProxy() != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(
                    repository.getProxy().getHost(), repository.getProxy().getPort())));
            try (AuthenticationContext proxyAuthContext = AuthenticationContext.forProxy(session, repository)) {
                if (proxyAuthContext != null) {
                    authentications.put(
                            Authenticator.RequestorType.PROXY,
                            new PasswordAuthentication(
                                    proxyAuthContext.get(AuthenticationContext.USERNAME),
                                    proxyAuthContext
                                            .get(AuthenticationContext.PASSWORD)
                                            .toCharArray()));
                }
            }
        }
        if (!authentications.isEmpty()) {
            builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return authentications.get(getRequestorType());
                }
            });
        }

        return builder.build();
    }
}
