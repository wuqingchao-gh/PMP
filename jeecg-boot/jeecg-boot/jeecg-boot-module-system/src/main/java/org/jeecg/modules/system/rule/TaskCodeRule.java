package org.jeecg.modules.system.rule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.handler.IFillRuleHandler;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.util.YouBianCodeUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.project.protree.entity.PmpProject;
import org.jeecg.modules.project.protree.mapper.PmpProjectMapper;
import org.jeecg.modules.system.entity.SysCategory;
import org.jeecg.modules.system.mapper.SysCategoryMapper;

import java.util.List;

/**
 * @Author scott
 * @Date 2019/12/9 11:32
 * @Description: 分类字典编码生成规则
 */
public class TaskCodeRule implements IFillRuleHandler {

    public static final String ROOT_PID_VALUE = "0";

    @Override
    public Object execute(JSONObject params, JSONObject formData) {

        String categoryPid = ROOT_PID_VALUE;
        String categoryCode = null;

        if (formData != null && formData.size() > 0) {
            Object obj = formData.get("parentnode");
            if (oConvertUtils.isNotEmpty(obj)) categoryPid = obj.toString();
        } else {
            if (params != null) {
                Object obj = params.get("parentnode");
                if (oConvertUtils.isNotEmpty(obj)) categoryPid = obj.toString();
            }
        }

        /*
         * 分成三种情况
         * 1.数据库无数据 调用YouBianCodeUtil.getNextYouBianCode(null);
         * 2.添加子节点，无兄弟元素 YouBianCodeUtil.getSubYouBianCode(parentCode,null);
         * 3.添加子节点有兄弟元素 YouBianCodeUtil.getNextYouBianCode(lastCode);
         * */
        //找同类 确定上一个最大的code值
        LambdaQueryWrapper<PmpProject> query = new LambdaQueryWrapper<PmpProject>().eq(PmpProject::getParentnode, categoryPid).orderByDesc(PmpProject::getParentnode);
        PmpProjectMapper baseMapper = (PmpProjectMapper) SpringContextUtils.getBean(PmpProjectMapper.class);
        List<PmpProject> list = baseMapper.selectList(query);
        if (list == null || list.size() == 0) {
            if (ROOT_PID_VALUE.equals(categoryPid)) {
                //情况1
                categoryCode = YouBianCodeUtil.getNextYouBianCode(null);
            } else {
                //情况2
                PmpProject parent = (PmpProject) baseMapper.selectById(categoryPid);
                categoryCode = YouBianCodeUtil.getSubYouBianCode(parent.getProjectcode(), null);
            }
        } else {
            //情况3
            categoryCode = YouBianCodeUtil.getNextYouBianCode(list.get(0).getProjectcode());
        }
        return categoryCode;
    }
}
