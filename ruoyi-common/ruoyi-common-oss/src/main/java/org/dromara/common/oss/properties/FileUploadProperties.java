package org.dromara.common.oss.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传配置属性
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 是否使用原文件名 (true: 使用原文件名, false: 使用UUID生成文件名)
     */
    private Boolean useOriginalName = false;

}
