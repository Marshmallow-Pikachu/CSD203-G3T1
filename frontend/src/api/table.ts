// src/api/user.ts
import { api } from "../api/client";

export type TariffRow = {
  id: number;
  exporter_code: string;
  exporter_name: string;
  importer_code: string;
  importer_name: string;
  importer_customs: string;
  importer_tax: string;
  agreement_code: string;
  agreement_name: string;
  hs_code: string;
  hs_description: string;
  rate_percent: number;
  valid_from: string;
  valid_to: string | null;
};

export async function fetchTariffsTable(): Promise<TariffRow[]> {
  const { data } = await api.get("/api/v1/tariffs/table");
  return data as TariffRow[];

}

export async function fetchAdminTariffs(): Promise<TariffRow[]> {
  const { data } = await api.get("/api/v1/admin/tariffs");
  return data as TariffRow[];
  
}