<template>
  <el-card class="table-card" v-loading="result.loading">
    <template v-slot:header>
      <span class="title">
        {{$t('api_test.home_page.running_task_list.title')}}
      </span>
    </template>
    <el-table border :data="tableData" class="adjust-table table-content" height="300px">
      <el-table-column prop="index"  :label="$t('api_test.home_page.running_task_list.table_coloum.index')" width="80" show-overflow-tooltip/>
      <el-table-column prop="name"  :label="$t('commons.name')" width="200" >
        <template v-slot:default="{row}">
          <!-- 若为只读用户不可点击之后跳转-->
          <span v-if="isReadOnly">
            {{ row.name }}
          </span>
          <el-link v-else type="info" @click="redirect(row)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="taskType"  :label="$t('api_test.home_page.running_task_list.table_coloum.task_type')" width="120" show-overflow-tooltip>
        <template v-slot:default="scope">
          <ms-tag v-if="scope.row.taskGroup == 'API_SCENARIO_TEST'" type="success" effect="plain" :content="$t('api_test.home_page.running_task_list.scenario_schedule')"/>
          <ms-tag v-if="scope.row.taskGroup == 'TEST_PLAN_TEST'" type="warning" effect="plain" :content="$t('api_test.home_page.running_task_list.test_plan_schedule')"/>
          <ms-tag v-if="scope.row.taskGroup == 'SWAGGER_IMPORT'" type="danger" effect="plain" :content="$t('api_test.home_page.running_task_list.swagger_schedule')"/>
        </template>
      </el-table-column>
      <el-table-column prop="rule"  :label="$t('api_test.home_page.running_task_list.table_coloum.run_rule')" width="120" show-overflow-tooltip/>
      <el-table-column width="100" :label="$t('api_test.home_page.running_task_list.table_coloum.task_status')">
        <template v-slot:default="scope">
          <div>
            <el-switch
              :disabled="isReadOnly"
              v-model="scope.row.taskStatus"
              class="captcha-img"
              @change="closeTaskConfirm(scope.row)"
            ></el-switch>
          </div>
        </template>


      </el-table-column>
      <el-table-column width="170" :label="$t('api_test.home_page.running_task_list.table_coloum.next_execution_time')">
        <template v-slot:default="scope">
          <span>{{ scope.row.nextExecutionTime | timestampFormatDate }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="creator"  :label="$t('api_test.home_page.running_task_list.table_coloum.create_user')" width="100" show-overflow-tooltip/>
      <el-table-column width="170" :label="$t('api_test.home_page.running_task_list.table_coloum.update_time')">
        <template v-slot:default="scope">
          <span>{{ scope.row.updateTime | timestampFormatDate }}</span>
        </template>
      </el-table-column>

    </el-table>
  </el-card>
</template>

<script>
import {checkoutTestManagerOrTestUser, getCurrentProjectID} from "@/common/js/utils";
import MsTag from "@/business/components/common/components/MsTag";
export default {
  name: "MsRunningTaskList",
  components: {
    MsTag
  },
  props: {
    callFrom: String,
  },
  data() {
    return {
      value: '100',
      result: {},
      tableData: [],
      visible: false,
      loading: false
    }
  },

  computed:{
    isReadOnly(){
      return !checkoutTestManagerOrTestUser();
    }
  },

  methods: {
    search() {
      let projectID = getCurrentProjectID();
      this.result = this.$get("/api/runningTask/"+projectID+"/"+this.callFrom, response => {
        this.tableData = response.data;
      });
    },

    closeTaskConfirm(row){
      let flag = row.taskStatus;
      row.taskStatus = !flag; //保持switch点击前的状态
      this.$confirm(this.$t('api_test.home_page.running_task_list.confirm.close_title'), this.$t('commons.prompt'), {
        confirmButtonText: this.$t('commons.confirm'),
        cancelButtonText: this.$t('commons.cancel'),
        type: 'warning'
      }).then(() => {
        this.updateTask(row);
      }).catch(() => {
      });
    },

    updateTask(taskRow){

      this.result = this.$post('/api/schedule/updateEnableByPrimyKey', taskRow, response => {
        this.search();
      });
    },
    redirect(param){
      if(param.taskGroup === 'TEST_PLAN_TEST'){
        this.$emit('redirectPage','testPlanEdit','', param.scenarioId);
      }else if(param.taskGroup === 'API_SCENARIO_TEST') {
        this.$emit('redirectPage','scenario','scenario', 'edit:'+param.scenarioId);
      }
    }
  },

  created() {
    this.search();
  },
  activated() {
    this.search();
  },
  handleStatus(scope) {
    console.log(scope.row.userId)
  }
  }
</script>

<style scoped>

.el-table {
  cursor:pointer;
}
.el-card /deep/ .el-card__header {
  border-bottom: 0px solid #EBEEF5;
}

</style>
