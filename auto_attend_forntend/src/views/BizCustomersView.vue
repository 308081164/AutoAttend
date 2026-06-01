<template>
  <div class="biz-page">
    <div class="page-head">
      <router-link to="/quote" class="head-back">← 报价列表</router-link>
      <h1>客户管理</h1>
      <p class="desc">维护客户信息与跟进入口</p>
      <div class="head-row">
        <input v-model="keyword" class="inp" placeholder="搜索姓名/公司/手机/邮箱" @keyup.enter="load" />
        <button type="button" class="btn primary" @click="load">搜索</button>
        <button type="button" class="btn secondary" @click="openCreate">新建客户</button>
      </div>
    </div>
    <div v-if="loading" class="placeholder">加载中…</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>姓名</th>
          <th>公司</th>
          <th>手机</th>
          <th>阶段</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in items" :key="c.id">
          <td>{{ c.id }}</td>
          <td>{{ c.name }}</td>
          <td>{{ c.company || '—' }}</td>
          <td>{{ c.phone || '—' }}</td>
          <td>{{ c.stage }}</td>
          <td>
            <router-link :to="'/quote/customers/' + c.id">详情</router-link>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && !items.length" class="placeholder">暂无客户</p>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3>新建客户</h3>
        <div class="form-grid">
          <label>姓名 <input v-model="form.name" class="inp" required /></label>
          <label>公司 <input v-model="form.company" class="inp" /></label>
          <label>手机 <input v-model="form.phone" class="inp" /></label>
          <label>邮箱 <input v-model="form.email" class="inp" /></label>
          <label>来源
            <select v-model="form.source" class="inp">
              <option value="manual">手工录入</option>
              <option value="referral">转介绍</option>
              <option value="marketplace">市场</option>
              <option value="other">其他</option>
            </select>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="showModal = false">取消</button>
          <button type="button" class="btn primary" :disabled="saving" @click="saveCreate">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BizCustomersView',
  data () {
    return {
      loading: true,
      items: [],
      keyword: '',
      showModal: false,
      saving: false,
      form: {
        name: '',
        company: '',
        phone: '',
        email: '',
        source: 'manual'
      }
    }
  },
  mounted () {
    this.load()
  },
  methods: {
    openCreate () {
      this.form = { name: '', company: '', phone: '', email: '', source: 'manual' }
      this.showModal = true
    },
    async load () {
      this.loading = true
      try {
        const params = { page: 1, pageSize: 100 }
        if (this.keyword.trim()) params.keyword = this.keyword.trim()
        const resp = await this.$http.get('/admin/biz/customers', { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.items = resp.data.data.items || []
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async saveCreate () {
      if (!this.form.name.trim()) {
        window.alert('请填写客户姓名')
        return
      }
      this.saving = true
      try {
        const resp = await this.$http.post('/admin/biz/customers', this.form)
        const payload = resp.data
        if (payload && payload.code === 0) {
          this.showModal = false
          await this.load()
          const newId = payload.data && payload.data.id
          if (newId) {
            this.$router.push('/quote/customers/' + newId)
          }
          return
        }
        window.alert((payload && payload.message) ? payload.message : '保存失败，请稍后重试')
      } catch (e) {
        console.error(e)
        const msg = e.response && e.response.data && e.response.data.message
        window.alert(msg || '保存失败，请检查网络或稍后重试')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.biz-page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-lg, 24px);
}
.page-head h1 {
  margin: var(--space-sm, 8px) 0;
}
.desc {
  color: #666;
  margin-bottom: 12px;
}
.head-back {
  font-size: 14px;
  color: #2563eb;
  text-decoration: none;
}
.head-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.inp {
  flex: 1;
  min-width: 200px;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.btn {
  padding: 8px 14px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}
.btn.primary {
  background: #2563eb;
  color: #fff;
}
.btn.secondary {
  background: #f3f4f6;
  color: #111;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.data-table th,
.data-table td {
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  text-align: left;
}
.placeholder {
  padding: 32px;
  text-align: center;
  color: #888;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.modal {
  background: #fff;
  padding: 20px;
  border-radius: 10px;
  width: 90%;
  max-width: 440px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 12px 0;
}
.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
