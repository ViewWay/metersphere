package io.metersphere.service;

import io.metersphere.api.dto.DeleteAPITestRequest;
import io.metersphere.api.dto.QueryAPITestRequest;
import io.metersphere.api.service.APITestService;
import io.metersphere.base.domain.*;
import io.metersphere.base.mapper.ApiTestFileMapper;
import io.metersphere.base.mapper.LoadTestFileMapper;
import io.metersphere.base.mapper.LoadTestMapper;
import io.metersphere.base.mapper.ProjectMapper;
import io.metersphere.base.mapper.ext.ExtProjectMapper;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.ServiceUtils;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.controller.request.ProjectRequest;
import io.metersphere.dto.ProjectDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.performance.request.DeleteTestPlanRequest;
import io.metersphere.performance.request.QueryProjectFileRequest;
import io.metersphere.performance.service.PerformanceTestService;
import io.metersphere.track.service.TestCaseService;
import io.metersphere.track.service.TestPlanProjectService;
import io.metersphere.track.service.TestPlanService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ProjectService {
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ExtProjectMapper extProjectMapper;
    @Resource
    private PerformanceTestService performanceTestService;
    @Resource
    private LoadTestMapper loadTestMapper;
    @Resource
    private TestPlanService testPlanService;
    @Resource
    private TestCaseService testCaseService;
    @Resource
    private APITestService apiTestService;
    @Resource
    private TestPlanProjectService testPlanProjectService;
    @Resource
    private FileService fileService;
    @Resource
    private LoadTestFileMapper loadTestFileMapper;
    @Resource
    private ApiTestFileMapper apiTestFileMapper;

    public Project addProject(Project project) {
        if (StringUtils.isBlank(project.getName())) {
            MSException.throwException(Translator.get("project_name_is_null"));
        }
        ProjectExample example = new ProjectExample();
        example.createCriteria()
                .andWorkspaceIdEqualTo(SessionUtils.getCurrentWorkspaceId())
                .andNameEqualTo(project.getName());
        if (projectMapper.countByExample(example) > 0) {
            MSException.throwException(Translator.get("project_name_already_exists"));
        }
        project.setId(UUID.randomUUID().toString());
        long createTime = System.currentTimeMillis();
        project.setCreateTime(createTime);
        project.setUpdateTime(createTime);
        // set workspace id
        project.setWorkspaceId(SessionUtils.getCurrentWorkspaceId());
        projectMapper.insertSelective(project);
        return project;
    }

    public List<ProjectDTO> getProjectList(ProjectRequest request) {
        if (StringUtils.isNotBlank(request.getName())) {
            request.setName(StringUtils.wrapIfMissing(request.getName(), "%"));
        }
        request.setOrders(ServiceUtils.getDefaultOrder(request.getOrders()));
        return extProjectMapper.getProjectWithWorkspace(request);
    }

    public void deleteProject(String projectId) {
        // delete test
        LoadTestExample loadTestExample = new LoadTestExample();
        loadTestExample.createCriteria().andProjectIdEqualTo(projectId);
        List<LoadTest> loadTests = loadTestMapper.selectByExample(loadTestExample);
        List<String> loadTestIdList = loadTests.stream().map(LoadTest::getId).collect(Collectors.toList());
        loadTestIdList.forEach(loadTestId -> {
            DeleteTestPlanRequest deleteTestPlanRequest = new DeleteTestPlanRequest();
            deleteTestPlanRequest.setId(loadTestId);
            deleteTestPlanRequest.setForceDelete(true);
            performanceTestService.delete(deleteTestPlanRequest);
        });

        // 删除项目下 测试跟踪 相关
        deleteTrackResourceByProjectId(projectId);

        // 删除项目下 接口测试 相关
        deleteAPIResourceByProjectId(projectId);
        // delete project
        projectMapper.deleteByPrimaryKey(projectId);
    }

    private void deleteTrackResourceByProjectId(String projectId) {
        List<String> testPlanIds = testPlanProjectService.getPlanIdByProjectId(projectId);
        if (!CollectionUtils.isEmpty(testPlanIds)) {
            testPlanIds.forEach(testPlanId -> {
                testPlanService.deleteTestPlan(testPlanId);
            });
        }
        testCaseService.deleteTestCaseByProjectId(projectId);
    }

    private void deleteAPIResourceByProjectId(String projectId) {
        QueryAPITestRequest request = new QueryAPITestRequest();
        request.setProjectId(projectId);
        apiTestService.list(request).forEach(test -> {
            DeleteAPITestRequest deleteAPITestRequest = new DeleteAPITestRequest();
            deleteAPITestRequest.setId(test.getId());
            deleteAPITestRequest.setForceDelete(true);
            apiTestService.delete(deleteAPITestRequest);
        });
    }

    public void updateProject(Project project) {
        project.setCreateTime(null);
        project.setUpdateTime(System.currentTimeMillis());
        checkProjectExist(project);
        projectMapper.updateByPrimaryKeySelective(project);
    }

    private void checkProjectExist(Project project) {
        if (project.getName() != null) {
            ProjectExample example = new ProjectExample();
            example.createCriteria()
                    .andNameEqualTo(project.getName())
                    .andWorkspaceIdEqualTo(SessionUtils.getCurrentWorkspaceId())
                    .andIdNotEqualTo(project.getId());
            if (projectMapper.selectByExample(example).size() > 0) {
                MSException.throwException(Translator.get("project_name_already_exists"));
            }
        }
    }

    public List<Project> listAll() {
        return projectMapper.selectByExample(null);
    }

    public List<Project> getRecentProjectList(ProjectRequest request) {
        ProjectExample example = new ProjectExample();
        ProjectExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(request.getWorkspaceId())) {
            criteria.andWorkspaceIdEqualTo(request.getWorkspaceId());
        }
        // 按照修改时间排序
        example.setOrderByClause("update_time desc");
        return projectMapper.selectByExample(example);
    }

    public Project getProjectById(String id) {
        return projectMapper.selectByPrimaryKey(id);
    }

    public void uploadFiles(String projectId, List<MultipartFile> files) {
        if (files != null) {
            for (MultipartFile file : files) {
                QueryProjectFileRequest request = new QueryProjectFileRequest();
                request.setFilename(file.getOriginalFilename());
                if (CollectionUtils.isEmpty(fileService.getProjectFiles(projectId, request))) {
                    fileService.saveFile(file, projectId);
                } else {
                    MSException.throwException(Translator.get("project_file_already_exists"));
                }
            }
        }
    }

    public void deleteFile(String fileId) {
        LoadTestFileExample example1 = new LoadTestFileExample();
        example1.createCriteria().andFileIdEqualTo(fileId);
        List<LoadTestFile> loadTestFiles = loadTestFileMapper.selectByExample(example1);
        String errorMessage = "";
        if (loadTestFiles.size() > 0) {
            List<String> testIds = loadTestFiles.stream().map(LoadTestFile::getTestId).distinct().collect(Collectors.toList());
            LoadTestExample example = new LoadTestExample();
            example.createCriteria().andIdIn(testIds);
            List<LoadTest> loadTests = loadTestMapper.selectByExample(example);
            errorMessage += Translator.get("load_test") + ": " + StringUtils.join(loadTests.stream().map(LoadTest::getName).toArray(), ",");
            errorMessage += "\n";
        }
        ApiTestFileExample example2 = new ApiTestFileExample();
        example2.createCriteria().andFileIdEqualTo(fileId);
        List<ApiTestFile> apiTestFiles = apiTestFileMapper.selectByExample(example2);
        if (apiTestFiles.size() > 0) {
            List<String> testIds = apiTestFiles.stream().map(ApiTestFile::getTestId).distinct().collect(Collectors.toList());
            LoadTestExample example = new LoadTestExample();
            example.createCriteria().andIdIn(testIds);
            QueryAPITestRequest request = new QueryAPITestRequest();
            request.setIds(testIds);
            List<ApiTest> apiTests = apiTestService.listByIds(request);
            errorMessage += Translator.get("api_test") + ": " + StringUtils.join(apiTests.stream().map(ApiTest::getName).toArray(), ",");
        }
        if (StringUtils.isNotBlank(errorMessage)) {
            MSException.throwException(errorMessage + Translator.get("project_file_in_use"));
        }
        fileService.deleteFileById(fileId);
    }
}
