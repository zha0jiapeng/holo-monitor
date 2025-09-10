package org.dromara.hm.domain.sd400mp;

import lombok.Data;

import java.util.*;

/**
 * MP事件列表响应对象
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
public class MPEventList {

    /**
     * PDE类信息映射
     */
    private Map<Integer, MPPdeClassInfo> pdClasses = new HashMap<>();

    /**
     * 设备名称映射
     */
    private Map<Long, String> namesEq = new HashMap<>();

    /**
     * 测点名称映射
     */
    private Map<Long, String> namesTp = new HashMap<>();

    /**
     * 事件分组映射
     */
    private Map<String, MPEventGroup> groups = new HashMap<>();

    /**
     * 设备列表（原始数据）
     */
    private List<MPEventEquipment> equipment = new ArrayList<>();
}
