/**
 * 将接口返回的 quoteNavVisible 规范为布尔值（兼容 JSON 数字、字符串等）。
 * 仅当明确为「假」时返回 false，否则为 true（与侧栏默认展示策略一致）。
 */
export function coerceQuoteNavVisible (v) {
  if (v === false || v === 0 || v === '0' || v === 'false' || v === 'FALSE' || v === 'off' || v === 'OFF') {
    return false
  }
  return true
}
