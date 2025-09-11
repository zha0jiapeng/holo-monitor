package org.dromara.hm.domain.sd400mp;

import cn.hutool.json.JSONObject;
import lombok.Data;

/**
 * MP标签对象，对应JavaScript中的MPTag类
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
public class MPTag {
    
    // 静态常量定义，对应JavaScript中的静态字段
    public static final String AMPLITUDE_UNITS = "mont/pd/au";
    public static final String SENSOR_TYPE = "mont/pd/mt";
    public static final String AVERAGE_AMPLITUDE = "mont/pd/magAv";
    public static final String PD_DATASET = "bin:mont/pd";
    public static final String PD_DATASET_FEATURES = "bin:mont/pd/wf/features";
    public static final String PD_DATASET_WAVEFORMS = "bin:mont/pd/wf/raw";
    public static final String PD_CLASS_ENUM = "sys:mont/pd/dia/class/enum";
    public static final String CONNECTION_STATE = "sys:cs";
    public static final String TESTPOINT_STATE = "sys:st";
    
    /**
     * 标签键
     */
    private String key;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 单位
     */
    private String units;
    
    /**
     * 父级
     */
    private String parent;
    
    /**
     * 保存类型
     */
    private Integer saveType;
    
    /**
     * 状态
     */
    private Boolean state;
    
    /**
     * 趋势
     */
    private Boolean trend;
    
    /**
     * 位标志
     */
    private Boolean bit;
    
    /**
     * 隐藏标志
     */
    private Boolean hidden;
    
    /**
     * 属性
     */
    private Integer prop;
    
    /**
     * 单位链接
     */
    private String unitLink;
    
    /**
     * 链接
     */
    private String link;
    
    /**
     * 构造函数，从JSON对象创建MPTag
     *
     * @param tag JSON标签对象
     */
    public MPTag(JSONObject tag) {
        this.key = tag.getStr("key");
        this.title = tag.getStr("title");
        this.units = tag.getStr("units");
        this.parent = tag.getStr("parent");
        this.saveType = tag.getInt("saveType", 0);
        this.state = tag.getBool("state", false);
        this.trend = tag.getBool("trend", false);
        this.bit = tag.getBool("bit", false);
        this.hidden = tag.getBool("hidden", false);
        this.prop = tag.getInt("prop", 0);
        this.unitLink = tag.getStr("unitLink");
        this.link = tag.getStr("link");
    }
    
    /**
     * 默认构造函数
     */
    public MPTag() {
        this.saveType = 0;
        this.state = false;
        this.trend = false;
        this.bit = false;
        this.hidden = false;
        this.prop = 0;
    }
}
