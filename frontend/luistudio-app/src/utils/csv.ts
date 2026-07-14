export type CsvValue = string | number | boolean | null | undefined

const escapeCell = (value: CsvValue) => `"${String(value ?? '').replaceAll('"', '""')}"`

export const buildExcelCompatibleCsv = (rows: CsvValue[][]) => {
  const body = rows.map((row) => row.map(escapeCell).join(',')).join('\r\n')
  return `\uFEFF${body}`
}
