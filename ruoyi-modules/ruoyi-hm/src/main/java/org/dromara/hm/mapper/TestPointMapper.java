package org.dromara.hm.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.hm.domain.TestPoint;
import org.dromara.hm.domain.vo.TestPointVo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 测点Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface TestPointMapper extends BaseMapperPlus<TestPoint, TestPointVo> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    Page<TestPointVo> customPageList(@Param("page") Page<TestPoint> page, @Param("ew") Wrapper<TestPoint> wrapper);

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default <P extends IPage<TestPointVo>> P selectVoPage(IPage<TestPoint> page, Wrapper<TestPoint> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default List<TestPointVo> selectVoList(Wrapper<TestPoint> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    @Override
    @DataPermission(value = {
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    }, joinStr = "AND")
    List<TestPoint> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    @Override
//    @DataPermission({
//        @DataColumn(key = "deptName", value = "dept_id"),
//        @DataColumn(key = "userName", value = "user_id")
//    })
    int updateById(@Param(Constants.ENTITY) TestPoint entity);

}
