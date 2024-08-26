/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
