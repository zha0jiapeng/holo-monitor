package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.HierarchyTypePropertyDict;
import org.dromara.hm.domain.vo.HierarchyTypePropertyDictVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性字典Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface HierarchyTypePropertyDictMapper extends BaseMapperPlus<HierarchyTypePropertyDict, HierarchyTypePropertyDictVo> {

    Page<HierarchyTypePropertyDictVo> customPageList(@Param("page") Page<HierarchyTypePropertyDict> page, @Param("ew") Wrapper<HierarchyTypePropertyDict> wrapper);

    @Override
    default <P extends IPage<HierarchyTypePropertyDictVo>> P selectVoPage(IPage<HierarchyTypePropertyDict> page, Wrapper<HierarchyTypePropertyDict> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyTypePropertyDictVo> selectVoList(Wrapper<HierarchyTypePropertyDict> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<HierarchyTypePropertyDict> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) HierarchyTypePropertyDict entity);

}
