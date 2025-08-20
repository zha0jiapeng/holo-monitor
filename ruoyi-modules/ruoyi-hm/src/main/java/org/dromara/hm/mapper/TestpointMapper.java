package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.Testpoint;
import org.dromara.hm.domain.vo.TestpointVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 测点Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface TestpointMapper extends BaseMapperPlus<Testpoint, TestpointVo> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    Page<TestpointVo> customPageList(@Param("page") Page<Testpoint> page, @Param("ew") Wrapper<Testpoint> wrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default <P extends IPage<TestpointVo>> P selectVoPage(IPage<Testpoint> page, Wrapper<Testpoint> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default List<TestpointVo> selectVoList(Wrapper<Testpoint> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    }, joinStr = "AND")
    List<Testpoint> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
//    @DataPermission({
//        @DataColumn(key = "deptName", value = "dept_id"),
//        @DataColumn(key = "userName", value = "user_id")
//    })
    int updateById(@Param(Constants.ENTITY) Testpoint entity);

}
