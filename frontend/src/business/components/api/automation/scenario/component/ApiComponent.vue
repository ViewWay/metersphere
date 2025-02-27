<template>
  <div>
    <api-base-component
      v-loading="loading"
      @copy="copyRow"
      @remove="remove"
      @active="active"
      :is-show-name-input="!isDeletedOrRef"
      :data="request"
      :draggable="draggable"
      :color="displayColor.color"
      :background-color="displayColor.backgroundColor"
      :is-max="isMax"
      :show-btn="showBtn"
      :title="displayTitle">

      <template v-slot:behindHeaderLeft>
        <el-tag size="mini" class="ms-tag" v-if="request.referenced==='Deleted'" type="danger">{{$t('api_test.automation.reference_deleted')}}</el-tag>
        <el-tag size="mini" class="ms-tag" v-if="request.referenced==='Copy'">{{ $t('commons.copy') }}</el-tag>
        <el-tag size="mini" class="ms-tag" v-if="request.referenced ==='REF'">{{ $t('api_test.scenario.reference') }}</el-tag>
        <span class="ms-tag">{{getProjectName(request.projectId)}}</span>
      </template>

      <template v-slot:button>
        <el-tooltip :content="$t('api_test.run')" placement="top">
          <el-button @click="run" icon="el-icon-video-play" class="ms-btn" size="mini" circle/>
        </el-tooltip>
      </template>

      <customize-req-info :is-customize-req="isCustomizeReq" :request="request"/>

      <p class="tip">{{ $t('api_test.definition.request.req_param') }} </p>
      <ms-api-request-form :isShowEnable="true" :referenced="true" :headers="request.headers " :request="request"
                           v-if="request.protocol==='HTTP' || request.type==='HTTPSamplerProxy'"/>
      <ms-tcp-basis-parameters :request="request" v-if="request.protocol==='TCP'|| request.type==='TCPSampler'" :showScript="false"/>
      <ms-sql-basis-parameters :request="request" v-if="request.protocol==='SQL'|| request.type==='JDBCSampler'"
                               :showScript="false"/>
      <ms-dubbo-basis-parameters :request="request"
                                 v-if="request.protocol==='DUBBO' || request.protocol==='dubbo://'|| request.type==='DubboSampler'"
                                 :showScript="false"/>

      <p class="tip">{{ $t('api_test.definition.request.res_param') }} </p>
      <div v-if="request.result">
        <el-tabs v-model="request.activeName" closable class="ms-tabs">
          <el-tab-pane :label="item.name" :name="item.name" v-for="(item,index) in request.result.scenarios" :key="index">
            <div v-for="(result,i) in item.requestResults" :key="i" style="margin-bottom: 5px">
              <api-response-component v-if="result.name===request.name" :result="result"/>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <api-response-component :currentProtocol="request.protocol" :result="request.requestResult" v-else/>

      <!-- 保存操作 -->
      <el-button type="primary" size="small" class="ms-btn-flot" @click="saveTestCase(item)"
                 v-if="!request.referenced">
        {{ $t('commons.save') }}
      </el-button>
    </api-base-component>
    <ms-run :debug="true" :reportId="reportId" :run-data="runData" :env-map="envMap"
            @runRefresh="runRefresh" ref="runTest"/>


  </div>
</template>

<script>
  import MsSqlBasisParameters from "../../../definition/components/request/database/BasisParameters";
  import MsTcpBasisParameters from "../../../definition/components/request/tcp/TcpBasisParameters";
  import MsDubboBasisParameters from "../../../definition/components/request/dubbo/BasisParameters";
  import MsApiRequestForm from "../../../definition/components/request/http/ApiHttpRequestForm";
  import MsRequestResultTail from "../../../definition/components/response/RequestResultTail";
  import MsRun from "../../../definition/components/Run";
  import {getUUID, getCurrentProjectID} from "@/common/js/utils";
  import ApiBaseComponent from "../common/ApiBaseComponent";
  import ApiResponseComponent from "./ApiResponseComponent";
  import CustomizeReqInfo from "@/business/components/api/automation/scenario/common/CustomizeReqInfo";

  export default {
    name: "MsApiComponent",
    props: {
      request: {},
      currentScenario: {},
      node: {},
      draggable: {
        type: Boolean,
        default: false,
      },
      isMax: {
        type: Boolean,
        default: false,
      },
      showBtn: {
        type: Boolean,
        default: true,
      },
      currentEnvironmentId: String,
      projectList: Array,
      envMap: Map
    },
    components: {
      CustomizeReqInfo,
      ApiBaseComponent, ApiResponseComponent,
      MsSqlBasisParameters, MsTcpBasisParameters, MsDubboBasisParameters, MsApiRequestForm, MsRequestResultTail, MsRun
    },
    data() {
      return {
        loading: false,
        reportId: "",
        runData: [],
        isShowInput: false,
      }
    },
    created() {
      if (!this.request.requestResult) {
        this.request.requestResult = {responseResult: {}};
      }
      // 跨项目关联，如果没有ID，则赋值本项目ID
      if (!this.request.projectId) {
        this.request.projectId = getCurrentProjectID();
      }
      // 加载引用对象数据
      this.getApiInfo();
      if (this.request.protocol === 'HTTP') {
        this.setUrl(this.request.url);
        this.setUrl(this.request.path);
        // 历史数据 auth 处理
        if (this.request.hashTree) {
          for (let index in this.request.hashTree) {
            if (this.request.hashTree[index].type == 'AuthManager') {
              this.request.authManager = this.request.hashTree[index];
              this.request.hashTree.splice(index, 1);
            }
          }
        }
      }
    },
    computed: {
      displayColor() {
        if (this.isApiImport) {
          return {
            color: "#F56C6C",
            backgroundColor: "#FCF1F1"
          }
        } else if (this.isExternalImport) {
          return {
            color: "#409EFF",
            backgroundColor: "#EEF5FE"
          }
        } else if (this.isCustomizeReq) {
          return {
            color: "#008080",
            backgroundColor: "#EBF2F2"
          }
        }
        return {};
      },
      displayTitle() {
        if (this.isApiImport) {
          return this.$t('api_test.automation.api_list_import');
        } else if (this.isExternalImport) {
          return this.$t('api_test.automation.external_import');
        } else if (this.isCustomizeReq) {
          return this.$t('api_test.automation.customize_req');
        }
        return "";
      },
      isApiImport() {
        if (this.request.referenced != undefined && this.request.referenced === 'Deleted' || this.request.referenced == 'REF' || this.request.referenced === 'Copy') {
          return true
        }
        return false;
      },
      isExternalImport() {
        if (this.request.referenced != undefined && this.request.referenced === 'OT_IMPORT') {
          return true
        }
        return false;
      },
      isCustomizeReq() {
        if (this.request.referenced == undefined || this.request.referenced === 'Created') {
          return true
        }
        return false;
      },
      isDeletedOrRef() {
        if (this.request.referenced != undefined && this.request.referenced === 'Deleted' || this.request.referenced === 'REF') {
          return true
        }
        return false;
      },
    },
    methods: {
      remove() {
        this.$emit('remove', this.request, this.node);
      },
      copyRow() {
        this.$emit('copyRow', this.request, this.node);
      },
      setUrl(url) {
        try {
          new URL(url);
          this.request.url = url;
        } catch (e) {
          if (url && (!url.startsWith("http://") || !url.startsWith("https://"))) {
            this.request.path = url;
            this.request.url = undefined;
          }
        }
      },
      getApiInfo() {
        if (this.request.id && this.request.referenced === 'REF') {
          let requestResult = this.request.requestResult;
          let url = this.request.refType && this.request.refType === 'CASE' ? "/api/testcase/get/" : "/api/definition/get/";
          let enable = this.request.enable;
          this.$get(url + this.request.id, response => {
            if (response.data) {
              Object.assign(this.request, JSON.parse(response.data.request));
              this.request.name = response.data.name;
              this.request.enable = enable;
              if (response.data.path && response.data.path != null) {
                this.request.path = response.data.path;
                this.request.url = response.data.url;
                this.setUrl(this.request.path);
              }
              if (response.data.method && response.data.method != null) {
                this.request.method = response.data.method;
              }
              this.request.requestResult = requestResult;
              this.request.id = response.data.id;
              //this.request.disabled = true;
              if (!this.request.projectId) {
                this.request.projectId = response.data.projectId;
              }
              this.reload();
              this.sort();
            } else {
              this.request.referenced = "Deleted";
            }
          })
        }
      },
      recursiveSorting(arr) {
        for (let i in arr) {
          arr[i].disabled = true;
          arr[i].index = Number(i) + 1;
          if (arr[i].hashTree != undefined && arr[i].hashTree.length > 0) {
            this.recursiveSorting(arr[i].hashTree);
          }
        }
      },
      sort() {
        for (let i in this.request.hashTree) {
          this.request.hashTree[i].disabled = true;
          this.request.hashTree[i].index = Number(i) + 1;
          if (this.request.hashTree[i].hashTree != undefined && this.request.hashTree[i].hashTree.length > 0) {
            this.recursiveSorting(this.request.hashTree[i].hashTree);
          }
        }
      },
      active(item) {
        this.request.active = !this.request.active;
        this.reload();
      },
      run() {
        if (this.isApiImport) {
          if (!this.envMap || this.envMap.size === 0) {
            this.$warning("请在环境配置中为该步骤所属项目选择运行环境！");
            return false;
          } else if (this.envMap && this.envMap.size > 0) {
            const env = this.envMap.get(this.request.projectId);
            if (!env) {
              this.$warning("请在环境配置中为该步骤所属项目选择运行环境！");
              return false;
            }
          }
        }
        this.request.active = true;
        this.loading = true;
        this.runData = [];
        this.runData.projectId = this.request.projectId;
        this.request.useEnvironment = this.currentEnvironmentId;
        this.request.customizeReq = this.isCustomizeReq;
        let debugData = {
          id: this.currentScenario.id, name: this.currentScenario.name, type: "scenario",
          variables: this.currentScenario.variables, referenced: 'Created', headers: this.currentScenario.headers,
          enableCookieShare: this.enableCookieShare, environmentId: this.currentEnvironmentId, hashTree: [this.request]
        };
        this.runData.push(debugData);
        /*触发执行操作*/
        this.reportId = getUUID();
      },
      runRefresh(data) {
        this.request.requestResult = data;
        this.request.result = undefined;
        this.loading = false;
        this.$emit('refReload',this.request,this.node);
      },
      reload() {
        this.loading = true
        this.$nextTick(() => {
          this.loading = false
        })
      },
      getProjectName(id) {
        const project = this.projectList.find(p => p.id === id);
        return project ? project.name : "";
      }
    }
  }
</script>

<style scoped>
  .ms-api-col-ot-import-button {
    background-color: #EEF5FE;
    margin-right: 20px;
    color: #409EFF;
  }

  /deep/ .el-card__body {
    padding: 10px;
  }

  .tip {
    padding: 3px 5px;
    font-size: 16px;
    border-radius: 4px;
    border-left: 4px solid #783887;
    margin: 20px 0;
  }

  .icon.is-active {
    transform: rotate(90deg);
  }

  .ms-tabs >>> .el-icon-close:before {
    content: "";

  }

  .ms-btn {
    background-color: #409EFF;
    color: white;
  }

  .ms-btn-flot {
    margin: 20px;
    float: right;
  }

  .ms-tag {
    margin-left: 20px;
  }
</style>
