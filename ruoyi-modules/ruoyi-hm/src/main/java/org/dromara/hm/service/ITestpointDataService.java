package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.domain.TestpointData;
import org.dromara.hm.domain.bo.TestpointDataBo;
import org.dromara.hm.domain.vo.TestpointDataVo;

import java.util.Collection;
import java.util.List;

/**
 * 测点数据Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ITestpointDataService extends IService<TestpointData> {

    /**
     * 查询单个
     *
     * @param id 主键
     * @return 测点数据
     */
    TestpointDataVo queryById(Long id);

    /**
     * 查询列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页查询条件
     * @return 测点数据列表
     */
    TableDataInfo<TestpointDataVo> queryPageList(TestpointDataBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     *
     * @param bo        查询条件
     * @param pageQuery 分页查询条件
     * @return 测点数据列表
     */
    TableDataInfo<TestpointDataVo> customPageList(TestpointDataBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     *
     * @param bo 查询条件
     * @return 测点数据列表
     */
    List<TestpointDataVo> queryList(TestpointDataBo bo);

    /**
     * 根据新增业务对象插入测点数据
     *
     * @param bo 测点数据新增业务对象
     * @return 是否成功
     */
    Boolean insertByBo(TestpointDataBo bo);

    /**
     * 根据编辑业务对象修改测点数据
     *
     * @param bo 测点数据编辑业务对象
     * @return 是否成功
     */
    Boolean updateByBo(TestpointDataBo bo);

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
    Boolean saveBatch(List<TestpointData> list);

    /**
     * 根据KKS编码查询测点数据列表
     *
     * @param kksCode KKS编码
     * @return 测点数据列表
     */
    List<TestpointDataVo> queryByKksCode(String kksCode);

    /**
     * 根据KKS编码和时间范围查询测点数据
     *
     * @param kksCode   KKS编码
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 测点数据列表
     */
    List<TestpointDataVo> queryByKksCodeAndTimeRange(String kksCode, String startTime, String endTime);

    /**
     * 根据报警状态查询测点数据
     *
     * @param alarmType 报警类型
     * @param st        SD400MP报警状态
     * @return 测点数据列表
     */
    List<TestpointDataVo> queryByAlarmStatus(String alarmType, Integer st);

    /**
     * 获取最新的测点数据
     *
     * @param kksCode KKS编码
     * @return 最新的测点数据
     */
    TestpointDataVo getLatestByKksCode(String kksCode);

}
