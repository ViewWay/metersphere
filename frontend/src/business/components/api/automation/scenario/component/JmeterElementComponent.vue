<template>
  <api-base-component
    @copy="copyRow"
    @remove="remove"
    @active="active"
    :data="request"
    :draggable="draggable"
    :color="defColor"
    :is-max="isMax"
    :show-btn="showBtn"
    :background-color="defBackgroundColor"
    :title="request.elementType">
    <div style="height: 300px;width: 100%">
      <ms-code-edit mode="xml" :data.sync="request.jmeterElement" theme="eclipse" ref="codeEdit"/>
    </div>
  </api-base-component>
</template>

<script>
  import MsCodeEdit from "../../../../common/components/MsCodeEdit";
  import MsInstructionsIcon from "../../../../common/components/MsInstructionsIcon";
  import MsDropdown from "../../../../common/components/MsDropdown";
  import ApiBaseComponent from "../common/ApiBaseComponent";
  import Jsr233ProcessorContent from "../common/Jsr233ProcessorContent";

  export default {
    name: "JmeterElementComponent",
    components: {Jsr233ProcessorContent, ApiBaseComponent, MsDropdown, MsInstructionsIcon, MsCodeEdit},
    props: {
      draggable: {
        type: Boolean,
        default: false,
      },
      isReadOnly: {
        type: Boolean,
        default:
          false
      },
      isMax: {
        type: Boolean,
        default: false,
      },
      showBtn: {
        type: Boolean,
        default: true,
      },
      request: {
        type: Object,
      },
      defTitle: {type: String, default: "Jmeter组件"},
      defColor: {type: String, default: "#606260"},
      defBackgroundColor: {type: String, default: "#F4F4FF"},
      node: {},
    },
    methods: {
      remove() {
        this.$emit('remove', this.request, this.node);
      },
      copyRow() {
        this.$emit('copyRow', this.request, this.node);
      },
      active() {
        this.request.active = !this.request.active;
      },
    }
  }
</script>

<style scoped>
  /deep/ .el-divider {
    margin-bottom: 10px;
  }
</style>
