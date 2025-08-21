package org.dromara.hm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.sd400mp.GlbProperties;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * SD400MP API转发控制器
 *
 * @author Mashir0
 * @date 2025-08-20
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sd400mp")
@Slf4j
public class Sd400mpController extends BaseController {

    private static final String URI = GlbProperties.uri;

    /**
     * 通用请求转发方法
     * 支持所有HTTP方法，转发请求到SD400MP API并保持原始请求头和参数
     *
     * @param request 原始HTTP请求
     * @param response HTTP响应
     * @param requestBody 请求体（可选）
     * @return 目标API的响应
     */
        @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> forwardRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) String requestBody) {

        try {
            // 1. 提取目标路径
            String requestURI = request.getRequestURI();
            String targetPath = requestURI.replace("/sd400mp", "");

            // 2. 构建目标URL
            String baseUrl = URI;
            String targetUrl = baseUrl + targetPath;

            // 3. 添加查询参数
            String queryString = request.getQueryString();
            if (StringUtils.isNotBlank(queryString)) {
                targetUrl += "?" + queryString;
            }

            // 4. 复制请求头
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // 跳过一些不应该转发的头
                if (!shouldSkipHeader(headerName)) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            }

            // 禁用压缩以避免解压问题
            headers.set("Accept-Encoding", "identity");

            // 5. 处理请求体
            // 注意：当@RequestBody注解存在时，Spring已经读取了请求体
            // 如果requestBody为null，说明请求体确实为空，不需要再次读取

            // 6. 创建HTTP实体
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                        // 7. 发送请求
            RestTemplate restTemplate = createRestTemplate();

            // 判断是否为二进制内容请求，如果是则使用byte[]处理
            ResponseEntity<?> responseEntity;
            boolean isBinaryContent = isBinaryRequest(targetPath, headers);

            if (isBinaryContent) {
                responseEntity = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class
                );
            } else {
                responseEntity = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    String.class
                );
            }

            // 8. 复制响应头
            HttpHeaders responseHeaders = new HttpHeaders();
            responseEntity.getHeaders().forEach((key, value) -> {
                if (!shouldSkipResponseHeader(key)) {
                    responseHeaders.addAll(key, value);
                }
            });

            Object responseBody = responseEntity.getBody();

            if (!isBinaryContent) {
                String stringBody = (String) responseBody;
                log.debug("响应体内容: {}", stringBody);

                // 确保Content-Type正确设置
                if (!responseHeaders.containsKey("Content-Type") && stringBody != null && !stringBody.isEmpty()) {
                    // 尝试判断响应体类型
                    String trimmedBody = stringBody.trim();
                    if (trimmedBody.startsWith("{") || trimmedBody.startsWith("[")) {
                        responseHeaders.set("Content-Type", "application/json;charset=UTF-8");
                    } else {
                        responseHeaders.set("Content-Type", "text/plain;charset=UTF-8");
                    }
                    log.debug("设置Content-Type: {}", responseHeaders.get("Content-Type"));
                }
            }

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);

        } catch (Exception e) {
            log.error("请求转发失败: {} {}", request.getMethod(), request.getRequestURI(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"请求转发失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 判断是否应该跳过某个请求头
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.equals("host") ||
               lowerHeaderName.equals("content-length") ||
               lowerHeaderName.equals("transfer-encoding") ||
               lowerHeaderName.equals("connection") ||
               lowerHeaderName.equals("upgrade") ||
               lowerHeaderName.startsWith("x-forwarded-");
    }

    /**
     * 判断是否应该跳过某个响应头
     */
    private boolean shouldSkipResponseHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.equals("transfer-encoding") ||
               lowerHeaderName.equals("connection") ||
               lowerHeaderName.equals("upgrade") ||
               lowerHeaderName.equals("server");
    }

    /**
     * 判断是否为二进制内容请求
     *
     * @param targetPath 目标路径
     * @param headers 请求头
     * @return 是否为二进制请求
     */
    private boolean isBinaryRequest(String targetPath, HttpHeaders headers) {
        // 1. 检查路径是否包含常见的文件下载端点
        String lowerPath = targetPath.toLowerCase();
        if (lowerPath.contains("/download") ||
            lowerPath.contains("/dataset") ||
            lowerPath.contains("/file") ||
            lowerPath.contains("/export") ||
            lowerPath.contains(".zip") ||
            lowerPath.contains(".rar") ||
            lowerPath.contains(".pdf") ||
            lowerPath.contains(".xlsx") ||
            lowerPath.contains(".docx")) {
            return true;
        }

        // 2. 检查Accept头是否包含二进制类型
        String accept = headers.getFirst("Accept");
        if (accept != null) {
            String lowerAccept = accept.toLowerCase();
            if (lowerAccept.contains("application/octet-stream") ||
                lowerAccept.contains("application/zip") ||
                lowerAccept.contains("application/pdf") ||
                lowerAccept.contains("application/vnd.ms-excel") ||
                lowerAccept.contains("application/vnd.openxmlformats")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 创建支持跳过SSL验证的RestTemplate
     * 适用于开发/测试环境，生产环境建议配置正确的证书
     */
    private RestTemplate createRestTemplate() {
        try {
            // 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // 信任所有客户端证书
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // 信任所有服务器证书
                    }
                }
            };

            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建HostnameVerifier，跳过主机名验证
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // 创建RestTemplate并配置SSL
            RestTemplate restTemplate = new RestTemplate();

            // 尝试使用HttpsURLConnection的方式（简单但全局）
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            log.debug("已创建跳过SSL验证的RestTemplate");
            return restTemplate;

        } catch (Exception e) {
            log.warn("创建SSL配置失败，使用默认RestTemplate", e);
            return new RestTemplate();
        }
    }


}
