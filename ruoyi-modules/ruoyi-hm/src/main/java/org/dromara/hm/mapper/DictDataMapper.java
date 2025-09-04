package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.DictData;
import org.dromara.hm.domain.vo.DictDataVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 字典数据Mapper接口
 *
 * @author Mashir0
 * @date 2024-01-01
 */
public interface DictDataMapper extends BaseMapperPlus<DictData, DictDataVo> {

    Page<DictDataVo> customPageList(@Param("page") Page<DictData> page, @Param("ew") Wrapper<DictData> wrapper);

    @Override
    default <P extends IPage<DictDataVo>> P selectVoPage(IPage<DictData> page, Wrapper<DictData> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    default List<DictDataVo> selectVoList(Wrapper<DictData> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    List<DictData> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) DictData entity);

}
