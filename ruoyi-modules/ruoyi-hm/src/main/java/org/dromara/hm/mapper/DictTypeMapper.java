package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.DictType;
import org.dromara.hm.domain.vo.DictTypeVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 字典类型Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface DictTypeMapper extends BaseMapperPlus<DictType, DictTypeVo> {

    Page<DictTypeVo> customPageList(@Param("page") Page<DictType> page, @Param("ew") Wrapper<DictType> wrapper);

    @Override
    default <P extends IPage<DictTypeVo>> P selectVoPage(IPage<DictType> page, Wrapper<DictType> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<DictTypeVo> selectVoList(Wrapper<DictType> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<DictType> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) DictType entity);

}
