package io.choerodon.test.manager.app.service.impl;

import io.choerodon.agile.api.dto.CopyConditionDTO;
import io.choerodon.agile.api.dto.IssueCreateDTO;
import io.choerodon.agile.api.dto.IssueDTO;
import io.choerodon.agile.api.dto.SearchDTO;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.test.manager.api.dto.*;
import io.choerodon.test.manager.app.service.ReporterFormService;
import io.choerodon.test.manager.app.service.TestCaseService;
import io.choerodon.test.manager.app.service.TestIssueFolderRelService;
import io.choerodon.test.manager.app.service.TestIssueFolderService;
import io.choerodon.test.manager.domain.service.ITestIssueFolderRelService;
import io.choerodon.test.manager.domain.test.manager.entity.TestIssueFolderRelE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by zongw.lee@gmail.com on 08/31/2018
 */
@Component
public class TestIssueFolderServiceRelImpl implements TestIssueFolderRelService {

    @Autowired
    ITestIssueFolderRelService iTestIssueFolderRelService;

    @Autowired
    TestCaseService testCaseService;

    @Autowired
    TestIssueFolderService testIssueFolderService;

    @Autowired
    ReporterFormService reporterFormService;

    @Override
    public Page<IssueComponentDetailFolderRelDTO> queryIssuesById(Long projectId, Long versionId, Long folderId, Long[] issueIds) {
        TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO(folderId, null, projectId, null, null);
        List<TestIssueFolderRelDTO> resultRelDTOS = new ArrayList<>();
        for (Long issueId : issueIds) {
            testIssueFolderRelDTO.setIssueId(issueId);
            resultRelDTOS.add(ConvertHelper.convert(iTestIssueFolderRelService.queryOne(ConvertHelper
                    .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class));
        }
        if (ObjectUtils.isEmpty(resultRelDTOS)) {
            return new Page<>();
        }
        List<IssueComponentDetailFolderRelDTO> issueComponentDetailFolderRelDTOS = new ArrayList<>();
        Map<Long, IssueInfosDTO> map = testCaseService.getIssueInfoMap(projectId, issueIds, true);
        if (ObjectUtils.isEmpty(map)) {
            return new Page<>();
        }
        for (TestIssueFolderRelDTO resultRelDTO : resultRelDTOS) {
            if (resultRelDTO != null && map.containsKey(resultRelDTO.getIssueId())) {
                IssueComponentDetailFolderRelDTO issueComponentDetailFolderRelDTO = new IssueComponentDetailFolderRelDTO(map.get(resultRelDTO.getIssueId()));
                issueComponentDetailFolderRelDTO.setObjectVersionNumber(resultRelDTO.getObjectVersionNumber());
                issueComponentDetailFolderRelDTOS.add(issueComponentDetailFolderRelDTO);
            }
        }
        Page page = new Page();
        page.setContent(issueComponentDetailFolderRelDTOS);
        return page;
    }

    @Override
    public Page<IssueComponentDetailFolderRelDTO> query(Long projectId, Long folderId, TestFolderRelQueryDTO testFolderRelQueryDTO, PageRequest pageRequest) {
        SearchDTO searchDTO = Optional.ofNullable(testFolderRelQueryDTO.getSearchDTO()).orElseGet(SearchDTO::new);
        //查询出所属的issue
        List<TestIssueFolderRelDTO> resultRelDTOS = new ArrayList<>();
        if (testFolderRelQueryDTO.getVersionIds() == null || testFolderRelQueryDTO.getVersionIds().length == 0) {
            TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO(folderId, null, projectId, null, null);
            resultRelDTOS.addAll(ConvertHelper.convertList(iTestIssueFolderRelService.query(ConvertHelper
                    .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class));
        } else {
            for (Long versionId : testFolderRelQueryDTO.getVersionIds()) {
                TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO(folderId, versionId, projectId, null, null);
                resultRelDTOS.addAll(ConvertHelper.convertList(iTestIssueFolderRelService.query(ConvertHelper
                        .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class));
            }
        }

        if (ObjectUtils.isEmpty(resultRelDTOS)) {
            return new Page<>();
        }
        //先去过滤issues
        //将list中issuedId转换为Long数组
        Long[] issueIds = resultRelDTOS.stream().map(TestIssueFolderRelDTO::getIssueId).toArray(Long[]::new);
        //将issueId放入searchDTO中
        Map args = Optional.ofNullable(searchDTO.getOtherArgs()).orElseGet(HashMap::new);
        args.put("issueIds", issueIds);
        if (searchDTO.getOtherArgs() == null) {
            searchDTO.setOtherArgs(args);
        }
        //返回过滤后的issueIds，进行分页
        Long[] allFilteredIssues = testCaseService.queryIssueIdsByOptions(searchDTO, projectId).stream().toArray(Long[]::new);
        if (ObjectUtils.isEmpty(allFilteredIssues)) {
            return new Page<>();
        }
        int pageNum = pageRequest.getPage();
        int pageSize = pageRequest.getSize();
        int highPage = (pageNum + 1) * pageSize;
        int lowPage = pageNum * pageSize;
        //创建一个Long数组，将对应分页的issuesId传给它
        int size = highPage >= allFilteredIssues.length ? allFilteredIssues.length - lowPage : pageSize;
        Long[] pagedIssues = new Long[size];
        System.arraycopy(allFilteredIssues, lowPage, pagedIssues, 0, size);
        return new CustomPage(queryIssuesById(projectId, null, folderId, pagedIssues).stream().collect(Collectors.toList()), allFilteredIssues);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TestIssueFolderRelDTO insertTestAndRelationship(IssueCreateDTO issueCreateDTO, Long projectId, Long folderId, Long versionId) {
        Long newFolderId = getDefaultFolderId(projectId, folderId, versionId);
        IssueDTO issueDTO = testCaseService.createTest(issueCreateDTO, projectId);
        TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO(newFolderId, versionId, projectId, issueDTO.getIssueId(), null);
        return ConvertHelper.convert(iTestIssueFolderRelService.insert(ConvertHelper
                .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<TestIssueFolderRelDTO> insertBatchRelationship(Long projectId, List<TestIssueFolderRelDTO> testIssueFolderRelDTOS) {
        List<TestIssueFolderRelDTO> resultTestIssueFolderRelDTOS = new ArrayList<>();
        Long newFolderId = null;
        if (!ObjectUtils.isEmpty(testIssueFolderRelDTOS)) {
            newFolderId = getDefaultFolderId(projectId, testIssueFolderRelDTOS.get(0).getFolderId(), testIssueFolderRelDTOS.get(0).getVersionId());
        }
        for (TestIssueFolderRelDTO testIssueFolderRelDTO : testIssueFolderRelDTOS) {
            testIssueFolderRelDTO.setFolderId(newFolderId);
            testIssueFolderRelDTO.setProjectId(projectId);
            TestIssueFolderRelDTO resultTestIssueFolderRelDTO = ConvertHelper.convert(iTestIssueFolderRelService.insert(ConvertHelper
                    .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);
            resultTestIssueFolderRelDTOS.add(resultTestIssueFolderRelDTO);
        }
        return resultTestIssueFolderRelDTOS;
    }

    private Long getDefaultFolderId(Long projectId, Long folderId, Long versionId) {
        if (folderId == null) {
            return testIssueFolderService.getDefaultFolderId(projectId, versionId);
        } else {
            return folderId;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long projectId, List<Long> issuesId) {
        TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO();
        for (Long issueId : issuesId) {
            testIssueFolderRelDTO.setIssueId(issueId);
            iTestIssueFolderRelService.delete(ConvertHelper
                    .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class));
        }
        testCaseService.batchDeleteIssues(projectId, issuesId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TestIssueFolderRelDTO updateVersionByFolderWithoutLockAndChangeIssueVersion(TestIssueFolderRelDTO testIssueFolderRelDTO, List<Long> issues) {
        TestIssueFolderRelDTO resTestIssueFolderRelDTO = ConvertHelper.convert(iTestIssueFolderRelService.updateVersionByFolderWithNoLock(ConvertHelper
                .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);
        testCaseService.batchIssueToVersionTest(testIssueFolderRelDTO.getProjectId(), testIssueFolderRelDTO.getVersionId(), issues);
        return resTestIssueFolderRelDTO;
    }

    @Override
    public List<TestIssueFolderRelDTO> queryByFolder(TestIssueFolderRelDTO testIssueFolderRelDTO) {
        return ConvertHelper.convertList(iTestIssueFolderRelService.query(ConvertHelper
                .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);
    }

    @Override
    public TestIssueFolderRelDTO cloneOneIssue(Long projectId, Long issueId, CopyConditionDTO copyConditionDTO) {
        TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO();
        testIssueFolderRelDTO.setIssueId(issueId);

        TestIssueFolderRelDTO resTestIssueFolderRelDTO = ConvertHelper.convert(iTestIssueFolderRelService.queryOne(
                ConvertHelper.convert(testIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);

        IssueDTO issueDTO = testCaseService.cloneIssueByIssueId(projectId, issueId, copyConditionDTO);
        if (ObjectUtils.isEmpty(issueDTO)) {
            return new TestIssueFolderRelDTO();
        }
        resTestIssueFolderRelDTO.setIssueId(issueDTO.getIssueId());
        resTestIssueFolderRelDTO.setId(null);
        return ConvertHelper.convert(iTestIssueFolderRelService.insert(
                ConvertHelper.convert(resTestIssueFolderRelDTO, TestIssueFolderRelE.class)), TestIssueFolderRelDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void moveFolderIssue(Long projectId, Long versionId, Long folderId, List<IssueInfosDTO> issueInfosDTOS) {
        TestIssueFolderRelDTO testIssueFolderRelDTO = new TestIssueFolderRelDTO(folderId, versionId, projectId, null, null);
        for (IssueInfosDTO issueInfosDTO : issueInfosDTOS) {
            testIssueFolderRelDTO.setObjectVersionNumber(issueInfosDTO.getObjectVersionNumber());
            testIssueFolderRelDTO.setIssueId(issueInfosDTO.getIssueId());
            iTestIssueFolderRelService.updateFolderByIssue(ConvertHelper
                    .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class));
        }
        testCaseService.batchIssueToVersionTest(projectId, versionId, issueInfosDTOS.stream().map(IssueInfosDTO::getIssueId).collect(Collectors.toList()));
    }

    /*
     * @param projectId
     * @param versionId 目标version
     * @param folderId 目标folder
     * @param issueInfosDTOS 插入成功的issue信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyIssue(Long projectId, Long versionId, Long folderId, List<IssueInfosDTO> issueInfosDTOS) {
        TestIssueFolderRelDTO testIssueFolderRelDTO;
        testIssueFolderRelDTO = new TestIssueFolderRelDTO(folderId, versionId, projectId, null, null);
        //远程服务复制issue，得到远程issue的ids
        if(!ObjectUtils.isEmpty(issueInfosDTOS)) {
            List<Long> issuesId = testCaseService.batchCloneIssue(projectId, versionId,
                    issueInfosDTOS.stream().map(IssueInfosDTO::getIssueId).toArray(Long[]::new));
            for (Long id : issuesId) {
                //插入issue与folder的关联
                testIssueFolderRelDTO.setIssueId(id);
                iTestIssueFolderRelService.insert(ConvertHelper
                        .convert(testIssueFolderRelDTO, TestIssueFolderRelE.class));
            }
        }
    }

}