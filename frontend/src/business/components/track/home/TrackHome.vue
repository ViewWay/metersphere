<template>
  <ms-container>
    <el-header height="0">
      <div style="float: right">
        <div v-if="tipsType==='1'">
          🤔️ 天凉了，保温杯买了吗？
        </div>
        <div v-else-if="tipsType==='2'">
          😔 觉得MeterSphere不好用就来
          <el-link href="https://github.com/metersphere/metersphere/issues" target="_blank" style="color: black"
                   type="primary">https://github.com/metersphere/metersphere/issues
          </el-link>
          吐个槽吧！
        </div>
        <div v-else-if="tipsType==='3'">
          😄 觉得MeterSphere好用就来
          <el-link href="https://github.com/metersphere/metersphere" target="_blank" style="color: black"
                   type="primary">https://github.com/metersphere/metersphere
          </el-link>
          点个star吧！
        </div>
        <div v-else>
          😊 MeterSphere温馨提醒 —— 多喝热水哟！
        </div>
      </div>
    </el-header>
    <ms-main-container v-loading="result.loading">
      <el-row :gutter="0"></el-row>
      <el-row :gutter="10">
        <el-col :span="6">
          <div class="square">
            <case-count-card :track-count-data="trackCountData" class="track-card" @redirectPage="redirectPage"/>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="square">
            <relevance-case-card :relevance-count-data="relevanceCountData" class="track-card" @redirectPage="redirectPage"/>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="square">
            <case-maintenance :case-option="caseOption" class="track-card"/>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="10">
        <el-col :span="12">
          <bug-count-card class="track-card"/>
        </el-col>
        <el-col :span="12">
          <ms-failure-test-case-list class="track-card"/>
        </el-col>
      </el-row>

      <el-row :gutter="10">
        <el-col :span="12">
          <review-list class="track-card"/>
        </el-col>
        <el-col :span="12">
          <ms-running-task-list :call-from="'track_home'" class="track-card" @redirectPage="redirectPage"/>
        </el-col>
      </el-row>


    </ms-main-container>
  </ms-container>
</template>

<script>

import MsMainContainer from "@/business/components/common/components/MsMainContainer";
import MsContainer from "@/business/components/common/components/MsContainer";
import CaseCountCard from "@/business/components/track/home/components/CaseCountCard";
import RelevanceCaseCard from "@/business/components/track/home/components/RelevanceCaseCard";
import {getCurrentProjectID,getUUID} from "@/common/js/utils";
import CaseMaintenance from "@/business/components/track/home/components/CaseMaintenance";
import {COUNT_NUMBER, COUNT_NUMBER_SHALLOW} from "@/common/js/constants";
import BugCountCard from "@/business/components/track/home/components/BugCountCard";
import ReviewList from "@/business/components/track/home/components/ReviewList";
import MsRunningTaskList from "@/business/components/api/homepage/components/RunningTaskList";
import MsFailureTestCaseList from "@/business/components/api/homepage/components/FailureTestCaseList";

require('echarts/lib/component/legend');
export default {
  name: "TrackHome",
  components: {
    ReviewList,
    BugCountCard,
    RelevanceCaseCard,
    CaseCountCard,
    MsMainContainer,
    MsContainer,
    CaseMaintenance,
    MsRunningTaskList,
    MsFailureTestCaseList
  },
  data() {
    return {
      tipsType: "1",
      result: {},
      trackCountData: {},
      relevanceCountData: {},
      caseOption: {}
    }
  },
  activated() {
    this.checkTipsType();
    this.init();
  },
  methods: {
    checkTipsType() {
      let random = Math.floor(Math.random() * (4 - 1 + 1)) + 1;
      this.tipsType = random + "";
    },
    init() {
      let selectProjectId = getCurrentProjectID();

      this.$get("/track/count/" + selectProjectId, response => {
        this.trackCountData = response.data;
      });

      this.$get("/track/relevance/count/" + selectProjectId, response => {
        this.relevanceCountData = response.data;
      });

      this.$get("/track/case/bar/" + selectProjectId, response => {
        let data = response.data;
        this.setBarOption(data);
      })
    },
    setBarOption(data) {
      let xAxis = [];
      data.map(d => {
        if (!xAxis.includes(d.xAxis)) {
          xAxis.push(d.xAxis);
        }
      });
      let yAxis1 = data.filter(d => d.groupName === 'FUNCTIONCASE').map(d => d.yAxis);
      let yAxis2 = data.filter(d => d.groupName === 'RELEVANCECASE').map(d => d.yAxis);
      let option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: xAxis
        },
        yAxis: {
          type: 'value',
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          }
        },
        legend: {
          data: ["功能用例数", "关联用例数"],
          orient: 'vertical',
          right: '80',
        },
        series: [{
          name: "功能用例数",
          data: yAxis1,
          type: 'bar',
          itemStyle: {
            normal: {
              color: this.$store.state.theme.theme ? this.$store.state.theme.theme : COUNT_NUMBER
            }
          }
        },
          {
            name: "关联用例数",
            data: yAxis2,
            type: 'bar',
            itemStyle: {
              normal: {
                color: this.$store.state.theme.theme ? this.$store.state.theme.theme : COUNT_NUMBER_SHALLOW
              }
            }
          }]
      };
      this.caseOption = option;
    },
    redirectPage(page,dataType,selectType){
      //test_plan 页面跳转
      this.$router.push('/track/plan/view/'+selectType);
      switch (page){
        case "case":
          this.$router.push({name:'testCase',params:{dataType:dataType,dataSelectRange:selectType, projectId: getCurrentProjectID()}});
          break;
      }
    }
  }
}
</script>

<style scoped>
.square {
  width: 100%;
  height: 400px;
}

.rectangle {
  width: 100%;
  height: 400px;
}

.el-row {
  margin-bottom: 20px;
  margin-left: 20px;
  margin-right: 20px;
}

.track-card {
  height: 100%;
}
</style>
