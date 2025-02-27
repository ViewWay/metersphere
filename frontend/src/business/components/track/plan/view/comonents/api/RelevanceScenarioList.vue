<template>
  <div>
    <el-card class="table-card" v-loading="result.loading">

      <template v-slot:header>
        <el-row>
          <el-col :span="8" :offset="11">
            <el-input :placeholder="$t('api_test.definition.request.select_case')" @blur="search"
                      @keyup.enter.native="search" class="search-input" size="small" v-model="condition.name"/>
          </el-col>

          <env-popover :env-map="projectEnvMap" :project-ids="projectIds" @setProjectEnvMap="setProjectEnvMap"
                       :project-list="projectList" ref="envPopover" class="env-popover"/>
        </el-row>
      </template>

      <el-table ref="scenarioTable" border :data="tableData" class="adjust-table" @select-all="handleSelectAll" @select="handleSelect">
        <el-table-column type="selection"/>

        <el-table-column prop="name" :label="$t('api_test.automation.scenario_name')"
                         show-overflow-tooltip/>
        <el-table-column prop="level" :label="$t('api_test.automation.case_level')"
                         show-overflow-tooltip>
          <template v-slot:default="scope">
            <ms-tag v-if="scope.row.level == 'P0'" type="info" effect="plain" content="P0"/>
            <ms-tag v-if="scope.row.level == 'P1'" type="warning" effect="plain" content="P1"/>
            <ms-tag v-if="scope.row.level == 'P2'" type="success" effect="plain" content="P2"/>
            <ms-tag v-if="scope.row.level == 'P3'" type="danger" effect="plain" content="P3"/>
          </template>

        </el-table-column>
        <el-table-column prop="tagNames" :label="$t('api_test.automation.tag')" min-width="120">
          <template v-slot:default="scope">
              <ms-tag v-for="itemName in scope.row.tags" :key="itemName" type="success" effect="plain" :content="itemName" style="margin-left: 5px"/>
          </template>
        </el-table-column>
        <el-table-column prop="userId" :label="$t('api_test.automation.creator')" show-overflow-tooltip/>
        <el-table-column prop="updateTime" :label="$t('api_test.automation.update_time')" width="180">
          <template v-slot:default="scope">
            <span>{{ scope.row.updateTime | timestampFormatDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stepTotal" :label="$t('api_test.automation.step')" show-overflow-tooltip/>
        <el-table-column prop="lastResult" :label="$t('api_test.automation.last_result')">
          <template v-slot:default="{row}">
            <el-link type="success" @click="showReport(row)" v-if="row.lastResult === 'Success'">{{ $t('api_test.automation.success') }}</el-link>
            <el-link type="danger" @click="showReport(row)" v-if="row.lastResult === 'Fail'">{{ $t('api_test.automation.fail') }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="passRate" :label="$t('api_test.automation.passing_rate')"
                         show-overflow-tooltip/>
      </el-table>
      <ms-table-pagination :change="search" :current-page.sync="currentPage" :page-size.sync="pageSize"
                           :total="total"/>
    </el-card>

  </div>
</template>

<script>
  import MsTableHeader from "@/business/components/common/components/MsTableHeader";
  import MsTablePagination from "@/business/components/common/pagination/TablePagination";
  import ShowMoreBtn from "@/business/components/track/case/components/ShowMoreBtn";
  import MsTag from "../../../../../common/components/MsTag";
  import {getUUID, getCurrentProjectID} from "@/common/js/utils";
  import MsApiReportDetail from "../../../../../api/automation/report/ApiReportDetail";
  import MsTableMoreBtn from "../../../../../api/automation/scenario/TableMoreBtn";
  import MsTestPlanList from "../../../../../api/automation/scenario/testplan/TestPlanList";
  import TestPlanScenarioListHeader from "./TestPlanScenarioListHeader";
  import {_handleSelect, _handleSelectAll} from "../../../../../../../common/js/tableUtils";
  import EnvPopover from "@/business/components/api/automation/scenario/EnvPopover";

  export default {
    name: "RelevanceScenarioList",
    components: {
      EnvPopover,
      TestPlanScenarioListHeader,
      MsTablePagination, MsTableMoreBtn, ShowMoreBtn, MsTableHeader, MsTag, MsApiReportDetail, MsTestPlanList},
    props: {
      referenced: {
        type: Boolean,
        default: false,
      },
      selectNodeIds: Array,
      projectId: String,
      planId: String,
    },
    data() {
      return {
        result: {},
        condition: {},
        currentScenario: {},
        schedule: {},
        selectAll: false,
        tableData: [],
        currentPage: 1,
        pageSize: 10,
        total: 0,
        reportId: "",
        infoDb: false,
        selectRows: new Set(),
        projectEnvMap: new Map(),
        projectList: [],
        projectIds: new Set(),
        map: new Map()
      }
    },
    watch: {
      selectNodeIds() {
        this.search();
      },
      projectId() {
        this.search();
      },
    },
    created() {
      this.getWsProjects();
    },
    methods: {
      search() {
        this.projectEnvMap.clear();
        this.projectIds.clear();
        if (!this.projectId) {
          return;
        }
        this.selectRows = new Set();
        this.loading = true;

        this.condition.filters = {status: ["Prepare", "Underway", "Completed"]};

        this.condition.moduleIds = this.selectNodeIds;

        if (this.projectId != null) {
          this.condition.projectId = this.projectId;
        }

        if (this.planId != null) {
          this.condition.planId = this.planId;
        }

        let url = "/test/plan/scenario/case/relevance/list/" + this.currentPage + "/" + this.pageSize;
        this.result = this.$post(url, this.condition, response => {
          let data = response.data;
          this.total = data.itemCount;
          this.tableData = data.listObject;
          this.tableData.forEach(item => {
            if (item.tags && item.tags.length > 0) {
              item.tags = JSON.parse(item.tags);
            }
          });
        });
      },
      handleSelectAll(selection) {
        _handleSelectAll(this, selection, this.tableData, this.selectRows);
        this.initProjectIds();
      },
      handleSelect(selection, row) {
        _handleSelect(this, selection, row, this.selectRows);
        this.initProjectIds();
      },
      setProjectEnvMap(projectEnvMap) {
        this.projectEnvMap = projectEnvMap;
      },
      getWsProjects() {
        this.$get("/project/listAll", res => {
          this.projectList = res.data;
        })
      },
      initProjectIds() {
        this.projectIds.clear();
        this.selectRows.forEach(row => {
          this.result = this.$get('/api/automation/getApiScenario/' + row.id, res => {
            let data = res.data;
            data.projectIds.forEach(d => this.projectIds.add(d));
            this.map.set(row.id, data.projectIds);
          })
        })
      },
      checkEnv() {
        return this.$refs.envPopover.checkEnv();
      }
    }
  }
</script>

<style scoped>
  /deep/ .el-drawer__header {
    margin-bottom: 0px;
  }

  .env-popover {
    float: right;
    margin-top: 4px;
  }
</style>
