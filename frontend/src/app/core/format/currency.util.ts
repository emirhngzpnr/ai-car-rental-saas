export function formatTryAmount(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0);

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'TRY',
    maximumFractionDigits: 2
  }).format(Number.isFinite(amount) ? amount : 0);
}
