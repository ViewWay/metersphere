<template>
  <div class="ms-header">
    <div class="ms-div" v-loading="loading" @click="showAll">
      <!-- 调试部分 -->
      <el-row class="ms-header-margin">
        <el-col :span="8">
          {{currentScenario.name}}
        </el-col>
        <el-col :span="8">
          {{$t('api_test.automation.step_total')}}：{{scenarioDefinition.length}}
        </el-col>
        <el-col :span="8">
          <el-link class="head" @click="showScenarioParameters">{{$t('api_test.automation.scenario_total')}}</el-link>
          ：{{varSize }}
        </el-col>
      </el-row>
    </div>
    <div class="ms-header-right">
      <el-checkbox v-model="cookieShare" @change="setCookieShare" style="margin-right: 20px">共享cookie</el-checkbox>

      <env-popover :env-map="envMap" :project-ids="projectIds" @setProjectEnvMap="setProjectEnvMap"
                   :project-list="projectList" ref="envPopover" class="ms-right"/>

      <el-button :disabled="scenarioDefinition.length < 1" size="mini" type="primary" v-prevent-re-click @click="runDebug">{{$t('api_test.request.debug')}}</el-button>

      <font-awesome-icon class="alt-ico" :icon="['fa', 'compress-alt']" size="lg" @click="unFullScreen"/>
      ｜
      <i class="el-icon-close alt-ico-close" @click="close"/>
    </div>
  </div>
</template>

<script>
  import {checkoutTestManagerOrTestUser, exportPdf} from "@/common/js/utils";
  import html2canvas from 'html2canvas';
  import EnvPopover from "../../scenario/EnvPopover";

  export default {
    name: "ScenarioHeader",
    components: {EnvPopover},
    props: {currentScenario: {}, scenarioDefinition: Array, enableCookieShare: Boolean, projectEnvMap: Map, projectIds: Set, projectList: Array},
    data() {
      return {
        envMap: new Map,
        loading: false,
        varSize: 0,
        cookieShare: false,
      }
    },
    mounted() {
      this.envMap = this.projectEnvMap;
      this.getVariableSize();
      this.cookieShare = this.enableCookieShare;
    },
    methods: {
      handleExport() {
        let name = this.$t('commons.report_statistics.test_case_analysis');
        this.$nextTick(function () {
          setTimeout(() => {
            html2canvas(document.getElementById('reportAnalysis'), {
              scale: 2
            }).then(function (canvas) {
              exportPdf(name, [canvas]);
            });
          }, 1000);
        });
      },
      showAll() {
        this.$emit('showAllBtn');
      },
      close() {
        this.$emit('closePage', this.currentScenario.name);
      },
      unFullScreen() {
        this.$emit('unFullScreen');
      },
      runDebug() {
        this.$emit('runDebug');
      },
      setCookieShare() {
        this.$emit('setCookieShare', this.cookieShare);
      },
      showScenarioParameters() {
        this.$emit('showScenarioParameters');
      },
      getVariableSize() {
        let size = 0;
        if (this.currentScenario.variables) {
          size += this.currentScenario.variables.length;
        }
        if (this.currentScenario.headers && this.currentScenario.headers.length > 1) {
          size += this.currentScenario.headers.length - 1;
        }
        this.varSize = size;
        this.reload();
      },
      reload() {
        this.loading = true
        this.$nextTick(() => {
          this.loading = false
        })
      },
      setProjectEnvMap(projectEnvMap) {
        this.$emit('setProjectEnvMap', projectEnvMap);
        this.envMap = projectEnvMap;
      }
    },
  }
</script>

<style scoped>
  .ms-header {
    border-bottom: 1px solid #E6E6E6;
    height: 50px;
    background-color: #FFF;
  }

  .ms-div {
    float: left;
    width: 60%;
    margin-left: 20px;
    margin-top: 12px;
  }

  .ms-header-right {
    float: right;
    margin-top: 2px;
    position: fixed;
    right: 20px;
    z-index: 1;

  }

  .alt-ico {
    font-size: 16px;
    margin: 10px 10px 0px;
  }

  .alt-ico-close {
    font-size: 18px;
    margin: 10px 10px 10px;
  }

  .alt-ico-close:hover {
    color: black;
    cursor: pointer;
  }

  .alt-ico:hover {
    color: black;
    cursor: pointer;
  }

  .ms-header-margin {
    margin-top: 3px;
  }

  .ms-right {
    margin-right: 40px;
  }

  .head {
    border-bottom: 1px solid #303133;
    color: #303133;
    font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", Arial, sans-serif;
    font-size: 13px;
  }
</style>
