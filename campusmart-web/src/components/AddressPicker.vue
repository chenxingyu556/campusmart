<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'list' ? '选择收货地址' : (editingId ? '编辑地址' : '新增地址')"
    width="560px"
    class="addr-dialog"
    @update:model-value="close"
  >
    <!-- ---------- 列表视图 ---------- -->
    <div v-if="mode === 'list'" v-loading="loading" class="list-wrap">
      <el-empty v-if="!loading && list.length === 0" description="还没有收货地址" :image-size="80" />

      <div
        v-for="a in list"
        :key="a.id"
        class="addr-card"
        :class="{ active: a.id === selectedId }"
        @click="pick(a)"
      >
        <div class="addr-main">
          <div class="addr-line1">
            <span class="name">{{ a.receiverName }}</span>
            <span class="phone">{{ a.receiverPhone }}</span>
            <el-tag v-if="a.isDefault" size="small" type="danger" effect="plain">默认</el-tag>
          </div>
          <div class="addr-line2">
            {{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}
          </div>
        </div>
        <div class="addr-ops" @click.stop>
          <el-button v-if="!a.isDefault" link size="small" @click="makeDefault(a)">设为默认</el-button>
          <el-button link size="small" @click="openEdit(a)">编辑</el-button>
          <el-button link size="small" type="danger" @click="remove(a)">删除</el-button>
        </div>
      </div>

      <el-button class="add-btn" plain @click="openCreate">+ 新增收货地址</el-button>
    </div>

    <!-- ---------- 表单视图 ---------- -->
    <div v-else>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="如：广东省" maxlength="20" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="如：广州市" maxlength="20" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="form.district" placeholder="如：天河区" maxlength="20" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="2"
            placeholder="街道、门牌号、楼栋等"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button @click="backToList">返回列表</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAddressList,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '@/api/address'

const props = defineProps({
  visible: { type: Boolean, default: false },
  selectedId: { type: Number, default: null }
})
const emit = defineEmits(['update:visible', 'select'])

const mode = ref('list')
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const editingId = ref(null)
const formRef = ref()

const emptyForm = () => ({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})
const form = reactive(emptyForm())

const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    list.value = await getAddressList()
  } finally {
    loading.value = false
  }
}

const close = (v) => {
  if (!v) {
    mode.value = 'list'
    emit('update:visible', false)
  }
}

const pick = (a) => {
  emit('select', a)
  emit('update:visible', false)
}

const openCreate = () => {
  editingId.value = null
  Object.assign(form, emptyForm())
  // 第一条地址默认设为默认地址
  form.isDefault = list.value.length === 0
  mode.value = 'form'
}

const openEdit = (a) => {
  editingId.value = a.id
  Object.assign(form, {
    receiverName: a.receiverName,
    receiverPhone: a.receiverPhone,
    province: a.province,
    city: a.city,
    district: a.district,
    detail: a.detail,
    isDefault: !!a.isDefault
  })
  mode.value = 'form'
}

const backToList = () => {
  mode.value = 'list'
}

const save = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateAddress(editingId.value, payload)
      ElMessage.success('已保存')
    } else {
      await addAddress(payload)
      ElMessage.success('已添加')
    }
    mode.value = 'list'
    await load()
  } finally {
    saving.value = false
  }
}

const makeDefault = async (a) => {
  await setDefaultAddress(a.id)
  ElMessage.success('已设为默认')
  await load()
}

const remove = async (a) => {
  await ElMessageBox.confirm(
    `确定删除「${a.receiverName} ${a.province}${a.city}${a.district}」这条地址吗？`,
    '删除地址',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteAddress(a.id)
  ElMessage.success('已删除')
  await load()
}

// 每次打开时刷新列表
const open = () => load()
defineExpose({ open })
</script>

<style scoped>
.list-wrap {
  max-height: 420px;
  overflow-y: auto;
}
.addr-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.addr-card:hover {
  border-color: #f5b7b3;
  background: #fffafa;
}
.addr-card.active {
  border-color: #e1251b;
  background: #fff5f4;
}
.addr-main {
  flex: 1;
  min-width: 0;
}
.addr-line1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}
.name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.phone {
  font-size: 13px;
  color: #606266;
}
.addr-line2 {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  word-break: break-all;
}
.addr-ops {
  flex: none;
  display: flex;
  align-items: center;
}
.addr-ops :deep(.el-button) {
  margin-left: 6px;
}
.add-btn {
  width: 100%;
  height: 40px;
  border-style: dashed;
}
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>
