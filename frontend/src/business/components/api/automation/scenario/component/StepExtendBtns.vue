<template>
  <div>
    <el-dropdown @command="handleCommand" class="scenario-ext-btn">
      <el-link type="primary" :underline="false">
        <el-icon class="el-icon-more"></el-icon>
      </el-link>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="copy">复制步骤</el-dropdown-item>
        <el-dropdown-item command="remove" v-tester>删除步骤</el-dropdown-item>
        <el-dropdown-item command="scenarioVar" v-tester v-if="data.type==='scenario'">查看场景变量</el-dropdown-item>
        <el-dropdown-item command="openScenario" v-tester v-if="data.type==='scenario' && data.referenced==='REF'">打开场景</el-dropdown-item>
        <el-dropdown-item command="saveAs" v-tester v-if="allSamplers.indexOf(data.type)!=-1">另存为接口定义</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <ms-variable-list ref="scenarioParameters"/>
    <ms-add-basis-api ref="api"/>
  </div>
</template>

<script>
  import {getCurrentProjectID, getUUID} from "@/common/js/utils";
  import {ELEMENTS} from "../Setting";
  import MsVariableList from "../variable/VariableList";
  import MsAddBasisApi from "../api/AddBasisApi";

  export default {
    name: "StepExtendBtns",
    components: {ELEMENTS, MsVariableList, MsAddBasisApi},
    props: {
      data: Object,
    },
    data() {
      return {
        allSamplers: ELEMENTS.get('AllSamplerProxy'),
      }
    },
    methods: {
      handleCommand(cmd) {
        switch (cmd) {
          case  "copy":
            this.$emit('copy');
            break;
          case "remove":
            this.$emit('remove');
            break;
          case "scenarioVar":
            this.$refs.scenarioParameters.open(this.data.variables, this.data.headers, true);
            break;
          case "openScenario":
            this.getScenario();
            break;
          case "saveAs":
            this.saveAsApi();
            break;
        }
      },
      getScenario() {
        this.result = this.$get("/api/automation/getApiScenario/" + this.data.id, response => {
          if (response.data) {
            this.$emit('openScenario', response.data);
          } else {
            this.$error("引用场景已经被删除");
          }
        });
      },
      saveAsApi(){
        this.$refs.api.open(this.data);
      }
    }
  }
</script>

<style scoped>
  .scenario-ext-btn {
    margin-left: 10px;
  }
</style>
