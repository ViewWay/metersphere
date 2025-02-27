<template>
  <div>
    <div>
      <el-link type="primary" style="float:right;margin-top: 5px" @click="open">{{$t('commons.adv_search.title')}}</el-link>

      <el-input :placeholder="$t('commons.search_by_id_name_tag')" @blur="search" @keyup.enter.native="search" class="search-input" size="small"
                v-model="condition.name"/>

      <el-table v-loading="result.loading"
                ref="caseTable"
                border
                :data="tableData" row-key="id" class="test-content adjust-table ms-select-all-fixed"
                @select-all="handleSelectAll"
                @filter-change="filter"
                @sort-change="sort"
                @header-dragend="headerDragend"
                @select="handleSelect" :height="screenHeight">

        <el-table-column type="selection" width="50"/>

        <ms-table-header-select-popover v-show="total>0"
                                        :page-size="pageSize>total?total:pageSize"
                                        :total="total"
                                        @selectPageAll="isSelectDataAll(false)"
                                        @selectAll="isSelectDataAll(true)"/>

        <el-table-column width="30" :resizable="false" min-width="30px" align="center">
          <template v-slot:default="scope">
            <!-- 选中后浮现提供批量操作的按钮-->
            <show-more-btn :is-show="scope.row.showMore" :buttons="buttons" :size="selectDataCounts" v-tester/>
          </template>
        </el-table-column>
        <template v-for="(item, index) in tableLabel">
          <el-table-column v-if="item.id == 'num'" prop="num" label="ID" min-width="120px" show-overflow-tooltip
                           :key="index">
            <template slot-scope="scope">
              <!-- 为只读用户的话不能编辑 -->
              <span style="cursor:pointer" v-if="isReadOnly"> {{ scope.row.num }} </span>
              <el-tooltip content="编辑" v-else>
                <a style="cursor:pointer" @click="handleTestCase(scope.row)"> {{ scope.row.num }} </a>
              </el-tooltip>
            </template>
          </el-table-column>

          <el-table-column v-if="item.id == 'name'" prop="name" min-width="160px" :label="$t('test_track.case.name')"
                           show-overflow-tooltip :key="index"/>

          <el-table-column
            v-if="item.id == 'priority'"
            prop="priority"
            :filters="priorityFilters"
            column-key="priority"
            min-width="120px"
            :label="$t('test_track.case.priority')"
            show-overflow-tooltip
            :key="index">
            <template v-slot:default="scope">
              <priority-table-item :value="scope.row.priority"/>
            </template>
          </el-table-column>

          <el-table-column
            v-if="item.id == 'custom'"
            sortable="custom"
            prop="path"
            min-width="180px"
            :label="$t('api_test.definition.api_path')"
            show-overflow-tooltip
            :key="index"/>

          <el-table-column v-if="item.id=='tags'" prop="tags" min-width="120px" :label="$t('commons.tag')"
                           :key="index">
            <template v-slot:default="scope">
                <ms-tag  v-for="(itemName,index)  in scope.row.tags" :key="index" type="success" effect="plain" :content="itemName" style="margin-left: 5px"/>
            </template>
          </el-table-column>

          <el-table-column
            v-if="item.id=='createUser'"
            prop="createUser"
            :label="'创建人'"
            show-overflow-tooltip
            :key="index"/>

          <el-table-column
            v-if="item.id=='custom'"
            sortable="custom"
            min-width="160"
            :label="$t('api_test.definition.api_last_time')"
            prop="updateTime"
            :key="index">
            <template v-slot:default="scope">
              <span>{{ scope.row.updateTime | timestampFormatDate }}</span>
            </template>
          </el-table-column>
        </template>
        <el-table-column fixed="right" v-if="!isReadOnly" :label="$t('commons.operating')" min-width="130"
                         align="center">
          <template slot="header">
            <header-label-operate @exec="customHeader"/>
          </template>
          <template v-slot:default="scope">
            <ms-table-operator-button :tip="$t('commons.edit')" icon="el-icon-edit" @exec="handleTestCase(scope.row)"
                                      v-tester/>
            <ms-table-operator-button :tip="$t('commons.delete')" icon="el-icon-delete" @exec="handleDelete(scope.row)"
                                      type="danger" v-tester/>
            <ms-api-case-table-extend-btns @showCaseRef="showCaseRef" @showEnvironment="showEnvironment"
                                           @createPerformance="createPerformance" :row="scope.row" v-tester/>
          </template>
        </el-table-column>
      </el-table>
      <header-custom ref="headerCustom" :initTableData="initTable" :optionalFields=headerItems
                     :type=type></header-custom>
      <ms-table-pagination :change="initTable" :current-page.sync="currentPage" :page-size.sync="pageSize"
                           :total="total"/>
    </div>

    <api-case-list @showExecResult="showExecResult" @refresh="initTable" :currentApi="selectCase" ref="caseList"/>
    <!--批量编辑-->
    <ms-batch-edit ref="batchEdit" @batchEdit="batchEdit" :typeArr="typeArr" :value-arr="valueArr"/>
    <!--选择环境(当创建性能测试的时候)-->
    <ms-set-environment ref="setEnvironment" :testCase="clickRow" @createPerformance="createPerformance"/>
    <!--查看引用-->
    <ms-reference-view ref="viewRef"/>
    <!--高级搜索-->
    <ms-table-adv-search-bar :condition.sync="condition" :showLink="false" ref="searchBar" @search="initTable"/>

  </div>

</template>

<script>

import MsTableOperator from "../../../../common/components/MsTableOperator";
import MsTableOperatorButton from "../../../../common/components/MsTableOperatorButton";
import MsTablePagination from "../../../../common/pagination/TablePagination";
import MsTag from "../../../../common/components/MsTag";
import MsApiCaseList from "../case/ApiCaseList";
import ApiCaseList from "../case/ApiCaseList";
import MsContainer from "../../../../common/components/MsContainer";
import MsBottomContainer from "../BottomContainer";
import ShowMoreBtn from "../../../../track/case/components/ShowMoreBtn";
import MsBatchEdit from "../basis/BatchEdit";
import {API_METHOD_COLOUR, CASE_PRIORITY, DUBBO_METHOD, REQ_METHOD, SQL_METHOD, TCP_METHOD} from "../../model/JsonData";

import {getBodyUploadFiles, getCurrentProjectID, getCurrentUser} from "@/common/js/utils";
import PriorityTableItem from "../../../../track/common/tableItems/planview/PriorityTableItem";
import MsApiCaseTableExtendBtns from "../reference/ApiCaseTableExtendBtns";
import MsReferenceView from "../reference/ReferenceView";
import MsSetEnvironment from "@/business/components/api/definition/components/basis/SetEnvironment";
import TestPlan from "@/business/components/api/definition/components/jmeter/components/test-plan";
import ThreadGroup from "@/business/components/api/definition/components/jmeter/components/thread-group";
import {parseEnvironment} from "@/business/components/api/test/model/EnvironmentModel";
import MsTableHeaderSelectPopover from "@/business/components/common/components/table/MsTableHeaderSelectPopover";
import MsTableAdvSearchBar from "@/business/components/common/components/search/MsTableAdvSearchBar";
import {API_CASE_CONFIGS} from "@/business/components/common/components/search/search-components";
import {_filter, _handleSelect, _handleSelectAll, _sort, getLabel,} from "@/common/js/tableUtils";
import {API_CASE_LIST} from "@/common/js/constants";
import {Api_Case_List} from "@/business/components/common/model/JsonData";
import HeaderCustom from "@/business/components/common/head/HeaderCustom";
import HeaderLabelOperate from "@/business/components/common/head/HeaderLabelOperate";

export default {
  name: "ApiCaseSimpleList",
  components: {
    HeaderLabelOperate,
    HeaderCustom,
    MsTableHeaderSelectPopover,
    MsSetEnvironment,
    ApiCaseList,
    PriorityTableItem,
    MsTableOperatorButton,
    MsTableOperator,
    MsTablePagination,
    MsTag,
    MsApiCaseList,
    MsContainer,
    MsBottomContainer,
    ShowMoreBtn,
    MsBatchEdit,
    MsApiCaseTableExtendBtns,
      MsReferenceView,
      MsTableAdvSearchBar
    },
    data() {
      return {
        type: API_CASE_LIST,
        headerItems: Api_Case_List,
        tableLabel: Api_Case_List,
        condition: {
          components: API_CASE_CONFIGS
        },
        selectCase: {},
        result: {},
        moduleId: "",
        selectDataRange: "all",
        deletePath: "/test/case/delete",
        selectRows: new Set(),
        clickRow: {},
        buttons: [
          {name: this.$t('api_test.definition.request.batch_delete'), handleClick: this.handleDeleteBatch},
          {name: this.$t('api_test.definition.request.batch_edit'), handleClick: this.handleEditBatch}
        ],
        typeArr: [
          {id: 'priority', name: this.$t('test_track.case.priority')},
          {id: 'method', name: this.$t('api_test.definition.api_type')},
          {id: 'path', name: this.$t('api_test.request.path')},
        ],
        priorityFilters: [
          {text: 'P0', value: 'P0'},
          {text: 'P1', value: 'P1'},
          {text: 'P2', value: 'P2'},
          {text: 'P3', value: 'P3'}
        ],
        valueArr: {
          priority: CASE_PRIORITY,
          method: REQ_METHOD,
        },
        methodColorMap: new Map(API_METHOD_COLOUR),
        tableData: [],
        currentPage: 1,
        pageSize: 10,
        total: 0,
        screenHeight: document.documentElement.clientHeight - 330,//屏幕高度
        environmentId: undefined,
        selectAll: false,
        unSelection: [],
        selectDataCounts: 0,
        environments: [],
      }
    },
    props: {
      currentProtocol: String,
      selectNodeIds: Array,
      activeDom: String,
      visible: {
        type: Boolean,
        default: false,
      },
      trashEnable: {
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
      relevanceProjectId: String,
      model: {
        type: String,
        default() {
          'api'
        }
      },
      planId: String
    },
    created: function () {
      this.initTable();
    },
    watch: {
      selectNodeIds() {
        this.initTable();
      },
      currentProtocol() {
        this.initTable();
      },
      trashEnable() {
        if (this.trashEnable) {
          this.initTable();
        }
      },
      relevanceProjectId() {
        this.initTable();
      }
    },
    computed: {

      // 接口定义用例列表
      isApiModel() {
        return this.model === 'api'
      },
    },
    methods: {
      customHeader() {
        this.$refs.headerCustom.open(this.tableLabel)
      },
      initTable() {
        getLabel(this, API_CASE_LIST);
        this.selectRows = new Set();
        this.condition.status = "";
        this.condition.moduleIds = this.selectNodeIds;
        if (this.trashEnable) {
          this.condition.status = "Trash";
          this.condition.moduleIds = [];
        }
        this.selectAll = false;
        this.unSelection = [];
        this.selectDataCounts = 0;
        this.condition.projectId = getCurrentProjectID();

        if (this.currentProtocol != null) {
          this.condition.protocol = this.currentProtocol;
        }

        //检查是否只查询本周数据
        this.isSelectThissWeekData();
        this.condition.selectThisWeedData = false;
        this.condition.id = null;
        if (this.selectDataRange == 'thisWeekCount') {
          this.condition.selectThisWeedData = true;
        } else if (this.selectDataRange != null) {
          let selectParamArr = this.selectDataRange.split("single:");

          if (selectParamArr.length == 2) {
            this.condition.id = selectParamArr[1];
          }
        }
        if (this.condition.projectId) {
          this.result = this.$post('/api/testcase/list/' + this.currentPage + "/" + this.pageSize, this.condition, response => {
            this.total = response.data.itemCount;
            this.tableData = response.data.listObject;
            this.unSelection = response.data.listObject.map(s => s.id);

            this.tableData.forEach(item => {
              if (item.tags && item.tags.length > 0) {
                item.tags = JSON.parse(item.tags);
              }
            })
          });
        }
      },
      open() {
        this.$refs.searchBar.open();
      },
      handleSelect(selection, row) {
        _handleSelect(this, selection, row, this.selectRows);
        this.selectRowsCount(this.selectRows)
      },
      showExecResult(row) {
        this.visible = false;
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
        _handleSelectAll(this, selection, this.tableData, this.selectRows);
        this.selectRowsCount(this.selectRows)
      },
      search() {
        this.changeSelectDataRangeAll();
        this.initTable();
      },
      buildPagePath(path) {
        return path + "/" + this.currentPage + "/" + this.pageSize;
      },

      handleTestCase(testCase) {
        this.$get('/api/definition/get/' + testCase.apiDefinitionId, (response) => {
          let api = response.data;
          let selectApi = api;
          let request = {};
          if (Object.prototype.toString.call(api.request).match(/\[object (\w+)\]/)[1].toLowerCase() === 'object') {
            request = api.request;
          } else {
            request = JSON.parse(api.request);
          }
          if (!request.hashTree) {
            request.hashTree = [];
          }
          selectApi.url = request.path;
          this.$refs.caseList.open(selectApi, testCase.id);
        });
      },
      reductionApi(row) {
        let ids = [row.id];
        this.$post('/api/testcase/reduction/', ids, () => {
          this.$success(this.$t('commons.save_success'));
          this.search();
        });
      },
      handleDeleteBatch() {
        // if (this.trashEnable) {
        this.$alert(this.$t('api_test.definition.request.delete_confirm') + "？", '', {
          confirmButtonText: this.$t('commons.confirm'),
          callback: (action) => {
            if (action === 'confirm') {
              let obj = {};
              obj.projectId = getCurrentProjectID();
              obj.selectAllDate = this.isSelectAllDate;
              obj.unSelectIds = this.unSelection;
              obj.ids = Array.from(this.selectRows).map(row => row.id);
              obj = Object.assign(obj, this.condition);
              this.$post('/api/testcase/deleteBatchByParam/', obj, () => {
                this.selectRows.clear();
                this.initTable();
                this.$success(this.$t('commons.delete_success'));
              });
            }
          }
        });
        // } else {
        //   this.$alert(this.$t('api_test.definition.request.delete_confirm') + "？", '', {
        //     confirmButtonText: this.$t('commons.confirm'),
        //     callback: (action) => {
        //       if (action === 'confirm') {
        //         let ids = Array.from(this.selectRows).map(row => row.id);
        //         this.$post('/api/testcase/removeToGc/', ids, () => {
        //           this.selectRows.clear();
        //           this.initTable();
        //           this.$success(this.$t('commons.delete_success'));
        //         });
        //       }
        //     }
        //   });
        // }
      },
      handleEditBatch() {
        if (this.currentProtocol == 'HTTP') {
          this.valueArr.method = REQ_METHOD;
        } else if (this.currentProtocol == 'TCP') {
          this.valueArr.method = TCP_METHOD;
        } else if (this.currentProtocol == 'SQL') {
          this.valueArr.method = SQL_METHOD;
        } else if (this.currentProtocol == 'DUBBO') {
          this.valueArr.method = DUBBO_METHOD;
        }
        this.$refs.batchEdit.open();
      },
      batchEdit(form) {
        let arr = Array.from(this.selectRows);
        let ids = arr.map(row => row.id);
        let param = {};
        param[form.type] = form.value;
        param.ids = ids;
        param.projectId = getCurrentProjectID();
        param.selectAllDate = this.isSelectAllDate;
        param.unSelectIds = this.unSelection;
        param = Object.assign(param, this.condition);
        this.$post('/api/testcase/batch/editByParam', param, () => {
          this.$success(this.$t('commons.save_success'));
          this.initTable();
        });
      },
      handleDelete(apiCase) {
        // if (this.trashEnable) {
        this.$alert(this.$t('api_test.definition.request.delete_confirm') + ' ' + apiCase.name + " ？", '', {
          confirmButtonText: this.$t('commons.confirm'),
          callback: (action) => {
            if (action === 'confirm') {
              this.$get('/api/testcase/delete/' + apiCase.id, () => {
                this.$success(this.$t('commons.delete_success'));
                this.initTable();
              });
            }
          }
        });
        return;
      },
      setEnvironment(data) {
        this.environmentId = data.id;
      },
      selectRowsCount(selection) {
        let selectedIDs = this.getIds(selection);
        let allIDs = this.tableData.map(s => s.id);
        this.unSelection = allIDs.filter(function (val) {
          return selectedIDs.indexOf(val) === -1
        });
        if (this.isSelectAllDate) {
          this.selectDataCounts = this.total - this.unSelection.length;
        } else {
          this.selectDataCounts = selection.size;
        }
      },
      isSelectDataAll(dataType) {
        this.isSelectAllDate = dataType;
        this.selectRowsCount(this.selectRows)
        //如果已经全选，不需要再操作了
        if (this.selectRows.size != this.tableData.length) {
          this.$refs.caseTable.toggleAllSelection(true);
        }
      },
      //判断是否只显示本周的数据。  从首页跳转过来的请求会带有相关参数
      isSelectThissWeekData() {
        this.selectDataRange = "all";
        let routeParam = this.$route.params.dataSelectRange;
        let dataType = this.$route.params.dataType;
        if (dataType === 'apiTestCase') {
          this.selectDataRange = routeParam;
        }
      },
      changeSelectDataRangeAll() {
        this.$emit("changeSelectDataRangeAll", "testCase");
      },
      getIds(rowSets) {
        let rowArray = Array.from(rowSets)
        let ids = rowArray.map(s => s.id);
        return ids;
      },
      showCaseRef(row) {
        let param = {};
        Object.assign(param, row);
        param.moduleId = undefined;
        this.$refs.viewRef.open(param);
      },
      showEnvironment(row) {

        let projectID = getCurrentProjectID();
        if (this.projectId) {
          this.$get('/api/environment/list/' + this.projectId, response => {
            this.environments = response.data;
            this.environments.forEach(environment => {
              parseEnvironment(environment);
            });
          });
        } else {
          this.environment = undefined;
        }
        this.clickRow = row;
        this.$refs.setEnvironment.open(row);
      },
      headerDragend(newWidth, oldWidth, column, event) {
        let finalWidth = newWidth;
        if (column.minWidth > finalWidth) {
          finalWidth = column.minWidth;
        }
        column.width = finalWidth;
        column.realWidth = finalWidth;
      },
      createPerformance(row, environment) {
        /**
         * 思路：调用后台创建性能测试的方法，把当前案例的hashTree在后台转化为jmx并文件创建性能测试。
         * 然后跳转到修改性能测试的页面
         *
         * 性能测试保存地址： performance/save
         *
         */
        if (!environment) {
          this.$warning(this.$t('api_test.environment.select_environment'));
          return;
        }
        let runData = [];
        let singleLoading = true;
        row.request = JSON.parse(row.request);
        row.request.name = row.id;
        row.request.useEnvironment = environment.id;
        runData.push(row.request);
        /*触发执行操作*/
        let testPlan = new TestPlan();
        let threadGroup = new ThreadGroup();
        threadGroup.hashTree = [];
        testPlan.hashTree = [threadGroup];
        runData.forEach(item => {
          threadGroup.hashTree.push(item);
        })
        let reqObj = {
          id: row.id,
          testElement: testPlan,
          name: row.name,
          projectId: getCurrentProjectID(),
        };
        let bodyFiles = getBodyUploadFiles(reqObj, runData);
        reqObj.reportId = "run";

        let url = "/api/genPerformanceTestXml";

        this.$fileUpload(url, null, bodyFiles, reqObj, response => {
          let jmxObj = {};
          jmxObj.name = response.data.name;
          jmxObj.xml = response.data.xml;
          jmxObj.attachFiles = response.data.attachFiles;
          jmxObj.attachByteFiles = response.data.attachByteFiles;
          this.$store.commit('setTest', {
            name: row.name,
            jmx: jmxObj
          })
          this.$router.push({
            path: "/performance/test/create"
          })
          // let performanceId = response.data;
          // if(performanceId!=null){
          //   this.$router.push({
          //     path: "/performance/test/edit/"+performanceId,
          //   })
          // }
        }, erro => {
          this.$emit('runRefresh', {});
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
    margin-right: 10px;
  }

  .ms-select-all >>> th:first-child {
    margin-top: 20px;
  }

  .ms-select-all >>> th:nth-child(2) .el-icon-arrow-down {
    top: -2px;
  }

</style>
