package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.TestpointOffline;
import org.dromara.hm.domain.bo.TestpointOfflineBo;
import org.dromara.hm.domain.vo.TestpointOfflineVo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


/**
 * 测点离线记录Service接口
 *
 * @author Mashir0
 * @date 2025-01-27
 */
public interface ITestpointOfflineService extends IService<TestpointOffline> {

    /**
     * 查询单个
     *
     * @return
     */
    TestpointOfflineVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<TestpointOfflineVo> queryPageList(TestpointOfflineBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<TestpointOfflineVo> customPageList(TestpointOfflineBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<TestpointOfflineVo> queryList(TestpointOfflineBo bo);

    /**
     * 根据新增业务对象插入测点离线记录
     *
     * @param bo 测点离线记录新增业务对象
     * @return
     */
    Boolean insertByBo(TestpointOfflineBo bo);

    /**
     * 根据编辑业务对象修改测点离线记录
     *
     * @param bo 测点离线记录编辑业务对象
     * @return
     */
    Boolean updateByBo(TestpointOfflineBo bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 记录测点离线
     *
     * @param kksCode 测点编码
     * @param offlineJudgmentThreshold 离线判断阈值
     * @param offlineTime 离线时间
     * @return
     */
    Boolean recordOffline(Long equipmentId ,String kksCode, Integer offlineJudgmentThreshold, LocalDateTime offlineTime);

    /**
     * 记录测点恢复在线
     *
     * @param kksCode 测点编码
     * @return
     */
    Boolean recordRecovery(String kksCode);

    /**
     * 检查是否存在离线记录（未恢复的）
     *
     * @param kksCode 测点编码
     * @return
     */
    Boolean isOfflineRecordExists(String kksCode);

    /**
     * 根据测点编码查询当前离线记录
     *
     * @param kksCode 测点编码
     * @return
     */
    TestpointOfflineVo queryCurrentOfflineByKksCode(String kksCode);

    /**
     * 根据测点编码查询历史离线记录
     *
     * @param kksCode 测点编码
     * @return
     */
    List<TestpointOfflineVo> queryHistoryByKksCode(String kksCode);

    /**
     * 获取离线状态统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<TestpointOfflineVo> getOfflineStatistics(LocalDateTime startTime, LocalDateTime endTime);


}
