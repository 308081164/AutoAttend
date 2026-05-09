<template>
  <div class="biz-page">
    <div class="page-head">
      <router-link to="/quote" class="head-back">← 报价列表</router-link>
      <h1>商机看板</h1>
      <p class="desc">按阶段查看商机，可快速新建</p>
      <button type="button" class="btn primary" @click="openCreate">新建商机</button>
    </div>
    <div v-if="loading" class="placeholder">加载中…</div>
    <template v-else>
      <section class="board">
        <div v-for="row in stageGroups" :key="row.stage" class="board-col">
          <h3>{{ stageLabel(row.stage) }}</h3>
          <div class="board-meta">{{ row.cnt }} 条 · {{ fmtMoney(row.totalAmount) }}</div>
        </div>
      </section>
      <h2 class="sub-title">全部商机</h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>阶段</th>
            <th>金额</th>
            <th>客户ID</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in items" :key="o.id">
            <td>{{ o.id }}</td>
            <td>{{ o.name }}</td>
            <td>{{ stageLabel(o.stage) }}</td>
            <td>{{ o.expectedAmount }}</td>
            <td>{{ o.customerId || '—' }}</td>
            <td>
              <button type="button" class="linkish" @click="editStage(o)">改阶段</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!items.length" class="muted">暂无商机</p>
    </template>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3>{{ modalTitle }}</h3>
        <div class="form-grid">
          <label v-if="!editingId">商机名称 <input v-model="form.name" class="inp" /></label>
          <label>阶段
            <select v-model="form.stage" class="inp">
              <option value="discovery">发现</option>
              <option value="qualification">确认需求</option>
              <option value="proposal">方案/报价</option>
              <option value="negotiation">谈判</option>
              <option value="closed_won">赢单</option>
              <option value="closed_lost">输单</option>
            </select>
          </label>
          <label v-if="!editingId">预计金额 <input v-model.number="form.expectedAmount" type="number" class="inp" /></label>
          <label v-if="!editingId">关联客户（可选）
            <select v-model="form.customerId" class="inp">
              <option value="">无</option>
              <option v-for="c in customers" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
            </select>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="showModal = false">取消</button>
          <button type="button" class="btn primary" :disabled="saving" @click="submitModal">{{ saving ? '提交中…' : '确定' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const STAGE_LABELS = {
  discovery: '发现',
  qualification: '确认需求',
  proposal: '方案/报价',
  negotiation: '谈判',
  closed_won: '赢单',
  closed_lost: '输单'
}

export default {
  name: 'BizOpportunitiesView',
  data () {
    return {
      loading: true,
      stageGroups: [],
      items: [],
      customers: [],
      showModal: false,
      editingId: null,
      saving: false,
      form: {
        name: '',
        stage: 'discovery',
        expectedAmount: 0,
        customerId: ''
      }
    }
  },
  computed: {
    modalTitle () {
      return this.editingId ? '修改阶段' : '新建商机'
    }
  },
  mounted () {
    this.load()
    this.loadCustomers()
  },
  methods: {
    stageLabel (s) {
      return STAGE_LABELS[s] || s || '—'
    },
    fmtMoney (n) {
      const v = Number(n)
      if (!Number.isFinite(v)) return '—'
      return '¥' + v.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
    },
    async load () {
      this.loading = true
      try {
        const [b, l] = await Promise.all([
          this.$http.get('/admin/biz/opportunities/board'),
          this.$http.get('/admin/biz/opportunities', { params: { page: 1, pageSize: 200 } })
        ])
        if (b.data && b.data.code === 0 && b.data.data) {
          this.stageGroups = b.data.data.stageGroups || []
        }
        if (l.data && l.data.code === 0 && l.data.data) {
          this.items = l.data.data.items || []
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async loadCustomers () {
      try {
        const resp = await this.$http.get('/admin/biz/customers', { params: { page: 1, pageSize: 500 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.customers = resp.data.data.items || []
        }
      } catch (e) {
        console.error(e)
      }
    },
    openCreate () {
      this.editingId = null
      this.form = {
        name: '',
        stage: 'discovery',
        expectedAmount: 0,
        customerId: ''
      }
      this.showModal = true
    },
    editStage (o) {
      this.editingId = o.id
      this.form = {
        name: o.name,
        stage: o.stage,
        expectedAmount: o.expectedAmount,
        customerId: o.customerId != null ? String(o.customerId) : ''
      }
      this.showModal = true
    },
    async submitModal () {
      this.saving = true
      try {
        if (this.editingId) {
          const o = this.items.find(x => x.id === this.editingId)
          if (!o) return
          await this.$http.put('/admin/biz/opportunities/' + this.editingId, {
            ...o,
            stage: this.form.stage
          })
        } else {
          if (!this.form.name.trim()) return
          const body = {
            name: this.form.name.trim(),
            stage: this.form.stage,
            expectedAmount: this.form.expectedAmount || 0,
            winProbability: 10
          }
          if (this.form.customerId) {
            body.customerId = Number(this.form.customerId)
          }
          await this.$http.post('/admin/biz/opportunities', body)
        }
        this.showModal = false
        this.editingId = null
        await this.load()
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.biz-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}
.head-back {
  font-size: 14px;
  color: #2563eb;
  text-decoration: none;
}
.desc {
  color: #666;
}
.btn.primary {
  margin-top: 8px;
  padding: 8px 16px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.board {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 20px 0;
}
.board-col {
  flex: 1;
  min-width: 140px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.board-col h3 {
  margin: 0 0 4px;
  font-size: 14px;
}
.board-meta {
  font-size: 12px;
  color: #666;
}
.sub-title {
  font-size: 1rem;
  margin: 20px 0 8px;
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
.linkish {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
  font-size: 14px;
}
.muted {
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
  max-width: 400px;
}
.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
  font-size: 13px;
}
.inp {
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.btn.secondary {
  padding: 8px 14px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
}
.placeholder {
  padding: 40px;
  text-align: center;
}
</style>
