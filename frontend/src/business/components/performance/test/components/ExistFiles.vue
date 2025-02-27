<template>
  <el-dialog :close-on-click-modal="false"
             :destroy-on-close="true"
             :title="$t('load_test.exist_jmx')" width="70%"
             :visible.sync="loadFileVisible">
    <el-row>

      <el-upload
        v-if="loadType === 'jmx'"
        style="padding-right: 10px;"
        accept=".jmx"
        action=""
        multiple
        :limit="fileNumLimit"
        :show-file-list="false"
        :before-upload="beforeUploadFile"
        :http-request="handleUpload"
        :on-exceed="handleExceed"
        :disabled="isReadOnly"
        :file-list="fileList">
        <ms-table-button :is-tester-permission="true" icon="el-icon-upload2"
                         :content="$t('load_test.upload_jmx')"/>
      </el-upload>
      <el-upload
        v-else
        style="padding-right: 10px;"
        accept=".jar,.csv,.json,.pdf,.jpg,.png,.jpeg,.doc,.docx,.xlsx"
        action=""
        :limit="fileNumLimit"
        multiple
        :show-file-list="false"
        :before-upload="beforeUploadFile"
        :http-request="handleUpload"
        :on-exceed="handleExceed"
        :disabled="isReadOnly"
        :file-list="fileList">
        <ms-table-button :is-tester-permission="true" icon="el-icon-upload2"
                         :content="$t('load_test.upload_file')"/>
      </el-upload>
    </el-row>

    <el-table v-loading="projectLoadingResult.loading"
              class="basic-config"
              :data="existFiles"
              @select-all="handleSelectAll"
              @select="handleSelectionChange">

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
    </el-table>
    <ms-table-pagination :change="getProjectFiles" :current-page.sync="currentPage" :page-size.sync="pageSize"
                         :total="total"/>

    <template v-slot:footer>
      <ms-dialog-footer @cancel="close" @confirm="handleImport"/>
    </template>
  </el-dialog>
</template>

<script>
import MsDialogFooter from "@/business/components/common/components/MsDialogFooter";
import MsTablePagination from "@/business/components/common/pagination/TablePagination";
import {getCurrentProjectID} from "@/common/js/utils";
import {findThreadGroup} from "@/business/components/performance/test/model/ThreadGroup";
import MsTableButton from "@/business/components/common/components/MsTableButton";
import axios from "axios";

export default {
  name: "ExistFiles",
  components: {MsTableButton, MsTablePagination, MsDialogFooter},
  props: {
    fileList: Array,
    tableData: Array,
    uploadList: Array,
    scenarios: Array,
    isReadOnly: Boolean,
  },
  data() {
    return {
      loadFileVisible: false,
      projectLoadingResult: {},
      currentPage: 1,
      pageSize: 5,
      total: 0,
      loadType: 'jmx',
      existFiles: [],
      selectIds: new Set,
      fileNumLimit: 10,
    }
  },
  methods: {
    open(loadType) {
      this.loadFileVisible = true;
      this.loadType = loadType;
      this.getProjectFiles();
    },
    close() {
      this.loadFileVisible = false;
      this.selectIds.clear();
    },
    handleSelectAll(selection) {
      if (selection.length > 0) {
        this.existFiles.forEach(item => {
          this.selectIds.add(item.id);
        });
      } else {
        this.existFiles.forEach(item => {
          if (this.selectIds.has(item.id)) {
            this.selectIds.delete(item.id);
          }
        });
      }
    },
    handleSelectionChange(selection, row) {
      if (this.selectIds.has(row.id)) {
        this.selectIds.delete(row.id);
      } else {
        this.selectIds.add(row.id);
      }
    },
    getProjectFiles() {
      this.projectLoadingResult = this.$get('/performance/project/' + this.loadType + '/' + getCurrentProjectID() + "/" + this.currentPage + "/" + this.pageSize, res => {
        let data = res.data;
        this.total = data.itemCount;
        this.existFiles = data.listObject;
      })
    },
    handleImport() {
      if (this.selectIds.size === 0) {
        this.loadFileVisible = false;
        return;
      }

      let rows = this.existFiles.filter(f => this.selectIds.has(f.id));
      for (let i = 0; i < rows.length; i++) {
        let row = rows[i];
        if (this.tableData.filter(f => f.name === row.name).length > 0) {
          this.$error(this.$t('load_test.delete_file'));
          this.selectIds.clear();
          return;
        }
      }

      if (this.loadType === 'resource') {
        rows.forEach(row => {
          this.fileList.push(row);
          this.tableData.push({
            name: row.name,
            size: (row.size / 1024).toFixed(2) + ' KB',
            type: 'JMX',
            updateTime: row.lastModified,
          });
        })
        this.$success(this.$t('test_track.case.import.success'));
        this.loadFileVisible = false;
        this.selectIds.clear();
        return;
      }

      this.projectLoadingResult = this.$post('/performance/export/jmx', [...this.selectIds], (response) => {
        let data = response.data;
        if (!data) {
          return;
        }
        data.forEach(d => {
          let threadGroups = findThreadGroup(d.jmx, d.name);
          threadGroups.forEach(tg => {
            tg.options = {};
            this.scenarios.push(tg);
          });
          let file = new File([d.jmx], d.name);
          this.uploadList.push(file);
          this.tableData.push({
            name: file.name,
            size: (file.size / 1024).toFixed(2) + ' KB',
            type: 'JMX',
            updateTime: file.lastModified,
          });
        });

        this.$emit('fileChange', this.scenarios);
        this.$success(this.$t('test_track.case.import.success'));
        this.loadFileVisible = false;
        this.selectIds.clear();
      });

    },
    async beforeUploadFile(file) {
      if (!this.fileValidator(file)) {
        /// todo: 显示错误信息
        return false;
      }

      if (this.tableData.filter(f => f.name === file.name).length > 0) {
        this.$error(this.$t('load_test.delete_file'));
        return false;
      }
      let valid = false;

      // 检查数据库是否存在同名文件
      async function f() {
        return await axios.post('/performance/file/' + getCurrentProjectID() + '/getMetadataByName', {filename: file.name})
      }

      await f().then(res => {
        let response = res.data;
        if (response.data.length === 0) {
          let type = file.name.substring(file.name.lastIndexOf(".") + 1);

          this.tableData.push({
            name: file.name,
            size: (file.size / 1024).toFixed(2) + ' KB',
            type: type.toUpperCase(),
            updateTime: file.lastModified,
          });
          valid = true;
        } else {
          this.$error(this.$t('load_test.project_file_exist'));
        }
      });


      return valid;
    },
    handleUpload(uploadResources) {
      let self = this;
      let file = uploadResources.file;
      self.uploadList.push(file);
      let type = file.name.substring(file.name.lastIndexOf(".") + 1);
      if (type.toLowerCase() !== 'jmx') {
        return;
      }
      let jmxReader = new FileReader();
      jmxReader.onload = (event) => {
        let threadGroups = findThreadGroup(event.target.result, file.name);
        threadGroups.forEach(tg => {
          tg.options = {};
          this.scenarios.push(tg);
        });
        self.$emit('fileChange', self.scenarios);
      };
      jmxReader.readAsText(file);
    },
    handleExceed() {
      this.$error(this.$t('load_test.file_size_limit'));
    },
    fileValidator(file) {
      /// todo: 是否需要对文件内容和大小做限制
      return file.size > 0;
    },
  }
}
</script>

<style scoped>

</style>
