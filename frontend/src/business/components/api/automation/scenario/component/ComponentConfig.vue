<template>
  <div class="request-form">
    <component :is="component" :isMax="isMax" :show-btn="showBtn"
               :scenario="scenario" :controller="scenario" :timer="scenario" :assertions="scenario" :extract="scenario" :jsr223-processor="scenario" :request="scenario" :currentScenario="currentScenario" :currentEnvironmentId="currentEnvironmentId" :node="node"
               :draggable="draggable" :title="title" :color="titleColor" :background-color="backgroundColor" @suggestClick="suggestClick(node)" :response="response"
               @remove="remove" @copyRow="copyRow" @refReload="refReload" @openScenario="openScenario" :project-list="projectList" :env-map="envMap"/>
  </div>
</template>

<script>
  import MsConstantTimer from "./ConstantTimer";
  import MsIfController from "./IfController";
  import {ELEMENT_TYPE} from "../Setting";
  import MsJsr233Processor from "./Jsr233Processor";
  import MsApiAssertions from "../../../definition/components/assertion/ApiAssertions";
  import MsApiExtract from "../../../definition/components/extract/ApiExtract";
  import MsApiComponent from "./ApiComponent";
  import MsLoopController from "./LoopController";
  import MsApiScenarioComponent from "./ApiScenarioComponent";
  import JmeterElementComponent from "./JmeterElementComponent";

  export default {
    name: "ComponentConfig",
    components: {MsConstantTimer, MsIfController, MsJsr233Processor, MsApiAssertions, MsApiExtract, MsApiComponent, MsLoopController, MsApiScenarioComponent, JmeterElementComponent},
    props: {
      type: String,
      scenario: {},
      draggable: {
        type: Boolean,
        default: true,
      },
      isMax: {
        type: Boolean,
        default: false,
      },
      showBtn: {
        type: Boolean,
        default: true,
      },
      currentScenario: {},
      currentEnvironmentId: String,
      response: {},
      node: {},
      projectList: Array,
      envMap: Map
    },
    data() {
      return {
        title: "",
        titleColor: "",
        backgroundColor: "",
      }
    },
    computed: {
      component({type}) {
        let name;
        switch (type) {
          case ELEMENT_TYPE.IfController:
            name = "MsIfController";
            break;
          case ELEMENT_TYPE.ConstantTimer:
            name = "MsConstantTimer";
            break;
          case ELEMENT_TYPE.JSR223Processor:
            name = this.getComponent(ELEMENT_TYPE.JSR223Processor);
            break;
          case ELEMENT_TYPE.JSR223PreProcessor:
            name = this.getComponent(ELEMENT_TYPE.JSR223PreProcessor);
            break;
          case ELEMENT_TYPE.JSR223PostProcessor:
            name = this.getComponent(ELEMENT_TYPE.JSR223PostProcessor);
            break;
          case ELEMENT_TYPE.Assertions:
            name = "MsApiAssertions";
            break;
          case ELEMENT_TYPE.Extract:
            name = "MsApiExtract";
            break;
          case ELEMENT_TYPE.CustomizeReq:
            name = "MsApiComponent";
            break;
          case  ELEMENT_TYPE.LoopController:
            name = "MsLoopController";
            break;
          case ELEMENT_TYPE.scenario:
            name = "MsApiScenarioComponent";
            break;
          case "AuthManager":
            break;
          case "JmeterElement":
            name = "JmeterElementComponent";
            break;
          default:
            name = "MsApiComponent";
            break;
        }
        return name;
      }
    },
    methods: {
      getComponent(type) {
        if (type === ELEMENT_TYPE.JSR223PreProcessor) {
          this.title = this.$t('api_test.definition.request.pre_script');
          this.titleColor = "#B8741A";
          this.backgroundColor = "#F9F1EA";
          return "MsJsr233Processor";
        } else if (type === ELEMENT_TYPE.JSR223PostProcessor) {
          this.title = this.$t('api_test.definition.request.post_script');
          this.titleColor = "#783887";
          this.backgroundColor = "#F2ECF3";
          return "MsJsr233Processor";
        } else {
          this.title = this.$t('api_test.automation.customize_script');
          this.titleColor = "#7B4D12";
          this.backgroundColor = "#F1EEE9";
          return "MsJsr233Processor";
        }
      },
      remove(row, node) {
        this.$emit('remove', row, node);

      },
      copyRow(row, node) {
        this.$emit('copyRow', row, node);

      },
      openScenario(data) {
        this.$emit('openScenario', data);
      },
      suggestClick(node) {
        this.$emit('suggestClick', node);
      },
      refReload(data, node) {
        this.$emit('refReload', data, node);
      }
    }
  }
</script>

<style scoped>
  .request-form >>> .debug-button {
    margin-left: auto;
    display: block;
    margin-right: 10px;
  }

</style>
