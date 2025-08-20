package org.dromara.hm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.JsonNode;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.TestPoint;
import org.dromara.hm.domain.bo.TestPointBo;
import org.dromara.hm.domain.vo.TestPointVo;

import java.util.Collection;
import java.util.List;

/**
 * 测点Service接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
public interface ITestPointService extends IService<TestPoint> {

    /**
     * 查询单个
     *
     * @return
     */
    TestPointVo queryById(Long id);

    /**
     * 查询列表
     */
    TableDataInfo<TestPointVo> queryPageList(TestPointBo bo, PageQuery pageQuery);

    /**
     * 自定义分页查询
     */
    TableDataInfo<TestPointVo> customPageList(TestPointBo bo, PageQuery pageQuery);

    /**
     * 查询列表
     */
    List<TestPointVo> queryList(TestPointBo bo);

    /**
     * 根据新增业务对象插入测点
     *
     * @param bo 测点新增业务对象
     * @return
     */
    Boolean insertByBo(TestPointBo bo);

    /**
     * 根据编辑业务对象修改测点
     *
     * @param bo 测点编辑业务对象
     * @return
     */
    Boolean updateByBo(TestPointBo bo);

    Boolean updateBatchByBo(List<TestPointBo> bo);

    /**
     * 校验并删除数据
     *
     * @param ids     主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<TestPoint> list);

    /**
     * 根据设备ID查询测点列表
     *
     * @param equipmentId 设备ID
     * @return 测点列表
     */
    List<TestPointVo> queryByEquipmentId(Long equipmentId);

    /**
     * 根据KKS编码查询测点
     *
     * @param kksCode KKS编码
     * @return 测点信息
     */
    TestPointVo queryByKksCode(String kksCode);

    /**
     * 从JSON数据导入单个测点
     *
     * @param testPointJson 单个测点的JSON数据
     * @return 导入结果
     */
    Boolean importFromJson(JsonNode testPointJson);


    Boolean unbind(List<Long> testPointIds);
}
