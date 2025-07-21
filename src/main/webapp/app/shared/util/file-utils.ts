export const extractFileName = (contentDisposition: string): string => {
  const fileNameRegex = /filename="([^"]*)"/;
  const matches = fileNameRegex.exec(contentDisposition);
  return matches && matches[1] ? matches[1] : 'document';
};
export const extractFileName2 = (contentDisposition: string, format: string, defaulName?: string): string => {
  const fileNameMatch = contentDisposition.match(/filename="?([^";]*)"?/);
  return fileNameMatch ? decodeURIComponent(fileNameMatch[1]) : `${defaulName}.${format === 'CSV' ? 'csv' : 'xlsx'}`;
};
