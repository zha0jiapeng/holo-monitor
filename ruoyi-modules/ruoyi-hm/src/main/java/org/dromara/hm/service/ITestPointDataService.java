package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.TestPointData;
import org.dromara.hm.domain.bo.TestPointDataBo;
import org.dromara.hm.domain.vo.TestPointDataVo;

import java.util.Collection;
import java.util.List;

/**
 * 测点数据Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ITestPointDataService extends IService<TestPointData> {

    /**
     * 查询单个
     *
     * @param id 主键
     * @return 测点数据
     */
    TestPointDataVo queryById(Long id);

    /**
     * 查询列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页查询条件
     * @return 测点数据列表
     */
    TableDataInfo<TestPointDataVo> queryPageList(TestPointDataBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     *
     * @param bo        查询条件
     * @param pageQuery 分页查询条件
     * @return 测点数据列表
     */
    TableDataInfo<TestPointDataVo> customPageList(TestPointDataBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     *
     * @param bo 查询条件
     * @return 测点数据列表
     */
    List<TestPointDataVo> queryList(TestPointDataBo bo);

    /**
     * 根据新增业务对象插入测点数据
     *
     * @param bo 测点数据新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(TestPointDataBo bo);

    /**
     * 根据编辑业务对象修改测点数据
     *
     * @param bo 测点数据编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(TestPointDataBo bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     *
     * @param list 测点数据列表
     * @return 是否成功
     */
    Boolean saveBatch(List<TestPointData> list);

    /**
     * 根据KKS编码查询测点数据列表
     *
     * @param kksCode KKS编码
     * @return 测点数据列表
     */
    List<TestPointDataVo> queryByKksCode(String kksCode);

    /**
     * 根据KKS编码和时间范围查询测点数据
     *
     * @param kksCode   KKS编码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 测点数据列表
     */
    List<TestPointDataVo> queryByKksCodeAndTimeRange(String kksCode, String startTime, String endTime);

    /**
     * 根据报警状态查询测点数据
     *
     * @param alarmType 报警类型
     * @param st        SD400MP报警状态
     * @return 测点数据列表
     */
    List<TestPointDataVo> queryByAlarmStatus(String alarmType, Integer st);

    /**
     * 获取最新的测点数据
     *
     * @param kksCode KKS编码
     * @return 最新的测点数据
     */
    TestPointDataVo getLatestByKksCode(String kksCode);

}
