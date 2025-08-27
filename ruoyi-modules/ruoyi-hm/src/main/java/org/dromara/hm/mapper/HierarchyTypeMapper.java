package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.HierarchyType;
import org.dromara.hm.domain.vo.HierarchyTypeVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 层级类型Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface HierarchyTypeMapper extends BaseMapperPlus<HierarchyType, HierarchyTypeVo> {

    Page<HierarchyTypeVo> customPageList(@Param("page") Page<HierarchyType> page, @Param("ew") Wrapper<HierarchyType> wrapper);

    @Override
    default <P extends IPage<HierarchyTypeVo>> P selectVoPage(IPage<HierarchyType> page, Wrapper<HierarchyType> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyTypeVo> selectVoList(Wrapper<HierarchyType> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<HierarchyType> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) HierarchyType entity);

}
