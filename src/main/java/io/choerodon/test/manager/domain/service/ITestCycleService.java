package io.choerodon.test.manager.domain.service;

import io.choerodon.test.manager.domain.test.manager.entity.TestCycleE;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

/**
 * Created by 842767365@qq.com on 6/11/18.
 */
public interface ITestCycleService {
    TestCycleE insert(TestCycleE testCycleE);

    void delete(TestCycleE testCycleE);

    List<TestCycleE> update(List<TestCycleE> testCycleE);

    Page<TestCycleE> query(TestCycleE testCycleE, PageRequest pageRequest);

    List<TestCycleE> querySubCycle(TestCycleE testCycleE);

    List<TestCycleE> getTestCycle(Long versionId);

	List<TestCycleE> sort(List<TestCycleE> testCycleES);

    List<TestCycleE> queryCycleWithBar(Long versionId);

	/**
	 * 查找Cycle存放默认路径
	 *
	 * @return
	 */
	Long findDefaultCycle(Long projectId);
}
