<template>
  <el-card class="table-card" v-loading="result.loading" body-style="padding:10px;">
    <div slot="header" >
      <span class="title">
        关联用例数量统计
      </span>
    </div>

    <el-container>
      <el-aside width="120px">
        <div class="main-number-show">
          <span class="count-number">
            {{relevanceCountData.allRelevanceCaseCount}}
          </span>
          <span style="color: #6C317C;">
            {{$t('api_test.home_page.unit_of_measurement')}}
          </span>
        </div>
      </el-aside>
      <el-main style="padding-left: 0px;padding-right: 0px;">
        <div style="width: 300px;margin:0 auto">

          <el-row align="center">
            <el-col :span="6" style="width:90px;padding: 5px;border-right-style: solid;border-right-width: 1px;border-right-color: #ECEEF4;">
              <div class="count-info-div" v-html="relevanceCountData.apiCaseCountStr"></div>
            </el-col>
            <el-col :span="6" style="width:90px;padding: 5px;border-right-style: solid;border-right-width: 1px;border-right-color: #ECEEF4;">
              <div class="count-info-div" v-html="relevanceCountData.scenarioCaseStr"></div>
            </el-col>
            <el-col :span="6" style="width:90px;padding: 5px;">
              <div class="count-info-div" v-html="relevanceCountData.performanceCaseCountStr"></div>
            </el-col>
          </el-row>
        </div>
      </el-main>
    </el-container>

    <el-container class="detail-container">
      <el-header style="height:20px;padding: 0px;margin-bottom: 10px;">
        <el-row>
          <el-col>
            {{$t('api_test.home_page.api_details_card.this_week_add')}}
            <el-link type="info" @click="redirectPage('thisWeekCount')" target="_blank" style="color: #000000">{{relevanceCountData.thisWeekAddedCount}}
            </el-link>
            {{$t('api_test.home_page.unit_of_measurement')}}
          </el-col>
        </el-row>
      </el-header>
      <el-main style="padding: 5px;margin-top: 10px">
        <el-container>
          <el-aside width="60%" class="count-number-show" style="margin-bottom: 0px;margin-top: 0px">
            <el-container>
              <el-aside width="30%">
                覆盖率:
              </el-aside>
              <el-main style="padding: 0px 0px 0px 0px; line-height: 100px; text-align: center;">
                <span class="count-number">
                {{relevanceCountData.coverageRage}}
              </span>
              </el-main>
            </el-container>
          </el-aside>
          <el-main style="padding: 5px">
            <el-card class="no-shadow-card" body-style="padding-left:5px;padding-right:5px">
              <main>
                <el-row>
                  <el-col>
                    <span class="default-property">
                      未覆盖
                      {{"\xa0\xa0"}}
                      <el-link type="info" @click="redirectPage('uncoverage')" target="_blank" style="color: #000000">
                        {{relevanceCountData.uncoverageCount}}
                      </el-link>
                    </span>
                  </el-col>
                  <el-col style="margin-top: 5px;">
                    <span class="main-property">
                      已覆盖
                      {{"\xa0\xa0"}}
                      <el-link type="info" @click="redirectPage('coverage')" target="_blank" style="color: #000000">
                        {{relevanceCountData.coverageCount}}
                      </el-link>
                    </span>
                  </el-col>
                </el-row>
              </main>
            </el-card>
          </el-main>
        </el-container>
      </el-main>
    </el-container>
  </el-card>
</template>

<script>
export default {
  name: "RelevanceCaseCard",
  props:{
    relevanceCountData:{},
  },
  data() {
    return {
      result: {

      }
    }
  },
  methods: {
    redirectPage(clickType){
      this.$emit("redirectPage","case","case",clickType);
    }
  }
}
</script>

<style scoped>
.el-aside {
  line-height: 100px;
  text-align: center;
}
.count-number{
  font-family:'ArialMT', 'Arial', sans-serif;
  font-size:33px;
  color: var(--count_number);
}

.main-number-show {
  width: 100px;
  height: 100px;
  border-style: solid;
  border-width: 7px;
  border-color: var(--count_number_shallow);
  border-radius:50%;

}

.count-number-show{
  margin:20px auto;
}
.detail-container{
  margin-top: 30px
}
.no-shadow-card{
  -webkit-box-shadow: 0 0px 0px 0 rgba(0,0,0,.1);
  box-shadow: 0 0px 0px 0 rgba(0,0,0,.1);
}
.default-property{
  font-size: 12px
}
.main-property{
  color: #F39021;
  font-size: 12px
}

.el-card /deep/ .el-card__header {
  border-bottom: 0px solid #EBEEF5;
}

.count-info-div{
  margin: 3px;
}
.count-info-div >>>p{
  font-size: 10px;
}
</style>
