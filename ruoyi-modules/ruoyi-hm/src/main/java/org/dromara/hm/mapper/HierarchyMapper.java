package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.Hierarchy;
import org.dromara.hm.domain.vo.HierarchyVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 层级Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface HierarchyMapper extends BaseMapperPlus<Hierarchy, HierarchyVo> {

    Page<HierarchyVo> customPageList(@Param("page") Page<Hierarchy> page, @Param("ew") Wrapper<Hierarchy> wrapper);

    @Override
    default <P extends IPage<HierarchyVo>> P selectVoPage(IPage<Hierarchy> page, Wrapper<Hierarchy> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyVo> selectVoList(Wrapper<Hierarchy> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<Hierarchy> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) Hierarchy entity);

    @Select("SELECT\n" +
        "\thhp.hierarchy_id\n " +
        "FROM\n " +
        "\thm_hierarchy_property hhp\n " +
        "left join hm_hierarchy_type_property hhtp\n " +
        "on hhp.type_property_id = hhtp.id\n " +
        "left JOIN hm_hierarchy_type_property_dict hhtpd\n " +
        "on hhtp.property_dict_id = hhtpd.id\n " +
        "where hhp.property_value = #{hierarchyId} and data_type = 1001 ")
    List<Long> selctChildHierarchyIOs(Long hierarchyId);

    @Select({
        "<script>",
        "SELECT",
        "    hhp.property_value hierarchy_id",
        "FROM",
        "    hm_hierarchy_property hhp",
        "    LEFT JOIN hm_hierarchy_type_property hhtp ON hhp.type_property_id = hhtp.id",
        "    LEFT JOIN hm_hierarchy_type_property_dict hhtpd ON hhtp.property_dict_id = hhtpd.id",
        "    INNER JOIN hm_hierarchy hh ON hh.id = hhp.hierarchy_id",
        "WHERE",
        "    hhtpd.dict_values = #{targetTypeId}",
        "    AND hhp.scope = 1",
//        "    AND hh.id IN",
//        "    <foreach item='id' collection='ids' open='(' close=')' separator=','>",
//        "        #{id}",
//        "    </foreach>",
        "</script>"
    })
    List<Long> selecttargetTypeHierarchyList(@Param("ids") List<Long> ids, @Param("targetTypeId") Long targetTypeId);
}
