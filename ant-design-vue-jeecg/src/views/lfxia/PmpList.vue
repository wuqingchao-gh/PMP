<template>
  <a-card :bordered="false">
    <!-- 查询区域 -->
    <!-- 操作按钮区域 -->
    <!-- <div class="table-operator">
       <a-button @click="handleAdd" icon="plus">添加任务</a-button> -->
      <!--<a-button type="primary" icon="download" @click="handleExportXls('分类字典')">导出</a-button>
      <a-upload name="file" :showUploadList="false" :multiple="false" :action="importExcelUrl" @change="handleImportExcel">
        <a-button type="primary" icon="import">导入</a-button>
      </a-upload>-->
      <!-- <a-dropdown v-if="selectedRowKeys.length > 0">
        <a-menu slot="overlay">
          <a-menu-item key="1" @click="batchDel">
            <a-icon type="delete" />删除
          </a-menu-item>
        </a-menu>
        <a-button style="margin-left: 8px">
          批量操作
          <a-icon type="down" />
        </a-button>
      </a-dropdown> </div>  -->

    <!-- table区域-begin -->
    <div>
      <div class="ant-alert ant-alert-info" style="margin-bottom: 16px;">
        <i class="anticon anticon-info-circle ant-alert-icon"></i> 已选择
        <a style="font-weight: 600">{{ selectedRowKeys.length }}</a>项
        <a style="margin-left: 24px" @click="onClearSelected">清空</a>
      </div>

      <a-table
        ref="table"
        size="middle"
        rowKey="id"
        :columns="columns"
        :dataSource="dataSource"
        :pagination="ipagination"
        :loading="loading"
        :expandedRowKeys="expandedRowKeys"
        @change="handleTableChange"
        @expand="handleExpand"
        v-bind="tableProps"
      >
        <template slot="projectname" slot-scope="text,record">
          <a href="javascript:;" @click="handleEditTaskDetail(record)">{{text}}</a>
        </template>
        <span slot="isdelete" slot-scope="text">
          <a-tag :color="text==1 ? 'volcano' : 'green'">{{ text == 0 ? '正常':'禁用'}}</a-tag>
        </span>
        <span slot="action" slot-scope="text, record">
          <a @click="handleEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm title="确定删除吗?" @confirm="() => handleDelete(record.id)">
            <a>删除</a>
          </a-popconfirm>
        </span>
      </a-table>
    </div>

    <pmpTaskList-modal ref="modalForm" @ok="modalFormOk"></pmpTaskList-modal>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'
import { JeecgListMixin } from '@/mixins/JeecgListMixin'
import JDate from '@/components/jeecg/JDate.vue'
//src\views\jgzhu\modules\PmpTaskListModal.vue
//import PmpTaskListModal from '@/jgzhu/modules/PmpTaskListModal'
import PmpTaskListModal from '@views/jgzhu/project/modules/PmpTaskListModal'
import { initDictOptions, filterMultiDictText } from '@/components/dict/JDictSelectUtil'

export default {
  name: 'SysCategoryList',
  mixins: [JeecgListMixin],
  components: {
    JDate,
    PmpTaskListModal
  },
  data() {
    return {
      description: '任务编码页面',
      // 表头
      columns: [
        {
          title: '名称',
          align: 'left',
          dataIndex: 'taskname'
        
        },
        {
          title: '任务编码',
          align: 'center',
          dataIndex: 'projectcode'
        },    
        {
          title: '是否禁用',
          align: 'center',
          dataIndex: 'isdelete',
          scopedSlots: { customRender: 'isdelete' }
        }
      ],
      url: {
        list: '/protree/pmpProjectXLF/rootList',
        childList: '/protree/pmpProjectXLF/childList',
        delete: '/protree/pmpProject/delete',
        deleteBatch: '/protree/pmpProject/deleteBatch',
        exportXlsUrl: '/protree/pmpProject/exportXls',
        importExcelUrl: 'protree/pmpProject/importExcel'
      },
      expandedRowKeys: [],
      hasChildrenField: 'haschild',
      pidField: 'parentnode',
      dictOptions: {}
    }
  },
  computed: {
    importExcelUrl() {
      return `${window._CONFIG['domianURL']}/${this.url.importExcelUrl}`
    },
    tableProps() {
      let _this = this
      return {
        // 列表项是否可选择
        rowSelection: {
          selectedRowKeys: _this.selectedRowKeys,
          onChange: selectedRowKeys => (_this.selectedRowKeys = selectedRowKeys)
        }
      }
    }
  },
  methods: {
    loadData(arg) {
      if (arg == 1) {
        this.ipagination.current = 1
      }
      this.loading = true
      this.expandedRowKeys = []
      let params = this.getQueryParams()
      return new Promise(resolve => {
        getAction(this.url.list, params).then(res => {
          if (res.success) {
            let result = res.result
            if (Number(result.total) > 0) {
              this.ipagination.total = Number(result.total)
              this.dataSource = this.getDataByResult(res.result.records)
              resolve()
            } else {
              this.ipagination.total = 0
              this.dataSource = []
            }
          } else {
            this.$message.warning(res.message)
          }
          this.loading = false
        })
      })
    },
    getDataByResult(result) {
      return result.map(item => {
        //判断是否标记了带有子节点
        if (item[this.hasChildrenField] == '1') {
          let loadChild = { id: item.id + '_loadChild', name: 'loading...', isLoading: true }
          item.children = [loadChild]
        }
        return item
      })
    },
    handleExpand(expanded, record) {
      // 判断是否是展开状态
      if (expanded) {
        this.expandedRowKeys.push(record.id)
        if (record.children.length > 0 && record.children[0].isLoading === true) {
          let params = this.getQueryParams() //查询条件
          params[this.pidField] = record.id
          getAction(this.url.childList, params).then(res => {
            if (res.success) {
              if (res.result && res.result.length > 0) {
                record.children = this.getDataByResult(res.result)
                this.dataSource = [...this.dataSource]
              } else {
                record.children = ''
                record.hasChildrenField = '0'
              }
            } else {
              this.$message.warning(res.message)
            }
          })
        }
      } else {
        let keyIndex = this.expandedRowKeys.indexOf(record.id)
        if (keyIndex >= 0) {
          this.expandedRowKeys.splice(keyIndex, 1)
        }
      }
    },
    initDictConfig() {},
    modalFormOk(formData, arr) {
      if (!formData.id) {
        this.addOk(formData, arr)
      } else {
        this.editOk(formData, this.dataSource)
        this.dataSource = [...this.dataSource]
      }
    },
    editOk(formData, arr) {
      if (arr && arr.length > 0) {
        for (let i = 0; i < arr.length; i++) {
          if (arr[i].id == formData.id) {
            arr[i] = formData
            break
          } else {
            this.editOk(formData, arr[i].children)
          }
        }
      }
    },
    async addOk(formData, arr) {
      if (!formData[this.pidField]) {
        this.loadData()
      } else {
        this.expandedRowKeys = []
        console.log('22222', arr)
        for (let i of arr) {
          await this.expandTreeNode(i)
        }
      }
    },
    expandTreeNode(nodeId) {
      return new Promise((resolve, reject) => {
        this.getFormDataById(nodeId, this.dataSource)
        let row = this.parentFormData
        this.expandedRowKeys.push(nodeId)
        let params = this.getQueryParams() //查询条件
        params[this.pidField] = nodeId
        getAction(this.url.childList, params).then(res => {
          console.log('11111', res)
          if (res.success) {
            if (res.result && res.result.length > 0) {
              row.children = this.getDataByResult(res.result)
              this.dataSource = [...this.dataSource]
              resolve()
            } else {
              reject()
            }
          } else {
            reject()
          }
        })
      })
    },
    getFormDataById(id, arr) {
      if (arr && arr.length > 0) {
        for (let i = 0; i < arr.length; i++) {
          if (arr[i].id == id) {
            this.parentFormData = arr[i]
          } else {
            this.getFormDataById(id, arr[i].children)
          }
        }
      }
    }
  }
}
</script>
<style scoped>
@import '~@assets/less/common.less';

.inputnum {
  width: 100%;
}
</style>