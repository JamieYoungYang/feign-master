/*
 * Copyright 2012-2023 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign;

import static feign.ExceptionPropagationPolicy.NONE;

import feign.Feign.ResponseMappingDecoder;
import feign.Logger.NoOpLogger;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.querymap.FieldQueryMapEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class BaseBuilder<B extends BaseBuilder<B, T>, T> implements Cloneable {

    // 自身引用，用于返回当前builder对象
    private final B thisB;
    // 请求拦截器列表
    protected final List<RequestInterceptor> requestInterceptors =
            new ArrayList<>();
    // 响应拦截器
    protected final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();
    // Feign日志展示级别，默认都不展示
    protected Logger.Level logLevel = Logger.Level.NONE;
    // 解析协议，定义哪些注释和值在接口上有效。
    protected Contract contract = new Contract.Default();
    // 请求重试器，请求失败时，多久重试一次，总共重试几次等等信息
    protected Retryer retryer = new Retryer.Default();
    // 日志收集器，可以决定日志的去处，控制台、文件或者其他，或者默认什么都不处理
    protected Logger logger = new NoOpLogger();
    // 请求编码器，对请求的request的body内容进行编码，默认只支持字符串参数和字符数组参数
    protected Encoder encoder = new Encoder.Default();
    // 响应解码器，对返回的response进行解处理，默认把body部分转为UTF-8字符串
    protected Decoder decoder = new Decoder.Default();
    protected boolean closeAfterDecode = true;
    protected boolean decodeVoid = false;
    // 使用Object作为map查询参数时，直接使用字段的值（与之不同的是调用get方法来获取字段的值）
    protected QueryMapEncoder queryMapEncoder = QueryMap.MapEncoder.FIELD.instance();
    // 异常解码器，当http响应码不在[200,300)区间内，并且对于404类型不特殊处理时，会调用异常解码器，可以对异常进行包装
    protected ErrorDecoder errorDecoder = new ErrorDecoder.Default();
    // 请求时的一些设置，例如连接时间、超时时间、是否重定向等
    protected Options options = new Options();
    // 反射处理工厂，用于生成FeignClient的接口的对应代理执行方法的InvocationHandler
    protected InvocationHandlerFactory invocationHandlerFactory =
            new InvocationHandlerFactory.Default();
    protected boolean dismiss404;
    // 异常包装策略，是默认直接抛出原始异常还是用RetryableException再包一次
    protected ExceptionPropagationPolicy propagationPolicy = NONE;
    // Feign Capability 暴露了Feign的核心组件，因此通过Capability可以对某些组件进行定制化处理
    protected List<Capability> capabilities = new ArrayList<>();


    public BaseBuilder() {
        super();
        thisB = (B) this;
    }

    public B logLevel(Logger.Level logLevel) {
        this.logLevel = logLevel;
        return thisB;
    }

    public B contract(Contract contract) {
        this.contract = contract;
        return thisB;
    }

    public B retryer(Retryer retryer) {
        this.retryer = retryer;
        return thisB;
    }

    public B logger(Logger logger) {
        this.logger = logger;
        return thisB;
    }

    public B encoder(Encoder encoder) {
        this.encoder = encoder;
        return thisB;
    }

    public B decoder(Decoder decoder) {
        this.decoder = decoder;
        return thisB;
    }

    /**
     * This flag indicates that the response should not be automatically closed upon completion of
     * decoding the message. This should be set if you plan on processing the response into a
     * lazy-evaluated construct, such as a {@link java.util.Iterator}.
     *
     * </p>
     * Feign standard decoders do not have built in support for this flag. If you are using this flag,
     * you MUST also use a custom Decoder, and be sure to close all resources appropriately somewhere
     * in the Decoder (you can use {@link Util#ensureClosed} for convenience).
     *
     * @since 9.6
     */
    public B doNotCloseAfterDecode() {
        this.closeAfterDecode = false;
        return thisB;
    }

    public B decodeVoid() {
        this.decodeVoid = true;
        return thisB;
    }

    public B queryMapEncoder(QueryMapEncoder queryMapEncoder) {
        this.queryMapEncoder = queryMapEncoder;
        return thisB;
    }

    /**
     * Allows to map the response before passing it to the decoder.
     */
    public B mapAndDecode(ResponseMapper mapper, Decoder decoder) {
        this.decoder = new ResponseMappingDecoder(mapper, decoder);
        return thisB;
    }

    /**
     * This flag indicates that the {@link #decoder(Decoder) decoder} should process responses with
     * 404 status, specifically returning null or empty instead of throwing {@link FeignException}.
     * <p>
     * <p/>
     * All first-party (ex gson) decoders return well-known empty values defined by
     * {@link Util#emptyValueOf}. To customize further, wrap an existing {@link #decoder(Decoder)
     * decoder} or make your own.
     * <p>
     * <p/>
     * This flag only works with 404, as opposed to all or arbitrary status codes. This was an
     * explicit decision: 404 -> empty is safe, common and doesn't complicate redirection, retry or
     * fallback policy. If your server returns a different status for not-found, correct via a custom
     * {@link #client(Client) client}.
     *
     * @since 11.9
     */
    public B dismiss404() {
        this.dismiss404 = true;
        return thisB;
    }


    /**
     * This flag indicates that the {@link #decoder(Decoder) decoder} should process responses with
     * 404 status, specifically returning null or empty instead of throwing {@link FeignException}.
     * <p>
     * <p/>
     * All first-party (ex gson) decoders return well-known empty values defined by
     * {@link Util#emptyValueOf}. To customize further, wrap an existing {@link #decoder(Decoder)
     * decoder} or make your own.
     * <p>
     * <p/>
     * This flag only works with 404, as opposed to all or arbitrary status codes. This was an
     * explicit decision: 404 -> empty is safe, common and doesn't complicate redirection, retry or
     * fallback policy. If your server returns a different status for not-found, correct via a custom
     * {@link #client(Client) client}.
     *
     * @since 8.12
     * @deprecated use {@link #dismiss404()} instead.
     */
    @Deprecated
    public B decode404() {
        this.dismiss404 = true;
        return thisB;
    }


    public B errorDecoder(ErrorDecoder errorDecoder) {
        this.errorDecoder = errorDecoder;
        return thisB;
    }

    public B options(Options options) {
        this.options = options;
        return thisB;
    }

    /**
     * Adds a single request interceptor to the builder.
     */
    public B requestInterceptor(RequestInterceptor requestInterceptor) {
        this.requestInterceptors.add(requestInterceptor);
        return thisB;
    }

    /**
     * Sets the full set of request interceptors for the builder, overwriting any previous
     * interceptors.
     */
    public B requestInterceptors(Iterable<RequestInterceptor> requestInterceptors) {
        this.requestInterceptors.clear();
        for (RequestInterceptor requestInterceptor : requestInterceptors) {
            this.requestInterceptors.add(requestInterceptor);
        }
        return thisB;
    }

    /**
     * Sets the full set of request interceptors for the builder, overwriting any previous
     * interceptors.
     */
    public B responseInterceptors(Iterable<ResponseInterceptor> responseInterceptors) {
        this.responseInterceptors.clear();
        for (ResponseInterceptor responseInterceptor : responseInterceptors) {
            this.responseInterceptors.add(responseInterceptor);
        }
        return thisB;
    }

    /**
     * Adds a single response interceptor to the builder.
     */
    public B responseInterceptor(ResponseInterceptor responseInterceptor) {
        this.responseInterceptors.add(responseInterceptor);
        return thisB;
    }


    /**
     * Allows you to override how reflective dispatch works inside of Feign.
     */
    public B invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
        this.invocationHandlerFactory = invocationHandlerFactory;
        return thisB;
    }

    public B exceptionPropagationPolicy(ExceptionPropagationPolicy propagationPolicy) {
        this.propagationPolicy = propagationPolicy;
        return thisB;
    }

    public B addCapability(Capability capability) {
        this.capabilities.add(capability);
        return thisB;
    }

    @SuppressWarnings("unchecked")
    B enrich() {
        // 默认就是空的
        if (capabilities.isEmpty()) {
            return thisB;
        }

        try {
            // clone一个对象进行操作，防止直接操作原对象
            B clone = (B) thisB.clone();

            // 获取类中定义的所有属性字段，并对每个字段进行逐个增强
            getFieldsToEnrich().forEach(field -> {
                // 字段设置为可访问
                field.setAccessible(true);
                try {
                    // 获取字段原始值
                    final Object originalValue = field.get(clone);
                    // 字段增强后的值
                    final Object enriched;
                    // 如果字段是List类型，则对List的每个元素进行单独增强
                    if (originalValue instanceof List) {
                        // 获取List存储的实际类型，例如List<RequestInterceptor> requestInterceptors, 这里的ownerType = RequestInterceptor
                        Type ownerType =
                                ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        // 循环对List的每个元素进行增强
                        enriched = ((List) originalValue).stream()
                                .map(value -> Capability.enrich(value, (Class<?>) ownerType, capabilities))
                                .collect(Collectors.toList());
                    } else {
                        enriched = Capability.enrich(originalValue, field.getType(), capabilities);
                    }
                    field.set(clone, enriched);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("Unable to enrich field " + field, e);
                } finally {
                    field.setAccessible(false);
                }
            });

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    List<Field> getFieldsToEnrich() {
        return Util.allFields(getClass())
                .stream()
                // exclude anything generated by compiler
                .filter(field -> !field.isSynthetic())
                // and capabilities itself
                .filter(field -> !Objects.equals(field.getName(), "capabilities"))
                // and thisB helper field
                .filter(field -> !Objects.equals(field.getName(), "thisB"))
                // skip primitive types
                .filter(field -> !field.getType().isPrimitive())
                // skip enumerations
                .filter(field -> !field.getType().isEnum())
                .collect(Collectors.toList());
    }

    public final T build() {
        return enrich().internalBuild();
    }

    protected abstract T internalBuild();

    protected ResponseInterceptor.Chain responseInterceptorChain() {
        ResponseInterceptor.Chain endOfChain =
                ResponseInterceptor.Chain.DEFAULT;
        ResponseInterceptor.Chain executionChain = this.responseInterceptors.stream()
                .reduce(ResponseInterceptor::andThen)
                .map(interceptor -> interceptor.apply(endOfChain))
                .orElse(endOfChain);

        return (ResponseInterceptor.Chain) Capability.enrich(executionChain,
                ResponseInterceptor.Chain.class, capabilities);
    }


}
