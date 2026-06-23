import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { readFileSync, mkdirSync, createWriteStream } from 'node:fs';
import { JSDOM } from 'jsdom';
import {
  buildContractDocDefinition,
  contractFileName,
  type PolTab,
} from '../src/utils/contractPdf.ts';

const require = createRequire(import.meta.url);
const PdfPrinter = require('pdfmake/src/printer');
const vfs = require('pdfmake/build/vfs_fonts');
const htmlToPdfmake = require('html-to-pdfmake');

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..');

const fontTable = vfs.pdfMake?.vfs ?? vfs.vfs ?? vfs;
const fontBuf = (name: string) => Buffer.from(fontTable[name], 'base64');
const printer = new PdfPrinter({
  Roboto: {
    normal: fontBuf('Roboto-Regular.ttf'),
    bold: fontBuf('Roboto-Medium.ttf'),
    italics: fontBuf('Roboto-Italic.ttf'),
    bolditalics: fontBuf('Roboto-MediumItalic.ttf'),
  },
});

const { window } = new JSDOM('');
const logoPath = resolve(root, 'src/assets/favicon-32x32.png');
const logoDataUrl = 'data:image/png;base64,' + readFileSync(logoPath).toString('base64');

const outDir = resolve(root, 'public/legal');
mkdirSync(outDir, { recursive: true });

async function main() {
  for (const tab of ['privacidade', 'termos'] as PolTab[]) {
    const docDefinition = buildContractDocDefinition({
      tab,
      htmlToPdfmake,
      window,
      logoDataUrl,
    });
    const pdfDoc = printer.createPdfKitDocument(docDefinition);
    const outPath = resolve(outDir, contractFileName(tab));
    const stream = createWriteStream(outPath);
    pdfDoc.pipe(stream);
    pdfDoc.end();
    await new Promise<void>((res, rej) => {
      stream.on('finish', () => res());
      stream.on('error', rej);
    });
    console.log('✓ gerado:', outPath);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
