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

import feign.Logger.Level;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Capabilities expose core feign artifacts to implementations so parts of core can be customized
 * around the time the client being built.
 * <p>
 * For instance, capabilities take the {@link Client}, make changes to it and feed the modified
 * version back to feign.
 *
 * @see Metrics5Capability
 */
public interface Capability {

    static Object enrich(Object componentToEnrich,
                         Class<?> capabilityToEnrich,
                         List<Capability> capabilities) {
        // componentToEnrich 是原始组件，capabilities是能力列表，这一步就是把所有能力都增加到原始组件上
        return capabilities.stream()
                .reduce(
                        componentToEnrich,
                        (target, capability) -> invoke(target, capability, capabilityToEnrich),
                        (component, enrichedComponent) -> enrichedComponent);
    }

    static Object enrich2(Object componentToEnrich,
                         Class<?> capabilityToEnrich,
                         List<Capability> capabilities) {
        // componentToEnrich 是原始组件，capabilities是能力列表，这一步就是把所有能力都增加到原始组件上
        for (Capability capability : capabilities) {
            componentToEnrich = invoke(componentToEnrich,capability, capabilityToEnrich);
        }
        return componentToEnrich;
    }

    static Object invoke(Object target, Capability capability, Class<?> capabilityToEnrich) {
        return Arrays.stream(capability.getClass().getMethods())
                // 过滤方法名必须是enrich
                .filter(method -> method.getName().equals("enrich"))
                // 过滤方法返回值与需要增强的对象类型需要一致,A.isAssignableFrom(B), A是否是B的或者B的超类
                .filter(method -> method.getReturnType().isAssignableFrom(capabilityToEnrich))
                // 只执行第一个方法enrich,感觉这里有bug，提了个issue给openfeign
                .findFirst()
                .map(method -> {
                    try {
                        return method.invoke(capability, target);
                    } catch (IllegalAccessException | IllegalArgumentException
                             | InvocationTargetException e) {
                        throw new RuntimeException("Unable to enrich " + target, e);
                    }
                })
                .orElse(target);
    }


    default Client enrich(Client client) {
        return client;
    }

    default AsyncClient<Object> enrich(AsyncClient<Object> client) {
        return client;
    }

    default Retryer enrich(Retryer retryer) {
        return retryer;
    }

    default RequestInterceptor enrich(RequestInterceptor requestInterceptor) {
        return requestInterceptor;
    }

    default ResponseInterceptor enrich(ResponseInterceptor responseInterceptor) {
        return responseInterceptor;
    }

    default ResponseInterceptor.Chain enrich(ResponseInterceptor.Chain chain) {
        return chain;
    }

    default Logger enrich(Logger logger) {
        return logger;
    }

    default Level enrich(Level level) {
        return level;
    }

    default Contract enrich(Contract contract) {
        return contract;
    }

    default Options enrich(Options options) {
        return options;
    }

    default Encoder enrich(Encoder encoder) {
        return encoder;
    }

    default Decoder enrich(Decoder decoder) {
        return decoder;
    }

    default ErrorDecoder enrich(ErrorDecoder decoder) {
        return decoder;
    }

    default InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return invocationHandlerFactory;
    }

    default QueryMapEncoder enrich(QueryMapEncoder queryMapEncoder) {
        return queryMapEncoder;
    }

    default AsyncResponseHandler enrich(AsyncResponseHandler asyncResponseHandler) {
        return asyncResponseHandler;
    }

    default <C> AsyncContextSupplier<C> enrich(AsyncContextSupplier<C> asyncContextSupplier) {
        return asyncContextSupplier;
    }

    default MethodInfoResolver enrich(MethodInfoResolver methodInfoResolver) {
        return methodInfoResolver;
    }
}
