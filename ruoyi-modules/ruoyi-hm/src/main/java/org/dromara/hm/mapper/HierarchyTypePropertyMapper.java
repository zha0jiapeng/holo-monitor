package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.HierarchyTypeProperty;
import org.dromara.hm.domain.vo.HierarchyTypePropertyVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 层级类型属性Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface HierarchyTypePropertyMapper extends BaseMapperPlus<HierarchyTypeProperty, HierarchyTypePropertyVo> {

    Page<HierarchyTypePropertyVo> customPageList(@Param("page") Page<HierarchyTypeProperty> page, @Param("ew") Wrapper<HierarchyTypeProperty> wrapper);

    @Override
    default <P extends IPage<HierarchyTypePropertyVo>> P selectVoPage(IPage<HierarchyTypeProperty> page, Wrapper<HierarchyTypeProperty> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyTypePropertyVo> selectVoList(Wrapper<HierarchyTypeProperty> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<HierarchyTypeProperty> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) HierarchyTypeProperty entity);

}
