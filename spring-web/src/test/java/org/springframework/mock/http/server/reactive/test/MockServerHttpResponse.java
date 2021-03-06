/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.http.server.reactive.test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ServerHttpResponse}.
 * @author Rossen Stoyanchev
 */
public class MockServerHttpResponse implements ServerHttpResponse {

	private HttpStatus status;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Publisher<? extends DataBuffer> body;

	private Publisher<? extends Publisher<? extends DataBuffer>> bodyWithFlushes;

	private DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@Override
	public boolean setStatusCode(HttpStatus status) {
		this.status = status;
		return true;
	}

	@Override
	public HttpStatus getStatusCode() {
		return this.status;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

	public Publisher<? extends DataBuffer> getBody() {
		return this.body;
	}

	public Publisher<? extends Publisher<? extends DataBuffer>> getBodyWithFlush() {
		return this.bodyWithFlushes;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		this.body = body;
		return Flux.from(this.body).then();
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		this.bodyWithFlushes = body;
		return Flux.from(this.bodyWithFlushes).then();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
	}

	@Override
	public Mono<Void> setComplete() {
		return Mono.empty();
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	/**
	 * Return the body of the response aggregated and converted to a String
	 * using the charset of the Content-Type response or otherwise defaulting
	 * to "UTF-8".
	 */
	public Mono<String> getBodyAsString() {
		Charset charset = getCharset();
		Charset charsetToUse = (charset != null ? charset : StandardCharsets.UTF_8);
		return Flux.from(this.body)
				.reduce(this.bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				})
				.map(buffer -> DataBufferTestUtils.dumpString(buffer, charsetToUse));
	}

	private Charset getCharset() {
		MediaType contentType = getHeaders().getContentType();
		if (contentType != null) {
			return contentType.getCharset();
		}
		return null;
	}

}
