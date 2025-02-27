<template>
  <ms-module-minder
    v-loading="result.loading"
    :tree-nodes="treeNodes"
    :data-map="dataMap"
    :tags="tags"
    :distinct-tags="[...tags, $t('test_track.plan.plan_status_prepare')]"
    @save="save"
  />
</template>

<script>
import MsModuleMinder from "@/business/components/common/components/MsModuleMinder";
import {getTestCaseDataMap} from "@/business/components/track/common/minder/minderUtils";
export default {
name: "TestPlanMinder",
  components: {MsModuleMinder},
  data() {
    return{
      dataMap: new Map(),
      result: {},
      tags: [this.$t('test_track.plan_view.pass'), this.$t('test_track.plan_view.failure'), this.$t('test_track.plan_view.blocking'), this.$t('test_track.plan_view.skip')],
    }
  },
  props: {
    treeNodes: {
      type: Array,
      default() {
        return []
      }
    },
    selectNodeIds: {
      type: Array
    },
    planId: {
      type: String
    },
    projectId: String
  },
  mounted() {
    this.$nextTick(() => {
      this.getTestCases();
    })
  },
  methods: {
    getTestCases() {
      if (this.projectId) {
        this.result = this.$get('/test/plan/case/list/minder/' + this.planId, response => {
          this.dataMap = getTestCaseDataMap(response.data, true, (data, item) => {
            if (item.stats === 'Pass') {
              data.resource.push(this.$t('test_track.plan_view.pass'));
            } else if (item.reviewStatus === 'Failure') {
              data.resource.push(this.$t('test_track.plan_view.failure'));
            } else if (item.reviewStatus === 'Blocking') {
              data.resource.push(this.$t('test_track.plan_view.blocking'));
            } else if (item.reviewStatus === 'Skip') {
              data.resource.push(this.$t('test_track.plan_view.skip'));
            } else {
              data.resource.push(this.$t('test_track.plan.plan_status_prepare'));
            }
          });
        });
      }
    },
    save(data) {
      let saveCases = [];
      this.buildSaveCase(data.root, saveCases);
      this.result = this.$post('/test/plan/case/minder/edit', saveCases, () => {
        this.$success(this.$t('commons.save_success'));
      });
    },
    buildSaveCase(root, saveCases) {
      let data = root.data;
      if (data.resource && data.resource.indexOf(this.$t('api_test.definition.request.case')) > -1) {
        this._buildSaveCase(root, saveCases, parent);
      } else {
        if (root.children) {
          root.children.forEach((childNode) => {
            this.buildSaveCase(childNode, saveCases, root.data);
          })
        }
      }
    },
    _buildSaveCase(node, saveCases) {
      let data = node.data;
      if (!data.changed) {
        return;
      }
      let testCase = {
        id: data.id,
      };
      if (data.resource.length > 1) {
        if (data.resource.indexOf(this.$t('test_track.plan_view.failure')) > -1) {
          testCase.status = 'Failure';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.pass')) > -1) {
          testCase.status = 'Pass';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.blocking')) > -1) {
          testCase.status = 'Blocking';
        } else if (data.resource.indexOf(this.$t('test_track.plan_view.skip')) > -1) {
          testCase.status = 'Skip';
        }
      }
      saveCases.push(testCase);
    }
  }
}
</script>

<style scoped>

</style>
