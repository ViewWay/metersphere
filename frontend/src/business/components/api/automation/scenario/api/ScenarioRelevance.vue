<template>
  <test-case-relevance-base
    :dialog-title="$t('api_test.automation.scenario_import')"
    @setProject="setProject"
    ref="baseRelevance">
    <template v-slot:aside>
      <ms-api-scenario-module
        style="margin-top: 5px;"
        @nodeSelectEvent="nodeChange"
        @refreshTable="refresh"
        @setModuleOptions="setModuleOptions"
        @enableTrash="false"
        :is-read-only="true"
        ref="nodeTree"/>
    </template>

    <ms-api-scenario-list
      :select-node-ids="selectNodeIds"
      :referenced="true"
      :trash-enable="false"
      @selection="setData"
      ref="apiScenarioList"/>

    <template v-slot:footer>
      <el-button type="primary" @click="copy" @keydown.enter.native.prevent>{{$t('commons.copy')}}</el-button>
      <el-button type="primary" @click="reference" @keydown.enter.native.prevent> {{ $t('api_test.scenario.reference') }}</el-button>
    </template>
  </test-case-relevance-base>
</template>

<script>
  import MsContainer from "../../../../common/components/MsContainer";
  import MsAsideContainer from "../../../../common/components/MsAsideContainer";
  import MsMainContainer from "../../../../common/components/MsMainContainer";
  import MsApiScenarioModule from "../ApiScenarioModule";
  import MsApiScenarioList from "../ApiScenarioList";
  import {getUUID} from "../../../../../../common/js/utils";
  import RelevanceDialog from "../../../../track/plan/view/comonents/base/RelevanceDialog";
  import TestCaseRelevanceBase from "@/business/components/track/plan/view/comonents/base/TestCaseRelevanceBase";

  export default {
    name: "ScenarioRelevance",
    components: {
      TestCaseRelevanceBase,
      RelevanceDialog,
      MsApiScenarioList,
      MsApiScenarioModule,
      MsMainContainer, MsAsideContainer, MsContainer
    },
    data() {
      return {
        result: {},
        currentProtocol: null,
        selectNodeIds: [],
        moduleOptions: {},
        isApiListEnable: true,
        currentScenario: [],
        currentScenarioIds: [],
        projectId: ''
      }
    },
    watch: {
      projectId() {
        this.$refs.apiScenarioList.search(this.projectId);
        this.$refs.nodeTree.list(this.projectId);
      }
    },
    methods: {
      reference() {
        let scenarios = [];
        if (!this.currentScenario || this.currentScenario.length < 1) {
          this.$emit('请选择场景');
          return;
        }
        this.currentScenario.forEach(item => {
          let obj = {id: item.id, name: item.name, type: "scenario", referenced: 'REF', resourceId: getUUID(), projectId: item.projectId};
          scenarios.push(obj);
        });
        this.$emit('save', scenarios);
        this.$refs.baseRelevance.close();
      },
      copy() {
        let scenarios = [];
        if (!this.currentScenarioIds || this.currentScenarioIds.length < 1) {
          this.$warning('请选择场景');
          return;
        }
        this.result = this.$post("/api/automation/getApiScenarios/", this.currentScenarioIds, response => {
          if (response.data) {
            response.data.forEach(item => {
              let scenarioDefinition = JSON.parse(item.scenarioDefinition);
              if (scenarioDefinition && scenarioDefinition.hashTree) {
                let obj = {id: item.id, name: item.name, type: "scenario", headers: scenarioDefinition.headers, variables: scenarioDefinition.variables, environmentMap: scenarioDefinition.environmentMap,
                  referenced: 'Copy', resourceId: getUUID(), hashTree: scenarioDefinition.hashTree, projectId: item.projectId};
                scenarios.push(obj);
              }
            });
            this.$emit('save', scenarios);
            this.$refs.baseRelevance.close();
          }
        })
      },
      close() {
        this.refresh();
        this.$refs.relevanceDialog.close();
      },
      open() {
        this.$refs.baseRelevance.open();
        if (this.$refs.apiScenarioList) {
          this.$refs.apiScenarioList.search(this.projectId);
        }
      },
      nodeChange(node, nodeIds, pNodes) {
        this.selectNodeIds = nodeIds;
      },
      handleProtocolChange(protocol) {
        this.currentProtocol = protocol;
      },
      setModuleOptions(data) {
        this.moduleOptions = data;
      },
      refresh() {
        this.$refs.apiScenarioList.search();
      },
      setData(data) {
        this.currentScenario = Array.from(data).map(row => row);
        this.currentScenarioIds = Array.from(data).map(row => row.id);
      },
      setProject(projectId) {
        this.projectId = projectId;
      },
    }
  }
</script>

<style scoped>

</style>
