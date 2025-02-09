export const extractFileName = (contentDisposition: string): string => {
  const fileNameRegex = /filename="([^"]*)"/;
  const matches = fileNameRegex.exec(contentDisposition);
  return matches && matches[1] ? matches[1] : 'document';
};
