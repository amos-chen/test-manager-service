package io.choerodon.test.manager.api.controller.v1;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.test.manager.api.dto.TestCycleCaseDTO;
import io.choerodon.test.manager.app.service.TestCycleCaseService;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Optional;

/**
 * Created by 842767365@qq.com on 6/12/18.
 */
@RestController
@RequestMapping(value = "/v1/cycle/case")
public class TestCycleCaseController {

    @Autowired
    TestCycleCaseService testCycleCaseService;

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("删除测试循环用例")
    @DeleteMapping
    public ResponseEntity delete(Long cycleCaseId) {
        testCycleCaseService.delete(cycleCaseId);
        return new ResponseEntity<>(true, HttpStatus.NO_CONTENT);
    }


    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("查询测试组下循环用例")
    @GetMapping("/query/{cycleId}")
    public ResponseEntity<Page<TestCycleCaseDTO>> queryByCycle(@PathVariable(name = "cycleId") Long cycleId,
                                                               @ApiIgnore
                                                               @ApiParam(value = "分页信息", required = true)
                                                               @SortDefault(value = "rank", direction = Sort.Direction.DESC)
                                                                       PageRequest pageRequest) {
        return Optional.ofNullable(testCycleCaseService.queryByCycle(cycleId, pageRequest))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.testCycleCase.query.cycleId"));
    }

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("查询一个循环用例")
    @GetMapping("/query/one/{executeId}")
    public ResponseEntity<TestCycleCaseDTO> queryOne(@PathVariable(name = "executeId") Long executeId) {
        return Optional.ofNullable(testCycleCaseService.queryOne(executeId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.testCycleCase.query.executeId"));
    }


    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("增加一个测试组下循环用例")
    @PostMapping("/{projectId}/insert")
    public ResponseEntity insertOneCase(@RequestBody TestCycleCaseDTO testCycleCaseDTO, @PathVariable(name = "projectId") Long projectId) {
        return Optional.ofNullable(testCycleCaseService.create(testCycleCaseDTO, projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.testCycleCase.insert"));

    }

    @Permission(level = ResourceLevel.PROJECT)
    @ApiOperation("修改一个测试组下循环用例")
    @PostMapping("/{projectId}/update")
    public ResponseEntity updateOneCase(@RequestBody TestCycleCaseDTO testCycleCaseDTO,
                                        @PathVariable(name = "projectId") Long projectId) {
        return Optional.ofNullable(testCycleCaseService.changeOneCase(testCycleCaseDTO, projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.testCycleCase.update"));
    }

	@Permission(level = ResourceLevel.PROJECT)
	@ApiOperation("获取时间内case活跃度")
	@PostMapping("/{projectId}/range")
	public ResponseEntity getActiveCase(Long range, Long projectId, String day) {
		return Optional.ofNullable(testCycleCaseService.getActiveCase(range, projectId, day))
				.map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
				.orElseThrow(() -> new CommonException("error.testCycleCase.get.range"));
	}

}
