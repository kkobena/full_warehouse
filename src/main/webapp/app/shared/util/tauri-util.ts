import { open } from '@tauri-apps/plugin-shell';
import { save } from '@tauri-apps/plugin-dialog';
import { downloadDir } from '@tauri-apps/api/path';
import { writeFile } from '@tauri-apps/plugin-fs';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from './warehouse-util';

export const saveBase64ToFile = async (base64: string, fileName: string, ext: string): Promise<void> => {
  const path = await save({
    defaultPath: (await downloadDir()) + `/${fileName}_${DATE_FORMAT_DD_MM_YYYY_HH_MM_SS}.${ext}`,
    filters: [
      {
        name: ext.toUpperCase(),
        extensions: [ext],
      },
    ],
  });
  if (!path) {
    return;
  }
  const binaryString = window.atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  await writeFile(path, bytes);
  await open(path);
};

export const printPdf = async (base64: string, fileName: string): Promise<void> => {
  await saveBase64ToFile(base64, fileName, 'pdf');
};

export const printCsv = async (base64: string, fileName: string): Promise<void> => {
  await saveBase64ToFile(base64, fileName, 'csv');
};

export const printExcel = async (base64: string, fileName: string): Promise<void> => {
  await saveBase64ToFile(base64, fileName, 'xlsx');
};

export const printPdf2 = async (base64: string, fileName: string): Promise<void> => {
  const path = await save({
    defaultPath: (await downloadDir()) + `/${fileName}_${DATE_FORMAT_DD_MM_YYYY_HH_MM_SS}.pdf`,
    filters: [
      {
        name: 'Pdf',
        extensions: ['pdf'],
      },
    ],
  });
  if (path) {
    const binaryString = window.atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    await writeFile(path, bytes);
    await open(path);
  }
};
export const handleBlobForTauri = (blob: Blob, filename: string, type: string = 'pdf'): void => {
  const reader = new FileReader();
  reader.onload = () => {
    const result = reader.result;
    if (typeof result !== 'string' || !result) {
      return;
    }
    const commaIndex = result.indexOf(',');
    const base64Payload = commaIndex >= 0 ? result.slice(commaIndex + 1) : result;
    switch (type) {
      case 'pdf':
        void printPdf(base64Payload, filename);
        break;
      case 'csv':
        void printCsv(base64Payload, filename);
        break;
      case 'excel':
        void printExcel(base64Payload, filename);
        break;
      default:
        void printPdf(base64Payload, filename);
    }
  };
  reader.readAsDataURL(blob);
};
