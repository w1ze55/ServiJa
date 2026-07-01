import { sectionsFor, contractMeta, type PolSection } from '../data/politicasTermos';

export type PolTab = 'privacidade' | 'termos';

type HtmlToPdfmake = (html: string, options: Record<string, unknown>) => unknown;

interface BuildOptions {
  tab: PolTab;
  htmlToPdfmake: HtmlToPdfmake;
  window: unknown;
  logoDataUrl?: string;
}

const COLORS = {
  primary: '#286ad8',
  primaryDark: '#143774',
  ink: '#0f172a',
  text: '#374151',
  muted: '#64748b',
  border: '#e2e8f0',
  fillSoft: '#f5f8fe',
  fillHeader: '#eef3fb',
  fillZebra: '#fafbfc',
};

const ROMAN: Array<[number, string]> = [
  [10, 'X'], [9, 'IX'], [5, 'V'], [4, 'IV'], [1, 'I'],
];

function toRoman(n: number): string {
  let out = '';
  for (const [v, s] of ROMAN) {
    while (n >= v) { out += s; n -= v; }
  }
  return out;
}

/** Remove ícones SVG e normaliza os callouts em uma "caixa de nota". */
function preprocessHtml(html: string): string {
  return html
    .replace(/<svg[\s\S]*?<\/svg>/gi, '')
    .replace(
      /<div class="pol-callout">[\s\S]*?<div class="pol-callout-title">([\s\S]*?)<\/div>\s*<div class="pol-callout-text">([\s\S]*?)<\/div>[\s\S]*?<\/div>\s*<\/div>/gi,
      (_m, title: string, text: string) =>
        `<table class="callout"><tbody><tr><td><strong>${title.trim()}.</strong> ${text.trim()}</td></tr></tbody></table>`
    );
}

/* eslint-disable @typescript-eslint/no-explicit-any */
function walk(node: any, fn: (n: any) => void): void {
  if (Array.isArray(node)) {
    node.forEach((n) => walk(n, fn));
    return;
  }
  if (node && typeof node === 'object') {
    fn(node);
    for (const k of Object.keys(node)) walk(node[k], fn);
  }
}

const dataTableLayout = {
  fillColor: (rowIndex: number) =>
    rowIndex === 0 ? COLORS.fillHeader : rowIndex % 2 === 0 ? COLORS.fillZebra : null,
  hLineWidth: () => 0.5,
  vLineWidth: () => 0,
  hLineColor: () => COLORS.border,
  paddingTop: () => 5,
  paddingBottom: () => 5,
  paddingLeft: () => 8,
  paddingRight: () => 8,
};

const calloutLayout = {
  fillColor: () => COLORS.fillSoft,
  hLineWidth: () => 0,
  vLineWidth: (i: number) => (i === 0 ? 3 : 0),
  vLineColor: () => COLORS.primary,
  paddingTop: () => 8,
  paddingBottom: () => 8,
  paddingLeft: () => 12,
  paddingRight: () => 12,
};

/** Aplica larguras e layout às tabelas geradas pelo html-to-pdfmake. */
function decorateTables(content: any): void {
  walk(content, (n) => {
    if (!n || !n.table || !Array.isArray(n.table.body)) return;
    const rows = n.table.body.length;
    const cols = n.table.body[0]?.length ?? 1;
    const isCallout = rows === 1 && cols === 1;
    if (isCallout) {
      n.table.widths = ['*'];
      n.layout = calloutLayout;
      n.margin = [0, 6, 0, 12];
    } else {
      n.table.widths = cols === 2 ? ['34%', '*'] : Array(cols).fill('*');
      n.table.headerRows = 1;
      n.layout = dataTableLayout;
      n.margin = [0, 4, 0, 12];
    }
  });
}

const defaultStyles = {
  p: { margin: [0, 0, 0, 7], lineHeight: 1.35, color: COLORS.text, fontSize: 10.5 },
  a: { color: COLORS.primary, decoration: 'underline' },
  li: { margin: [0, 0, 0, 4], lineHeight: 1.3, color: COLORS.text, fontSize: 10.5 },
  ul: { margin: [0, 2, 0, 8] },
  ol: { margin: [0, 2, 0, 8] },
  strong: { bold: true, color: COLORS.ink },
  b: { bold: true, color: COLORS.ink },
  h3: { bold: true, fontSize: 11.5, color: COLORS.ink, margin: [0, 8, 0, 5] },
  th: { bold: true, fontSize: 8.5, color: COLORS.muted },
  td: { fontSize: 10, color: COLORS.text, lineHeight: 1.3 },
};

function partiesParagraphs(tab: PolTab): any[] {
  const c = contractMeta.company;
  if (tab === 'termos') {
    return [
      { text: 'DAS PARTES', bold: true, color: COLORS.primaryDark, fontSize: 10, margin: [0, 0, 0, 6] },
      {
        text: [
          { text: 'CONTRATADA: ', bold: true, color: COLORS.ink },
          `${c.name}, inscrita no CNPJ sob o nº ${c.cnpj}, com sede em ${c.city}, doravante denominada "Plataforma" ou "Servi Já".`,
        ],
        margin: [0, 0, 0, 6],
      },
      {
        text: [
          { text: 'CONTRATANTE / USUÁRIO: ', bold: true, color: COLORS.ink },
          'a pessoa física ou jurídica que realiza cadastro e adere a este instrumento ao utilizar a plataforma, na condição de contratante ou de prestador de serviços, doravante denominada "Usuário".',
        ],
      },
    ];
  }
  return [
    { text: 'DAS PARTES', bold: true, color: COLORS.primaryDark, fontSize: 10, margin: [0, 0, 0, 6] },
    {
      text: [
        { text: 'CONTROLADOR: ', bold: true, color: COLORS.ink },
        `${c.name}, inscrita no CNPJ sob o nº ${c.cnpj}, com sede em ${c.city}, responsável pelas decisões referentes ao tratamento dos dados pessoais.`,
      ],
      margin: [0, 0, 0, 6],
    },
    {
      text: [
        { text: 'TITULAR: ', bold: true, color: COLORS.ink },
        'o usuário, pessoa natural, a quem se referem os dados pessoais tratados pela plataforma.',
      ],
    },
  ];
}

function signatureBlock(tab: PolTab): any[] {
  const left =
    tab === 'termos'
      ? [`${contractMeta.company.name}`, 'CONTRATADA']
      : [`${contractMeta.company.name}`, 'Controlador dos Dados'];
  const right =
    tab === 'termos'
      ? ['Usuário', 'CONTRATANTE / USUÁRIO']
      : ['Titular dos Dados', 'Titular'];

  const sigCell = (lines: string[]) => ({
    stack: [
      { text: '\n\n', fontSize: 8 },
      { canvas: [{ type: 'line', x1: 0, y1: 0, x2: 200, y2: 0, lineWidth: 0.7, lineColor: '#94a3b8' }] },
      { text: lines[0], bold: true, color: COLORS.ink, fontSize: 10, margin: [0, 6, 0, 1] },
      { text: lines[1], color: COLORS.muted, fontSize: 8.5 },
    ],
  });

  return [
    { text: '', margin: [0, 16, 0, 0] },
    {
      text: 'E, por estarem assim de acordo, as partes reconhecem como válido e eficaz o presente instrumento, cujo aceite eletrônico, no momento do cadastro ou do uso da plataforma, equivale à assinatura para todos os fins de direito.',
      color: COLORS.text,
      fontSize: 10,
      lineHeight: 1.35,
      margin: [0, 0, 0, 18],
    },
    {
      text: `${contractMeta.company.city}, _____ de _________________ de __________.`,
      color: COLORS.text,
      fontSize: 10,
      margin: [0, 0, 0, 28],
    },
    {
      columns: [sigCell(left), { width: 24, text: '' }, sigCell(right)],
    },
  ];
}

export function buildContractDocDefinition(opts: BuildOptions): any {
  const { tab, htmlToPdfmake, window, logoDataUrl } = opts;
  const sections = sectionsFor(tab);
  const docTitle = tab === 'privacidade' ? 'POLÍTICA DE PRIVACIDADE' : 'TERMOS DE USO';
  const subtitle =
    tab === 'privacidade'
      ? 'Tratamento de dados pessoais na plataforma Servi Já — em conformidade com a LGPD (Lei nº 13.709/2018)'
      : 'Instrumento de adesão da plataforma de intermediação de serviços Servi Já';

  const convertSection = (s: PolSection) => {
    const html = preprocessHtml(s.body);
    const content = htmlToPdfmake(html, {
      window,
      defaultStyles,
      tableAutoSize: true,
      removeExtraBlanks: true,
    });
    decorateTables(content);
    return content;
  };

  const clauses: any[] = [];
  sections.forEach((s, i) => {
    clauses.push({
      text: `CLÁUSULA ${toRoman(i + 1)} — ${s.title.toUpperCase()}`,
      style: 'clause',
      tocItem: true,
      tocStyle: { fontSize: 10, color: COLORS.text },
      tocNumberStyle: { fontSize: 10, color: COLORS.primary, bold: true },
      tocMargin: [0, 2, 0, 0],
    });
    clauses.push(convertSection(s));
  });

  const cover: any[] = [];
  if (logoDataUrl) {
    cover.push({ image: logoDataUrl, width: 52, alignment: 'center', margin: [0, 60, 0, 14] });
  } else {
    cover.push({ text: '', margin: [0, 80, 0, 0] });
  }
  cover.push(
    { text: contractMeta.company.shortName.toUpperCase(), alignment: 'center', color: COLORS.primary, bold: true, fontSize: 13, characterSpacing: 2, margin: [0, 0, 0, 2] },
    { text: docTitle, alignment: 'center', color: COLORS.ink, bold: true, fontSize: 26, margin: [0, 28, 0, 8] },
    { text: subtitle, alignment: 'center', color: COLORS.muted, fontSize: 11, lineHeight: 1.4, margin: [60, 0, 60, 34] },
    {
      table: { widths: ['*'], body: [[{ stack: partiesParagraphs(tab), margin: [4, 2, 4, 2] }]] },
      layout: calloutLayout,
      margin: [0, 0, 0, 22],
    },
    {
      columns: [
        { text: [{ text: 'Versão\n', color: COLORS.muted, fontSize: 8 }, { text: contractMeta.version, bold: true, color: COLORS.ink, fontSize: 11 }], alignment: 'center' },
        { text: [{ text: 'Última atualização\n', color: COLORS.muted, fontSize: 8 }, { text: contractMeta.updatedAt, bold: true, color: COLORS.ink, fontSize: 11 }], alignment: 'center' },
        { text: [{ text: 'Conformidade\n', color: COLORS.muted, fontSize: 8 }, { text: 'LGPD', bold: true, color: COLORS.ink, fontSize: 11 }], alignment: 'center' },
      ],
      margin: [0, 0, 0, 0],
    },
    { text: '', pageBreak: 'after' }
  );

  return {
    pageSize: 'A4',
    pageMargins: [56, 54, 56, 64],
    info: {
      title: `${docTitle} — ${contractMeta.company.shortName}`,
      author: contractMeta.company.name,
      subject: subtitle,
      keywords: 'Servi Já, LGPD, termos, privacidade, contrato',
    },
    defaultStyle: { font: 'Roboto', fontSize: 10.5, color: COLORS.text, lineHeight: 1.35 },
    styles: {
      clause: { fontSize: 13, bold: true, color: COLORS.primaryDark, margin: [0, 16, 0, 8] },
      tocTitle: { fontSize: 18, bold: true, color: COLORS.ink, margin: [0, 0, 0, 14] },
    },
    footer: (currentPage: number, pageCount: number) => {
      if (currentPage === 1) return null;
      return {
        margin: [56, 12, 56, 0],
        columns: [
          { text: `${contractMeta.company.shortName} · ${docTitle} · v${contractMeta.version}`, color: COLORS.muted, fontSize: 7.5 },
          { text: `Página ${currentPage} de ${pageCount}`, color: COLORS.muted, fontSize: 7.5, alignment: 'right' },
        ],
      };
    },
    content: [
      ...cover,
      { toc: { title: { text: 'Índice', style: 'tocTitle' } } },
      { text: '', pageBreak: 'after' },
      ...clauses,
      ...signatureBlock(tab),
    ],
  };
}

export function contractFileName(tab: PolTab): string {
  return tab === 'privacidade'
    ? 'Politica-de-Privacidade-ServiJa.pdf'
    : 'Termos-de-Uso-ServiJa.pdf';
}

/** Converte uma URL de imagem em data URL para uso na capa (navegador). */
async function toDataUrl(url?: string): Promise<string | undefined> {
  if (!url) return undefined;
  try {
    const res = await fetch(url);
    const blob = await res.blob();
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  } catch {
    return undefined;
  }
}

/**
 * Gera e baixa o PDF do contrato no navegador.
 * @param logoUrl URL do logo (ex.: asset importado) para a capa.
 */
export async function downloadContractPdf(tab: PolTab, logoUrl?: string): Promise<void> {
  const [pdfMakeMod, vfsMod, htmlMod] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
    import('html-to-pdfmake'),
  ]);

  const pdfMake: any = (pdfMakeMod as any).default ?? pdfMakeMod;
  const vfs: any = (vfsMod as any).default ?? vfsMod;
  // pdfmake 0.2.x: o vfs_fonts pode expor o objeto direto ou sob .pdfMake.vfs
  pdfMake.vfs = vfs.pdfMake?.vfs ?? vfs.vfs ?? vfs;
  const htmlToPdfmake: HtmlToPdfmake = (htmlMod as any).default ?? htmlMod;

  const logoDataUrl = await toDataUrl(logoUrl);
  const docDefinition = buildContractDocDefinition({
    tab,
    htmlToPdfmake,
    window,
    logoDataUrl,
  });

  pdfMake.createPdf(docDefinition).download(contractFileName(tab));
}
