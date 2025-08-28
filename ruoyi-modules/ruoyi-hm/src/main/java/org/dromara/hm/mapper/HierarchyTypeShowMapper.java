package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.HierarchyTypeShow;
import org.dromara.hm.domain.vo.HierarchyTypeShowVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 层级类型展示Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface HierarchyTypeShowMapper extends BaseMapperPlus<HierarchyTypeShow, HierarchyTypeShowVo> {

    Page<HierarchyTypeShowVo> customPageList(@Param("page") Page<HierarchyTypeShow> page, @Param("ew") Wrapper<HierarchyTypeShow> wrapper);

    @Override
    default <P extends IPage<HierarchyTypeShowVo>> P selectVoPage(IPage<HierarchyTypeShow> page, Wrapper<HierarchyTypeShow> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<HierarchyTypeShowVo> selectVoList(Wrapper<HierarchyTypeShow> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<HierarchyTypeShow> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) HierarchyTypeShow entity);

}
