<template>
  <div class="biz-page">
    <div class="page-head">
      <router-link to="/quote/customers" class="head-back">← 客户列表</router-link>
      <h1>客户详情 #{{ customerId }}</h1>
    </div>
    <div v-if="loading" class="placeholder">加载中…</div>
    <template v-else-if="customer">
      <section class="card-block">
        <h2>基本信息</h2>
        <div class="form-grid">
          <label>姓名 <input v-model="edit.name" class="inp" /></label>
          <label>公司 <input v-model="edit.company" class="inp" /></label>
          <label>手机 <input v-model="edit.phone" class="inp" /></label>
          <label>邮箱 <input v-model="edit.email" class="inp" /></label>
          <label>阶段
            <select v-model="edit.stage" class="inp">
              <option value="lead">线索</option>
              <option value="contacted">已联系</option>
              <option value="qualified">已确认</option>
              <option value="negotiation">谈判中</option>
              <option value="closed_won">成交</option>
              <option value="closed_lost">流失</option>
            </select>
          </label>
        </div>
        <button type="button" class="btn primary" :disabled="saving" @click="saveCustomer">{{ saving ? '保存中…' : '保存修改' }}</button>
      </section>

      <section class="card-block">
        <h2>跟进记录</h2>
        <div class="follow-form">
          <select v-model="fu.method" class="inp sm">
            <option value="note">备忘</option>
            <option value="call">电话</option>
            <option value="email">邮件</option>
            <option value="meeting">会议</option>
          </select>
          <input v-model="fu.content" class="inp grow" placeholder="跟进内容" />
          <button type="button" class="btn primary" :disabled="fuSaving" @click="addFollowup">添加</button>
        </div>
        <ul class="fu-list">
          <li v-for="f in followups" :key="f.id" class="fu-item">
            <span class="fu-meta">{{ f.method }} · {{ f.createdAt }}</span>
            <div>{{ f.content }}</div>
          </li>
        </ul>
        <p v-if="!followups.length" class="muted">暂无记录</p>
      </section>

      <section class="card-block">
        <h2>关联报价项目</h2>
        <table v-if="quotes.length" class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>状态</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="q in quotes" :key="q.id">
              <td>{{ q.id }}</td>
              <td>{{ q.name }}</td>
              <td>{{ q.status }}</td>
              <td><router-link :to="'/quote/' + q.id">打开</router-link></td>
            </tr>
          </tbody>
        </table>
        <p v-else class="muted">暂无关联报价（可在报价项目「项目基础」中选择客户）</p>
      </section>
    </template>
    <p v-else class="placeholder">客户不存在</p>
  </div>
</template>

<script>
export default {
  name: 'BizCustomerDetailView',
  data () {
    return {
      loading: true,
      customer: null,
      edit: {},
      saving: false,
      followups: [],
      quotes: [],
      fu: { method: 'note', content: '' },
      fuSaving: false
    }
  },
  computed: {
    customerId () {
      return this.$route.params.id
    }
  },
  watch: {
    '$route.params.id' () {
      this.loadAll()
    }
  },
  mounted () {
    this.loadAll()
  },
  methods: {
    async loadAll () {
      this.loading = true
      try {
        const id = this.customerId
        const [c, f, q] = await Promise.all([
          this.$http.get('/admin/biz/customers/' + id),
          this.$http.get('/admin/biz/customers/' + id + '/followups', { params: { page: 1, pageSize: 50 } }),
          this.$http.get('/admin/biz/customers/' + id + '/quote-projects')
        ])
        if (c.data && c.data.code === 0) {
          this.customer = c.data.data
          const d = this.customer || {}
          this.edit = {
            name: d.name || '',
            company: d.company || '',
            phone: d.phone || '',
            email: d.email || '',
            stage: d.stage || 'lead'
          }
        } else {
          this.customer = null
        }
        if (f.data && f.data.code === 0 && f.data.data) {
          this.followups = f.data.data.items || []
        }
        if (q.data && q.data.code === 0) {
          this.quotes = q.data.data || []
        }
      } catch (e) {
        console.error(e)
        this.customer = null
      } finally {
        this.loading = false
      }
    },
    async saveCustomer () {
      this.saving = true
      try {
        await this.$http.put('/admin/biz/customers/' + this.customerId, {
          ...this.customer,
          ...this.edit
        })
        await this.loadAll()
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
      }
    },
    async addFollowup () {
      if (!this.fu.content.trim()) return
      this.fuSaving = true
      try {
        await this.$http.post('/admin/biz/customers/' + this.customerId + '/followups', {
          method: this.fu.method,
          content: this.fu.content.trim()
        })
        this.fu.content = ''
        const resp = await this.$http.get('/admin/biz/customers/' + this.customerId + '/followups', { params: { page: 1, pageSize: 50 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.followups = resp.data.data.items || []
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.fuSaving = false
      }
    }
  }
}
</script>

<style scoped>
.biz-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}
.head-back {
  font-size: 14px;
  color: #2563eb;
  text-decoration: none;
}
.card-block {
  margin-bottom: 28px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.card-block h2 {
  font-size: 1rem;
  margin: 0 0 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}
@media (max-width: 600px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.inp {
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.inp.sm {
  max-width: 120px;
}
.inp.grow {
  flex: 1;
}
.btn.primary {
  padding: 8px 16px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.follow-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.fu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.fu-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.fu-meta {
  font-size: 12px;
  color: #888;
}
.muted {
  color: #888;
  font-size: 14px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.data-table th,
.data-table td {
  border: 1px solid #e5e7eb;
  padding: 6px 8px;
}
.placeholder {
  padding: 40px;
  text-align: center;
}
</style>
