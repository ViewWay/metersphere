<template>
  <div class="card-container">
    <el-card class="card-content" v-loading="result.loading">
      <template v-slot:header>
        <test-plan-case-list-header
          :project-id="getProjectId()"
          :condition="condition"
          :plan-id="planId"
          @refresh="initTable"
          @relevanceCase="$emit('relevanceCase')"
          @setEnvironment="setEnvironment"
          v-if="isPlanModel"/>
      </template>

      <el-table v-loading="result.loading"
                border
                :data="tableData" row-key="id" class="test-content adjust-table"
                @select-all="handleSelectAll"
                @filter-change="filter"
                @sort-change="sort"
                @select="handleSelect" :height="screenHeight">
        <el-table-column type="selection"/>
        <el-table-column width="40" :resizable="false" align="center">
          <template v-slot:default="scope">
            <show-more-btn :is-show="scope.row.showMore && !isReadOnly" :buttons="buttons" :size="selectRows.size"/>
          </template>
        </el-table-column>
        <template v-for="(item, index) in tableLabel">
          <el-table-column v-if="item.id == 'num'" prop="num" sortable="custom" label="ID" show-overflow-tooltip
                           :key="index"/>
          <el-table-column v-if="item.id == 'name'" prop="name" sortable="custom"
                           :label="$t('api_test.definition.api_name')" show-overflow-tooltip :key="index"/>

          <el-table-column
            v-if="item.id == 'priority'"
            prop="priority"
            :filters="priorityFilters"
            sortable="custom"
            column-key="priority"
            :label="$t('test_track.case.priority')"
            show-overflow-tooltip
            :key="index">
            <template v-slot:default="scope">
              <priority-table-item :value="scope.row.priority"/>
            </template>
          </el-table-column>

          <el-table-column
            v-if="item.id == 'path'"
            prop="path"
            :label="$t('api_test.definition.api_path')"
            show-overflow-tooltip
            :key="index"/>

          <el-table-column
            v-if="item.id == 'createUser'"
            prop="createUser"
            column-key="user_id"
            sortable="custom"
            :filters="userFilters"
            :label="'创建人'"
            show-overflow-tooltip
            :key="index"/>

          <el-table-column
            v-if="item.id == 'custom'"
            sortable="custom"
            width="160"
            :label="$t('api_test.definition.api_last_time')"
            prop="updateTime"
            :key="index">
            <template v-slot:default="scope">
              <span>{{ scope.row.updateTime | timestampFormatDate }}</span>
            </template>
          </el-table-column>

          <el-table-column
            v-if="item.id == 'tags'"
            prop="tags"
            :label="$t('commons.tag')"
            :key="index">
            <template v-slot:default="scope">
                <ms-tag v-for="(itemName,index)  in scope.row.tags" :key="index" type="success" effect="plain" :content="itemName" style="margin-left: 5px"/>
            </template>
          </el-table-column>

          <el-table-column v-if="item.id == 'execResult'" :label="'执行状态'" min-width="130" align="center" :key="index">
            <template v-slot:default="scope">
              <div v-loading="rowLoading === scope.row.id">
                <el-link type="danger"
                         v-if="scope.row.execResult && scope.row.execResult === 'error'"
                         @click="getReportResult(scope.row)" v-text="getResult(scope.row.execResult)"/>
                <el-link v-else-if="scope.row.execResult && scope.row.execResult === 'success'"
                         @click="getReportResult(scope.row)" v-text="getResult(scope.row.execResult)">

                </el-link>
                <div v-else v-text="getResult(scope.row.execResult)"/>

                <div v-if="scope.row.id" style="color: #999999;font-size: 12px">
                  <span> {{ scope.row.updateTime | timestampFormatDate }}</span>
                  {{ scope.row.updateUser }}
                </div>
              </div>
            </template>
          </el-table-column>
        </template>
        <el-table-column v-if="!isReadOnly" :label="$t('commons.operating')" align="center">
          <template slot="header">
            <header-label-operate @exec="customHeader"/>
          </template>
          <template v-slot:default="scope">
            <ms-table-operator-button class="run-button" :is-tester-permission="true" :tip="$t('api_test.run')"
                                      icon="el-icon-video-play"
                                      @exec="singleRun(scope.row)" v-tester/>
            <ms-table-operator-button :is-tester-permission="true" :tip="$t('test_track.plan_view.cancel_relevance')"
                                      icon="el-icon-unlock" type="danger" @exec="handleDelete(scope.row)" v-tester/>
          </template>
        </el-table-column>

      </el-table>
      <header-custom ref="headerCustom" :initTableData="initTable" :optionalFields=headerItems
                     :type=type></header-custom>
      <ms-table-pagination :change="initTable" :current-page.sync="currentPage" :page-size.sync="pageSize"
                           :total="total"/>

      <test-plan-api-case-result :response="response" ref="apiCaseResult"/>

      <!-- 执行组件 -->
      <ms-run :debug="false" :type="'API_PLAN'" :reportId="reportId" :run-data="runData"
              @runRefresh="runRefresh" ref="runTest"/>

      <!-- 批量编辑 -->
      <batch-edit :dialog-title="$t('test_track.case.batch_edit_case')" :type-arr="typeArr" :value-arr="valueArr"
                  :select-row="selectRows" ref="batchEdit" @batchEdit="batchEdit"/>

    </el-card>
  </div>

</template>

<script>

import MsTableOperator from "../../../../../common/components/MsTableOperator";
import MsTableOperatorButton from "../../../../../common/components/MsTableOperatorButton";
import MsTablePagination from "../../../../../common/pagination/TablePagination";
import MsTag from "../../../../../common/components/MsTag";
import MsApiCaseList from "../../../../../api/definition/components/case/ApiCaseList";
import ApiCaseList from "../../../../../api/definition/components/case/ApiCaseList";
import MsContainer from "../../../../../common/components/MsContainer";
import MsBottomContainer from "../../../../../api/definition/components/BottomContainer";
import ShowMoreBtn from "../../../../case/components/ShowMoreBtn";
import BatchEdit from "@/business/components/track/case/components/BatchEdit";
import {API_METHOD_COLOUR, CASE_PRIORITY, RESULT_MAP} from "../../../../../api/definition/model/JsonData";
import {getCurrentProjectID, strMapToObj} from "@/common/js/utils";
import ApiListContainer from "../../../../../api/definition/components/list/ApiListContainer";
import PriorityTableItem from "../../../../common/tableItems/planview/PriorityTableItem";
import {getBodyUploadFiles, getUUID} from "../../../../../../../common/js/utils";
import TestPlanCaseListHeader from "./TestPlanCaseListHeader";
import MsRun from "../../../../../api/definition/components/Run";
import TestPlanApiCaseResult from "./TestPlanApiCaseResult";
import TestPlan from "../../../../../api/definition/components/jmeter/components/test-plan";
import ThreadGroup from "../../../../../api/definition/components/jmeter/components/thread-group";
import {TEST_PLAN_API_CASE, WORKSPACE_ID} from "@/common/js/constants";
import {_filter, _sort, getLabel} from "@/common/js/tableUtils";
import HeaderCustom from "@/business/components/common/head/HeaderCustom";
import {Test_Plan_Api_Case} from "@/business/components/common/model/JsonData";
import HeaderLabelOperate from "@/business/components/common/head/HeaderLabelOperate";


export default {
  name: "TestPlanApiCaseList",
  components: {
    BatchEdit,
    HeaderLabelOperate,
    HeaderCustom,
    TestPlanApiCaseResult,
    MsRun,
    TestPlanCaseListHeader,
    ApiCaseList,
    PriorityTableItem,
    ApiListContainer,
    MsTableOperatorButton,
    MsTableOperator,
    MsTablePagination,
    MsTag,
    MsApiCaseList,
    MsContainer,
    MsBottomContainer,
    ShowMoreBtn,
  },
  data() {
    return {
      type: TEST_PLAN_API_CASE,
      headerItems: Test_Plan_Api_Case,
      tableLabel: Test_Plan_Api_Case,
      condition: {},
      selectCase: {},
      result: {},
      moduleId: "",
      status: 'default',
      deletePath: "/test/case/delete",
      selectRows: new Set(),
      buttons: [
        {name: this.$t('test_track.case.batch_unlink'), handleClick: this.handleDeleteBatch},
        {name: this.$t('api_test.automation.batch_execute'), handleClick: this.handleBatchExecute},
        {name: this.$t('test_track.case.batch_edit_case'), handleClick: this.handleBatchEdit}
      ],
      typeArr: [
        {id: 'projectEnv', name: this.$t('api_test.definition.request.run_env')},
      ],
      priorityFilters: [
        {text: 'P0', value: 'P0'},
        {text: 'P1', value: 'P1'},
        {text: 'P2', value: 'P2'},
        {text: 'P3', value: 'P3'}
      ],
      valueArr: {
        priority: CASE_PRIORITY,
        userId: [],
        projectEnv: []
      },
      methodColorMap: new Map(API_METHOD_COLOUR),
      tableData: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      screenHeight: document.documentElement.clientHeight - 330,//屏幕高度
      // environmentId: undefined,
      currentCaseProjectId: "",
      runData: [],
      reportId: "",
      response: {},
      rowLoading: "",
      userFilters: [],
      projectIds: [],
      projectList: []
    }
  },
  props: {
    currentProtocol: String,
    selectNodeIds: Array,
    visible: {
      type: Boolean,
      default: false,
    },
    isApiListEnable: {
      type: Boolean,
      default: false,
    },
    isReadOnly: {
      type: Boolean,
      default: false
    },
    isCaseRelevance: {
      type: Boolean,
      default: false,
    },
    model: {
      type: String,
      default() {
        'api'
      }
    },
    planId: String,
    reviewId: String,
    clickType: String
  },
  created: function () {
    this.getMaintainerOptions();
    this.initTable();
  },
  activated() {
    this.status = 'default'
  },
  watch: {
    selectNodeIds() {
      this.initTable();
    },
    currentProtocol() {
      this.initTable();
    },
    planId() {
      this.initTable();
    },
    reviewId() {
      this.initTable();
    }
  },
  computed: {
    // 测试计划关联测试列表
    isRelevanceModel() {
      return this.model === 'relevance'
    },
    // 测试计划接口用例列表
    isPlanModel() {
      return this.model === 'plan'
    },
    // 接口定义用例列表
    isApiModel() {
      return this.model === 'api'
    },
  },
  methods: {
    customHeader() {
      this.$refs.headerCustom.open(this.tableLabel)
    },
    getMaintainerOptions() {
      let workspaceId = localStorage.getItem(WORKSPACE_ID);
      this.$post('/user/ws/member/tester/list', {workspaceId: workspaceId}, response => {
        this.valueArr.userId = response.data;
        this.userFilters = response.data.map(u => {
          return {text: u.name, value: u.id}
        });
      });
    },
    isApiListEnableChange(data) {
      this.$emit('isApiListEnableChange', data);
    },
    initTable() {
      getLabel(this, TEST_PLAN_API_CASE);
      this.selectRows = new Set();
      this.condition.status = "";
      this.condition.moduleIds = this.selectNodeIds;
      if (this.currentProtocol != null) {
        this.condition.protocol = this.currentProtocol;
      }
      if (this.clickType) {
        if (this.status == 'default') {
          this.condition.status = this.clickType;
        } else {
          this.condition.status = null;
        }
        this.status = 'all';
      }
      if (this.reviewId) {
        this.condition.reviewId = this.reviewId;
        this.result = this.$post('/test/case/review/api/case/list/' + this.currentPage + "/" + this.pageSize, this.condition, response => {
          this.total = response.data.itemCount;
          this.tableData = response.data.listObject;
          this.tableData.forEach(item => {
            if (item.tags && item.tags.length > 0) {
              item.tags = JSON.parse(item.tags);
            }
          })
        });
      }
      if (this.planId) {
        this.condition.planId = this.planId;
        this.result = this.$post('/test/plan/api/case/list/' + this.currentPage + "/" + this.pageSize, this.condition, response => {
          this.total = response.data.itemCount;
          this.tableData = response.data.listObject;
          this.tableData.forEach(item => {
            if (item.tags && item.tags.length > 0) {
              item.tags = JSON.parse(item.tags);
            }
          })
        });
      }

    },
    handleSelect(selection, row) {
      row.hashTree = [];
      if (this.selectRows.has(row)) {
        this.$set(row, "showMore", false);
        this.selectRows.delete(row);
      } else {
        this.$set(row, "showMore", true);
        this.selectRows.add(row);
      }
    },
    showExecResult(row) {
      this.$emit('showExecResult', row);
    },
    filter(filters) {
      _filter(filters, this.condition);
      this.initTable();
    },
    sort(column) {
      // 每次只对一个字段排序
      if (this.condition.orders) {
        this.condition.orders = [];
      }
      _sort(column, this.condition);
      this.initTable();
    },
    handleSelectAll(selection) {
      if (selection.length > 0) {
        this.tableData.forEach(item => {
          this.$set(item, "showMore", true);
          this.selectRows.add(item);
        });
      } else {
        this.selectRows.clear();
        this.tableData.forEach(row => {
          this.$set(row, "showMore", false);
        })
      }
    },
    search() {
      this.initTable();
    },
    buildPagePath(path) {
      return path + "/" + this.currentPage + "/" + this.pageSize;
    },
    reductionApi(row) {
      let ids = [row.id];
      this.$post('/api/testcase/reduction/', ids, () => {
        this.$success(this.$t('commons.save_success'));
        this.search();
      });
    },
    handleDeleteBatch() {
      this.$alert(this.$t('api_test.definition.request.delete_confirm') + "？", '', {
        confirmButtonText: this.$t('commons.confirm'),
        callback: (action) => {
          if (action === 'confirm') {
            let param = {};
            param.ids = Array.from(this.selectRows).map(row => row.id);
            if (this.reviewId) {
              param.testCaseReviewId = this.reviewId
              this.$post('/test/case/review/api/case/batch/delete', param, () => {
                this.selectRows.clear();
                this.initTable();
                this.$emit('refresh');
                this.$success(this.$t('test_track.cancel_relevance_success'));
              });
            }
            if (this.planId) {
              param.planId = this.planId;
              this.$post('/test/plan/api/case/batch/delete', param, () => {
                this.selectRows.clear();
                this.initTable();
                this.$emit('refresh');
                this.$success(this.$t('test_track.cancel_relevance_success'));
              });
            }

          }
        }
      });
    },
    getResult(data) {
      if (RESULT_MAP.get(data)) {
        return RESULT_MAP.get(data);
      } else {
        return RESULT_MAP.get("default");
      }
    },
    runRefresh(data) {
      this.rowLoading = "";
      this.$success(this.$t('schedule.event_success'));
      this.initTable();
    },
    singleRun(row) {
      if (!row.environmentId) {
        this.$warning(this.$t('api_test.environment.select_environment'));
        return;
      }
      this.runData = [];

      this.rowLoading = row.id;

      this.$get('/api/testcase/get/' + row.caseId, (response) => {
        let apiCase = response.data;
        let request = JSON.parse(apiCase.request);
        request.name = row.id;
        request.id = row.id;
        request.useEnvironment = row.environmentId;
        this.runData.push(request);
        /*触发执行操作*/
        this.reportId = getUUID().substring(0, 8);
      });
    },
    handleBatchEdit() {
      this.$refs.batchEdit.open(this.selectRows.size);
      this.$refs.batchEdit.setSelectRows(this.selectRows);
    },
    batchEdit(form) {
      let param = {};
      // 批量修改环境
      if (form.type === 'projectEnv') {
        let map = new Map();
        param.projectEnvMap = strMapToObj(form.projectEnvMap);
        this.selectRows.forEach(row => {
          map[row.id] = row.projectId;
        })
        param.selectRows = map;
        this.$post('/test/plan/api/case/batch/update/env', param, () => {
          this.$success(this.$t('commons.save_success'));
          this.initTable();
        });
      } else {
        // 批量修改其它
      }
    },
    handleBatchExecute() {
      this.selectRows.forEach(row => {
        this.$get('/api/testcase/get/' + row.caseId, (response) => {
          let apiCase = response.data;
          let request = JSON.parse(apiCase.request);
          request.name = row.id;
          request.id = row.id;
          request.useEnvironment = row.environmentId;
          let runData = [];
          runData.push(request);
          this.batchRun(runData, getUUID().substring(0, 8));
        });
      });
      this.$message('任务执行中，请稍后刷新查看结果');
      this.search();
    },
    batchRun(runData, reportId) {
      let testPlan = new TestPlan();
      let threadGroup = new ThreadGroup();
      threadGroup.hashTree = [];
      testPlan.hashTree = [threadGroup];
      runData.forEach(item => {
        threadGroup.hashTree.push(item);
      });
      let reqObj = {id: reportId, testElement: testPlan, type: 'API_PLAN', reportId: "run"};
      let bodyFiles = getBodyUploadFiles(reqObj, runData);
      this.$fileUpload("/api/definition/run", null, bodyFiles, reqObj, response => {
      });
    },
    handleDelete(apiCase) {
      if (this.planId) {
        this.$get('/test/plan/api/case/delete/' + apiCase.id, () => {
          this.$success(this.$t('test_track.cancel_relevance_success'));
          this.$emit('refresh');
          this.initTable();
        });
      }
      if (this.reviewId) {
        this.$get('/test/case/review/api/case/delete/' + apiCase.id, () => {
          this.$success(this.$t('test_track.cancel_relevance_success'));
          this.$emit('refresh');
          this.initTable();
        });
      }
      return;
    },
    getProjectId() {
      if (!this.isRelevanceModel) {
        return getCurrentProjectID();
      } else {
        return this.currentCaseProjectId;
      }
    },
    setEnvironment(data) {
      //   this.environmentId = data.id;
    },
    getReportResult(apiCase) {
      let url = "/api/definition/report/getReport/" + apiCase.id + '/' + 'API_PLAN';
      this.$get(url, response => {
        if (response.data) {
          this.response = JSON.parse(response.data.content);
          this.$refs.apiCaseResult.open();
        }
      });
    },
  },
}
</script>

<style scoped>
.operate-button > div {
  display: inline-block;
  margin-left: 10px;
}

.request-method {
  padding: 0 5px;
  color: #1E90FF;
}

.api-el-tag {
  color: white;
}

.search-input {
  float: right;
  width: 300px;
  /*margin-bottom: 20px;*/
  margin-right: 20px;
}

</style>
