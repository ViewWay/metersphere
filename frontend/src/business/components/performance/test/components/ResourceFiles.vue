<template>
  <el-dialog :close-on-click-modal="false"
             :destroy-on-close="true"
             :title="$t('load_test.exist_jmx')" width="70%"
             :visible.sync="loadFileVisible">
    <el-row>
      <el-upload
        style="padding-right: 10px;"
        accept=".jmx,.jar,.csv,.json,.pdf,.jpg,.png,.jpeg,.doc,.docx,.xlsx"
        action=""
        :limit="fileNumLimit"
        multiple
        :show-file-list="false"
        :before-upload="beforeUploadFile"
        :http-request="handleUpload"
        :on-exceed="handleExceed"
        :file-list="fileList">
        <ms-table-button :is-tester-permission="true" icon="el-icon-upload2"
                         :content="$t('load_test.upload_file')"/>
      </el-upload>
    </el-row>

    <el-table v-loading="projectLoadingResult.loading"
              class="basic-config"
              :data="existFiles">

      <el-table-column type="selection"/>
      <el-table-column
        prop="name"
        :label="$t('load_test.file_name')">
      </el-table-column>
      <el-table-column
        prop="type"
        :label="$t('load_test.file_type')">
      </el-table-column>
      <el-table-column
        :label="$t('load_test.last_modify_time')">
        <template v-slot:default="scope">
          <i class="el-icon-time"/>
          <span class="last-modified">{{ scope.row.updateTime | timestampFormatDate }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('commons.operating')">
        <template v-slot:default="scope">
          <ms-table-operator-button :is-tester-permission="true"
                                    icon="el-icon-delete"
                                    type="danger"
                                    :tip="$t('commons.delete')"
                                    @exec="handleDelete(scope.row)">
          </ms-table-operator-button>
        </template>
      </el-table-column>
    </el-table>
    <ms-table-pagination :change="getProjectFiles" :current-page.sync="currentPage" :page-size.sync="pageSize"
                         :total="total"/>

  </el-dialog>
</template>

<script>
import MsTablePagination from "@/business/components/common/pagination/TablePagination";
import MsTableButton from "@/business/components/common/components/MsTableButton";
import MsDialogFooter from "@/business/components/common/components/MsDialogFooter";
import {getCurrentProjectID} from "@/common/js/utils";
import MsTableOperatorButton from "@/business/components/common/components/MsTableOperatorButton";
import {Message} from "element-ui";

export default {
  name: "MsResourceFiles",
  components: {MsTableOperatorButton, MsDialogFooter, MsTableButton, MsTablePagination},
  data() {
    return {
      loadFileVisible: false,
      projectLoadingResult: {},
      currentPage: 1,
      pageSize: 5,
      total: 0,
      existFiles: [],
      fileList: [],
      uploadList: [],
      fileNumLimit: 10,
    }
  },
  methods: {
    open() {
      this.loadFileVisible = true;
      this.getProjectFiles();
    },
    close() {
      this.loadFileVisible = false;
      this.selectIds.clear();
    },
    getProjectFiles() {
      this.projectLoadingResult = this.$get('/performance/project/all/' + getCurrentProjectID() + "/" + this.currentPage + "/" + this.pageSize, res => {
        let data = res.data;
        this.total = data.itemCount;
        this.existFiles = data.listObject;
      })
    },
    fileValidator(file) {
      /// todo: 是否需要对文件内容和大小做限制
      return file.size > 0;
    },
    beforeUploadFile(file) {
      if (!this.fileValidator(file)) {
        /// todo: 显示错误信息
        return false;
      }

      return true;
    },
    handleUpload(uploadResources) {
      let file = uploadResources.file;
      let formData = new FormData();
      let url = '/project/upload/files/' + getCurrentProjectID()
      formData.append("file", file);
      let options = {
        method: 'POST',
        url: url,
        data: formData,
        headers: {
          'Content-Type': undefined
        }
      }
      this.$request(options, (response) => {
        this.$success(this.$t('commons.save_success'));
        this.getProjectFiles();
      });
    },
    handleExceed() {
      this.$error(this.$t('load_test.file_size_limit'));
    },
    handleDelete(row) {
      console.log(row);
      this.$confirm(this.$t('project.file_delete_tip', [row.name]), '', {
        confirmButtonText: this.$t('commons.confirm'),
        cancelButtonText: this.$t('commons.cancel'),
        type: 'warning'
      }).then(() => {
        this.$get('/project/delete/file/' + row.id, response => {
          Message.success(this.$t('commons.delete_success'));
          this.getProjectFiles();
        });
      }).catch(() => {

      });
    }
  }
}
</script>

<style scoped>

</style>
