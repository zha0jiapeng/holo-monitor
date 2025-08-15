package org.dromara.job.snailjob;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.job.core.MapHandler;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.annotation.MapExecutor;
import com.aizuda.snailjob.client.job.core.annotation.ReduceExecutor;
import com.aizuda.snailjob.client.job.core.dto.MapArgs;
import com.aizuda.snailjob.client.job.core.dto.ReduceArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MapReduce任务 动态分配 分片后合并结果
 * <a href="https://juejin.cn/post/7448551286506913802"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "testMapReduceAnnotation1")
public class TestMapReduceAnnotation1 {

    @MapExecutor
    public ExecuteResult rootMapExecute(MapArgs mapArgs, MapHandler mapHandler) {
        int partitionSize = 50;
        List<List<Integer>> partition = IntStream.rangeClosed(1, 200)
                .boxed()
                .collect(Collectors.groupingBy(i -> (i - 1) / partitionSize))
                .values()
                .stream()
                .toList();
        SnailJobLog.REMOTE.info("端口:{}完成分配任务", SpringUtil.getProperty("server.port"));
        return mapHandler.doMap(partition, "doCalc");
    }

    @MapExecutor(taskName = "doCalc")
    public ExecuteResult doCalc(MapArgs mapArgs) {
        List<Integer> sourceList = (List<Integer>) mapArgs.getMapResult();
        // 遍历sourceList的每一个元素,计算出一个累加值partitionTotal
        int partitionTotal = sourceList.stream().mapToInt(i -> i).sum();
        // 打印日志到服务器
        ThreadUtil.sleep(3, TimeUnit.SECONDS);
        SnailJobLog.REMOTE.info("端口:{},partitionTotal:{}", SpringUtil.getProperty("server.port"), partitionTotal);
        return ExecuteResult.success(partitionTotal);
    }

    @ReduceExecutor
    public ExecuteResult reduceExecute(ReduceArgs reduceArgs) {
        int reduceTotal = reduceArgs.getMapResult().stream().mapToInt(i -> Integer.parseInt((String) i)).sum();
        SnailJobLog.REMOTE.info("端口:{},reduceTotal:{}", SpringUtil.getProperty("server.port"), reduceTotal);
        return ExecuteResult.success(reduceTotal);
    }
}
