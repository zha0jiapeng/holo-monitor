package org.dromara.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.service.IFlwCommonService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 办理人权限处理器
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Component
@Slf4j
public class WorkflowPermissionHandler implements PermissionHandler {

    private final IFlwCommonService flwCommonService;

    /**
     * 办理人权限标识，比如用户，角色，部门等，用于校验是否有权限办理任务
     * 后续在{@link FlowParams#getPermissionFlag}  中获取
     * 返回当前用户权限集合
     */
    @Override
    public List<String> permissions() {
        return Collections.singletonList(LoginHelper.getUserIdStr());
    }

    /**
     * 获取当前办理人
     *
     * @return 当前办理人
     */
    @Override
    public String getHandler() {
        return LoginHelper.getUserIdStr();
    }

    /**
     * 转换办理人，比如设计器中预设了能办理的人，如果其中包含角色或者部门id等，可以通过此接口进行转换成用户id
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        if (CollUtil.isNotEmpty(permissions)) {
            permissions = flwCommonService.buildUser(permissions);
        }
        return permissions;
    }
}
