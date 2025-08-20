package org.dromara.common.oss.config;

import org.dromara.common.oss.properties.FileUploadProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * OSS配置类
 *
 * @author Lion Li
 */
@EnableConfigurationProperties({FileUploadProperties.class})
@AutoConfiguration
public class OssConfig {

}
