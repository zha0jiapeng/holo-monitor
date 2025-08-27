package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.HierarchyProperty;
import org.dromara.hm.domain.vo.HierarchyPropertyVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 层级属性Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface HierarchyPropertyMapper extends BaseMapperPlus<HierarchyProperty, HierarchyPropertyVo> {

    Page<HierarchyPropertyVo> customPageList(@Param("page") Page<HierarchyProperty> page, @Param("ew") Wrapper<HierarchyProperty> wrapper);

    @Override
    default <P extends IPage<HierarchyPropertyVo>> P selectVoPage(IPage<HierarchyProperty> page, Wrapper<HierarchyProperty> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyPropertyVo> selectVoList(Wrapper<HierarchyProperty> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<HierarchyProperty> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) HierarchyProperty entity);

}
