package org.dromara.hm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.hm.domain.EquipmentFile;
import org.dromara.hm.domain.bo.EquipmentFileBo;
import org.dromara.hm.domain.vo.EquipmentFileVo;
import org.dromara.hm.enums.AlgorithmTypeEnum;
import org.dromara.hm.enums.EquipmentFileTypeEnum;
import org.dromara.hm.mapper.EquipmentFileMapper;
import org.dromara.hm.service.IEquipmentFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 设备文件Service业务层处理
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class EquipmentFileServiceImpl implements IEquipmentFileService {

    private final EquipmentFileMapper baseMapper;

    @Override
    public EquipmentFileVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<EquipmentFileVo> queryPageList(EquipmentFileBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<EquipmentFile> lqw = buildQueryWrapper(bo);
        Page<EquipmentFileVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 自定义分页查询
     */
    @Override
    public TableDataInfo<EquipmentFileVo> customPageList(EquipmentFileBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<EquipmentFile> lqw = buildQueryWrapper(bo);
        Page<EquipmentFileVo> result = baseMapper.customPageList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<EquipmentFileVo> queryList(EquipmentFileBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<EquipmentFile> buildQueryWrapper(EquipmentFileBo bo) {
        LambdaQueryWrapper<EquipmentFile> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getEquipmentId() != null, EquipmentFile::getEquipmentId, bo.getEquipmentId());
        lqw.eq(bo.getFileId() != null, EquipmentFile::getFileId, bo.getFileId());
        lqw.eq(bo.getFileType() != null, EquipmentFile::getFileType, bo.getFileType());
        lqw.eq(bo.getAlgorithmType() != null, EquipmentFile::getAlgorithmType, bo.getAlgorithmType());
        lqw.like(StringUtils.isNotBlank(bo.getRemark()), EquipmentFile::getRemark, bo.getRemark());
        lqw.orderByDesc(EquipmentFile::getCreateTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(EquipmentFileBo bo) {
        EquipmentFile add = MapstructUtils.convert(bo, EquipmentFile.class);
        if (add != null) {
            validEntityBeforeSave(add);
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(EquipmentFileBo bo) {
        EquipmentFile update = MapstructUtils.convert(bo, EquipmentFile.class);
        if (update != null) {
            validEntityBeforeSave(update);
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 进行删除前校验
            for (Long id : ids) {
                EquipmentFile equipmentFile = baseMapper.selectById(id);
                if (equipmentFile == null) {
                    throw new ServiceException("设备文件关联记录不存在: " + id);
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<EquipmentFile> list) {
        return baseMapper.insertBatch(list);
    }

    @Override
    public List<EquipmentFileVo> selectByEquipmentId(Long equipmentId) {
        if (equipmentId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByEquipmentId(equipmentId);
    }

    @Override
    public List<EquipmentFileVo> selectByFileId(Long fileId) {
        if (fileId == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByFileId(fileId);
    }

    @Override
    public List<EquipmentFileVo> selectByEquipmentIdAndFileType(Long equipmentId, Integer fileType) {
        if (equipmentId == null || fileType == null) {
            return new ArrayList<>();
        }
        return baseMapper.selectByEquipmentIdAndFileType(equipmentId, fileType);
    }

    @Override
    public Boolean deleteByEquipmentId(Long equipmentId) {
        if (equipmentId == null) {
            return false;
        }
        return baseMapper.deleteByEquipmentId(equipmentId) > 0;
    }

    @Override
    public Boolean deleteByFileId(Long fileId) {
        if (fileId == null) {
            return false;
        }
        return baseMapper.deleteByFileId(fileId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchInsertByEquipmentId(Long equipmentId, List<Long> fileIds, Integer fileType, Integer algorithmType) {
        if (equipmentId == null || fileIds == null || fileIds.isEmpty()) {
            return false;
        }

        // 校验文件类型是否在枚举中定义
        if (fileType != null) {
            EquipmentFileTypeEnum fileTypeEnum = EquipmentFileTypeEnum.getByCode(fileType);
            if (fileTypeEnum == null) {
                throw new ServiceException("文件类型值[" + fileType + "]不在定义的枚举范围内");
            }
        }

        // 校验算法类型是否在枚举中定义
        if (algorithmType != null) {
            AlgorithmTypeEnum algorithmTypeEnum = AlgorithmTypeEnum.getByCode(algorithmType);
            if (algorithmTypeEnum == null) {
                throw new ServiceException("算法类型值[" + algorithmType + "]不在定义的枚举范围内");
            }
        }

        List<EquipmentFile> equipmentFiles = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long fileId : fileIds) {
            EquipmentFile equipmentFile = new EquipmentFile();
            equipmentFile.setEquipmentId(equipmentId);
            equipmentFile.setFileId(fileId);
            equipmentFile.setFileType(fileType);
            equipmentFile.setAlgorithmType(algorithmType);
            equipmentFile.setCreateTime(now);

            // 再次校验每个对象
            validEntityBeforeSave(equipmentFile);
            equipmentFiles.add(equipmentFile);
        }

        return baseMapper.insertBatch(equipmentFiles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateByEquipmentId(Long equipmentId, List<Long> fileIds, Integer fileType, Integer algorithmType) {
        if (equipmentId == null) {
            return false;
        }

        // 先删除原有的关联记录
        baseMapper.deleteByEquipmentId(equipmentId);

        // 如果有新的文件ID列表，则批量插入
        if (fileIds != null && !fileIds.isEmpty()) {
            return batchInsertByEquipmentId(equipmentId, fileIds, fileType, algorithmType);
        }

        return true;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(EquipmentFile entity) {
        // 校验文件类型是否在枚举中定义
        if (entity.getFileType() != null) {
            EquipmentFileTypeEnum fileTypeEnum = EquipmentFileTypeEnum.getByCode(entity.getFileType());
            if (fileTypeEnum == null) {
                throw new ServiceException("文件类型值[" + entity.getFileType() + "]不在定义的枚举范围内");
            }
        }

        // 校验算法类型是否在枚举中定义
        if (entity.getAlgorithmType() != null) {
            AlgorithmTypeEnum algorithmTypeEnum = AlgorithmTypeEnum.getByCode(entity.getAlgorithmType());
            if (algorithmTypeEnum == null) {
                throw new ServiceException("算法类型值[" + entity.getAlgorithmType() + "]不在定义的枚举范围内");
            }
        }

        // 校验设备ID和文件ID是否重复
        if (entity.getEquipmentId() != null && entity.getFileId() != null) {
            LambdaQueryWrapper<EquipmentFile> lqw = Wrappers.lambdaQuery();
            lqw.eq(EquipmentFile::getEquipmentId, entity.getEquipmentId());
            lqw.eq(EquipmentFile::getFileId, entity.getFileId());
            if (entity.getId() != null) {
                lqw.ne(EquipmentFile::getId, entity.getId());
            }
            if (baseMapper.selectCount(lqw) > 0) {
                throw new ServiceException("该设备已关联此文件");
            }
        }
    }
}
