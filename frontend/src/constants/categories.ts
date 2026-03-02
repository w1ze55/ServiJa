export interface Category {
    value: string;
    label: string;
    icon?: string;
}

export const CATEGORIES: Category[] = [
    { value: 'Advocacia', label: 'Advocacia' },
    { value: 'Arquitetura', label: 'Arquitetura' },
    { value: 'Contabilidade', label: 'Contabilidade' },
    { value: 'Gastronomia', label: 'Gastronomia' },
    { value: 'Cuidados e Assistência', label: 'Cuidados e Assistência' },
    { value: 'Design Gráfico', label: 'Design Gráfico' },
    { value: 'Desenvolvimento Web', label: 'Desenvolvimento Web' },
    { value: 'Limpeza e Serviços Domésticos', label: 'Limpeza' },
    { value: 'Eletricista', label: 'Eletricista' },
    { value: 'Encanador', label: 'Encanador' },
    { value: 'Fotografia e Vídeo', label: 'Fotografia e Vídeo' },
    { value: 'Jardinagem', label: 'Jardinagem' },
    { value: 'Marcenaria', label: 'Marcenaria' },
    { value: 'Mecânica Automotiva', label: 'Mecânica' },
    { value: 'Construção Civil', label: 'Construção Civil' },
    { value: 'Saúde e Bem-estar', label: 'Saúde e Bem-estar' },
    { value: 'Educação', label: 'Educação' },
    { value: 'Escrita e Tradução', label: 'Escrita e Tradução' },
    { value: 'Marketing Digital', label: 'Marketing Digital' },
    { value: 'Beleza e Estética', label: 'Beleza e Estética' },
    { value: 'Eventos', label: 'Eventos' },
    { value: 'Outro', label: 'Outro' },
].sort((a, b) => {
    if (a.value === 'Outro') return 1;
    if (b.value === 'Outro') return -1;
    return a.label.localeCompare(b.label, 'pt-BR');
});
