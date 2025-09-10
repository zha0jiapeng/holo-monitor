package org.dromara.common.core.utils.sd400mp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MP ID单个对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class MPIDJson {
    private String id;

    public static MPIDJson create(Object obj) {
        if (obj == null) return null;
        return new MPIDJson(obj.toString());
    }
}

/**
 * MP ID多个对象JSON
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Data
public class MPIDMultipleJson {

    /**
     * ID对象列表
     */
    private List<MPIDJson> items = new ArrayList<>();

    /**
     * 添加单个ID
     *
     * @param obj ID对象
     */
    public void add(Object obj) {
        MPIDJson idJson = MPIDJson.create(obj);
        if (idJson != null) {
            this.items.add(idJson);
        }
    }

    /**
     * 添加ID列表
     *
     * @param objs ID对象列表
     */
    public void addRange(List<?> objs) {
        if (objs != null) {
            for (Object obj : objs) {
                this.add(obj);
            }
        }
    }

    /**
     * 创建MPIDMultipleJson实例
     *
     * @param idList ID列表
     * @return MPIDMultipleJson实例
     */
    public static MPIDMultipleJson create(List<?> idList) {
        MPIDMultipleJson result = new MPIDMultipleJson();
        if (idList != null) {
            result.addRange(idList);
        }
        return result;
    }

    /**
     * 创建MPIDMultipleJson实例（单个ID）
     *
     * @param obj 单个ID对象
     * @return MPIDMultipleJson实例
     */
    public static MPIDMultipleJson create(Object obj) {
        MPIDMultipleJson result = new MPIDMultipleJson();
        result.add(obj);
        return result;
    }
}
