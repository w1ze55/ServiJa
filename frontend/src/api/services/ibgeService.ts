const IBGE_BASE_URL = 'https://servicodados.ibge.gov.br/api/v1/localidades';

export interface IbgeState {
  id: number;
  sigla: string;
  nome: string;
}

interface IbgeCity {
  id: number;
  nome: string;
}

const buildUrl = (path: string) => `${IBGE_BASE_URL}${path}`;

const fetchJson = async <T>(path: string): Promise<T> => {
  const response = await fetch(buildUrl(path));

  if (!response.ok) {
    throw new Error(`IBGE request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
};

export const ibgeService = {
  async listStates(): Promise<IbgeState[]> {
    return fetchJson<IbgeState[]>('/estados?orderBy=nome');
  },

  async listCitiesByState(uf: string): Promise<string[]> {
    const normalizedUf = uf.trim().toUpperCase();
    const cities = await fetchJson<IbgeCity[]>(`/estados/${normalizedUf}/municipios?orderBy=nome`);
    return cities.map((city) => city.nome);
  },
};
