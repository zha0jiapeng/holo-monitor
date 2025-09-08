package org.dromara.common.web.config;

import cn.hutool.json.JSONUtil;
import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;
import jakarta.annotation.PostConstruct;
import org.dromara.common.web.annotation.BrotliCompress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Brotli压缩响应体处理器
 * 自动检测@BrotliCompress注解并进行相应的压缩处理
 */
@ControllerAdvice
public class BrotliCompressionAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(BrotliCompressionAdvice.class);

    private static final String BROTLI_ENCODING = "br";
    private static final List<MediaType> COMPRESSIBLE_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_HTML,
            MediaType.TEXT_XML
    );

    @PostConstruct
    public void init() {
        if (!Brotli4jLoader.isAvailable()) {
            log.warn("Brotli native library is not available, compression will be disabled");
        } else {
            log.info("Brotli compression support enabled");
        }
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 只支持JSON消息转换器
        if (!converterType.getName().contains("Json") && !converterType.getName().contains("MappingJackson")) {
            return false;
        }

        // 检查方法或类是否有@BrotliCompress注解
        BrotliCompress methodAnnotation = returnType.getMethodAnnotation(BrotliCompress.class);
        if (methodAnnotation != null) {
            return methodAnnotation.enabled();
        }

        BrotliCompress classAnnotation = AnnotationUtils.findAnnotation(returnType.getContainingClass(), BrotliCompress.class);
        return classAnnotation != null && classAnnotation.enabled();
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 如果Brotli不可用，直接返回原响应
        if (!Brotli4jLoader.isAvailable()) {
            return body;
        }

        // 检查客户端是否支持Brotli压缩
        List<String> acceptEncodingHeaders = request.getHeaders().get(HttpHeaders.ACCEPT_ENCODING);
        boolean clientSupportsBrotli = acceptEncodingHeaders != null &&
                acceptEncodingHeaders.stream().anyMatch(header -> header.contains(BROTLI_ENCODING));

        if (!clientSupportsBrotli) {
            log.debug("Client does not support Brotli encoding");
            return body;
        }

        // 检查内容类型是否支持压缩
        if (selectedContentType == null || !isCompressibleType(selectedContentType)) {
            log.debug("Content type {} is not compressible", selectedContentType);
            return body;
        }

        // 获取注解配置
        BrotliCompress annotation = getBrotliCompressAnnotation(returnType);
        if (annotation == null) {
            return body;
        }

        try {
            // 将响应体转换为字符串
            String responseString = convertToString(body);
            if (responseString == null || responseString.isEmpty()) {
                return body;
            }

            byte[] originalBytes = responseString.getBytes(StandardCharsets.UTF_8);

            // 检查是否达到压缩阈值
            if (originalBytes.length < annotation.threshold()) {
                log.debug("Response size {} is below compression threshold {}",
                         originalBytes.length, annotation.threshold());
                return body;
            }

            // 执行Brotli压缩
            byte[] compressedBytes = compressWithBrotli(originalBytes, annotation);

                        if (compressedBytes != null && compressedBytes.length < originalBytes.length) {
                // 设置响应头
                response.getHeaders().set(HttpHeaders.CONTENT_ENCODING, BROTLI_ENCODING);
                response.getHeaders().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(compressedBytes.length));
                response.getHeaders().set(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);

                log.info("Brotli compression applied: {} bytes -> {} bytes ({}% reduction) [quality={}]",
                         originalBytes.length, compressedBytes.length,
                         100 - (compressedBytes.length * 100 / originalBytes.length),
                         annotation.quality());

                // 直接写入压缩数据到响应体
                try {
                    response.getBody().write(compressedBytes);
                    response.getBody().flush();
                    // 返回null表示已经处理完响应，Spring无需再处理
                    return null;
                } catch (Exception e) {
                    log.error("Failed to write compressed response", e);
                    return body;
                }
            } else {
                log.debug("Brotli compression did not reduce response size, returning original");
                return body;
            }

        } catch (Exception e) {
            log.error("Error applying Brotli compression", e);
            return body;
        }
    }

    /**
     * 获取BrotliCompress注解
     */
    private BrotliCompress getBrotliCompressAnnotation(MethodParameter returnType) {
        BrotliCompress methodAnnotation = returnType.getMethodAnnotation(BrotliCompress.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(returnType.getContainingClass(), BrotliCompress.class);
    }

    /**
     * 检查内容类型是否支持压缩
     */
    private boolean isCompressibleType(MediaType contentType) {
        return COMPRESSIBLE_TYPES.stream()
                .anyMatch(type -> type.isCompatibleWith(contentType));
    }

    /**
     * 将响应体转换为字符串
     */
    private String convertToString(Object body) {
        if (body == null) {
            return null;
        }

        if (body instanceof String) {
            return (String) body;
        }

        // 使用FastJSON序列化其他类型的对象
        try {
            return JSONUtil.toJsonStr(body);
        } catch (Exception e) {
            log.error("Failed to serialize response body to JSON", e);
            return body.toString();
        }
    }

    /**
     * 使用Brotli进行压缩
     */
    private byte[] compressWithBrotli(byte[] input, BrotliCompress annotation) {
        try {
            // 验证参数范围
            int quality = Math.max(0, Math.min(11, annotation.quality()));
            int window = Math.max(10, Math.min(24, annotation.window()));

            Encoder.Parameters params = new Encoder.Parameters()
                    .setQuality(quality)
                    .setWindow(window);

            return Encoder.compress(input, params);
        } catch (Exception e) {
            log.error("Brotli compression failed", e);
            return null;
        }
    }


}
