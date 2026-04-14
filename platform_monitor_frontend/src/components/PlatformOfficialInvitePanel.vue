<template>
  <section class="official-panel">
    <div class="official-head">
      <h3 class="official-title">官方邀请码</h3>
      <p class="official-desc">
        平台统一发放，与租户无关（类似激活码）。新用户注册或会员页兑换可使用；须设置总可用次数；未填有效天数则默认 30 天。
      </p>
    </div>
    <div class="official-form">
      <label class="lbl">有效天数</label>
      <input v-model.number="validDays" type="number" min="1" max="3650" class="inp" placeholder="30">
      <label class="lbl">可用次数</label>
      <input v-model.number="maxUses" type="number" min="1" max="1000000" class="inp inp-wide" placeholder="1">
      <button type="button" class="btn-gen" :disabled="loading || creating" @click="createCode">
        {{ creating ? '生成中…' : '生成邀请码' }}
      </button>
      <button type="button" class="btn-refresh" :disabled="loading" @click="loadList">刷新列表</button>
    </div>
    <p v-if="msg" :class="msgOk ? 'msg ok' : 'msg err'">{{ msg }}</p>
    <div v-if="loading" class="muted">加载中…</div>
    <div v-else-if="rows.length === 0" class="muted">暂无官方邀请码，请生成。</div>
    <div v-else class="table-wrap">
      <table class="tbl">
        <thead>
          <tr>
            <th>邀请码</th>
            <th>有效期至</th>
            <th>次数</th>
            <th>已用</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id">
            <td class="mono code-cell">
              <span class="code-text">{{ r.code }}</span>
              <button type="button" class="btn-copy" @click="copyCode(r.code)">复制</button>
            </td>
            <td class="small">{{ r.expiresAt || '—' }}</td>
            <td>{{ r.maxUses != null ? r.maxUses : '—' }}</td>
            <td>{{ r.usedCount != null ? r.usedCount : 0 }}</td>
            <td class="small">{{ r.createdAt || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'PlatformOfficialInvitePanel',
  data () {
    return {
      rows: [],
      loading: true,
      creating: false,
      validDays: 30,
      maxUses: 1,
      msg: '',
      msgOk: true
    }
  },
  mounted () {
    this.loadList()
  },
  methods: {
    async loadList () {
      this.loading = true
      this.msg = ''
      try {
        const { data } = await http.get('/platform/invite-codes', { params: { limit: 80 } })
        if (data.code === 0 && Array.isArray(data.data)) {
          this.rows = data.data
        } else {
          this.msg = (data && data.message) || '加载失败'
          this.msgOk = false
        }
      } catch (e) {
        this.msg = '请求失败'
        this.msgOk = false
      } finally {
        this.loading = false
      }
    },
    async createCode () {
      this.creating = true
      this.msg = ''
      try {
        const d = {}
        const vd = this.validDays != null && this.validDays > 0 ? this.validDays : 30
        d.validDays = vd
        const mu = this.maxUses != null && this.maxUses > 0 ? this.maxUses : 1
        d.maxUses = mu
        const { data } = await http.post('/platform/invite-codes', d)
        if (data.code === 0) {
          this.msg = '已生成官方邀请码'
          this.msgOk = true
          await this.loadList()
        } else {
          this.msg = (data && data.message) || '生成失败'
          this.msgOk = false
        }
      } catch (e) {
        this.msg = '请求失败'
        this.msgOk = false
      } finally {
        this.creating = false
      }
    },
    async copyCode (code) {
      const t = String(code || '')
      try {
        await navigator.clipboard.writeText(t)
        this.msg = '已复制到剪贴板'
        this.msgOk = true
      } catch (e) {
        this.msg = '复制失败，请手动选择复制'
        this.msgOk = false
      }
    }
  }
}
</script>

<style scoped>
.official-panel {
  margin-bottom: 20px;
  padding: 16px 18px;
  background: linear-gradient(180deg, #0f172a 0%, #0b1224 100%);
  border: 1px solid #334155;
  border-radius: 12px;
}
.official-head { margin-bottom: 12px; }
.official-title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: #f1f5f9;
}
.official-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #94a3b8;
}
.official-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.lbl { font-size: 13px; color: #94a3b8; }
.inp {
  width: 100px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #1e293b;
  color: #f8fafc;
  font-size: 14px;
}
.inp-wide { width: 120px; }
.btn-gen {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #1d4ed8;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-gen:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-refresh {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #1e293b;
  color: #e2e8f0;
  font-size: 14px;
  cursor: pointer;
}
.btn-refresh:disabled { opacity: 0.55; cursor: not-allowed; }
.msg { margin: 8px 0 0; font-size: 13px; }
.msg.ok { color: #86efac; }
.msg.err { color: #fca5a5; }
.muted { color: #64748b; font-size: 13px; margin: 8px 0 0; }
.table-wrap { overflow-x: auto; margin-top: 10px; }
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 640px;
}
.tbl th {
  text-align: left;
  padding: 8px 10px;
  color: #94a3b8;
  font-weight: 500;
  border-bottom: 1px solid #334155;
}
.tbl td {
  padding: 10px;
  border-bottom: 1px solid #1e293b;
  vertical-align: middle;
  color: #e2e8f0;
}
.code-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.code-text {
  font-family: ui-monospace, monospace;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #f8fafc;
}
.btn-copy {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #64748b;
  background: #334155;
  color: #f1f5f9;
  cursor: pointer;
}
.btn-copy:hover { background: #475569; }
.small { font-size: 12px; color: #cbd5e1; white-space: nowrap; }
.mono { font-family: ui-monospace, monospace; }
</style>
