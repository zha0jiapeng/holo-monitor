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


}
