package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.TestpointOffline;
import org.dromara.hm.domain.vo.TestpointOfflineVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 测点离线记录Mapper接口
 *
 * @author Mashir0
 * @date 2025-01-27
 */
public interface TestpointOfflineMapper extends BaseMapperPlus<TestpointOffline, TestpointOfflineVo> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    Page<TestpointOfflineVo> customPageList(@Param("page") Page<TestpointOffline> page, @Param("ew") Wrapper<TestpointOffline> wrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default <P extends IPage<TestpointOfflineVo>> P selectVoPage(IPage<TestpointOffline> page, Wrapper<TestpointOffline> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default List<TestpointOfflineVo> selectVoList(Wrapper<TestpointOffline> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    }, joinStr = "AND")
    List<TestpointOffline> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
    int updateById(@Param(Constants.ENTITY) TestpointOffline entity);

}
