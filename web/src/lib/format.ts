const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

export function formatCost(value: number | null): string | null {
  return value === null ? null : rupees.format(value)
}
