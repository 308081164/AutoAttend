/** localStorage：协作「当前统计/列表」所代表的成员子账号（需与 JWT 范围一致，由后端校验） */
export const COLLAB_ACTING_USER_ID_KEY = 'autoattend_collab_acting_user_id'

export function getStoredCollabActingUserId () {
  try {
    const v = window.localStorage.getItem(COLLAB_ACTING_USER_ID_KEY)
    if (v == null || String(v).trim() === '') return null
    const n = Number(v)
    return Number.isFinite(n) ? String(Math.trunc(n)) : null
  } catch (e) {
    return null
  }
}

export function setStoredCollabActingUserId (id) {
  if (id == null || id === '') {
    window.localStorage.removeItem(COLLAB_ACTING_USER_ID_KEY)
    return
  }
  window.localStorage.setItem(COLLAB_ACTING_USER_ID_KEY, String(id))
}
